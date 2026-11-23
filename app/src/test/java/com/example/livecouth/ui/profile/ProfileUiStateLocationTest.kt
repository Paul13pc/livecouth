package com.example.livecouth.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileUiStateLocationTest {
    @Test
    fun `permission denial switches back to manual climate mode`() {
        val initialState = ProfileUiState(
            usesDeviceLocationForClimate = true,
            isResolvingDeviceLocation = true,
        )

        val denied = initialState.withManualClimateLocation(DeviceLocationNotice.PERMISSION_DENIED)
        val permanentlyDenied = initialState.withManualClimateLocation(
            DeviceLocationNotice.PERMISSION_PERMANENTLY_DENIED,
        )

        assertFalse(denied.usesDeviceLocationForClimate)
        assertFalse(denied.isResolvingDeviceLocation)
        assertEquals(DeviceLocationNotice.PERMISSION_DENIED, denied.deviceLocationNotice)
        assertFalse(permanentlyDenied.usesDeviceLocationForClimate)
        assertEquals(
            DeviceLocationNotice.PERMISSION_PERMANENTLY_DENIED,
            permanentlyDenied.deviceLocationNotice,
        )
    }
}
