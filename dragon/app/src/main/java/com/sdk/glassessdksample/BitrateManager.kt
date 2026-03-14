package com.sdk.glassessdksample

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BitrateManager {
    private val timings = mutableListOf<BitrateTiming>()
    private var runCounter = 0
    private var currentRun: BitrateTiming? = null
    private var currentId: Int = 0
    val timeFormat = SimpleDateFormat("HH.mmssSSS", Locale.getDefault())

    fun now(): String {
        return timeFormat.format(Date())
    }

    fun newRun(): BitrateTiming {
        val timing = BitrateTiming(runId = ++runCounter)
        timings.add(timing)
        currentRun = timing
        currentId += 1
        return timing
    }

    fun reRun(): BitrateTiming {
        val timing = BitrateTiming(runId = 0)
        timings.add(timing)
        currentRun = timing
        currentId += 1
        return timing
    }

    fun getRunId(): Int {
        val runId = currentId
        return runId
    }

    fun getCurrentRun(): BitrateTiming? = BitrateManager.currentRun

    fun exportToCsv(file: File) {
        file.bufferedWriter().use { writer ->
            writer.write("runId,inputRate,outputRate\n")
            timings.forEach { t ->
                writer.write(
                    "${t.runId},${t.inputRate},${t.outputRate}\n"
                )
            }
        }
    }
}