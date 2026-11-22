package com.example.livecouth.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WaterReminderScheduler {

    private const val WORK_TAG = "water_reminder_work"

    fun schedulePeriodicReminders(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            2, TimeUnit.HOURS,
        ).addTag(WORK_TAG).build()

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
            .addTag("test_water_reminder")
            .build()
        WorkManager.getInstance(context).enqueue(testWorkRequest)
    }
}
