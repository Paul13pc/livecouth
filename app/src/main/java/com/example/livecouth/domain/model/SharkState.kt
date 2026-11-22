package com.example.livecouth.domain.model

import com.example.livecouth.R

enum class SharkState(
    val minPercentage: Float,
    val maxPercentage: Float,
    val phraseResId: Int,
) {
    STRANDED(0f, 0.20f, R.string.shark_stranded),
    RECOVERING(0.20f, 0.50f, R.string.shark_recovering),
    SWIMMING(0.50f, 0.80f, R.string.shark_swimming),
    HAPPY(0.80f, 1.00f, R.string.shark_happy),
    CELEBRATING(1.00f, Float.MAX_VALUE, R.string.shark_celebrating);

    companion object {
        /**
         * Pure, testable function to calculate SharkState from current ml and goal ml.
         */
        fun calculateState(currentMl: Int, goalMl: Int): SharkState {
            if (goalMl <= 0) return STRANDED
            val progress = currentMl.toFloat() / goalMl.toFloat()
            return when {
                progress >= 1.0f -> CELEBRATING
                progress >= 0.80f -> HAPPY
                progress >= 0.50f -> SWIMMING
                progress >= 0.20f -> RECOVERING
                else -> STRANDED
            }
        }
    }
}
