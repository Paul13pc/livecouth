package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ClimateAdjustmentCalculatorTest {

    @Test
    fun `temperature bands include exact boundaries`() {
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(24.99f, 80))
        assertEquals(250, ClimateAdjustmentCalculator.calcularAjusteClima(25f, 80))
        assertEquals(250, ClimateAdjustmentCalculator.calcularAjusteClima(29.99f, 80))
        assertEquals(500, ClimateAdjustmentCalculator.calcularAjusteClima(30f, 80))
        assertEquals(500, ClimateAdjustmentCalculator.calcularAjusteClima(35f, 80))
        assertEquals(750, ClimateAdjustmentCalculator.calcularAjusteClima(35.01f, 80))
    }

    @Test
    fun `humidity adds one hundred only above eighty percent`() {
        assertEquals(250, ClimateAdjustmentCalculator.calcularAjusteClima(25f, 80))
        assertEquals(350, ClimateAdjustmentCalculator.calcularAjusteClima(25f, 81))
        assertEquals(100, ClimateAdjustmentCalculator.calcularAjusteClima(20f, 100))
    }

    @Test
    fun `non finite temperature has no temperature adjustment`() {
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(Float.NaN, 80))
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(Float.NaN, 81))
    }

    @Test
    fun `out of range weather values produce no adjustment`() {
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(-31f, 90))
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(61f, 90))
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(30f, -1))
        assertEquals(0, ClimateAdjustmentCalculator.calcularAjusteClima(30f, 101))
    }
}
