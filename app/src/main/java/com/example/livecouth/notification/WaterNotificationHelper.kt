package com.example.livecouth.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.livecouth.MainActivity

object WaterNotificationHelper {

    const val CHANNEL_ID = "water_reminder_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_ADD_250_ML = "com.example.livecouth.ACTION_ADD_250_ML"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios de Agua"
            val descriptionText = "Notificaciones adaptativas para beber agua"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWaterReminderNotification(
        context: Context,
        currentMl: Int,
        goalMl: Int,
    ) {
        createNotificationChannel(context)

        // Intent to open App when tapping notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action intent for +250 ml button without opening the app
        val addWaterIntent = Intent(context, AddWaterReceiver::class.java).apply {
            action = ACTION_ADD_250_ML
        }
        val addWaterPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            addWaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationContent = "Llevas $currentMl / $goalMl ml. ¡Tómate un vaso de agua para mantener feliz a tu tiburón! 🦈💧"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Tu tiburón necesita agua! 🦈")
            .setContentText(notificationContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_input_add,
                "+250 ml",
                addWaterPendingIntent,
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
