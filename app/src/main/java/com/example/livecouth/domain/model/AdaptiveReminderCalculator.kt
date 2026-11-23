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
        if (currentHour !in 0..23 || startHour !in 0..23 || endHour !in 0..23) return false
        return shouldNotifyAtMinute(
            currentMl = currentMl,
            goalMl = goalMl,
            currentMinute = currentHour * 60,
            startMinute = startHour * 60,
            endMinute = endHour * 60,
        )
    }

    fun shouldNotifyAtMinute(
        currentMl: Int,
        goalMl: Int,
        currentMinute: Int,
        startMinute: Int,
        endMinute: Int,
    ): Boolean {
        if (startMinute !in 0 until 24 * 60 ||
            endMinute !in 0 until 24 * 60 ||
            currentMinute !in startMinute until endMinute
        ) return false

        // If goal already reached or exceeded, don't notify
        if (goalMl <= 0 || currentMl >= goalMl) return false

        val totalActiveMinutes = endMinute - startMinute
        val elapsedActiveMinutes = currentMinute - startMinute
        val expectedRatio = elapsedActiveMinutes.toFloat() / totalActiveMinutes.toFloat()
        val expectedMl = goalMl * expectedRatio

        // Notify if user is behind schedule by at least 100 ml margin
        val toleranceMarginMl = 100
        return currentMl < (expectedMl - toleranceMarginMl)
    }
}
