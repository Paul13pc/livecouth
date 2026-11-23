package com.example.livecouth.domain.model

import com.example.livecouth.R

enum class DrinkType(
    val defaultHydrationFactor: Float,
    val labelResId: Int,
) {
    WATER(1.0f, R.string.drink_type_water),
    TEA(0.9f, R.string.drink_type_tea),
    JUICE(0.8f, R.string.drink_type_juice),
    COFFEE(0.7f, R.string.drink_type_coffee),
    OTHER(1.0f, R.string.drink_type_other),
}
