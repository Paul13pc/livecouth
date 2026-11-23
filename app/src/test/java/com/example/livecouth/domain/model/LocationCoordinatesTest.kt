package com.example.livecouth.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCoordinatesTest {
    @Test
    fun `accepts coordinate range boundaries`() {
        assertTrue(LocationCoordinates(-90f, -180f).areValid())
        assertTrue(LocationCoordinates(90f, 180f).areValid())
    }

    @Test
    fun `rejects out of range and non finite coordinates`() {
        assertFalse(LocationCoordinates(-90.1f, 0f).areValid())
        assertFalse(LocationCoordinates(0f, 180.1f).areValid())
        assertFalse(LocationCoordinates(Float.NaN, 0f).areValid())
        assertFalse(LocationCoordinates(0f, Float.POSITIVE_INFINITY).areValid())
    }
}
