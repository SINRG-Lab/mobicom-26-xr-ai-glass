package com.sdk.glassessdksample

import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.compareTo
import kotlin.coroutines.CoroutineContext

class FileAudioStreamer(
    private val scope: CoroutineScope,
    private val inputStreamProvider: () -> InputStream, // For streaming
    sampleRate: Int = 16_000,
    channels: Int = 1,
    bytesPerSample: Int = 2,
    private val frameMs: Int = 20,
    private val onChunkReady: (ByteArray) -> Unit,
    private val onStreamingComplete: (() -> Unit) ?= null
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext + SupervisorJob()
    private var streamingJob: Job? = null
    private val chunkSize = sampleRate * channels * bytesPerSample * frameMs / 1000
    var totalBytes = 0L
    var startTime = System.currentTimeMillis()
    private var firstTime = true
    private var pastRun: Int = 0

    fun start() {
        check(streamingJob == null) { "FileAudioStreamer already running" }

        streamingJob = launch(Dispatchers.Default) {
            inputStreamProvider().use { input ->
                val buffer = ByteArray(chunkSize)
                Log.d("Dragon", "Streaming started")
                while (isActive) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead <= 0) break
                    onChunk(buffer.copyOf(bytesRead))
                    delay(frameMs.toLong())
                }
                Log.d("Dragon", "Streaming finished")
            }
            onStreamingComplete?.invoke()
        }

        /*
        streamingJob = launch(Dispatchers.IO) {
            val input = inputStreamProvider.invoke()
            input.use { stream ->
                val buffer = ByteArray(chunkSize)

                Log.d(TAG, "Streaming started")

                while (isActive) {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead <= 0) break

                    onChunk(buffer.copyOf(bytesRead))
                    delay(frameMs.toLong())
                }

                Log.d(TAG, "Streaming finished")
            }
        }
        */

        Log.i(TAG, "FileAudioStreamer started")
        startTime = System.currentTimeMillis()
        Log.d(TAG, "Start time: $startTime")

        // val run = LatencyManager.newRun()
        // run.startTime = LatencyManager.now()
    }

    fun stop() {
        val run = BitrateManager.newRun()
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("BitrateTest", "Seconds from start: $elapsedSec s")

        if (elapsedSec > 0) {
            val bitrate = (totalBytes * 8) / elapsedSec // bits per second
            Log.d("BitrateTest", "Current bitrate: $bitrate bps")

            run.inputRate = bitrate.toString()
            // Log.d("Ossian", "run number: $run")
        }

        totalBytes = 0L

        streamingJob?.cancel()
        streamingJob = null
        Log.i(TAG, "FileAudioStreamer stopped")

        // val run = BitrateManager.reRun()
    }

    fun onChunk(chunk: ByteArray) {
        // Log.d("BitrateTest", "Chunk size = $chunk")

        totalBytes += chunk.size
        // Log.d("BitrateTest", "Total bytes length = ${totalBytes}")

        if (LatencyManager.getRunId() != pastRun) {
            firstTime = true
        }

        if (firstTime) {
            Log.d("Ossian", "Input transmission started at ${LatencyManager.now()}")
            LatencyManager.getCurrentRun()?.startInputTransmissionTime = LatencyManager.now()

            // Log.d("Ossian", "STT started at ${LatencyManager.now()}")
            // LatencyManager.getCurrentRun()?.startSttTime = LatencyManager.now()

            firstTime = false
        }

        pastRun = LatencyManager.getRunId()

        // startTime = System.currentTimeMillis()

        onChunkReady(chunk)
    }

    companion object { private const val TAG = "FileAudioStreamer" }
}