package com.bluewater.revenuepredictor.mongo

import com.bluewater.revenuepredictor.config.LocalConfig
import com.bluewater.revenuepredictor.domain.ForecastCalibration
import com.bluewater.revenuepredictor.domain.PredictionAudit
import com.bluewater.revenuepredictor.domain.RevenueInitiative
import org.slf4j.LoggerFactory

object MockMongoDatabase {
    private val logger = LoggerFactory.getLogger(MockMongoDatabase::class.java)

    val initiatives = linkedMapOf<String, RevenueInitiative>()
    val audits = linkedMapOf<String, PredictionAudit>()
    val calibrations = linkedMapOf<String, ForecastCalibration>()

    suspend fun pingOrPretend() {
        logger.info(
            "Mongo ready (mock mode): uri={} database={}",
            LocalConfig.mongoUri,
            LocalConfig.mongoDatabase,
        )
    }
}

