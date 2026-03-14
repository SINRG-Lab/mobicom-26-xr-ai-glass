package com.sdk.glassessdksample

import android.util.Base64
import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WhisperWebSocket (
    private val apiKey: String,
    // private val onPartialTranscription: (String) -> Unit,
    private val onFinalTranscription: (String) -> Unit,
) : WebSocketListener() {
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect() {
        // val url = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2025-06-03"
        val url = "wss://api.deepgram.com/v1/listen?punctuate=true&interim_results=true&language=en" // &endpointing=300&utterance_end_ms=1000&utterances=true&vad_events=true"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $apiKey")
            // .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WhisperWS", "WebSocket connected")
    }

    override fun onMessage (webSocket: WebSocket, text: String) {
        // Log.d("WhisperWS", "Raw Message: $text")
        val json = JSONObject(text)
        val type = json.optString("type")

        if (type == "Results") {
            val results = json.optJSONObject("channel")
                ?.optJSONArray("alternatives")
                ?.optJSONObject(0)
            val transcript = results?.optString("transcript") ?: ""
            val isFinal = json.optBoolean("is_final", false)

            if (transcript.isNotEmpty()) {
                if (isFinal) {
                    onFinalTranscription(transcript)
                }
            }
        } else {
            // Log.d("WhisperWS", "Event [$type]: $text")
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WhisperWS", "Error: ${t.message}")
        connect()
    }

    fun sendAudioChunk(chunk: ByteArray) {
        // Log.d("WhisperWS", "Audio: ${ByteString.of(*chunk)}")
        webSocket.send(ByteString.of(*chunk))
    }

    fun close() {
        webSocket.close(1000, null)
    }
}