package com.example.livecouth.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.livecouth.domain.model.AdaptiveReminderCalculator
import com.example.livecouth.domain.model.ClimateAdjustmentCalculator
import com.example.livecouth.domain.model.ReminderSettingsValidator
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.domain.repository.WaterRepository
import com.example.livecouth.domain.repository.WeatherRepository
import com.example.livecouth.domain.repository.WeatherResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val waterRepository: WaterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val weatherRepository: WeatherRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userProfile = userPreferencesRepository.userProfile.first()
        val todayTotalMl = waterRepository.getTodayTotalMl().first()

        if (inputData.getBoolean(INPUT_TEST_MODE, false)) {
            WaterNotificationHelper.showWaterReminderNotification(
                context = applicationContext,
                currentMl = todayTotalMl,
                goalMl = userProfile.dailyGoalMl,
                firstQuickAddButton = waterRepository.observeQuickAddButtons().first().firstOrNull(),
            )
            return Result.success()
        }

        val reminderSettings = userPreferencesRepository.reminderSettings.first()
        if (!reminderSettings.enabled ||
            ReminderSettingsValidator.validate(reminderSettings) != null
        ) {
            return Result.success()
        }

        val now = Calendar.getInstance()
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val mondayFirstDayIndex = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (!ReminderSettingsValidator.isDayActive(reminderSettings, mondayFirstDayIndex) ||
            ReminderSettingsValidator.isQuietTime(reminderSettings, currentMinute) ||
            currentMinute !in reminderSettings.startMinute until reminderSettings.endMinute
        ) {
            return Result.success()
        }

        val weather = weatherRepository.getTodayWeather(
            manualCity = userProfile.city,
            useDeviceLocation = userPreferencesRepository.usesDeviceLocationForClimate.first(),
        )
        val climateAdjustmentMl = when (weather) {
            is WeatherResult.Available -> ClimateAdjustmentCalculator.calcularAjusteClima(
                weather.weather.maxTemperatureCelsius,
                weather.weather.maxHumidityPercent,
            )

            is WeatherResult.Offline -> weather.cachedWeather
                ?.takeIf { it.isValid() }
                ?.let {
                    ClimateAdjustmentCalculator.calcularAjusteClima(
                        it.maxTemperatureCelsius,
                        it.maxHumidityPercent,
                    )
                }
                ?: userPreferencesRepository.getManualClimate(todayDate())?.adjustmentMl
                ?: 0
        }
        val goalMl = userProfile.dailyGoalMl + climateAdjustmentMl

        if (todayTotalMl >= goalMl) {
            if (userPreferencesRepository.claimDailyGoalNotification(todayDate())) {
                WaterNotificationHelper.showGoalReachedNotification(
                    context = applicationContext,
                    currentMl = todayTotalMl,
                    goalMl = goalMl,
                )
            }
            return Result.success()
        }

        if (inputData.getBoolean(INPUT_SNOOZE_MODE, false)) {
            WaterNotificationHelper.showWaterReminderNotification(
                context = applicationContext,
                currentMl = todayTotalMl,
                goalMl = goalMl,
                firstQuickAddButton = waterRepository.observeQuickAddButtons().first().firstOrNull(),
            )
            return Result.success()
        }

        if (inputData.getBoolean(INPUT_GOAL_CHECK_MODE, false)) return Result.success()

        val shouldNotify = AdaptiveReminderCalculator.shouldNotifyAtMinute(
            currentMl = todayTotalMl,
            goalMl = goalMl,
            currentMinute = currentMinute,
            startMinute = reminderSettings.startMinute,
            endMinute = reminderSettings.endMinute,
        )

        if (shouldNotify) {
            WaterNotificationHelper.showWaterReminderNotification(
                context = applicationContext,
                currentMl = todayTotalMl,
                goalMl = goalMl,
                firstQuickAddButton = waterRepository.observeQuickAddButtons().first().firstOrNull(),
            )
        }

        return Result.success()
    }

    private fun todayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        const val INPUT_TEST_MODE = "test_mode"
        const val INPUT_SNOOZE_MODE = "snooze_mode"
        const val INPUT_GOAL_CHECK_MODE = "goal_check_mode"
    }
}
