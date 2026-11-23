package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SharkStateTest {

    @Test
    fun `percentage selects thirsty below 30 percent`() {
        assertEquals(SharkState.THIRSTY, SharkState.calculateStateFromPercentage(0f))
        assertEquals(SharkState.THIRSTY, SharkState.calculateStateFromPercentage(0.299f))
    }

    @Test
    fun `percentage selects jumping from 30 to under 70 percent`() {
        assertEquals(SharkState.JUMPING, SharkState.calculateStateFromPercentage(0.30f))
        assertEquals(SharkState.JUMPING, SharkState.calculateStateFromPercentage(0.699f))
    }

    @Test
    fun `percentage selects free swimming from 70 percent including excess`() {
        assertEquals(SharkState.FREE_SWIMMING, SharkState.calculateStateFromPercentage(0.70f))
        assertEquals(SharkState.FREE_SWIMMING, SharkState.calculateStateFromPercentage(1f))
        assertEquals(SharkState.FREE_SWIMMING, SharkState.calculateStateFromPercentage(1.5f))
    }

    @Test
    fun `calculateState uses goal percentage and handles invalid goals`() {
        assertEquals(SharkState.THIRSTY, SharkState.calculateState(500, 0))
        assertEquals(SharkState.THIRSTY, SharkState.calculateState(500, -100))
        assertEquals(SharkState.JUMPING, SharkState.calculateState(600, 2000))
        assertEquals(SharkState.FREE_SWIMMING, SharkState.calculateState(2000, 2000))
    }
}
