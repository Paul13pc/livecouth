package com.example.livecouth.domain.model

object WeatherRefreshPolicy {
    private const val EARTH_RADIUS_KM = 6371.0
    const val MAX_DISTANCE_KM = 25.0

    fun shouldRefresh(
        lastQueryDate: String?,
        lastQueryLocation: LocationCoordinates?,
        today: String,
        currentLocation: LocationCoordinates,
    ): Boolean {
        if (!currentLocation.areValid()) return false
        if (lastQueryDate != today) return true
        if (lastQueryLocation == null || !lastQueryLocation.areValid()) return true
        return distanceKm(lastQueryLocation, currentLocation) > MAX_DISTANCE_KM
    }

    fun distanceKm(first: LocationCoordinates, second: LocationCoordinates): Double {
        val firstLatitude = Math.toRadians(first.latitude.toDouble())
        val secondLatitude = Math.toRadians(second.latitude.toDouble())
        val latitudeDelta = secondLatitude - firstLatitude
        val longitudeDelta = Math.toRadians(
            (second.longitude - first.longitude).toDouble(),
        )
        val haversine = sinSquared(latitudeDelta / 2.0) +
            kotlin.math.cos(firstLatitude) * kotlin.math.cos(secondLatitude) *
            sinSquared(longitudeDelta / 2.0)
        return 2.0 * EARTH_RADIUS_KM * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    private fun sinSquared(value: Double): Double {
        val sine = kotlin.math.sin(value)
        return sine * sine
    }
}
