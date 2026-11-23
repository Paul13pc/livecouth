package com.example.livecouth.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.livecouth.MainActivity
import com.example.livecouth.R
import com.example.livecouth.data.local.entity.QuickAddButton
import kotlin.math.roundToInt

object WaterNotificationHelper {

    const val CHANNEL_ID = "water_reminder_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_ADD_250_ML = "com.example.livecouth.ACTION_ADD_250_ML"
    const val ACTION_ADD_WATER = "com.example.livecouth.ACTION_ADD_WATER"
    const val ACTION_SNOOZE = "com.example.livecouth.ACTION_SNOOZE"
    const val EXTRA_WATER_AMOUNT_ML = "extra_water_amount_ml"
    private const val NOTIFICATION_GROUP = "com.example.livecouth.WATER_REMINDERS"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWaterReminderNotification(
        context: Context,
        currentMl: Int,
        goalMl: Int,
        firstQuickAddButton: QuickAddButton? = null,
    ) {
        createNotificationChannel(context)

        val progressPercent = if (goalMl > 0) {
            ((currentMl.toDouble() / goalMl) * 100).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val titleRes = when {
            progressPercent < 35 -> R.string.notification_title_low
            progressPercent < 75 -> R.string.notification_title_medium
            else -> R.string.notification_title_near_goal
        }
        val messageArrayRes = when {
            progressPercent < 35 -> R.array.reminder_messages_low
            progressPercent < 75 -> R.array.reminder_messages_medium
            else -> R.array.reminder_messages_near_goal
        }
        val progressText = context.getString(
            R.string.notification_progress,
            currentMl,
            goalMl,
            progressPercent,
        )
        val message = context.resources.getStringArray(messageArrayRes).random()
        val detail = "$progressText\n$message"

        val add250PendingIntent = addWaterPendingIntent(context, requestCode = 1, amountMl = 250)
        val bottlePendingIntent = addWaterPendingIntent(
            context,
            requestCode = 2,
            amountMl = firstQuickAddButton?.amountMl ?: 250,
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, AddWaterReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = baseNotificationBuilder(context)
            .setContentTitle(context.getString(titleRes))
            .setContentText(progressText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .addAction(
                android.R.drawable.ic_input_add,
                context.getString(R.string.notification_action_add_250),
                add250PendingIntent,
            )
            .addAction(
                android.R.drawable.ic_input_add,
                context.getString(R.string.notification_action_my_bottle),
                bottlePendingIntent,
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                context.getString(R.string.notification_action_later),
                snoozePendingIntent,
            )
            .build()

        notify(context, notification)
    }

    fun showGoalReachedNotification(context: Context, currentMl: Int, goalMl: Int) {
        createNotificationChannel(context)
        val progressText = context.getString(
            R.string.notification_goal_text,
            currentMl,
            goalMl,
        )
        val notification = baseNotificationBuilder(context)
            .setContentTitle(context.getString(R.string.notification_goal_title))
            .setContentText(progressText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(progressText))
            .build()

        notify(context, notification)
    }

    private fun baseNotificationBuilder(context: Context): NotificationCompat.Builder {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shark)
            .setColor(ContextCompat.getColor(context, R.color.notification_ocean))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(NOTIFICATION_GROUP)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
    }

    private fun addWaterPendingIntent(
        context: Context,
        requestCode: Int,
        amountMl: Int,
    ): PendingIntent {
        val intent = Intent(context, AddWaterReceiver::class.java)
            .setAction(ACTION_ADD_WATER)
            .putExtra(EXTRA_WATER_AMOUNT_ML, amountMl)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(context: Context, notification: android.app.Notification) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
