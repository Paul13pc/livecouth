package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SharkStateTest {

    @Test
    fun `calculateState returns STRANDED when progress is under 20 percent`() {
        assertEquals(SharkState.STRANDED, SharkState.calculateState(0, 2000))
        assertEquals(SharkState.STRANDED, SharkState.calculateState(399, 2000))
    }

    @Test
    fun `calculateState returns RECOVERING when progress is between 20 and 50 percent`() {
        assertEquals(SharkState.RECOVERING, SharkState.calculateState(400, 2000))
        assertEquals(SharkState.RECOVERING, SharkState.calculateState(999, 2000))
    }

    @Test
    fun `calculateState returns SWIMMING when progress is between 50 and 80 percent`() {
        assertEquals(SharkState.SWIMMING, SharkState.calculateState(1000, 2000))
        assertEquals(SharkState.SWIMMING, SharkState.calculateState(1599, 2000))
    }

    @Test
    fun `calculateState returns HAPPY when progress is between 80 and 100 percent`() {
        assertEquals(SharkState.HAPPY, SharkState.calculateState(1600, 2000))
        assertEquals(SharkState.HAPPY, SharkState.calculateState(1999, 2000))
    }

    @Test
    fun `calculateState returns CELEBRATING when progress is 100 percent or more`() {
        assertEquals(SharkState.CELEBRATING, SharkState.calculateState(2000, 2000))
        assertEquals(SharkState.CELEBRATING, SharkState.calculateState(3000, 2000))
    }

    @Test
    fun `calculateState handles zero or negative goal gracefully`() {
        assertEquals(SharkState.STRANDED, SharkState.calculateState(500, 0))
        assertEquals(SharkState.STRANDED, SharkState.calculateState(500, -100))
    }
}
