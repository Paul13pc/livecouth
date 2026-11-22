package com.example.livecouth.domain.model

data class UserProfile(
    val name: String = "",
    val weightKg: Float = 70f,
    val age: Int = 25,
    val activityLevel: ActivityLevel = ActivityLevel.MEDIO,
    val isOnboardingCompleted: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderStartHour: Int = 8,
    val reminderEndHour: Int = 21,
) {
    val dailyGoalMl: Int
        get() = WaterGoalCalculator.calculateGoalMl(weightKg, activityLevel)
}
