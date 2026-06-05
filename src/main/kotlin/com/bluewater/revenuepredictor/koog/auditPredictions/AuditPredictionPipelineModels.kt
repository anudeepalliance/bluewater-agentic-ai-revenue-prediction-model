package com.bluewater.revenuepredictor.koog.auditPredictions

import com.bluewater.revenuepredictor.domain.ForecastCalibration
import com.bluewater.revenuepredictor.domain.PredictionAudit
import com.bluewater.revenuepredictor.domain.RevenueInitiative
import kotlinx.serialization.Serializable

internal data class AuditPredictionPipelineState(
    val initiativeId: String,
    val actualRevenueUsd: Int,
    val influencedAccounts: Int,
    val performanceNotes: String,
    val loadedInitiative: RevenueInitiative? = null,
    val updatedInitiative: RevenueInitiative? = null,
    val calibrationContextMarkdown: String = "",
    val audit: PredictionAudit? = null,
    val updatedCalibrations: List<ForecastCalibration> = emptyList(),
)

internal sealed class AuditPredictionPipelineResult {
    data class Success(
        val updatedInitiative: RevenueInitiative,
        val audit: PredictionAudit,
        val updatedCalibrations: List<ForecastCalibration>,
        val warnings: List<String>,
        val executionMode: String,
    ) : AuditPredictionPipelineResult()

    data class Failure(val reason: String) : AuditPredictionPipelineResult()
}

@Serializable
internal data class PredictionAuditorResponse(
    val summaryMarkdown: String,
    val likelyMissReasons: List<String>,
    val recommendedAdjustments: List<String>,
    val audiencePattern: String,
    val confidenceAdjustment: Double,
    val recommendedRevenueAdjustmentPercent: Double,
)

