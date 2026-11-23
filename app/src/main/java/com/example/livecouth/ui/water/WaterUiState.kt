package com.example.livecouth.ui.water

import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.domain.model.ClimateAdjustmentCalculator
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.DailyWeather
import com.example.livecouth.domain.model.ManualClimate
import com.example.livecouth.domain.model.SharkState

data class WaterUiState(
    val currentMl: Int = 0,
    val baseGoalMl: Int = 2000,
    val todayLogs: List<WaterLog> = emptyList(),
    val sharkState: SharkState = SharkState.THIRSTY,
    val showCustomAmountDialog: Boolean = false,
    val showQuickButtonsEditor: Boolean = false,
    val isWaterAddedAnimationTriggered: Boolean = false,
    val isCelebrationTriggered: Boolean = false,
    val cityName: String = CityLocation.MANTA.name,
    val weather: DailyWeather? = null,
    val isWeatherLoading: Boolean = true,
    val isWeatherUnavailable: Boolean = false,
    val isGoalReady: Boolean = false,
    val manualClimate: ManualClimate? = null,
    val quickAddButtons: List<QuickAddButton> = emptyList(),
) {
    val climateAdjustmentMl: Int
        get() = weather?.takeIf { it.isValid() }?.let {
            ClimateAdjustmentCalculator.calcularAjusteClima(
                it.maxTemperatureCelsius,
                it.maxHumidityPercent,
            )
        } ?: if (isWeatherUnavailable) {
            manualClimate?.adjustmentMl ?: 0
        } else {
            0
        }

    val goalMl: Int
        get() = baseGoalMl + climateAdjustmentMl

    val progress: Float
        get() = if (goalMl > 0) (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) else 0f
}
