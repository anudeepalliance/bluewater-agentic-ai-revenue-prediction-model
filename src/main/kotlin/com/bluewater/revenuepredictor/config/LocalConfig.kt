package com.bluewater.revenuepredictor.config

object LocalConfig {
    val port: Int = System.getenv("BLUEWATER_PORT")?.toIntOrNull() ?: 8080
    val mongoUri: String = System.getenv("BLUEWATER_MONGO_URI") ?: "mongodb://localhost:27017/bluewater_predictor"
    val mongoDatabase: String = System.getenv("BLUEWATER_MONGO_DATABASE") ?: "bluewater_predictor"
    val llmApiKey: String = System.getenv("BLUEWATER_LLM_API_KEY").orEmpty()
    val llmBaseUrl: String = System.getenv("BLUEWATER_LLM_BASE_URL").orEmpty()
    val llmModel: String = System.getenv("BLUEWATER_LLM_MODEL").orEmpty().ifBlank { "gpt-4.1-mini" }

    val liveLlmConfigured: Boolean
        get() = llmApiKey.isNotBlank() && llmBaseUrl.isNotBlank() && llmModel.isNotBlank()
}
