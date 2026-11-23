package com.example.livecouth.domain.model

data class CityLocation(
    val name: String,
    val latitude: Float,
    val longitude: Float,
) {
    companion object {
        val MANTA = CityLocation(
            name = "Manta, Ecuador",
            latitude = -0.9677f,
            longitude = -80.7089f,
        )
        val QUITO = CityLocation(
            name = "Quito, Ecuador",
            latitude = -0.1807f,
            longitude = -78.4678f,
        )
        val GUAYAQUIL = CityLocation(
            name = "Guayaquil, Ecuador",
            latitude = -2.1709f,
            longitude = -79.9224f,
        )
        val CUENCA = CityLocation(
            name = "Cuenca, Ecuador",
            latitude = -2.9001f,
            longitude = -79.0059f,
        )

        fun supportedCity(name: String): CityLocation? =
            listOf(MANTA, QUITO, GUAYAQUIL, CUENCA)
                .firstOrNull {
                    it.name.equals(name.trim(), ignoreCase = true) ||
                        it.name.substringBefore(',').equals(name.trim(), ignoreCase = true)
                }
    }

    fun hasValidCoordinates(): Boolean =
        latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90f..90f &&
            longitude in -180f..180f
}
