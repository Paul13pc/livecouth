package com.example.livecouth.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Float,
        @Query("longitude") longitude: Float,
        @Query("daily") daily: String = "temperature_2m_max,relative_humidity_2m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1,
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val daily: OpenMeteoDaily? = null,
)

data class OpenMeteoDaily(
    val time: List<String>? = null,
    @SerializedName("temperature_2m_max")
    val maxTemperatureCelsius: List<Double?>? = null,
    @SerializedName("relative_humidity_2m_max")
    val maxHumidityPercent: List<Double?>? = null,
)
