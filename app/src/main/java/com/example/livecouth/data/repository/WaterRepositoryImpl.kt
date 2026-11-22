package com.example.livecouth.data.repository

import com.example.livecouth.data.local.dao.WaterLogDao
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val waterLogDao: WaterLogDao,
) : WaterRepository {

    override suspend fun insertWaterLog(waterLog: WaterLog): Long {
        return waterLogDao.insertWaterLog(waterLog)
    }

    override suspend fun deleteWaterLog(waterLog: WaterLog) {
        waterLogDao.deleteWaterLog(waterLog)
    }

    override fun getTodayLogs(): Flow<List<WaterLog>> {
        val (start, end) = getTodayTimeRange()
        return waterLogDao.getTodayLogs(start, end)
    }

    override fun getTodayTotalMl(): Flow<Int> {
        val (start, end) = getTodayTimeRange()
        return waterLogDao.getTodayTotalMl(start, end).map { total ->
            total ?: 0
        }
    }

    override fun getLogsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>> {
        return waterLogDao.getLogsBetween(startTimestamp, endTimestamp)
    }

    private fun getTodayTimeRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }
}
