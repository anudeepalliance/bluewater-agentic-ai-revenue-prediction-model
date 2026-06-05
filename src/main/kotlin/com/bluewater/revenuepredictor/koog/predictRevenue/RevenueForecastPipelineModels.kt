package com.bluewater.revenuepredictor.koog.predictRevenue

import com.bluewater.revenuepredictor.domain.RevenueInitiative
import com.bluewater.revenuepredictor.domain.SolutionLine
import kotlinx.serialization.Serializable

internal data class RevenueForecastInput(
    val solutionLines: List<SolutionLine>,
    val goal: String,
    val audience: String,
    val numberOfIdeas: Int,
)

internal data class RevenueForecastPipelineState(
    val originalBrief: String,
    val nowMs: Long,
    val parsedInput: RevenueForecastInput? = null,
    val historicalPerformanceMarkdown: String = "",
    val calibrationMarkdown: String = "",
    val assembledContextMarkdown: String = "",
    val generatedIdeasJson: String = "",
    val parsedInitiatives: List<RevenueInitiative> = emptyList(),
    val savedInitiativeIds: List<String> = emptyList(),
)

internal sealed class RevenueForecastPipelineResult {
    data class Success(
        val rawMarkdown: String,
        val savedInitiativeIds: List<String>,
        val executionMode: String,
    ) : RevenueForecastPipelineResult()

    data class Failure(val reason: String) : RevenueForecastPipelineResult()
}

@Serializable
internal data class RevenueInitiativeDraftResponse(
    val ideas: List<RevenueInitiativeDraft>,
)

@Serializable
internal data class RevenueInitiativeDraft(
    val initiativeName: String,
    val objective: String,
    val solutionLines: List<String>,
    val targetSegment: String,
    val channelPlan: String,
    val rationale: String,
    val predictedRevenueUsd: Int,
    val confidenceScore: Double,
)

