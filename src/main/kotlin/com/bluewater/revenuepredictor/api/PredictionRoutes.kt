package com.bluewater.revenuepredictor.api

import com.bluewater.revenuepredictor.domain.CreateRevenueInitiativeRequest
import com.bluewater.revenuepredictor.domain.ForecastRunRequest
import com.bluewater.revenuepredictor.domain.ForecastRunResponse
import com.bluewater.revenuepredictor.domain.PredictionAuditRequest
import com.bluewater.revenuepredictor.domain.PredictionAuditResponse
import com.bluewater.revenuepredictor.koog.auditPredictions.AuditPredictionPipelineResult
import com.bluewater.revenuepredictor.koog.auditPredictions.runAuditPredictionPipeline
import com.bluewater.revenuepredictor.koog.predictRevenue.RevenueForecastPipelineResult
import com.bluewater.revenuepredictor.koog.predictRevenue.runRevenueForecastPipeline
import com.bluewater.revenuepredictor.repository.PredictionLearningRepository
import com.bluewater.revenuepredictor.repository.RevenueInitiativeRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.predictionRoutes() {
    get("/initiatives") {
        call.respond(RevenueInitiativeRepository.list())
    }

    post("/initiatives") {
        val body = call.receive<CreateRevenueInitiativeRequest>()
        RevenueInitiativeRepository.upsert(body.record)
        call.respond(HttpStatusCode.OK, body.record)
    }

    post("/predictions/run") {
        when (val result = runRevenueForecastPipeline(call.receive<ForecastRunRequest>().brief)) {
            is RevenueForecastPipelineResult.Success -> call.respond(
                HttpStatusCode.OK,
                ForecastRunResponse(
                    savedInitiativeIds = result.savedInitiativeIds,
                    rawMarkdown = result.rawMarkdown,
                    executionMode = result.executionMode,
                ),
            )

            is RevenueForecastPipelineResult.Failure -> call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to result.reason),
            )
        }
    }

    post("/predictions/{id}/audit") {
        val initiativeId = call.parameters["id"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing id"))
        val body = call.receive<PredictionAuditRequest>()
        when (
            val result = runAuditPredictionPipeline(
                initiativeId = initiativeId,
                actualRevenueUsd = body.actualRevenueUsd,
                influencedAccounts = body.influencedAccounts,
                performanceNotes = body.performanceNotes,
            )
        ) {
            is AuditPredictionPipelineResult.Success -> call.respond(
                HttpStatusCode.OK,
                PredictionAuditResponse(
                    updatedInitiative = result.updatedInitiative,
                    predictionAudit = result.audit,
                    updatedCalibrations = result.updatedCalibrations,
                    warnings = result.warnings,
                    executionMode = result.executionMode,
                ),
            )

            is AuditPredictionPipelineResult.Failure -> call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to result.reason),
            )
        }
    }

    get("/calibrations") {
        call.respond(PredictionLearningRepository.listCalibrations())
    }

    get("/audits") {
        call.respond(PredictionLearningRepository.listAudits())
    }
}

