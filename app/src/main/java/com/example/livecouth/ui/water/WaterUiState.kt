package com.example.livecouth.ui.water

import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.SharkState

data class WaterUiState(
    val currentMl: Int = 0,
    val goalMl: Int = 2000,
    val todayLogs: List<WaterLog> = emptyList(),
    val sharkState: SharkState = SharkState.STRANDED,
    val showCustomAmountDialog: Boolean = false,
    val isWaterAddedAnimationTriggered: Boolean = false,
) {
    val progress: Float
        get() = if (goalMl > 0) (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1.5f) else 0f
}
