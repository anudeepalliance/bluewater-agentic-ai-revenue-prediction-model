package com.bluewater.revenuepredictor.koog.auditPredictions

import com.bluewater.revenuepredictor.domain.ForecastCalibration
import com.bluewater.revenuepredictor.domain.PredictionAudit
import com.bluewater.revenuepredictor.domain.RevenueInitiative
import com.bluewater.revenuepredictor.repository.PredictionLearningRepository
import com.bluewater.revenuepredictor.repository.RevenueInitiativeRepository

internal object AuditPredictionPipelineDataAccess {
    suspend fun loadInitiative(id: String): RevenueInitiative? =
        RevenueInitiativeRepository.getById(id)

    suspend fun persistInitiative(initiative: RevenueInitiative) =
        RevenueInitiativeRepository.upsert(initiative)

    suspend fun loadCalibrations(initiative: RevenueInitiative): List<ForecastCalibration> =
        PredictionLearningRepository.listCalibrationsFor(initiative.solutionLines)

    suspend fun persistAudit(audit: PredictionAudit) =
        PredictionLearningRepository.persistAudit(audit)

    suspend fun persistCalibrations(calibrations: List<ForecastCalibration>) =
        calibrations.forEach { PredictionLearningRepository.persistCalibration(it) }
}

