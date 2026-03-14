package com.sdk.glassessdksample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Base64
import androidx.core.content.ContextCompat
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.DetectLabelsRequest
import com.amazonaws.services.rekognition.model.Image
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ByteString
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AIClient(
    private val openAiKey: String,
    private val accessKey: String,
    private val secretAccessKey: String,
    private val scope: CoroutineScope,
    private val context: Context
) {
    private var client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .eventListener(object : EventListener() {
            override fun connectEnd(
                call: Call,
                inetSocketAddress: InetSocketAddress,
                proxy: Proxy,
                protocol: Protocol?
            ) {
                /*
                Log.d("AIClient", "Connected to IP: ${inetSocketAddress.address.hostAddress}")
                Log.d("Ossian", "LLM network transmission ended at ${LatencyManager.now()}")
                LatencyManager.getCurrentRun()?.finishUplinkTime = LatencyManager.now()

                Log.d("Ossian", "LLM transmission started at ${LatencyManager.now()}")
                LatencyManager.getCurrentRun()?.startLlmTime = LatencyManager.now()
                */
            }
        })
        .build()
    private lateinit var ttsQueue: TTSWebSocketQueue
    private lateinit var TTSws: TTSWebSocket
    private lateinit var ttsPlayer : TtsPlayer
    private lateinit var uberAPI: UberAPI
    private lateinit var healthAPI: HealthAPI
    private var fullTextList: MutableList<String> = mutableListOf()
    private val ttsBuffer = StringBuilder()
    lateinit var googleSignInAccount: GoogleSignInAccount
    @Volatile
    private var cancelStreaming = false
    private var firstTime = true
    private var pastRun: Int = 0
    private var firstTime1 = true
    private var pastRun1: Int = 0

    suspend fun transcribeAudio(audioData: ByteArray): String? = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav",
                RequestBody.create("audio/wav".toMediaTypeOrNull(), audioData))
            .addFormDataPart("model", "whisper-1")
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .addHeader("Authorization", "Bearer $openAiKey")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("AIClient", "Failed transcription: ${response.code} ${response.body?.string()}")
                return@withContext null
            }
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            json.getString("text")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun streamChatResponse(prompt: String?, image: ByteArray?, caption: Boolean, onCompleteText: (String) -> Unit) {
        Log.d("Ossian", "LLM transmission started at ${LatencyManager.now()}")
        LatencyManager.getCurrentRun()?.startLlmTime = LatencyManager.now()

        if (!caption) {
            startTtsSession()
            ttsQueue = TTSWebSocketQueue(this)
        }

        streamChat(prompt, image, onChunk = { chunk ->
            if (LatencyManager.getRunId() != pastRun) {
                firstTime = true
            }

            if (firstTime) {
                Log.d("Ossian", "LLM transmission ended at ${LatencyManager.now()}")
                LatencyManager.getCurrentRun()?.finishLlmTime = LatencyManager.now()

                firstTime = false
            }

            pastRun = LatencyManager.getRunId()

            if (!caption) {
                bufferAndSpeak(chunk)
                // Log.d("AIClient", "Chunk transmitted: $chunk")
            }
            fullTextList.add(chunk)
        }, onComplete = {
            // LatencyManager.getCurrentRun()?.finishLlmTime = LatencyManager.now()
            var fullText: String = fullTextList.joinToString(separator = "")
            fullTextList.clear()
            if (caption) {
                onCompleteText(fullText)
            }
            /*
            GlobalScope.launch {
                val toolAwareResponse = chatWithTools(prompt, image)
                val finalResponse = toolAwareResponse ?: fullText
                onCompleteText(finalResponse)
            }
            */
        })

        /*
        chatWithTools(prompt, image, onChunk = { chunk ->
            if (!caption) {
                bufferAndSpeak(chunk)
                // Log.d("AIClient", "Chunk transmitted: $chunk")
            }
            fullTextList.add(chunk)
        }, onComplete = {
            var fullText: String = fullTextList.joinToString(separator = "")
            fullTextList.clear()
            onCompleteText(fullText)
            /*
            GlobalScope.launch {
                Log.d("AIClient", "Llm transmission end")
                // synthesizeSpeech(fullText)
            }
            */
        })
        */
    }

    fun streamChat(prompt: String?, image: ByteArray?, onChunk: (String) -> Unit, onComplete: () -> Unit) {
        // Log.d("AIClient", "Llm transmission begin")
        cancelStreaming = false
        val contentArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })

            if (image != null) {
                val base64Image = Base64.encodeToString(image, Base64.NO_WRAP)
                val imageUrlObject = JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64Image")
                }

                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", imageUrlObject)
                })
            }
        }

        val messageArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            })
        }

        val body = JSONObject().apply {
            put("model", "gpt-4o-mini-2024-07-18")
            put("messages", messageArray)
            // put("tools", tools)
            // put("tool_choice", "auto")
            put("stream", true)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $openAiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AIClient", "Error streaming GPT response", e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Log.d("AIClient", "Call: $response")
                response.body?.source()?.use { source ->
                    while (!source.exhausted() && !cancelStreaming) {
                        val line = source.readUtf8Line() ?: continue
                        if (line.startsWith("data: ")) {
                            val payload = line.removePrefix("data: ").trim()
                            if (payload == "[DONE]") {
                                onComplete()
                                break
                            }
                            val json = JSONObject(payload)
                            val delta = json
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .optJSONObject("delta")
                            val content = delta?.optString("content", "")
                            // Log.d("AIClient", "content = $content")
                            if (!content.isNullOrEmpty()) {
                                if (!cancelStreaming) {
                                    onChunk(content)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    suspend fun chatWithTools(prompt: String?, image: ByteArray?) : String? = withContext(Dispatchers.IO) {
        // Log.d("AIClient", "Llm transmission begin")
        cancelStreaming = false
        val contentArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })

            if (image != null) {
                val base64Image = Base64.encodeToString(image, Base64.NO_WRAP)
                val imageUrlObject = JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64Image")
                }

                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", imageUrlObject)
                })
            }
        }

        val messageArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            })
        }

        return@withContext continueChat(messageArray, buildTools())
    }

    suspend fun continueChat (messageArray: JSONArray, tools: JSONArray) : String? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini-2024-07-18")
            put("messages", messageArray)
            put("tools", tools)
            put("tool_choice", "auto")
            // put("stream", true)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $openAiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: return@withContext null
            Log.d("AIClient", "GPT response: $responseBodyString")
            val json = JSONObject(responseBodyString)

            if (!response.isSuccessful) {
                val errorMsg = json.optJSONObject("error")?.optString("message")
                Log.e("AIClient", "Chat error: $errorMsg ($responseBodyString)")
                return@withContext null
            }

            val choices = json.optJSONArray("choices")
            if (choices != null) {
                val assistantMessages = choices.getJSONObject(0).optJSONObject("message")
                val toolCalls = assistantMessages.optJSONArray("tool_calls")
                if (toolCalls != null && toolCalls.length() > 0) {
                    cancelStreaming = true
                    val toolCall = toolCalls.getJSONObject(0)
                    val functionName = toolCall.getJSONObject("function").getString("name")
                    val arguments = JSONObject(toolCall.getJSONObject("function").getString("arguments"))

                    val toolResult = when (functionName) {
                        "get_current_location" -> uberAPI.getCurrentLocationAsJson()?.getString("longitude")
                        "describe_photo" -> ""
                        "order_uber_ride" -> {
                            val destination = arguments.getString("destination")
                            val result = uberAPI.orderUber(destination)
                            if (result.startsWith("AUTH_REQUIRED:")) {
                                val authUrl = result.removePrefix("AUTH_REQUIRED:")
                                "Please authorize Uber here: $authUrl"
                            } else {
                                result
                            }
                        }
                        "get_health_data" -> {
                            val metric = arguments.getString("metric")
                            getHealthData(metric)
                        }
                        "make_phone_call" -> {
                            val phoneNumber = arguments.getString("phone number")
                            makePhoneCall(phoneNumber)
                        }
                        "send_text_message" -> {
                            val phoneNumber = arguments.getString("phone number")
                            val textMessage = arguments.getString("message")
                            sendTextMessage(phoneNumber, textMessage)
                            // Add logic to reprompt to ask for message is user does not include message
                        }
                        else -> "Unknown tool"
                    }

                    /*
                    if (functionName == "describe_photo") {
                        endAllProcesses()
                        if (!isThumbnailActive) {
                            isThumbnailActive = true
                            val thumbnailSize = 0x02
                            sendControlCommand(
                                byteArrayOf(
                                    0x02,
                                    0x01,
                                    0x06,
                                    thumbnailSize.toByte(),
                                    thumbnailSize.toByte(),
                                    0x02
                                )
                            )
                        }
                    }
                    */

                    messageArray.put(assistantMessages)

                    val toolMessage = JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", toolCall.getString("id"))
                        put("content", toolResult)
                    }

                    messageArray.put(toolMessage)

                    // Recurse until GPT produces final response
                    return@withContext continueChat(messageArray, tools)
                }
                else {
                    val content = assistantMessages.optString("content")
                    bufferAndSpeak(content)
                    return@withContext content
                }
            }
            else {
                Log.e("AIClient", "Chat response missing 'choices': $responseBodyString")
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildTools(): JSONArray {
        val tools = JSONArray().apply {
            // Create tool for taking picture (User asks: "What is in front of me right now")
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_health_data")
                    put("description", "Gets the health data of the user such as heart rate or sleep")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("metric", JSONObject().apply {
                                put("type", "string")
                                put("enum", JSONArray().put("heart_rate").put("steps").put("sleep").put("weight").put("calories"))
                                put("description", "The health metric to retrieve.")
                            })
                        })
                        put("required", JSONArray().put("metric"))
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "make_phone_call")
                    put("description", "Makes a phone call on behalf of the user")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply{
                            put("phone number", JSONObject().apply {
                                put("type", "string")
                                put("description", "Phone number that the user is calling")
                            })
                        })
                        put("required", JSONArray().put("phone number"))
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "send_text_message")
                    put("description", "Sends a text message on behalf of the user")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply{
                            put("phone number", JSONObject().apply {
                                put("type", "string")
                                put("description", "Phone number that the user is texting")
                            })
                            put("message", JSONObject().apply {
                                put("type", "string")
                                put("description", "Message that the user is sending")
                            })
                        })
                        put("required", JSONArray().put("phone number"))
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_current_location")
                    put("description", "Gets the current GPS location of the user")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject()) // no input needed
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "describe_photo")
                    put("description", "Takes a picture and describes it to the user")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject()) // no input needed
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "order_uber_ride")
                    put("description", "Orders an Uber ride to a destination")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("destination", JSONObject().apply {
                                put("type", "string")
                                put("description", "The name or coordinates of the destination")
                            })
                        })
                        put("required", JSONArray().put("destination"))
                    })
                })
            })
        }
        return tools
    }

    private var i = 0

    private fun bufferAndSpeak(chunk: String) {
        if (LatencyManager.getRunId() != pastRun1) {
            firstTime1 = true
        }

        if (firstTime1) {
            Log.d("Ossian", "Tts network transmission started at ${LatencyManager.now()}")
            LatencyManager.getCurrentRun()?.startTtsTime = LatencyManager.now()

            firstTime1 = false
        }

        pastRun1 = LatencyManager.getRunId()

        var chunk1 = chunk
        if (chunk == "," ||  chunk == "-") {
            chunk1 = " "
        }

        if (chunk == "." || chunk == "!" || chunk == "?") {
            chunk1 = "   "
        }

        ttsBuffer.append(chunk1)

        if (ttsBuffer.isNotEmpty() &&
            (ttsBuffer.endsWith(".") ||
             ttsBuffer.endsWith("!") ||
             ttsBuffer.endsWith("?") ||
             ttsBuffer.split(" ").size > 10) ||
             i < 2) {

                var phrase = ttsBuffer.toString().trim()
                ttsBuffer.clear()
                ttsQueue.enqueue(phrase)

                i = i + 1

                // Log.d("AIClient", "Phrase enqueued: $phrase")

                /*
                GlobalScope.launch {
                    Log.d("AIClient", "Phrase transmitted: $phrase")
                    // Log.d("AIClient", "TTS transmission begin")
                    TTSws.sendTextChunk(phrase)
                }
                */
        }
    }

    private fun flushRemainingSpeech() {
        if (ttsBuffer.isNotEmpty()) {
            val phrase = ttsBuffer.toString().trim()
            ttsBuffer.clear()

            GlobalScope.launch {
                Log.d("AIClient", "Phrase transmitted: $phrase")
                // Log.d("AIClient", "TTS transmission end")
                TTSws.sendTextChunk(phrase)
            }
        }

        if (ttsBuffer.isEmpty()) {
            GlobalScope.launch {
                // Log.d("AIClient", "TTS transmission end")
                while (true) {
                    TTSws.sendTextChunk("       ")
                    delay(1000)
                }
            }
        }
    }

    suspend fun detectLabelsInImage(imageData: ByteArray): List<String> = withContext(Dispatchers.IO) {
        try {
            val awsCreds = BasicAWSCredentials(
                accessKey,
                secretAccessKey
            )

            val rekognitionClient = AmazonRekognitionClient(awsCreds)
            rekognitionClient.setRegion(Region.getRegion(Regions.US_EAST_1))

            val request = DetectLabelsRequest()
                .withImage(Image().withBytes(ByteBuffer.wrap(imageData)))
                .withMaxLabels(10)
                .withMinConfidence(75f)

            val result = rekognitionClient.detectLabels(request)
            return@withContext result.labels.map { it.name }
        }
        catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    fun setUberAPI(api: UberAPI) {
        uberAPI = api
    }

    fun setHealthAPI(api: HealthAPI) {
        healthAPI = api
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

    fun startTtsSession() {
        val intent = Intent(context, ForegroundTTSService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Log.d("Ossian", "TTS start")
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun sendChunkToForegroundService(chunk: String) {
        // Log.d("AIClient", "chunk sent: $chunk")
        val intent = Intent(context, ForegroundTTSService::class.java).apply {
            putExtra(ForegroundTTSService.EXTRA_TEXT, chunk)
        }
        context.startService(intent) // delivers to onStartCommand of existing service
    }

    fun stopTtsSession() {
        val intent = Intent(context, ForegroundTTSService::class.java)
        context.stopService(intent)
    }
}
