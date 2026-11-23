package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSettingsTest {

    @Test
    fun activeWindowMustEndAfterItStarts() {
        assertNull(ReminderSettingsValidator.validate(ReminderSettings()))
        assertEquals(
            ReminderSettingsError.INVALID_ACTIVE_WINDOW,
            ReminderSettingsValidator.validate(
                ReminderSettings(startMinute = 21 * 60, endMinute = 8 * 60),
            ),
        )
    }

    @Test
    fun customFrequencyRequiresAtLeastThirtyMinutes() {
        assertEquals(
            ReminderSettingsError.INVALID_INTERVAL,
            ReminderSettingsValidator.validate(
                ReminderSettings(
                    frequency = ReminderFrequency.CUSTOM,
                    customIntervalMinutes = 29,
                ),
            ),
        )
        assertNull(
            ReminderSettingsValidator.validate(
                ReminderSettings(
                    frequency = ReminderFrequency.CUSTOM,
                    customIntervalMinutes = 30,
                ),
            ),
        )
    }

    @Test
    fun quietHoursCanCrossMidnight() {
        val settings = ReminderSettings(
            quietHoursEnabled = true,
            quietStartMinute = 22 * 60,
            quietEndMinute = 7 * 60,
        )

        assertTrue(ReminderSettingsValidator.isQuietTime(settings, 23 * 60))
        assertTrue(ReminderSettingsValidator.isQuietTime(settings, 6 * 60 + 59))
        assertFalse(ReminderSettingsValidator.isQuietTime(settings, 7 * 60))
        assertFalse(ReminderSettingsValidator.isQuietTime(settings, 12 * 60))
    }

    @Test
    fun quietHoursMustHaveDifferentTimes() {
        assertEquals(
            ReminderSettingsError.INVALID_QUIET_WINDOW,
            ReminderSettingsValidator.validate(
                ReminderSettings(
                    quietHoursEnabled = true,
                    quietStartMinute = 8 * 60,
                    quietEndMinute = 8 * 60,
                ),
            ),
        )
    }

    @Test
    fun activeDayMaskUsesMondayFirstOrder() {
        val settings = ReminderSettings(activeDaysMask = (1 shl 0) or (1 shl 6))

        assertTrue(ReminderSettingsValidator.isDayActive(settings, 0))
        assertFalse(ReminderSettingsValidator.isDayActive(settings, 1))
        assertTrue(ReminderSettingsValidator.isDayActive(settings, 6))
        assertFalse(ReminderSettingsValidator.isDayActive(settings, 7))
    }
}
