package com.example.livecouth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.livecouth.data.local.entity.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog): Long

    @Update
    suspend fun updateWaterLog(waterLog: WaterLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemote(waterLog: WaterLog)

    @Query("SELECT * FROM water_logs WHERE syncId = :syncId LIMIT 1")
    suspend fun findBySyncId(syncId: String): WaterLog?

    @Query("SELECT * FROM water_logs")
    suspend fun getAllForSync(): List<WaterLog>

    @Query("SELECT COUNT(*) FROM water_logs")
    suspend fun countAll(): Int

    @Query("UPDATE water_logs SET sincronizado = 1 WHERE syncId = :syncId AND updatedAt = :updatedAt")
    suspend fun markSynced(syncId: String, updatedAt: Long)

    @Query("DELETE FROM water_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM water_logs WHERE eliminado = 0 AND fechaHora BETWEEN :startTimestamp AND :endTimestamp ORDER BY fechaHora DESC")
    fun getTodayLogs(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>>

    @Query("SELECT CAST(SUM(ROUND(cantidadMl * hydrationFactor)) AS INTEGER) FROM water_logs WHERE eliminado = 0 AND fechaHora BETWEEN :startTimestamp AND :endTimestamp")
    fun getTodayTotalMl(startTimestamp: Long, endTimestamp: Long): Flow<Int?>

    @Query("SELECT * FROM water_logs WHERE eliminado = 0 AND fechaHora BETWEEN :startTimestamp AND :endTimestamp ORDER BY fechaHora ASC")
    fun getLogsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLog>>
}
