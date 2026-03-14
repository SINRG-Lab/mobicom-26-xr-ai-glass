package com.sdk.glassessdksample

object LLMManager {
    private var modelName: ModelName? = ModelName()

    fun getModelName(): ModelName? = modelName
}