package com.sdk.glassessdksample

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ForegroundTTSService: Service() {
    private lateinit var ttsWebSocket: TTSWebSocket
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client by lazy {
        OkHttpClient.Builder()
            .pingInterval(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        ttsWebSocket = TTSWebSocket("64aca23e42527ea7ca7153e158195db12259ea7f", scope, client)
        ttsWebSocket.connect()

        val notification = NotificationCompat.Builder(this, "TTS_CHANNEL")
            .setContentTitle("TTS Running")
            .setContentText("Reading responses...")
            .setSmallIcon(R.drawable.ic_tts)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Log.d("TTSService", "Service started")
        val text = intent?.getStringExtra("EXTRA_TEXT")
        if (!text.isNullOrEmpty()) {
            scope.launch {
                ttsWebSocket.sendTextChunk(text)
            }
        }
        /*
        else {
            ttsWebSocket.flush()
        }
        */
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_TEXT = "EXTRA_TEXT"
    }
}