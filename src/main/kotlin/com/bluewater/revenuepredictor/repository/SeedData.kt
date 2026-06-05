package com.bluewater.revenuepredictor.repository

import com.bluewater.revenuepredictor.domain.ForecastCalibration
import com.bluewater.revenuepredictor.domain.RevenueInitiative
import com.bluewater.revenuepredictor.domain.RevenueInitiativeStatus
import com.bluewater.revenuepredictor.domain.SolutionLine

object SeedData {
    suspend fun seedIfEmpty() {
        if (RevenueInitiativeRepository.list().isNotEmpty()) return

        val now = System.currentTimeMillis()
        listOf(
            RevenueInitiative(
                id = "bw-init-001",
                initiativeName = "Lifecycle expansion for AI builder teams",
                objective = "Lift pipeline from high-intent technical buyers already trialing GPU-backed products.",
                solutionLines = listOf(SolutionLine.GPUS, SolutionLine.APP_RUNTIME),
                targetSegment = "Mid-market AI product teams",
                channelPlan = "Email nurture plus executive webinar follow-up",
                rationale = "Historical wins cluster around teams moving from prototype workloads into production deployment.",
                predictedRevenueUsd = 148000,
                confidenceScore = 0.71,
                status = RevenueInitiativeStatus.COMPLETE,
                createdAtEpochMs = now - 12_000_000,
                launchDateEpochMs = now - 9_000_000,
                observedRevenueUsd = 132000,
                influencedAccounts = 19,
                notes = "Strong engagement from teams already evaluating inference infrastructure.",
            ),
            RevenueInitiative(
                id = "bw-init-002",
                initiativeName = "Storage modernization for data-heavy SaaS teams",
                objective = "Expand revenue among operators consolidating media and backup workloads.",
                solutionLines = listOf(SolutionLine.OBJECT_STORAGE),
                targetSegment = "Growth-stage SaaS operations leaders",
                channelPlan = "ABM email sequence with ROI worksheet",
                rationale = "Deals move faster when cost clarity and migration friction are surfaced early.",
                predictedRevenueUsd = 94000,
                confidenceScore = 0.66,
                status = RevenueInitiativeStatus.COMPLETE,
                createdAtEpochMs = now - 11_000_000,
                launchDateEpochMs = now - 8_500_000,
                observedRevenueUsd = 101000,
                influencedAccounts = 14,
                notes = "Under-forecast due to strong expansion among existing customers.",
            ),
            RevenueInitiative(
                id = "bw-init-003",
                initiativeName = "Container acceleration for platform teams",
                objective = "Increase qualified pipeline among teams replacing self-managed clusters.",
                solutionLines = listOf(SolutionLine.CONTAINER_PLATFORM, SolutionLine.ANALYTICS_STREAMS),
                targetSegment = "Platform engineering leaders",
                channelPlan = "Thought-leadership newsletter plus live migration clinic",
                rationale = "Buyers respond best when platform simplification and governance outcomes are paired.",
                predictedRevenueUsd = 176000,
                confidenceScore = 0.64,
                status = RevenueInitiativeStatus.READY,
                createdAtEpochMs = now - 6_500_000,
                notes = "Queued for the next forecasting cycle.",
            ),
        ).forEach { RevenueInitiativeRepository.upsert(it) }

        listOf(
            ForecastCalibration(
                id = "bw-cal-001",
                solutionLine = SolutionLine.GPUS,
                audiencePattern = "AI teams moving from pilot to production",
                averageErrorPercent = 12.0,
                recommendedRevenueAdjustmentPercent = -8.0,
                confidenceAdjustment = -0.05,
                ruleSummary = "GPU forecasts should be slightly conservative when pipeline depends on net-new buyers only.",
                evidenceInitiativeIds = listOf("bw-init-001"),
            ),
            ForecastCalibration(
                id = "bw-cal-002",
                solutionLine = SolutionLine.OBJECT_STORAGE,
                audiencePattern = "Existing customers expanding backup and archival workloads",
                averageErrorPercent = 7.0,
                recommendedRevenueAdjustmentPercent = 5.0,
                confidenceAdjustment = 0.03,
                ruleSummary = "Expansion-heavy storage motions tend to outperform initial model estimates.",
                evidenceInitiativeIds = listOf("bw-init-002"),
            ),
        ).forEach { PredictionLearningRepository.persistCalibration(it) }
    }
}

