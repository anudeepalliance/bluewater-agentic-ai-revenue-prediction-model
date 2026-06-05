package com.bluewater.revenuepredictor.domain

import kotlinx.serialization.Serializable

@Serializable
data class CreateRevenueInitiativeRequest(val record: RevenueInitiative)

@Serializable
data class ForecastRunRequest(val brief: String)

@Serializable
data class ForecastRunResponse(
    val savedInitiativeIds: List<String>,
    val rawMarkdown: String,
    val executionMode: String,
)

@Serializable
data class PredictionAuditRequest(
    val actualRevenueUsd: Int,
    val influencedAccounts: Int = 0,
    val performanceNotes: String = "",
)

@Serializable
data class PredictionAuditResponse(
    val updatedInitiative: RevenueInitiative,
    val predictionAudit: PredictionAudit,
    val updatedCalibrations: List<ForecastCalibration>,
    val warnings: List<String>,
    val executionMode: String,
)

