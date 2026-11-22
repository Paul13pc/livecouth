package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WaterGoalCalculatorTest {

    @Test
    fun `calculateGoalMl with BAJO activity level`() {
        // 70 kg * 35 = 2450 ml
        val goal = WaterGoalCalculator.calculateGoalMl(70f, ActivityLevel.BAJO)
        assertEquals(2450, goal)
    }

    @Test
    fun `calculateGoalMl with MEDIO activity level adds 300 ml`() {
        // 70 kg * 35 = 2450 + 300 = 2750 ml
        val goal = WaterGoalCalculator.calculateGoalMl(70f, ActivityLevel.MEDIO)
        assertEquals(2750, goal)
    }

    @Test
    fun `calculateGoalMl with ALTO activity level adds 600 ml`() {
        // 70 kg * 35 = 2450 + 600 = 3050 ml
        val goal = WaterGoalCalculator.calculateGoalMl(70f, ActivityLevel.ALTO)
        assertEquals(3050, goal)
    }

    @Test
    fun `calculateGoalMl returns default 2000 for invalid weight`() {
        val goalZero = WaterGoalCalculator.calculateGoalMl(0f, ActivityLevel.MEDIO)
        assertEquals(2000, goalZero)

        val goalNegative = WaterGoalCalculator.calculateGoalMl(-10f, ActivityLevel.ALTO)
        assertEquals(2000, goalNegative)
    }
}
