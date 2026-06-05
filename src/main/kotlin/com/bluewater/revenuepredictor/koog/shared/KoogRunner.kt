package com.bluewater.revenuepredictor.koog.shared

import ai.koog.agents.core.agent.AIAgent
import com.bluewater.revenuepredictor.config.LocalConfig

internal suspend fun runSingleMessageAgainstLlmIfConfigured(
    systemPrompt: String,
    userMessage: String,
    maxIterations: Int = 4,
): String? {
    if (!LocalConfig.liveLlmConfigured) return null
    val agent = AIAgent(
        promptExecutor = buildLlmExecutor(),
        llmModel = configuredLlmModel,
        systemPrompt = systemPrompt,
        maxIterations = maxIterations,
    )
    return try {
        agent.run(userMessage)
    } finally {
        agent.close()
    }
}

