package com.bluewater.revenuepredictor.repository

import com.bluewater.revenuepredictor.domain.RevenueInitiative
import com.bluewater.revenuepredictor.mongo.MockMongoDatabase

object RevenueInitiativeRepository {
    suspend fun list(): List<RevenueInitiative> =
        MockMongoDatabase.initiatives.values.sortedByDescending { it.createdAtEpochMs }

    suspend fun getById(id: String): RevenueInitiative? =
        MockMongoDatabase.initiatives[id]

    suspend fun upsert(record: RevenueInitiative) {
        MockMongoDatabase.initiatives[record.id] = record
    }
}

