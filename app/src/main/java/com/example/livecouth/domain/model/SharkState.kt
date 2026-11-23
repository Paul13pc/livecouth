package com.example.livecouth.domain.model

import com.example.livecouth.R

enum class SharkState(
    val phraseResId: Int,
) {
    THIRSTY(R.string.shark_stranded),
    JUMPING(R.string.shark_recovering),
    FREE_SWIMMING(R.string.shark_swimming);

    companion object {
        fun calculateStateFromPercentage(percentage: Float): SharkState {
            val progress = percentage.coerceIn(0f, 1f)
            return when {
                progress < 0.30f -> THIRSTY
                progress < 0.70f -> JUMPING
                else -> FREE_SWIMMING
            }
        }

        fun calculateState(currentMl: Int, goalMl: Int): SharkState {
            if (goalMl <= 0) return THIRSTY
            val progress = currentMl.toFloat() / goalMl.toFloat()
            return calculateStateFromPercentage(progress)
        }
    }
}
