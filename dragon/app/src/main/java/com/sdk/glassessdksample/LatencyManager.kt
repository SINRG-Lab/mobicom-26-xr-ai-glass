package com.sdk.glassessdksample

import android.util.Log
import java.io.File
import java.io.FileWriter

object LatencyManager {
    private val runs = mutableListOf<LatencyTiming>()
    private var currentRun: LatencyTiming? = null
    private var runCounter = 0

    fun newRun(): LatencyTiming {
        runCounter += 1
        val run = LatencyTiming(runId = runCounter)
        currentRun = run
        runs.add(run)
        Log.d("Latency", "New run created: $runCounter")
        return run
    }

    fun getCurrentRun(): LatencyTiming? = currentRun

    fun getRunId(): Int = currentRun?.runId ?: 0

    fun now(): Long = System.currentTimeMillis()

    fun reset() {
        runs.clear()
        currentRun = null
        runCounter = 0
    }

    fun exportToCsv(file: File) {
        val writer = FileWriter(file, false)
        writer.appendLine(
            "runId,start,finish,startInput,finishInput,startStt,finishStt,startInference,finishInference,startTts,finishTts,startOutput,finishOutput"
        )

        runs.forEach { r ->
            writer.appendLine(
                listOf(
                    r.runId,
                    r.startTime,
                    r.finishTime,
                    r.startInputTransmissionTime,
                    r.finishInputTransmissionTime,
                    r.startSttTime,
                    r.finishSttTime,
                    r.startLlmTime,
                    r.finishLlmTime,
                    r.startTtsTime,
                    r.finishTtsTime,
                    r.startOutputTransmissionTime,
                    r.finishOutputTransmissionTime
                ).joinToString(",")
            )
        }
        writer.flush()
        writer.close()
    }
}