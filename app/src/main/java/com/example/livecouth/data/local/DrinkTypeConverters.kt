package com.example.livecouth.data.local

import androidx.room.TypeConverter
import com.example.livecouth.domain.model.DrinkType

class DrinkTypeConverters {
    @TypeConverter
    fun toDrinkType(value: String): DrinkType = DrinkType.valueOf(value)

    @TypeConverter
    fun fromDrinkType(value: DrinkType): String = value.name
}
