package com.example.livecouth.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.livecouth.domain.model.AdaptiveReminderCalculator
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.domain.repository.WaterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val waterRepository: WaterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userProfile = userPreferencesRepository.userProfile.first()

        if (!userProfile.reminderEnabled) {
            return Result.success()
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val todayTotalMl = waterRepository.getTodayTotalMl().first()

        val shouldNotify = AdaptiveReminderCalculator.shouldNotify(
            currentMl = todayTotalMl,
            goalMl = userProfile.dailyGoalMl,
            currentHour = currentHour,
            startHour = userProfile.reminderStartHour,
            endHour = userProfile.reminderEndHour,
        )

        if (shouldNotify) {
            WaterNotificationHelper.showWaterReminderNotification(
                context = applicationContext,
                currentMl = todayTotalMl,
                goalMl = userProfile.dailyGoalMl,
            )
        }

        return Result.success()
    }
}
