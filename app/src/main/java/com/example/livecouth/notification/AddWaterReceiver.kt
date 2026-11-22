package com.example.livecouth.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.livecouth.data.local.entity.WaterLog
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
        if (intent.action == WaterNotificationHelper.ACTION_ADD_250_ML) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    waterRepository.insertWaterLog(
                        WaterLog(cantidadMl = 250),
                    )
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(WaterNotificationHelper.NOTIFICATION_ID)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
