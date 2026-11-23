package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HydrationCalculatorTest {

    @Test
    fun `hydration factor determines credited milliliters`() {
        assertEquals(500, HydrationCalculator.calculateHydratedMl(500, DrinkType.WATER.defaultHydrationFactor))
        assertEquals(450, HydrationCalculator.calculateHydratedMl(500, DrinkType.TEA.defaultHydrationFactor))
        assertEquals(400, HydrationCalculator.calculateHydratedMl(500, DrinkType.JUICE.defaultHydrationFactor))
        assertEquals(350, HydrationCalculator.calculateHydratedMl(500, DrinkType.COFFEE.defaultHydrationFactor))
        assertEquals(275, HydrationCalculator.calculateHydratedMl(500, 0.55f))
    }

    @Test
    fun `invalid amount or factor contributes no hydration`() {
        assertEquals(0, HydrationCalculator.calculateHydratedMl(0, 1f))
        assertEquals(0, HydrationCalculator.calculateHydratedMl(250, -0.5f))
        assertEquals(0, HydrationCalculator.calculateHydratedMl(250, Float.NaN))
    }
}
