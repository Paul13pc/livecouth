package com.example.livecouth.domain.model

data class DailyWeather(
    val cityName: String,
    val latitude: Float,
    val longitude: Float,
    val date: String,
    val maxTemperatureCelsius: Float,
    val maxHumidityPercent: Int,
) {
    fun isValid(): Boolean =
        cityName.isNotBlank() &&
            LocationCoordinates(latitude, longitude).areValid() &&
            date.isNotBlank() &&
            maxTemperatureCelsius.isFinite() &&
            maxTemperatureCelsius in -30f..60f &&
            maxHumidityPercent in 0..100
}
