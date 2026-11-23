package com.example.livecouth.domain.model

import com.example.livecouth.R

object ClimateAdjustmentCalculator {

    fun calcularAjusteClima(tempMax: Float, humedad: Int): Int {
        if (
            !tempMax.isFinite() ||
            tempMax !in -30f..60f ||
            humedad !in 0..100
        ) {
            return 0
        }
        val temperatureAdjustment = when {
            tempMax < 25f -> 0
            tempMax < 30f -> 250
            tempMax <= 35f -> 500
            else -> 750
        }
        return temperatureAdjustment + if (humedad > 80) 100 else 0
    }
}

enum class ManualClimate(val adjustmentMl: Int, val labelResId: Int) {
    TEMPERATE(0, R.string.manual_weather_temperate),
    HOT(250, R.string.manual_weather_hot),
    VERY_HOT(750, R.string.manual_weather_very_hot),
}
