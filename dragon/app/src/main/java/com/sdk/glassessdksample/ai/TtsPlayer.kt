package com.sdk.glassessdksample

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.Closeable
import java.util.concurrent.Executors

class TtsPlayer(
    scope: CoroutineScope,
    sampleRate: Int = 24_000,
    channelMask: Int = AudioFormat.CHANNEL_OUT_MONO,
    encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
) : Closeable {

    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate, channelMask, encoding
    ).coerceAtLeast(48_000)

    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(bufferSize)
        .build()

    private val dispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "tts-writer").apply {
                Process.setThreadPriority(
                    Process.THREAD_PRIORITY_AUDIO
                )
            }
        }.asCoroutineDispatcher()

    private val channel = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    private val writerJob = scope.launch(dispatcher) {
        for (pcm in channel) {
            var copied = 0
            while (copied < pcm.size) {
                val written = track.write(pcm, copied, pcm.size - copied)
                if (written < 0) {
                    Log.e(TAG, "Write error: $written")
                    break
                }
                copied += written
            }
        }
        /*
        val tempBuffer = ByteArray(bufferSize)
        var offset = 0

        for (pcm in channel) {
            var copied = 0
            while (copied < pcm.size) {
                val toCopy = minOf(pcm.size - copied, tempBuffer.size - offset)
                System.arraycopy(pcm, copied, tempBuffer, offset, toCopy)
                offset += toCopy
                copied += toCopy

                if (offset >= tempBuffer.size) {
                    val written = track.write(tempBuffer, 0, tempBuffer.size)
                    if (written < 0) Log.e(TAG, "Write error: $written")
                    // else Log.d(TAG, "Written bytes: $written")
                    offset = 0
                }
            }
        }

        if (offset > 0) {
            val written = track.write(tempBuffer, 0, offset)
            if (written < 0) Log.e(TAG, "Write error: $written")
        }
        */
    }

    fun start() {
        track.play()
    }

    /** Non‑blocking; returns immediately. */
    fun offer(chunk: ByteArray) {
        channel.trySend(chunk)
        // track.play()
        /*
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            // val silence = ByteArray(bufferSize) { 0 }
            // track.write(silence, 0, silence.size)
            track.play()
        }
        */
    }

    suspend fun finish() {
        channel.close()
        writerJob.join() // Wait until writer flushes remaining audio
        track.stop()
        track.release()
        dispatcher.close()
        Log.i(TAG, "TtsPlayer finished")
    }

    override fun close() {
        channel.close()
        writerJob.cancel()
        track.stop()
        // track.flush()
        track.release()
        dispatcher.close()
        Log.i(TAG, "TtsPlayer closed")
    }

    companion object { private const val TAG = "TtsPlayer" }
}