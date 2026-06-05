package com.bluewater.revenuepredictor.repository

import com.bluewater.revenuepredictor.domain.ForecastCalibration
import com.bluewater.revenuepredictor.domain.PredictionAudit
import com.bluewater.revenuepredictor.domain.SolutionLine
import com.bluewater.revenuepredictor.mongo.MockMongoDatabase

object PredictionLearningRepository {
    suspend fun listAudits(): List<PredictionAudit> =
        MockMongoDatabase.audits.values.sortedByDescending { it.createdAtEpochMs }

    suspend fun listCalibrations(): List<ForecastCalibration> =
        MockMongoDatabase.calibrations.values.sortedByDescending { it.updatedAtEpochMs }

    suspend fun listCalibrationsFor(solutionLines: List<SolutionLine>): List<ForecastCalibration> =
        listCalibrations().filter { calibration -> calibration.solutionLine in solutionLines }

    suspend fun persistAudit(audit: PredictionAudit) {
        MockMongoDatabase.audits[audit.id] = audit
    }

    suspend fun persistCalibration(calibration: ForecastCalibration) {
        MockMongoDatabase.calibrations[calibration.id] = calibration
    }
}

