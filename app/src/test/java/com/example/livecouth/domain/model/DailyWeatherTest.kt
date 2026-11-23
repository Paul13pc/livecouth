package com.example.livecouth.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWeatherTest {
    private val validWeather = DailyWeather(
        cityName = "Manta",
        latitude = -0.9677f,
        longitude = -80.7089f,
        date = "2026-11-22",
        maxTemperatureCelsius = 31f,
        maxHumidityPercent = 80,
    )

    @Test
    fun `accepts valid forecast values and boundaries`() {
        assertTrue(validWeather.isValid())
        assertTrue(validWeather.copy(maxTemperatureCelsius = -30f, maxHumidityPercent = 0).isValid())
        assertTrue(validWeather.copy(maxTemperatureCelsius = 60f, maxHumidityPercent = 100).isValid())
    }

    @Test
    fun `rejects invalid coordinates weather values and empty city`() {
        assertFalse(validWeather.copy(latitude = 91f).isValid())
        assertFalse(validWeather.copy(maxTemperatureCelsius = 60.1f).isValid())
        assertFalse(validWeather.copy(maxTemperatureCelsius = Float.NaN).isValid())
        assertFalse(validWeather.copy(maxHumidityPercent = 101).isValid())
        assertFalse(validWeather.copy(cityName = " ").isValid())
    }
}
