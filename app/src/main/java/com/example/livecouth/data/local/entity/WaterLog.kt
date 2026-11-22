package com.example.livecouth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cantidadMl: Int,
    val fechaHora: Long = System.currentTimeMillis(),
)
