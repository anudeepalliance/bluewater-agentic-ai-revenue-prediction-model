package com.bluewater.revenuepredictor.koog.shared

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.bluewater.revenuepredictor.config.LocalConfig

val configuredLlmModel: LLModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = LocalConfig.llmModel,
    capabilities = listOf(
        LLMCapability.Temperature,
        LLMCapability.Completion,
    ),
)

fun buildLlmExecutor(): PromptExecutor {
    val client = OpenAILLMClient(
        apiKey = LocalConfig.llmApiKey,
        settings = OpenAIClientSettings(
            baseUrl = LocalConfig.llmBaseUrl,
            chatCompletionsPath = "v1/chat/completions",
        ),
    )
    return MultiLLMPromptExecutor(mapOf(configuredLlmModel.provider to client))
}

