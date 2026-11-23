package com.example.livecouth.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.livecouth.domain.model.ReminderSettings
import com.example.livecouth.domain.model.ReminderSettingsValidator
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WaterReminderScheduler {

    private const val WORK_TAG = "water_reminder_work"

    fun schedulePeriodicReminders(context: Context, settings: ReminderSettings) {
        require(settings.enabled && ReminderSettingsValidator.validate(settings) == null) {
            "La configuración de recordatorios no es válida."
        }
        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            settings.intervalMinutes.toLong(),
            TimeUnit.MINUTES,
        )
            .setInitialDelay(nextReminderDelayMillis(settings), TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
    }

    /**
     * Immediate execution trigger for testing without waiting 2 hours.
     */
    fun triggerImmediateTestReminder(context: Context) {
        val testWorkRequest = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInputData(Data.Builder().putBoolean(WaterReminderWorker.INPUT_TEST_MODE, true).build())
            .addTag("test_water_reminder")
            .build()
        WorkManager.getInstance(context).enqueue(testWorkRequest)
    }

    fun scheduleSnoozedReminder(context: Context) {
        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInputData(Data.Builder().putBoolean(WaterReminderWorker.INPUT_SNOOZE_MODE, true).build())
            .setInitialDelay(30, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SNOOZE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun triggerGoalCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInputData(Data.Builder().putBoolean(WaterReminderWorker.INPUT_GOAL_CHECK_MODE, true).build())
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            GOAL_CHECK_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun nextReminderDelayMillis(settings: ReminderSettings): Long {
        val now = Calendar.getInstance()
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (daysAhead in 0..7) {
            val date = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, daysAhead)
            }
            val mondayFirstDayIndex = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7
            if (!ReminderSettingsValidator.isDayActive(settings, mondayFirstDayIndex)) continue

            val candidateStart = when {
                daysAhead > 0 || currentMinute < settings.startMinute -> settings.startMinute.toLong()
                currentMinute >= settings.endMinute -> continue
                else -> {
                    val elapsedMinutes = currentMinute - settings.startMinute
                    settings.startMinute.toLong() +
                        (elapsedMinutes / settings.intervalMinutes + 1L) * settings.intervalMinutes
                }
            }

            var candidateMinute = candidateStart
            while (candidateMinute < settings.endMinute) {
                val minuteOfDay = candidateMinute.toInt()
                if (!ReminderSettingsValidator.isQuietTime(settings, minuteOfDay)) {
                    val reminderAt = (date.clone() as Calendar).apply {
                        add(Calendar.MINUTE, minuteOfDay)
                    }
                    return (reminderAt.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
                }
                candidateMinute += settings.intervalMinutes
            }
        }

        return TimeUnit.DAYS.toMillis(7)
    }

    private const val SNOOZE_WORK_NAME = "water_reminder_snooze"
    private const val GOAL_CHECK_WORK_NAME = "water_reminder_goal_check"
}
