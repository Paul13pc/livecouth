package com.example.livecouth.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveReminderCalculatorTest {

    @Test
    fun `shouldNotify returns false if outside active hours`() {
        // Current hour 7 AM, active window 8 AM - 21 PM
        assertFalse(AdaptiveReminderCalculator.shouldNotify(currentMl = 0, goalMl = 2000, currentHour = 7, startHour = 8, endHour = 21))
        // Current hour 22 PM
        assertFalse(AdaptiveReminderCalculator.shouldNotify(currentMl = 0, goalMl = 2000, currentHour = 22, startHour = 8, endHour = 21))
    }

    @Test
    fun `shouldNotify returns false if goal already reached`() {
        assertFalse(AdaptiveReminderCalculator.shouldNotify(currentMl = 2000, goalMl = 2000, currentHour = 12, startHour = 8, endHour = 21))
        assertFalse(AdaptiveReminderCalculator.shouldNotify(currentMl = 2500, goalMl = 2000, currentHour = 14, startHour = 8, endHour = 21))
    }

    @Test
    fun `shouldNotify returns true when behind schedule`() {
        // At 14:00 (midday in 8..21 window = ~46% expected ~ 920 ml). Current ml = 200 ml.
        assertTrue(AdaptiveReminderCalculator.shouldNotify(currentMl = 200, goalMl = 2000, currentHour = 14, startHour = 8, endHour = 21))
    }

    @Test
    fun `shouldNotify returns false when on track`() {
        // At 14:00 (midday ~ 920 ml expected). Current ml = 1000 ml.
        assertFalse(AdaptiveReminderCalculator.shouldNotify(currentMl = 1000, goalMl = 2000, currentHour = 14, startHour = 8, endHour = 21))
    }

    @Test
    fun minutePrecisionRespectsTheActiveWindowAndExpectedProgress() {
        assertTrue(
            AdaptiveReminderCalculator.shouldNotifyAtMinute(
                currentMl = 200,
                goalMl = 2000,
                currentMinute = 14 * 60 + 15,
                startMinute = 8 * 60 + 30,
                endMinute = 21 * 60 + 30,
            ),
        )
        assertFalse(
            AdaptiveReminderCalculator.shouldNotifyAtMinute(
                currentMl = 0,
                goalMl = 2000,
                currentMinute = 21 * 60 + 30,
                startMinute = 8 * 60 + 30,
                endMinute = 21 * 60 + 30,
            ),
        )
    }
}
