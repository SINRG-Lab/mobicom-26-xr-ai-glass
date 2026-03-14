package com.sdk.glassessdksample

data class LatencyTiming(
    var runId: Int = 0,
    var startTime: Long? = null,
    var finishTime: Long? = null,

    var startInputTransmissionTime: Long? = null,
    var finishInputTransmissionTime: Long? = null,

    var startSttTime: Long? = null,
    var finishSttTime: Long? = null,

    var startLlmTime: Long? = null,
    var finishLlmTime: Long? = null,

    var startTtsTime: Long? = null,
    var finishTtsTime: Long? = null,

    var startOutputTransmissionTime: Long? = null,
    var finishOutputTransmissionTime: Long? = null
)
