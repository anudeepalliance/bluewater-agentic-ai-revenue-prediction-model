package com.bluewater.revenuepredictor.koog.predictRevenue

import com.bluewater.revenuepredictor.domain.RevenueInitiative
import com.bluewater.revenuepredictor.domain.SolutionLine
import com.bluewater.revenuepredictor.repository.PredictionLearningRepository
import com.bluewater.revenuepredictor.repository.RevenueInitiativeRepository

internal object RevenueForecastPipelineDataAccess {
    suspend fun loadHistoricalPerformanceMarkdown(solutionLines: List<SolutionLine>): String {
        val rows = RevenueInitiativeRepository.list()
            .filter { initiative -> initiative.solutionLines.any { it in solutionLines } }
            .take(6)

        return buildString {
            appendLine("## Historical initiative evidence")
            if (rows.isEmpty()) {
                appendLine("- No historical initiatives found.")
            } else {
                rows.forEach { row ->
                    appendLine(
                        "- ${row.initiativeName}: predicted \$${row.predictedRevenueUsd}, observed \$${row.observedRevenueUsd}, segment ${row.targetSegment}",
                    )
                }
            }
        }.trim()
    }

    suspend fun loadCalibrationMarkdown(solutionLines: List<SolutionLine>): String {
        val rows = PredictionLearningRepository.listCalibrationsFor(solutionLines)
        return buildString {
            appendLine("## Calibration guidance")
            if (rows.isEmpty()) {
                appendLine("- No calibration rules stored yet.")
            } else {
                rows.forEach { row ->
                    appendLine(
                        "- ${row.solutionLine.displayName}: adjustment ${row.recommendedRevenueAdjustmentPercent}% and confidence ${row.confidenceAdjustment}; ${row.ruleSummary}",
                    )
                }
            }
        }.trim()
    }

    suspend fun persistInitiatives(initiatives: List<RevenueInitiative>): List<String> {
        initiatives.forEach { RevenueInitiativeRepository.upsert(it) }
        return initiatives.map { it.id }
    }
}

