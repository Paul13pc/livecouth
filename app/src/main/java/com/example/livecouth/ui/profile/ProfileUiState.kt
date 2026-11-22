package com.example.livecouth.ui.profile

import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.WaterGoalCalculator

data class ProfileUiState(
    val name: String = "",
    val weightText: String = "70",
    val ageText: String = "25",
    val activityLevel: ActivityLevel = ActivityLevel.MEDIO,
    val reminderEnabled: Boolean = true,
    val reminderStartHour: Int = 8,
    val reminderEndHour: Int = 21,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
) {
    val weightKg: Float
        get() = weightText.toFloatOrNull() ?: 0f

    val age: Int
        get() = ageText.toIntOrNull() ?: 0

    val calculatedGoalMl: Int
        get() = WaterGoalCalculator.calculateGoalMl(weightKg, activityLevel)
}
