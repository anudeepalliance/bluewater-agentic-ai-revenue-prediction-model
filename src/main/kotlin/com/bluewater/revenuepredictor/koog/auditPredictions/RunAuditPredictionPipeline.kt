package com.bluewater.revenuepredictor.koog.auditPredictions

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import com.bluewater.revenuepredictor.config.LocalConfig
import com.bluewater.revenuepredictor.domain.ForecastCalibration
import com.bluewater.revenuepredictor.domain.PredictionAudit
import com.bluewater.revenuepredictor.domain.PredictionOutcome
import com.bluewater.revenuepredictor.domain.RevenueInitiativeStatus
import com.bluewater.revenuepredictor.koog.shared.SkillPromptLoader
import com.bluewater.revenuepredictor.koog.shared.buildLlmExecutor
import com.bluewater.revenuepredictor.koog.shared.configuredLlmModel
import com.bluewater.revenuepredictor.koog.shared.json
import com.bluewater.revenuepredictor.koog.shared.runSingleMessageAgainstLlmIfConfigured
import com.bluewater.revenuepredictor.koog.shared.stripMarkdownCodeFences
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.math.abs

private val logger = LoggerFactory.getLogger("AuditPredictionPipeline")

internal suspend fun runAuditPredictionPipeline(
    initiativeId: String,
    actualRevenueUsd: Int,
    influencedAccounts: Int,
    performanceNotes: String,
): AuditPredictionPipelineResult {
    val agent = AIAgent(
        promptExecutor = buildLlmExecutor(),
        llmModel = configuredLlmModel,
        strategy = buildAuditPredictionGraphStrategy(),
        systemPrompt = null,
        maxIterations = 12,
    )
    val initialState = AuditPredictionPipelineState(
        initiativeId = initiativeId,
        actualRevenueUsd = actualRevenueUsd,
        influencedAccounts = influencedAccounts,
        performanceNotes = performanceNotes,
    )
    return try {
        agent.run(agentInput = initialState, sessionId = "bw-audit-${initiativeId.hashCode().toUInt()}")
    } catch (failure: Throwable) {
        logger.error("Prediction audit pipeline failed: {}", failure.message, failure)
        AuditPredictionPipelineResult.Failure(failure.message ?: "Unknown audit failure")
    } finally {
        agent.close()
    }
}

private fun buildAuditPredictionGraphStrategy(): AIAgentGraphStrategy<AuditPredictionPipelineState, AuditPredictionPipelineResult> =
    strategy("bluewater-prediction-audit") {
        val loadInitiative by node<AuditPredictionPipelineState, AuditPredictionPipelineState> { state ->
            val loaded = AuditPredictionPipelineDataAccess.loadInitiative(state.initiativeId)
                ?: error("Revenue initiative not found")
            val updated = loaded.copy(
                observedRevenueUsd = state.actualRevenueUsd,
                influencedAccounts = state.influencedAccounts,
                notes = state.performanceNotes.ifBlank { loaded.notes },
                status = RevenueInitiativeStatus.COMPLETE,
            )
            state.copy(loadedInitiative = loaded, updatedInitiative = updated)
        }

        val loadCalibrationContext by node<AuditPredictionPipelineState, AuditPredictionPipelineState> { state ->
            val initiative = requireUpdatedInitiative(state)
            val calibrations = AuditPredictionPipelineDataAccess.loadCalibrations(initiative)
            state.copy(
                calibrationContextMarkdown = buildString {
                    appendLine("## Existing calibration context")
                    if (calibrations.isEmpty()) {
                        appendLine("- No prior calibration context.")
                    } else {
                        calibrations.forEach { calibration ->
                            appendLine("- ${calibration.solutionLine.displayName}: ${calibration.ruleSummary}")
                        }
                    }
                }.trim(),
            )
        }

        val createAudit by node<AuditPredictionPipelineState, AuditPredictionPipelineState> { state ->
            val initiative = requireUpdatedInitiative(state)
            val varianceUsd = state.actualRevenueUsd - initiative.predictedRevenueUsd
            val variancePercent = if (initiative.predictedRevenueUsd == 0) 0.0
            else ((varianceUsd.toDouble() / initiative.predictedRevenueUsd.toDouble()) * 100.0)
            val outcome = when {
                abs(variancePercent) <= 10.0 -> PredictionOutcome.ACCURATE
                varianceUsd > 0 -> PredictionOutcome.UNDERPREDICTED
                else -> PredictionOutcome.OVERPREDICTED
            }

            val liveJson = runSingleMessageAgainstLlmIfConfigured(
                systemPrompt = SkillPromptLoader.load("bluewater-prediction-auditor.md"),
                userMessage = buildAuditPrompt(initiative, state, varianceUsd, variancePercent, outcome),
            )
            val parsed = liveJson?.let {
                runCatching {
                    json.decodeFromString<PredictionAuditorResponse>(stripMarkdownCodeFences(it))
                }.getOrNull()
            } ?: fallbackAuditorResponse(initiative, variancePercent, outcome)

            val audit = PredictionAudit(
                id = "bw-audit-${UUID.randomUUID()}",
                initiativeId = initiative.id,
                initiativeName = initiative.initiativeName,
                predictedRevenueUsd = initiative.predictedRevenueUsd,
                actualRevenueUsd = state.actualRevenueUsd,
                varianceUsd = varianceUsd,
                variancePercent = variancePercent,
                outcome = outcome,
                confidenceScoreAtPrediction = initiative.confidenceScore,
                influencedAccounts = state.influencedAccounts,
                summaryMarkdown = parsed.summaryMarkdown,
                likelyMissReasons = parsed.likelyMissReasons,
                recommendedAdjustments = parsed.recommendedAdjustments,
            )

            val calibrations = initiative.solutionLines.map { solutionLine ->
                ForecastCalibration(
                    id = "bw-cal-${solutionLine.name.lowercase()}-${UUID.randomUUID()}",
                    solutionLine = solutionLine,
                    audiencePattern = parsed.audiencePattern,
                    averageErrorPercent = variancePercent,
                    recommendedRevenueAdjustmentPercent = parsed.recommendedRevenueAdjustmentPercent,
                    confidenceAdjustment = parsed.confidenceAdjustment,
                    ruleSummary = parsed.recommendedAdjustments.firstOrNull()
                        ?: "Recalibrate using the most recent closed-loop revenue signal.",
                    evidenceInitiativeIds = listOf(initiative.id),
                )
            }

            state.copy(audit = audit, updatedCalibrations = calibrations)
        }

        val persistLearning by node<AuditPredictionPipelineState, AuditPredictionPipelineState> { state ->
            AuditPredictionPipelineDataAccess.persistInitiative(requireUpdatedInitiative(state))
            AuditPredictionPipelineDataAccess.persistAudit(requireAudit(state))
            AuditPredictionPipelineDataAccess.persistCalibrations(state.updatedCalibrations)
            state
        }

        edge(nodeStart forwardTo loadInitiative)
        edge(loadInitiative forwardTo loadCalibrationContext)
        edge(loadCalibrationContext forwardTo createAudit)
        edge(createAudit forwardTo persistLearning)
        edge(
            persistLearning forwardTo nodeFinish transformed { state ->
                AuditPredictionPipelineResult.Success(
                    updatedInitiative = requireUpdatedInitiative(state),
                    audit = requireAudit(state),
                    updatedCalibrations = state.updatedCalibrations,
                    warnings = emptyList(),
                    executionMode = if (LocalConfig.liveLlmConfigured) "live-koog" else "mock-koog",
                ) as AuditPredictionPipelineResult
            },
        )
    }

private fun buildAuditPrompt(
    initiative: com.bluewater.revenuepredictor.domain.RevenueInitiative,
    state: AuditPredictionPipelineState,
    varianceUsd: Int,
    variancePercent: Double,
    outcome: PredictionOutcome,
): String = buildString {
    appendLine("# Closed-loop forecast audit")
    appendLine("Initiative: ${initiative.initiativeName}")
    appendLine("Target segment: ${initiative.targetSegment}")
    appendLine("Predicted revenue: ${initiative.predictedRevenueUsd}")
    appendLine("Actual revenue: ${state.actualRevenueUsd}")
    appendLine("Variance USD: $varianceUsd")
    appendLine("Variance percent: ${"%.2f".format(variancePercent)}")
    appendLine("Outcome: ${outcome.name}")
    appendLine("Performance notes: ${state.performanceNotes.ifBlank { "None supplied." }}")
    appendLine()
    appendLine(state.calibrationContextMarkdown)
}

private fun fallbackAuditorResponse(
    initiative: com.bluewater.revenuepredictor.domain.RevenueInitiative,
    variancePercent: Double,
    outcome: PredictionOutcome,
): PredictionAuditorResponse {
    val summary = when (outcome) {
        PredictionOutcome.ACCURATE -> "The forecast stayed within a reasonable planning range. The initiative likely benefited from a familiar audience and a channel mix that matched prior wins."
        PredictionOutcome.UNDERPREDICTED -> "The initiative outperformed the original estimate. Existing calibration likely understated expansion potential for this audience."
        PredictionOutcome.OVERPREDICTED -> "The initiative missed the original estimate. The model likely gave too much weight to top-of-funnel intent and not enough to conversion friction."
    }
    return PredictionAuditorResponse(
        summaryMarkdown = summary,
        likelyMissReasons = listOf(
            "Segment readiness varied more than the original forecast assumed.",
            "Revenue timing was sensitive to channel execution quality.",
        ),
        recommendedAdjustments = listOf(
            "Re-weight forecasts for ${initiative.targetSegment} by recent closed-won evidence.",
            "Lower confidence when conversion depends on net-new education rather than expansion demand.",
        ),
        audiencePattern = initiative.targetSegment,
        confidenceAdjustment = if (outcome == PredictionOutcome.ACCURATE) 0.02 else -0.05,
        recommendedRevenueAdjustmentPercent = -variancePercent / 2.0,
    )
}

private fun requireUpdatedInitiative(state: AuditPredictionPipelineState) =
    requireNotNull(state.updatedInitiative) { "Initiative was not loaded." }

private fun requireAudit(state: AuditPredictionPipelineState) =
    requireNotNull(state.audit) { "Prediction audit was not created." }
