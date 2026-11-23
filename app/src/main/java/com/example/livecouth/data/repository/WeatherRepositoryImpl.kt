package com.example.livecouth.data.repository

import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.livecouth.R
import com.example.livecouth.data.location.FusedDeviceLocationProvider
import com.example.livecouth.data.remote.OpenMeteoApi
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.DailyWeather
import com.example.livecouth.domain.model.LocationCoordinates
import com.example.livecouth.domain.model.WeatherRefreshPolicy
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.domain.repository.WeatherRepository
import com.example.livecouth.domain.repository.WeatherResult
import com.google.gson.JsonParseException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: OpenMeteoApi,
    private val dataStore: DataStore<Preferences>,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceLocationProvider: FusedDeviceLocationProvider,
    @ApplicationContext private val context: Context,
) : WeatherRepository {
    private val requestMutex = Mutex()

    private object Keys {
        val LAST_ATTEMPT_DATE = stringPreferencesKey("weather_last_attempt_date")
        val LAST_ATTEMPT_LATITUDE = floatPreferencesKey("weather_last_attempt_latitude")
        val LAST_ATTEMPT_LONGITUDE = floatPreferencesKey("weather_last_attempt_longitude")
        val CACHED_CITY_NAME = stringPreferencesKey("weather_cached_city_name")
        val CACHED_LATITUDE = floatPreferencesKey("weather_cached_latitude")
        val CACHED_LONGITUDE = floatPreferencesKey("weather_cached_longitude")
        val CACHED_DATE = stringPreferencesKey("weather_cached_date")
        val CACHED_MAX_TEMPERATURE = floatPreferencesKey("weather_cached_max_temperature")
        val CACHED_MAX_HUMIDITY = floatPreferencesKey("weather_cached_max_humidity")
    }

    private data class WeatherTarget(
        val coordinates: LocationCoordinates,
        val isDeviceLocation: Boolean,
        val newlyResolvedCoordinates: LocationCoordinates? = null,
    )

    override suspend fun getTodayWeather(
        manualCity: CityLocation,
        useDeviceLocation: Boolean,
    ): WeatherResult = requestMutex.withLock {
        fetchTodayWeather(manualCity, useDeviceLocation)
    }

    private suspend fun fetchTodayWeather(
        manualCity: CityLocation,
        useDeviceLocation: Boolean,
    ): WeatherResult {
        val today = todayDate()
        val preferences = try {
            dataStore.data.first()
        } catch (_: IOException) {
            return WeatherResult.Offline(null, manualCity.name)
        }
        val cachedWeather = readCachedWeather(preferences)
        val lastAttemptCoordinates = LocationCoordinates(
            latitude = preferences[Keys.LAST_ATTEMPT_LATITUDE] ?: Float.NaN,
            longitude = preferences[Keys.LAST_ATTEMPT_LONGITUDE] ?: Float.NaN,
        ).takeIf { it.areValid() }
        val target = resolveTarget(
            manualCity = manualCity,
            useDeviceLocation = useDeviceLocation,
            today = today,
            lastQueryDate = preferences[Keys.LAST_ATTEMPT_DATE],
            lastQueryLocation = lastAttemptCoordinates,
        )
        val requestedCityName = if (target.isDeviceLocation) {
            resolveCityName(target.coordinates)
        } else {
            manualCity.name
        }
        val coordinates = target.coordinates
        if (!coordinates.areValid()) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        }
        if (
            !WeatherRefreshPolicy.shouldRefresh(
                lastQueryDate = preferences[Keys.LAST_ATTEMPT_DATE],
                lastQueryLocation = lastAttemptCoordinates,
                today = today,
                currentLocation = coordinates,
            )
        ) {
            val freshCache = cachedWeather?.takeIf {
                it.date == today &&
                    WeatherRefreshPolicy.distanceKm(
                        LocationCoordinates(it.latitude, it.longitude),
                        coordinates,
                    ) <= WeatherRefreshPolicy.MAX_DISTANCE_KM
            }
            return if (freshCache != null) {
                WeatherResult.Available(freshCache.copy(cityName = requestedCityName))
            } else {
                WeatherResult.Offline(cachedWeather, requestedCityName)
            }
        }

        try {
            dataStore.edit { current ->
                current[Keys.LAST_ATTEMPT_DATE] = today
                current[Keys.LAST_ATTEMPT_LATITUDE] = coordinates.latitude
                current[Keys.LAST_ATTEMPT_LONGITUDE] = coordinates.longitude
            }
            target.newlyResolvedCoordinates?.let {
                userPreferencesRepository.saveLastDeviceLocation(it)
            }
        } catch (_: IOException) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        }

        val response = try {
            api.getDailyForecast(coordinates.latitude, coordinates.longitude)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        } catch (_: HttpException) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        } catch (_: JsonParseException) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        }

        val daily = response.daily
        val date = daily?.time?.firstOrNull()
        val maxTemperature = daily?.maxTemperatureCelsius?.firstOrNull()
        val maxHumidity = daily?.maxHumidityPercent?.firstOrNull()
        if (
            date == null ||
            date != today ||
            maxTemperature == null ||
            !maxTemperature.isFinite() ||
            maxTemperature !in -30.0..60.0 ||
            maxHumidity == null ||
            !maxHumidity.isFinite() ||
            maxHumidity !in 0.0..100.0
        ) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        }

        val weather = DailyWeather(
            cityName = requestedCityName,
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            date = date,
            maxTemperatureCelsius = maxTemperature.toFloat(),
            maxHumidityPercent = maxHumidity.roundToInt(),
        )
        if (!weather.isValid()) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        }

        try {
            dataStore.edit { prefs ->
                prefs[Keys.CACHED_CITY_NAME] = weather.cityName
                prefs[Keys.CACHED_LATITUDE] = weather.latitude
                prefs[Keys.CACHED_LONGITUDE] = weather.longitude
                prefs[Keys.CACHED_DATE] = weather.date
                prefs[Keys.CACHED_MAX_TEMPERATURE] = weather.maxTemperatureCelsius
                prefs[Keys.CACHED_MAX_HUMIDITY] = weather.maxHumidityPercent.toFloat()
            }
        } catch (_: IOException) {
            return WeatherResult.Offline(cachedWeather, requestedCityName)
        }
        return WeatherResult.Available(weather)
    }

    private suspend fun resolveTarget(
        manualCity: CityLocation,
        useDeviceLocation: Boolean,
        today: String,
        lastQueryDate: String?,
        lastQueryLocation: LocationCoordinates?,
    ): WeatherTarget {
        if (!useDeviceLocation) {
            return WeatherTarget(
                coordinates = LocationCoordinates(manualCity.latitude, manualCity.longitude),
                isDeviceLocation = false,
            )
        }
        val savedLocation = try {
            userPreferencesRepository.getLastDeviceLocation()
        } catch (_: IOException) {
            null
        }
        val lastKnownLocation = deviceLocationProvider.getLastKnownLocation()
        val shouldRefreshDailyLocation =
            lastQueryDate != today || lastQueryLocation == null || savedLocation == null
        val movedMoreThanThreshold = lastKnownLocation != null &&
            lastQueryLocation != null &&
            WeatherRefreshPolicy.distanceKm(lastQueryLocation, lastKnownLocation) >
            WeatherRefreshPolicy.MAX_DISTANCE_KM
        val currentLocation = if (shouldRefreshDailyLocation) {
            deviceLocationProvider.getCurrentLocation()
        } else {
            null
        }
        val selectedLocation = when {
            currentLocation != null -> currentLocation
            movedMoreThanThreshold -> lastKnownLocation
            else -> savedLocation ?: lastQueryLocation ?: lastKnownLocation
        }
        return if (selectedLocation == null) {
            WeatherTarget(
                coordinates = LocationCoordinates(manualCity.latitude, manualCity.longitude),
                isDeviceLocation = false,
            )
        } else {
            WeatherTarget(
                coordinates = selectedLocation,
                isDeviceLocation = true,
                newlyResolvedCoordinates = currentLocation ?: lastKnownLocation.takeIf {
                    movedMoreThanThreshold
                },
            )
        }
    }

    private suspend fun resolveCityName(coordinates: LocationCoordinates): String =
        withContext(Dispatchers.IO) {
            try {
                val address = Geocoder(context, Locale.getDefault())
                    .getFromLocation(
                        coordinates.latitude.toDouble(),
                        coordinates.longitude.toDouble(),
                        1,
                    )
                    ?.firstOrNull()
                sequenceOf(
                    address?.locality,
                    address?.subAdminArea,
                    address?.adminArea,
                ).firstOrNull { !it.isNullOrBlank() }
                    ?: context.getString(R.string.climate_location_city_fallback)
            } catch (_: IOException) {
                context.getString(R.string.climate_location_city_fallback)
            } catch (_: IllegalArgumentException) {
                context.getString(R.string.climate_location_city_fallback)
            } catch (_: IllegalStateException) {
                context.getString(R.string.climate_location_city_fallback)
            }
        }

    private fun readCachedWeather(preferences: Preferences): DailyWeather? {
        val cityName = preferences[Keys.CACHED_CITY_NAME] ?: return null
        val humidity = preferences[Keys.CACHED_MAX_HUMIDITY] ?: return null
        if (!humidity.isFinite() || humidity !in 0f..100f || humidity % 1f != 0f) return null
        return DailyWeather(
            cityName = cityName,
            latitude = preferences[Keys.CACHED_LATITUDE] ?: return null,
            longitude = preferences[Keys.CACHED_LONGITUDE] ?: return null,
            date = preferences[Keys.CACHED_DATE] ?: return null,
            maxTemperatureCelsius = preferences[Keys.CACHED_MAX_TEMPERATURE] ?: return null,
            maxHumidityPercent = humidity.toInt(),
        ).takeIf { it.isValid() }
    }

    private fun todayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
