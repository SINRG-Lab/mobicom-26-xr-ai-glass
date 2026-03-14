package com.sdk.glassessdksample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.oudmon.ble.base.communication.LargeDataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope

class DeepgramClient (
    private val apiKey: String,
    private val scope: CoroutineScope,
    private val listenModel: String = "nova-3",
    private val thinkModel: String? = "gpt-4o-mini",
    private val speakModel: String = "tts-1",
    private val context: Context,
    private val onPartialTranscription: (String) -> Unit,
    private val onFinalTranscription: (String) -> Unit,
    private val onImage: (String) -> Unit
) : WebSocketListener() {
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private lateinit var ttsPlayer : TtsPlayer
    private var pastRun: Int = 0
    private var pastRun1: Int = 0
    private var isConnected = false
    private var firstTime = true
    private var endTime = true
    private var isTool = false
    var startTime = System.currentTimeMillis()
    var totalBytes = 0L
    var bitrate: Double = 0.0
    private lateinit var uberAPI: UberAPI
    private lateinit var healthAPI: HealthAPI
    private lateinit var spotifyAPI: SpotifyAPI
    lateinit var googleSignInAccount: GoogleSignInAccount
    private lateinit var fullAddress: String
    private lateinit var steps: String
    private lateinit var heart_rate: String
    private lateinit var sleep: String
    private lateinit var phoneNumber: String
    private lateinit var message: String
    private var prompt = ""
    private val toolKeywords = listOf(
        "location",
        "image",
        "steps",
        "sleep",
        "heart rate",
        "phone call",
        "text message"
    )

    fun connect() {
        val url = "wss://agent.deepgram.com/v1/agent/converse"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $apiKey")
            .addHeader("Settings", "application/json")
            .build()

        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("DeepgramClient", "WebSocket connected")
        ttsPlayer = TtsPlayer(scope = scope)
        ttsPlayer.start()

        val settingsMessage = settings().toString()
        webSocket.send(settingsMessage)
        // startKeepAlive()
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        // Log.w("TTS_WS", "Closing: $code / $reason")
        isConnected = false
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        // Log.w("TTS_WS", "Closed: $code / $reason")
        isConnected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // Log.e("TTS_WS", "Error: ${t.message}")
        // webSocket.close(1001, "Reconnecting")
        // ensureConnected()
        isConnected = false
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("DeepgramClient", "text transmitted: $text")
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "output_audio_buffer.delta" -> {
                    if (LatencyManager.getRunId() != pastRun1) {
                        endTime = true
                    }

                    if (endTime) {
                        Log.d("Ossian", "Test ended at ${LatencyManager.now()}")
                        // LatencyManager.getCurrentRun()?.finishTime = LatencyManager.now()

                        endTime = false
                    }

                    val audioBase64 = json.getString("audio")
                    val audioBytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
                    ttsPlayer.offer(audioBytes)
                }
                "transcript.partial" -> {
                    val transcript = json.optString("text")
                    if (transcript.isNotEmpty()) onPartialTranscription(transcript)
                }
                "transcript.final" -> {
                    val transcript = json.optString("text")
                    if (transcript.isNotEmpty()) onFinalTranscription(transcript)
                }
                "SettingsApplied" -> {
                    isConnected = true
                }
                "UserStartedSpeaking" -> {
                    /*
                    Log.d("Ossian", "Input transmission finished at ${LatencyManager.now()}")
                    LatencyManager.getCurrentRun()?.finishInputTransmissionTime = LatencyManager.now()
                    */

                    Log.d("Ossian", "STT started at ${LatencyManager.now()}")
                    // LatencyManager.getCurrentRun()?.startSttTime = LatencyManager.now()
                }
                "ConversationText" -> {
                    if (json.optString("role") == "user") {
                        Log.d("Ossian", "STT finished at ${LatencyManager.now()}")
                        LatencyManager.getCurrentRun()?.finishSttTime = LatencyManager.now()
                        prompt = "$prompt ${json.optString("content")}"

                        Log.d("Ossian", "LLM started at ${LatencyManager.now()}")
                        LatencyManager.getCurrentRun()?.startLlmTime = LatencyManager.now()
                    }
                    if (json.optString("role") == "assistant") {
                        val shouldSuppress = toolKeywords.any { keyword ->
                            json.optString("content").contains(keyword, ignoreCase = true)
                        }

                        if (shouldSuppress) {
                            Log.d("DeepgramClient", "Suppressed assistant response: ${json.optString("content")}")
                            return
                        }

                        Log.d("Ossian", "LLM finished at ${LatencyManager.now()}")
                        LatencyManager.getCurrentRun()?.finishLlmTime = LatencyManager.now()

                        Log.d("Ossian", "TTS started at ${LatencyManager.now()}")
                        LatencyManager.getCurrentRun()?.startTtsTime = LatencyManager.now()

                        startTime = System.currentTimeMillis()
                        Log.d("Bitrate", "start time: $startTime")
                    }
                }
                "History" -> {
                    if (json.optString("role") == "assistant") {
                        val callRegex = Regex("""Make a phone call to (\d+)""")
                        val callMatch = callRegex.find(json.optString("content"))

                        val textRegex = Regex("""Send a text message to (\d+) with the message (\d+)""")
                        val textMatch = textRegex.find(json.optString("content"))

                        GlobalScope.launch {
                            if (json.optString("content") == "image") {
                                Log.d("Ossian", "content = ${json.optString("content")}")
                                onImage(prompt)

                                return@launch
                            }
                            if (json.optString("content") == "location") {
                                json.optString("content") == ""

                                fullAddress = uberAPI.getCurrentLocationAsJson()?.getString("address").toString()
                                Log.d("Ossian", "Full Address: $fullAddress")

                                val updateSettings = JSONObject().apply {
                                    put("type", "InjectAgentMessage")
                                    put("content", "Your current location is $fullAddress")
                                }
                                webSocket.send(updateSettings.toString())

                                return@launch
                            }
                            if (json.optString("content") == "steps") {
                                json.optString("content") == ""

                                steps = getHealthData("steps")
                                Log.d("Ossian", "steps: $steps")

                                val updateSettings = JSONObject().apply {
                                    put("type", "InjectAgentMessage")
                                    put("content", steps)
                                }
                                webSocket.send(updateSettings.toString())

                                return@launch
                            }
                            if (json.optString("content") == "heart rate") {
                                json.optString("content") == ""

                                heart_rate = getHealthData("heart_rate")
                                Log.d("Ossian", "heart rate: $heart_rate")

                                val updateSettings = JSONObject().apply {
                                    put("type", "InjectAgentMessage")
                                    put("content", heart_rate)
                                }
                                webSocket.send(updateSettings.toString())

                                return@launch
                            }
                            if (json.optString("content") == "sleep") {
                                json.optString("content") == ""

                                sleep = getHealthData("sleep")
                                Log.d("Ossian", "sleep: $sleep")

                                val updateSettings = JSONObject().apply {
                                    put("type", "InjectAgentMessage")
                                    put("content", sleep)
                                }
                                webSocket.send(updateSettings.toString())

                                return@launch
                            }
                            /*
                            if (json.optString("content") == "music") {
                                songName = getSong()
                                val token = getSpotifyAccessToken()

                                val trackUri = spotifyAPI.searchTrack()
                                if (trackUri != null) {
                                    spotifyAPI.playTrack(token, trackUri)

                                    val updateSettings = JSONObject().apply {
                                        put("type", "InjectAgentMessage")
                                        put("content", "Now playing $songName on Spotify.")
                                    }
                                    webSocket.send(updateSettings.toString())

                                    isTool = true
                                    return@launch
                                } else {
                                    val updateSettings = JSONObject().apply {
                                        put("type", "InjectAgentMessage")
                                        put("content", "I couldn't find that song on Spotify.")
                                    }
                                    webSocket.send(updateSettings.toString())
                                }
                            }
                            */
                            if (callMatch != null) {
                                phoneNumber = callMatch.groupValues[1]
                                makePhoneCall(phoneNumber)

                                val updateSettings = JSONObject().apply {
                                    put("type", "InjectAgentMessage")
                                    put("content", "$phoneNumber has been called")
                                }
                                webSocket.send(updateSettings.toString())

                                return@launch
                            }
                            if (textMatch != null) {
                                phoneNumber = textMatch.groupValues[1]
                                message = textMatch.groupValues[2]
                                sendTextMessage(phoneNumber, message)

                                val updateSettings = JSONObject().apply {
                                    put("type", "InjectAgentMessage")
                                    put("content", "The message $message has been sent to $phoneNumber")
                                }
                                webSocket.send(updateSettings.toString())

                                return@launch
                            }
                        }
                    }
                }
                "Error" -> {
                    if (json.optString("description") == "We waited too long for a websocket message. Please ensure that you're sending binary messages containing user speech.") {
                        val url = "wss://agent.deepgram.com/v1/agent/converse"

                        // w->a：bc730200c0800301

                        val request = Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Token $apiKey")
                            .addHeader("Settings", "application/json")
                            .build()

                        // webSocket = client.newWebSocket(request, this)

                        // webSocket.close(1000, null)
                        // ttsPlayer.close()
                    }
                }
                "AgentAudioDone" -> {
                    val run = BitrateManager.newRun()
                    run.outputRate = bitrate.toString()
                    Log.d("BitrateTest", "run number: $run")

                    totalBytes = 0L

                    // webSocket.close(1000,null)
                    // ttsPlayer.close()
                }
                else -> {
                    Log.d("DeepgramClient", "Unhandled message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("DeepgramClient", "Failed to parse JSON: ${e.message}")
        }
    }

    override fun onMessage (webSocket: WebSocket, bytes: ByteString) {
        prompt = ""
        if (!isTool) {
            // Log.d("TTS_WS", "bytes transmitted: $bytes")
            totalBytes += bytes.size

            /*
            Log.d("BitrateTest", "Byte size: ${totalBytes}")

            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
            Log.d("BitrateTest", "Elapsed seconds: $elapsedSec s")

            bitrate = (totalBytes * 8) / elapsedSec // bits per second
            Log.d("BitrateTest", "Current bitrate: $bitrate bps")
            */

            if (LatencyManager.getRunId() != pastRun1) {
                endTime = true
            }

            if (endTime) {
                val timeFormat = SimpleDateFormat("HH.mmssSSS", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                Log.d("Ossian", "TTS transmission ended at $currentTime")
                Log.d("Ossian", "Test ended at $currentTime")

                LatencyManager.getCurrentRun()?.finishTime = LatencyManager.now()
                LatencyManager.getCurrentRun()?.finishTtsTime = LatencyManager.now()

                endTime = false
            }

            pastRun1 = LatencyManager.getRunId()

            val audioBytes = bytes.toByteArray()
            ttsPlayer.offer(audioBytes)

            // startTime = System.currentTimeMillis()
            // Log.d("Bitrate", "start time: $startTime")

            /*
            val json = JSONObject(text)
            val type = json.optString("type")

            if (type == "output_audio_buffer.delta") {
                val audioBase64 = json.getString("audio")
                val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                ttsPlayer.offer(audioBytes)
            }
            */
        }
    }

    fun sendAudioChunk(chunk: ByteArray) {
        // Log.d("WhisperWS", "Audio: ${ByteString.of(*chunk)}")
        webSocket.send(ByteString.of(*chunk))
    }

    fun sendTextChunk(chunk: String) {
        val message = JSONObject().apply {
            put("type", "Speak")
            put("text", chunk)
        }
        Log.d("TTS_WS", "Sent text chunk: $chunk")

        if (LatencyManager.getRunId() != pastRun) {
            firstTime = true
        }

        if (firstTime) {
            val timeFormat = SimpleDateFormat("HH.mmssSSS", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            Log.d("Ossian", "TTS transmission started at $currentTime")

            // LatencyManager.getCurrentRun()?.startTtsTime = LatencyManager.now()

            firstTime = false
        }

        pastRun = LatencyManager.getRunId()

        webSocket.send(message.toString())
    }

    fun sendControlCommand(bytes: ByteArray) {
        LargeDataHandler.getInstance().glassesControl(bytes) { _, resp ->
            // Log.d("Glasses", "Command sent, status=${resp.errorCode}")
        }
    }

    fun settings(): JSONObject {
        val settings = JSONObject().apply {
            put("type", "Settings")
            put("audio", JSONObject().apply {
                put("input", JSONObject().apply {
                    put("encoding", "linear16")
                    put("sample_rate", 16000)
                })
                put("output", JSONObject().apply {
                    put("encoding", "linear16")
                    put("sample_rate", 24000)
                })
            })
            put("agent", JSONObject().apply {
                put("listen", JSONObject().apply {
                    put("provider", JSONObject().apply {
                        put("type", "deepgram")
                        put("model", listenModel)
                    })
                })
                put("think", JSONObject().apply {
                    put("provider", JSONObject().apply {
                        if (thinkModel == "gpt-4o-mini") {
                            put("type", "open_ai")
                            put("model", thinkModel)
                        }
                        if (thinkModel == "gemini-2.5-flash") {
                            put("type", "google")
                            put("model", thinkModel)
                        }
                        if (thinkModel == "claude-sonnet-4-20250514") {
                            put("type", "anthropic")
                            put("model", thinkModel)
                        }
                    })
                    put("prompt", "You are a helpful AI assistant embedded within smart glasses. Keep your responses concise and fit to be read out loud. Note that you may be injected with messages if the user has to access information that you do not have. " +
                                                  "If this is the case, just send the name of the information you don't have as the content in the assistant role under the type History while making the content in the assistant role under the type ConversationText just empty text, rather than putting the prompt through the LLM. " +
                                                  "Currently, send 'location' if the user is requesting location data, " +
                                                  "'image' if the user is requesting information about an image (like asking about something in front of them or their surroundings, for instance)" +
                                                  "'steps' if the user is requesting data about their steps," +
                                                  "'sleep' if the user is requesting data about their sleep, " +
                                                  "'heart rate' if the user is requesting data about their heart rate, " +
                                                  "'phone call' along with the phone number as a string if the user is requesting to make a phone call, and 'text message' along with the phone number and message as strings if the user is requesting to send a text message")
                })
                put("speak", JSONObject().apply {
                    put("provider", JSONObject().apply {
                        put("type", "deepgram")
                        put("model", speakModel)
                        // put("voice", "alloy")
                    })
                })
            })
        }
        return settings
    }

    fun setUberAPI(api: UberAPI) {
        uberAPI = api
    }

    fun setHealthAPI(api: HealthAPI) {
        healthAPI = api
    }

    fun setSpotifyAPI(api: SpotifyAPI) {
        spotifyAPI = api
    }

    suspend fun getHealthData(metric: String): String {
        return when (metric) {
            "heart_rate" -> healthAPI.getHeartRateFromGoogleFit()
            "steps" -> healthAPI.getStepsFromGoogleFit()
            "sleep" -> healthAPI.getSleepDataFromGoogleFit()
            else -> "Metric not supported yet"
        }
    }

    fun initHealthAPI() {
        healthAPI = HealthAPI(context, googleSignInAccount)
        setHealthAPI(healthAPI)
    }

    fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            context.startActivity(intent)
        }
    }

    fun sendTextMessage(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    }

    fun isConnected(): Boolean {
        return isConnected
    }
}