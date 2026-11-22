package com.example.livecouth.domain.model

object AdaptiveReminderCalculator {

    /**
     * Pure function to determine if a water reminder notification should be sent.
     *
     * @param currentMl Total ml consumed today.
     * @param goalMl User's daily water goal.
     * @param currentHour Hour of the day (0..23).
     * @param startHour Start hour for reminders (e.g. 8).
     * @param endHour End hour for reminders (e.g. 21).
     * @return true if currentMl is behind the expected target for currentHour.
     */
    fun shouldNotify(
        currentMl: Int,
        goalMl: Int,
        currentHour: Int,
        startHour: Int = 8,
        endHour: Int = 21,
    ): Boolean {
        // If outside active window, don't notify
        if (currentHour < startHour || currentHour > endHour) return false

        // If goal already reached or exceeded, don't notify
        if (currentMl >= goalMl) return false

        val totalActiveHours = (endHour - startHour).coerceAtLeast(1)
        val elapsedActiveHours = (currentHour - startHour).coerceIn(0, totalActiveHours)

        // Expected progress ratio at this time of day
        val expectedRatio = elapsedActiveHours.toFloat() / totalActiveHours.toFloat()
        val expectedMl = goalMl * expectedRatio

        // Notify if user is behind schedule by at least 100 ml margin
        val toleranceMarginMl = 100
        return currentMl < (expectedMl - toleranceMarginMl)
    }
}
