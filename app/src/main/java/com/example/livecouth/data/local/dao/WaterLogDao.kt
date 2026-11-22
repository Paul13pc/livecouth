package com.example.livecouth.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.livecouth.data.local.entity.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog): Long

    @Delete
    suspend fun deleteWaterLog(waterLog: WaterLog)

    @Query("SELECT * FROM water_logs WHERE fechaHora BETWEEN :startTimestamp AND :endTimestamp ORDER BY fechaHora DESC")
    fun getTodayLogs(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>>

    @Query("SELECT SUM(cantidadMl) FROM water_logs WHERE fechaHora BETWEEN :startTimestamp AND :endTimestamp")
    fun getTodayTotalMl(startTimestamp: Long, endTimestamp: Long): Flow<Int?>

    @Query("SELECT * FROM water_logs WHERE fechaHora BETWEEN :startTimestamp AND :endTimestamp ORDER BY fechaHora ASC")
    fun getLogsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>>
}
