package com.sdk.glassessdksample

import android.Manifest
import android.content.Context
import android.media.*
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import kotlin.coroutines.CoroutineContext

class MicStreamer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    private val sampleRate: Int = 16_000,
    private val channels: Int = 1,
    private val frameMs: Int = 20,
    private val bytesPerSample: Int = 2,
    private val onChunkReady: (ByteArray) -> Unit,
    private val onStreamingComplete: (ByteArray) -> Unit
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext + SupervisorJob()
    private var recorder: AudioRecord? = null
    private var streamingJob: Job? = null
    private val TAG = "MicStreamer"

    private var fileOutputStream: FileOutputStream? = null
    private val recordingBuffer = ByteArrayOutputStream()
    private var recordedBytes: Long = 0

    var totalBytes = 0L
    var startTime = System.currentTimeMillis()

    var firstTime = true

    private val chunkSize = sampleRate * channels * bytesPerSample * frameMs / 1000

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        val outputFile = File(context.getExternalFilesDir(null), "recording.wav")
        fileOutputStream = FileOutputStream(outputFile)
        writeWavHeader(fileOutputStream!!, sampleRate, channels, audioFormat)
        recordedBytes = 0

        recordingBuffer.reset()

        check(streamingJob == null) { "MicStreamer already running" }

        val bluetoothMic = getBluetoothMic()

        val audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC) // or VOICE_RECOGNITION
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(chunkSize * 2)
            .build()

        bluetoothMic?.let { audioRecord.setPreferredDevice(it) }

        require(audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord init failed"
        }

        recorder = audioRecord
        audioRecord.startRecording()

        streamingJob = launch(Dispatchers.IO) {
            Process.setThreadPriority(
                Process.THREAD_PRIORITY_AUDIO
            )
            val buffer = ByteArray(chunkSize)
            while (isActive) {
                val n = recorder?.read(buffer, 0, buffer.size)
                // Log.d(TAG, "chunk: $n")
                if (n != null) {
                    /*
                    when {
                        n > 0  -> if (!isSilence(buffer)) { onChunk(buffer.copyOf(n)) }
                        n == 0 -> yield()
                        else   -> throw IllegalStateException("read() returned $n")
                    }
                    */
                    if (n > 0) {
                        onChunk(buffer.copyOf(n))

                        fileOutputStream?.write(buffer, 0, n)
                        recordedBytes += n
                        recordingBuffer.write(buffer,0,n)
                        // Log.d("Ossian", "Recorded bytes: $recordedBytes")

                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                        if (elapsedSec > 0) {
                            val run = BitrateManager.newRun()

                            val bitrate = (recordedBytes * 8) / elapsedSec // bits per second
                            Log.d("BitrateTest", "Current bitrate is $bitrate bps after $elapsedSec seconds")

                            run.inputRate = bitrate.toString()
                        }
                    }
                }
                if (firstTime == true) {
                    // LatencyManager.getCurrentRun()?.finishInputTransmissionTime = LatencyManager.now()
                    firstTime = false
                }
            }
        }
        Log.i(TAG, "MicStreamer started with ${bluetoothMic?.productName ?: "phone mic"}\"")
        startTime = System.currentTimeMillis()

        // LatencyManager.getCurrentRun()?.startInputTransmissionTime = LatencyManager.now()
    }

    fun stop() {
        firstTime = true

        streamingJob?.cancel()
        streamingJob = null

        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        val outputFile = File(context.getExternalFilesDir(null), "recording.wav")
        fileOutputStream?.let { fos ->
            fos.close()
            outputFile
            randomAccessFileUpdateWavHeader(outputFile, recordedBytes, sampleRate, channels, audioFormat)
        }

        val fullRecording: ByteArray = recordingBuffer.toByteArray()

        // onStreamingComplete(fullRecording)
        fileOutputStream = null

        Log.i(TAG, "MicStreamer stopped")
    }

    fun onChunk(chunk: ByteArray) {
        // Log.d("Ossian", "Received chunk: $chunk")

        onChunkReady(chunk)

        /*
        totalBytes += chunk.size
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        if ((.9 < elapsedSec && elapsedSec < 1.1) || (1.9 < elapsedSec && elapsedSec < 2.1) || (2.9 < elapsedSec && elapsedSec < 3.1) || (3.9 < elapsedSec && elapsedSec < 4.1) || (4.9 < elapsedSec && elapsedSec < 5.1)) {
            val run = BitrateManager.newRun()

            val bitrate = (totalBytes * 8) / elapsedSec // bits per second
            Log.d("BitrateTest", "Current bitrate is $bitrate bps after $elapsedSec seconds")

            run.inputRate = bitrate.toString()
        }
        */
    }

    private fun getBluetoothMic(): AudioDeviceInfo? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }
    }

    private fun isSilence(buffer: ByteArray, threshold: Int = 1000): Boolean {
        var sum = 0
        for (i in buffer.indices step 2) {
            val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
            sum += kotlin.math.abs(sample)
        }
        val avg = sum / (buffer.size / 2)
        return avg < threshold
    }

    private fun writeWavHeader(
        out: OutputStream,
        sampleRate: Int,
        channels: Int,
        encoding: Int
    ) {
        val bitsPerSample = if (encoding == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)

        // ChunkID "RIFF"
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        // ChunkSize (placeholder, will fix later)
        // Subchunk1ID "WAVE"
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // Subchunk1ID "fmt "
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        // Subchunk1Size = 16 for PCM
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // AudioFormat = 1 (PCM)
        header[20] = 1
        header[21] = 0

        // NumChannels
        header[22] = channels.toByte()
        header[23] = 0

        // SampleRate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()

        // ByteRate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        // BlockAlign
        val blockAlign = (channels * bitsPerSample) / 8
        header[32] = blockAlign.toByte()
        header[33] = 0

        // BitsPerSample
        header[34] = bitsPerSample.toByte()
        header[35] = 0

        // Subchunk2ID "data"
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        out.write(header, 0, 44)
    }

    private fun randomAccessFileUpdateWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        encoding: Int
    ) {
        val bitsPerSample = if (encoding == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = totalAudioLen + 36

        val raf = RandomAccessFile(file, "rw")

        raf.seek(4)
        raf.write((totalDataLen and 0xff).toInt())
        raf.write(((totalDataLen shr 8) and 0xff).toInt())
        raf.write(((totalDataLen shr 16) and 0xff).toInt())
        raf.write(((totalDataLen shr 24) and 0xff).toInt())

        raf.seek(40)
        raf.write((totalAudioLen and 0xff).toInt())
        raf.write(((totalAudioLen shr 8) and 0xff).toInt())
        raf.write(((totalAudioLen shr 16) and 0xff).toInt())
        raf.write(((totalAudioLen shr 24) and 0xff).toInt())

        raf.close()
    }
}