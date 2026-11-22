package com.example.livecouth.domain.repository

import com.example.livecouth.data.local.entity.WaterLog
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    suspend fun insertWaterLog(waterLog: WaterLog): Long
    suspend fun deleteWaterLog(waterLog: WaterLog)
    fun getTodayLogs(): Flow<List<WaterLog>>
    fun getTodayTotalMl(): Flow<Int>
    fun getLogsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>>
}
