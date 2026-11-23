package com.example.livecouth.domain.model

import kotlin.math.roundToInt

object HydrationCalculator {
    fun calculateHydratedMl(amountMl: Int, hydrationFactor: Float): Int {
        if (amountMl <= 0 || !hydrationFactor.isFinite() || hydrationFactor < 0f) return 0
        return (amountMl * hydrationFactor).roundToInt()
    }
}
