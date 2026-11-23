package com.example.livecouth.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRefreshPolicyTest {
    private val lastLocation = LocationCoordinates(0f, 0f)

    @Test
    fun `refreshes when the query date changes`() {
        assertTrue(
            WeatherRefreshPolicy.shouldRefresh(
                lastQueryDate = "2026-11-21",
                lastQueryLocation = lastLocation,
                today = "2026-11-22",
                currentLocation = lastLocation,
            ),
        )
    }

    @Test
    fun `same day refreshes only after moving more than twenty five kilometers`() {
        assertFalse(
            WeatherRefreshPolicy.shouldRefresh(
                lastQueryDate = "2026-11-22",
                lastQueryLocation = lastLocation,
                today = "2026-11-22",
                currentLocation = LocationCoordinates(0f, 0.224f),
            ),
        )
        assertTrue(
            WeatherRefreshPolicy.shouldRefresh(
                lastQueryDate = "2026-11-22",
                lastQueryLocation = lastLocation,
                today = "2026-11-22",
                currentLocation = LocationCoordinates(0f, 0.225f),
            ),
        )
    }

    @Test
    fun `invalid current coordinates never trigger a request`() {
        assertFalse(
            WeatherRefreshPolicy.shouldRefresh(
                lastQueryDate = null,
                lastQueryLocation = null,
                today = "2026-11-22",
                currentLocation = LocationCoordinates(91f, 0f),
            ),
        )
    }
}
