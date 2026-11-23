package com.example.livecouth.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.repository.WaterRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddWaterReceiver : BroadcastReceiver() {

    @Inject
    lateinit var waterRepository: WaterRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            WaterNotificationHelper.ACTION_SNOOZE -> {
                WaterReminderScheduler.scheduleSnoozedReminder(context)
                cancelReminderNotification(context)
            }
            WaterNotificationHelper.ACTION_ADD_250_ML -> addWater(context, 250)
            WaterNotificationHelper.ACTION_ADD_WATER -> {
                addWater(
                    context,
                    intent.getIntExtra(WaterNotificationHelper.EXTRA_WATER_AMOUNT_ML, 0),
                )
            }
        }
    }

    private fun addWater(context: Context, amountMl: Int) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!InputValidation.isValidWaterAmount(amountMl)) {
                    Log.w(TAG, "Ignored invalid notification water amount.")
                    return@launch
                }
                waterRepository.insertWaterLog(WaterLog(cantidadMl = amountMl))
                SyncScheduler.enqueueImmediate(context)
                WaterReminderScheduler.triggerGoalCheck(context)
                cancelReminderNotification(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun cancelReminderNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(WaterNotificationHelper.NOTIFICATION_ID)
    }

    private companion object {
        const val TAG = "AddWaterReceiver"
    }
}
