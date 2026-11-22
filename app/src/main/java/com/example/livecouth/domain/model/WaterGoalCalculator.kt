package com.example.livecouth.domain.model

import kotlin.math.roundToInt

object WaterGoalCalculator {

    /**
     * Pure function to calculate daily water goal in ml.
     * Formula: peso * 35 ml + (300 ml if activity is MEDIO, 600 ml if ALTO).
     */
    fun calculateGoalMl(weightKg: Float, activityLevel: ActivityLevel): Int {
        if (weightKg <= 0f) return 2000

        val baseMl = weightKg * 35f
        val activityBonusMl = when (activityLevel) {
            ActivityLevel.BAJO -> 0
            ActivityLevel.MEDIO -> 300
            ActivityLevel.ALTO -> 600
        }

        return (baseMl + activityBonusMl).roundToInt()
    }
}
