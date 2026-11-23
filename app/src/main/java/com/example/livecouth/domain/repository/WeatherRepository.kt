package com.example.livecouth.domain.repository

import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.DailyWeather

interface WeatherRepository {
    suspend fun getTodayWeather(
        manualCity: CityLocation,
        useDeviceLocation: Boolean,
    ): WeatherResult
}

sealed interface WeatherResult {
    data class Available(val weather: DailyWeather) : WeatherResult
    data class Offline(
        val cachedWeather: DailyWeather?,
        val requestedCityName: String,
    ) : WeatherResult
}
