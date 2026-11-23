package com.example.livecouth.domain.model

data class LocationCoordinates(
    val latitude: Float,
    val longitude: Float,
) {
    fun areValid(): Boolean =
        latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90f..90f &&
            longitude in -180f..180f
}
