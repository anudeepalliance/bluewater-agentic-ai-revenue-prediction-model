package com.bluewater.revenuepredictor.koog.predictRevenue

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import com.bluewater.revenuepredictor.config.LocalConfig
import com.bluewater.revenuepredictor.domain.RevenueInitiative
import com.bluewater.revenuepredictor.domain.SolutionLine
import com.bluewater.revenuepredictor.koog.shared.SkillPromptLoader
import com.bluewater.revenuepredictor.koog.shared.buildLlmExecutor
import com.bluewater.revenuepredictor.koog.shared.configuredLlmModel
import com.bluewater.revenuepredictor.koog.shared.json
import com.bluewater.revenuepredictor.koog.shared.runSingleMessageAgainstLlmIfConfigured
import com.bluewater.revenuepredictor.koog.shared.stripMarkdownCodeFences
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("RevenueForecastPipeline")

internal suspend fun runRevenueForecastPipeline(brief: String): RevenueForecastPipelineResult {
    val initialState = RevenueForecastPipelineState(
        originalBrief = brief,
        nowMs = System.currentTimeMillis(),
    )
    val agent = AIAgent(
        promptExecutor = buildLlmExecutor(),
        llmModel = configuredLlmModel,
        strategy = buildRevenueForecastGraphStrategy(),
        systemPrompt = null,
        maxIterations = 14,
    )
    return try {
        agent.run(agentInput = initialState, sessionId = "bw-revenue-forecast-${brief.hashCode().toUInt()}")
    } catch (failure: Throwable) {
        logger.error("Revenue forecast pipeline failed: {}", failure.message, failure)
        RevenueForecastPipelineResult.Failure(failure.message ?: "Unknown revenue forecast failure")
    } finally {
        agent.close()
    }
}

private fun buildRevenueForecastGraphStrategy(): AIAgentGraphStrategy<RevenueForecastPipelineState, RevenueForecastPipelineResult> =
    strategy("bluewater-revenue-forecast") {
        val parseInput by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            state.copy(parsedInput = parseForecastInput(state.originalBrief))
        }

        val loadHistory by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            val parsedInput = requireParsedInput(state)
            state.copy(
                historicalPerformanceMarkdown = RevenueForecastPipelineDataAccess
                    .loadHistoricalPerformanceMarkdown(parsedInput.solutionLines),
            )
        }

        val loadCalibrations by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            val parsedInput = requireParsedInput(state)
            state.copy(
                calibrationMarkdown = RevenueForecastPipelineDataAccess
                    .loadCalibrationMarkdown(parsedInput.solutionLines),
            )
        }

        val assembleContext by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            val parsedInput = requireParsedInput(state)
            state.copy(
                assembledContextMarkdown = buildString {
                    appendLine("# Revenue forecast request")
                    appendLine("Goal: ${parsedInput.goal}")
                    appendLine("Audience: ${parsedInput.audience}")
                    appendLine("Ideas requested: ${parsedInput.numberOfIdeas}")
                    appendLine("Solutions: ${parsedInput.solutionLines.joinToString { it.displayName }}")
                    appendLine()
                    appendLine(state.historicalPerformanceMarkdown)
                    appendLine()
                    appendLine(state.calibrationMarkdown)
                }.trim(),
            )
        }

        val generateIdeas by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            val liveJson = runSingleMessageAgainstLlmIfConfigured(
                systemPrompt = SkillPromptLoader.load("bluewater-revenue-forecaster.md"),
                userMessage = state.assembledContextMarkdown,
            )
            val finalJson = liveJson ?: fallbackForecastJson(state)
            state.copy(generatedIdeasJson = finalJson)
        }

        val parseIdeas by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            val parsedInput = requireParsedInput(state)
            val parsedResponse = json.decodeFromString<RevenueInitiativeDraftResponse>(
                stripMarkdownCodeFences(state.generatedIdeasJson),
            )
            val initiatives = parsedResponse.ideas.take(parsedInput.numberOfIdeas).mapIndexed { index, draft ->
                RevenueInitiative(
                    id = "bw-idea-${UUID.randomUUID()}",
                    initiativeName = draft.initiativeName,
                    objective = draft.objective,
                    solutionLines = draft.solutionLines.mapNotNull { raw ->
                        SolutionLine.values().firstOrNull {
                            it.name.equals(raw.replace(' ', '_').uppercase(), ignoreCase = true) ||
                                it.displayName.equals(raw, ignoreCase = true)
                        }
                    }.ifEmpty { parsedInput.solutionLines },
                    targetSegment = draft.targetSegment,
                    channelPlan = draft.channelPlan,
                    rationale = draft.rationale,
                    predictedRevenueUsd = draft.predictedRevenueUsd.coerceAtLeast(25_000),
                    confidenceScore = draft.confidenceScore.coerceIn(0.35, 0.95),
                    notes = "Generated from Koog revenue forecast pipeline idea ${index + 1}.",
                )
            }
            state.copy(parsedInitiatives = initiatives)
        }

        val persistIdeas by node<RevenueForecastPipelineState, RevenueForecastPipelineState> { state ->
            state.copy(savedInitiativeIds = RevenueForecastPipelineDataAccess.persistInitiatives(state.parsedInitiatives))
        }

        edge(nodeStart forwardTo parseInput)
        edge(parseInput forwardTo loadHistory)
        edge(loadHistory forwardTo loadCalibrations)
        edge(loadCalibrations forwardTo assembleContext)
        edge(assembleContext forwardTo generateIdeas)
        edge(generateIdeas forwardTo parseIdeas)
        edge(parseIdeas forwardTo persistIdeas)
        edge(
            persistIdeas forwardTo nodeFinish transformed { state ->
                RevenueForecastPipelineResult.Success(
                    rawMarkdown = renderInitiativesAsMarkdown(state.parsedInitiatives),
                    savedInitiativeIds = state.savedInitiativeIds,
                    executionMode = if (LocalConfig.liveLlmConfigured) "live-koog" else "mock-koog",
                ) as RevenueForecastPipelineResult
            },
        )
    }

internal fun parseForecastInput(brief: String): RevenueForecastInput {
    val goal = Regex("""(?im)^Goal:\s*(.+)$""").find(brief)?.groupValues?.getOrNull(1)
        ?: brief.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    require(goal.isNotBlank()) { "A forecasting goal is required." }

    val audience = Regex("""(?im)^Audience:\s*(.+)$""").find(brief)?.groupValues?.getOrNull(1)
        ?: "Marketing leaders at growth-stage B2B software companies"

    val numberOfIdeas = Regex("""(?im)^Ideas:\s*(\d+)\s*$""").find(brief)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?.coerceIn(1, 5)
        ?: 3

    val solutionLines = SolutionLine.parseMany(
        Regex("""(?im)^Solutions?:\s*(.+)$""").find(brief)?.groupValues?.getOrNull(1)
    ).ifEmpty { listOf(SolutionLine.GPUS, SolutionLine.APP_RUNTIME) }

    return RevenueForecastInput(
        solutionLines = solutionLines,
        goal = goal,
        audience = audience,
        numberOfIdeas = numberOfIdeas,
    )
}

private fun renderInitiativesAsMarkdown(initiatives: List<RevenueInitiative>): String = buildString {
    initiatives.forEachIndexed { index, initiative ->
        appendLine("## Idea ${index + 1}: ${initiative.initiativeName}")
        appendLine("- Solutions: ${initiative.solutionLines.joinToString { it.displayName }}")
        appendLine("- Target segment: ${initiative.targetSegment}")
        appendLine("- Predicted revenue: $${initiative.predictedRevenueUsd}")
        appendLine("- Confidence: ${"%.0f".format(initiative.confidenceScore * 100)}%")
        appendLine("- Objective: ${initiative.objective}")
        appendLine("- Channel plan: ${initiative.channelPlan}")
        appendLine("- Why it could work: ${initiative.rationale}")
        appendLine()
    }
}.trim()

private fun fallbackForecastJson(state: RevenueForecastPipelineState): String {
    val input = requireParsedInput(state)
    val ideas = (1..input.numberOfIdeas).map { index ->
        val leadSolution = input.solutionLines[(index - 1) % input.solutionLines.size]
        val predictedRevenue = 85_000 + (index * 27_500) + (input.solutionLines.size * 9_000)
        RevenueInitiativeDraft(
            initiativeName = "${leadSolution.displayName} expansion play for ${input.audience}",
            objective = input.goal,
            solutionLines = input.solutionLines.map { it.name },
            targetSegment = input.audience,
            channelPlan = when (index % 3) {
                0 -> "Executive webinar paired with a short lifecycle email sequence"
                1 -> "ABM email sequence plus ROI calculator follow-up"
                else -> "Thought-leadership newsletter with a conversion-focused workshop CTA"
            },
            rationale = "Historical initiatives and calibration guidance suggest this audience converts when the value story ties revenue outcomes to operational simplicity.",
            predictedRevenueUsd = predictedRevenue,
            confidenceScore = 0.58 + (index * 0.06),
        )
    }
    return json.encodeToString(RevenueInitiativeDraftResponse(ideas))
}

private fun requireParsedInput(state: RevenueForecastPipelineState) =
    requireNotNull(state.parsedInput) { "Forecast input was not parsed." }
