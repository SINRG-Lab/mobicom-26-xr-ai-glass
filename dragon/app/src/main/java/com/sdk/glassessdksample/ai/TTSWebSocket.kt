package com.sdk.glassessdksample

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TTSWebSocket (
    private val apiKey: String,
    private val scope: CoroutineScope,
    private val client: OkHttpClient
    // private val onPartialTranscription: (String) -> Unit,
    // private val onFinalTranscription: (String) -> Unit,
) : WebSocketListener() {
    private lateinit var webSocket: WebSocket
    private lateinit var ttsPlayer : TtsPlayer
    private var pastRun: Int = 0
    private var pastRun1: Int = 0
    private var isConnected = false
    private var firstTime = true
    private var endTime = true
    var totalBytes = 0L
    var startTime = System.currentTimeMillis()

    fun connect() {
        val url = "wss://api.deepgram.com/v1/speak?model=aura-2-thalia-en&encoding=linear16&sample_rate=24000"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $apiKey")
            // .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("TTS_WS", "WebSocket connected")
        ttsPlayer = TtsPlayer(scope = scope)
        ttsPlayer.start()
        isConnected = true
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

    override fun onMessage (webSocket: WebSocket, bytes: ByteString) {
        Log.d("TTS_WS", "Bytes transmitted: $bytes")
        /*
        totalBytes += bytes.size
        Log.d("BitrateTest", "Accumulated byte size: ${totalBytes}")

        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("BitrateTest", "Elapsed seconds: $elapsedSec s")

        val bitrate = (totalBytes * 8) / elapsedSec // bits per second
        Log.d("BitrateTest", "Current bitrate: $bitrate bps")

        val run = BitrateManager.newRun()
        run.outputRate = bitrate.toString()
        Log.d("BitrateTest", "run number: $run")
        */

        if (LatencyManager.getRunId() != pastRun1) {
            endTime = true
        }

        if (endTime) {
            Log.d("Ossian", "Test ended at ${LatencyManager.now()}")

            LatencyManager.getCurrentRun()?.finishTtsTime = LatencyManager.now()
            LatencyManager.getCurrentRun()?.finishTime = LatencyManager.now()

            endTime = false
        }

        pastRun1 = LatencyManager.getRunId()

        val audioBytes = bytes.toByteArray()
        ttsPlayer.offer(audioBytes)

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

    override fun onMessage (webSocket: WebSocket, text: String) {
        // Log.d("TTS_WS", "Event: $text")
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            if (type.equals("Close", ignoreCase = true) || json.optBoolean("is_final") || json.optBoolean("done")) {
                scope.launch {
                    ttsPlayer.finish()
                }
            }
            // Log.d("TTS_WS", "Event: $text")
        } catch (e: Exception) {
            // Log.w("TTS_WS", "Non-JSON message: $text")
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
            totalBytes = 0L

            startTime = System.currentTimeMillis()
            Log.d("Bitrate", "start time: $startTime")

            Log.d("Ossian", "Network transmission ended at ${LatencyManager.now()}")
            LatencyManager.getCurrentRun()?.finishUplinkTime = LatencyManager.now()

            Log.d("Ossian", "TTS transmission started at ${LatencyManager.now()}")
            LatencyManager.getCurrentRun()?.startTtsTime = LatencyManager.now()

            firstTime = false
        }

        pastRun = LatencyManager.getRunId()

        webSocket.send(message.toString())
    }

    fun close() {
        webSocket.close(1000, "TTS complete")
    }
}