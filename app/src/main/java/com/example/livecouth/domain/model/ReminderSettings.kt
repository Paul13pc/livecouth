package com.example.livecouth.domain.model

enum class ReminderFrequency(val minutes: Int?) {
    EVERY_30_MINUTES(30),
    EVERY_HOUR(60),
    EVERY_90_MINUTES(90),
    EVERY_2_HOURS(120),
    EVERY_3_HOURS(180),
    CUSTOM(null),
}

enum class ReminderSettingsError {
    INVALID_ACTIVE_WINDOW,
    INVALID_INTERVAL,
    INVALID_QUIET_WINDOW,
}

data class ReminderSettings(
    val enabled: Boolean = true,
    val startMinute: Int = 8 * 60,
    val endMinute: Int = 21 * 60,
    val frequency: ReminderFrequency = ReminderFrequency.EVERY_2_HOURS,
    val customIntervalMinutes: Int = 60,
    val activeDaysMask: Int = 0b1111111,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinute: Int = 22 * 60,
    val quietEndMinute: Int = 7 * 60,
) {
    val intervalMinutes: Int
        get() = frequency.minutes ?: customIntervalMinutes
}

object ReminderSettingsValidator {
    fun validate(settings: ReminderSettings): ReminderSettingsError? {
        if (!settings.enabled) return null
        return when {
            settings.startMinute !in 0 until MINUTES_PER_DAY ||
                settings.endMinute !in 0 until MINUTES_PER_DAY ||
                settings.endMinute <= settings.startMinute ->
                ReminderSettingsError.INVALID_ACTIVE_WINDOW

            settings.intervalMinutes < MIN_REMINDER_INTERVAL_MINUTES ->
                ReminderSettingsError.INVALID_INTERVAL

            settings.quietHoursEnabled && (
                settings.quietStartMinute !in 0 until MINUTES_PER_DAY ||
                    settings.quietEndMinute !in 0 until MINUTES_PER_DAY ||
                    settings.quietStartMinute == settings.quietEndMinute
                ) ->
                ReminderSettingsError.INVALID_QUIET_WINDOW

            else -> null
        }
    }

    fun isQuietTime(settings: ReminderSettings, currentMinute: Int): Boolean {
        if (!settings.quietHoursEnabled || currentMinute !in 0 until MINUTES_PER_DAY) return false
        return if (settings.quietStartMinute < settings.quietEndMinute) {
            currentMinute >= settings.quietStartMinute && currentMinute < settings.quietEndMinute
        } else {
            currentMinute >= settings.quietStartMinute || currentMinute < settings.quietEndMinute
        }
    }

    fun isDayActive(settings: ReminderSettings, mondayFirstDayIndex: Int): Boolean =
        mondayFirstDayIndex in 0..6 &&
            settings.activeDaysMask and (1 shl mondayFirstDayIndex) != 0

    const val MIN_REMINDER_INTERVAL_MINUTES = 30
    const val MINUTES_PER_DAY = 24 * 60
}
