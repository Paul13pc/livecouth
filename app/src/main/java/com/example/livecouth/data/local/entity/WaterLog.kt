package com.example.livecouth.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.livecouth.domain.model.DrinkType
import com.example.livecouth.domain.model.HydrationCalculator
import java.util.UUID

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["syncId"], unique = true)],
)
data class WaterLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cantidadMl: Int,
    val fechaHora: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'WATER'")
    val drinkType: DrinkType = DrinkType.WATER,
    @ColumnInfo(defaultValue = "1.0")
    val hydrationFactor: Float = DrinkType.WATER.defaultHydrationFactor,
    @ColumnInfo(defaultValue = "''")
    val syncId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val sincronizado: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val eliminado: Boolean = false,
) {
    @get:Ignore
    val hydratedMl: Int
        get() = HydrationCalculator.calculateHydratedMl(cantidadMl, hydrationFactor)
}
