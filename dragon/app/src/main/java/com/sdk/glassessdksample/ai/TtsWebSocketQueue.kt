package com.sdk.glassessdksample

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class TTSWebSocketQueue(
    private val aiClient: AIClient
) {
    private val chunkQueue = ConcurrentLinkedQueue<String>()
    private var sending = false

    fun enqueue(chunk: String) {
        chunkQueue.offer(chunk)
        sendNext()
    }

    private fun sendNext() {
        if (sending) return

        val next = chunkQueue.poll() ?: return
        sending = true
        // Log.d("TTSQueue", "Chunk sent: $next")
        aiClient.sendChunkToForegroundService(next)

        CoroutineScope(Dispatchers.IO).launch {
            delay(100)
            sending = false
            sendNext() // Recursive call to send next again
        }
    }
}