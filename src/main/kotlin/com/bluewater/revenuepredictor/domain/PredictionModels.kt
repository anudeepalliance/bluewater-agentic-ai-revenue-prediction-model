package com.bluewater.revenuepredictor.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionAudit(
    @SerialName("_id") val id: String,
    val initiativeId: String,
    val initiativeName: String,
    val predictedRevenueUsd: Int,
    val actualRevenueUsd: Int,
    val varianceUsd: Int,
    val variancePercent: Double,
    val outcome: PredictionOutcome,
    val confidenceScoreAtPrediction: Double,
    val influencedAccounts: Int,
    val summaryMarkdown: String,
    val likelyMissReasons: List<String>,
    val recommendedAdjustments: List<String>,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Serializable
enum class PredictionOutcome {
    ACCURATE,
    OVERPREDICTED,
    UNDERPREDICTED,
}

@Serializable
data class ForecastCalibration(
    @SerialName("_id") val id: String,
    val solutionLine: SolutionLine,
    val audiencePattern: String,
    val averageErrorPercent: Double,
    val recommendedRevenueAdjustmentPercent: Double,
    val confidenceAdjustment: Double,
    val ruleSummary: String,
    val evidenceInitiativeIds: List<String>,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

