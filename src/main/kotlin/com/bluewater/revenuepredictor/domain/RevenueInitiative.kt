package com.bluewater.revenuepredictor.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RevenueInitiative(
    @SerialName("_id") val id: String,
    val initiativeName: String,
    val objective: String,
    val solutionLines: List<SolutionLine>,
    val targetSegment: String,
    val channelPlan: String,
    val rationale: String,
    val predictedRevenueUsd: Int,
    val confidenceScore: Double,
    val status: RevenueInitiativeStatus = RevenueInitiativeStatus.IDEATED,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val launchDateEpochMs: Long = 0L,
    val observedRevenueUsd: Int = 0,
    val influencedAccounts: Int = 0,
    val notes: String = "",
)

@Serializable
enum class RevenueInitiativeStatus {
    IDEATED,
    READY,
    LIVE,
    COMPLETE,
}

