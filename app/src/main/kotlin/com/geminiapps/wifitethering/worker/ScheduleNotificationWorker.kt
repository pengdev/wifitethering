package com.geminiapps.wifitethering.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geminiapps.wifitethering.MainActivity
import com.geminiapps.wifitethering.R
import com.geminiapps.wifitethering.data.model.ScheduleAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduleNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_LABEL = "label"
        const val KEY_ACTION = "action"
        private const val CHANNEL_ID = "hotspot_scheduler"
    }

    override suspend fun doWork(): Result {
        val label = inputData.getString(KEY_LABEL) ?: "Hotspot Reminder"
        val actionName = inputData.getString(KEY_ACTION) ?: ScheduleAction.REMIND_ENABLE.name
        val action = runCatching { ScheduleAction.valueOf(actionName) }.getOrDefault(ScheduleAction.REMIND_ENABLE)

        val (title, body) = when (action) {
            ScheduleAction.REMIND_ENABLE -> "Time to enable your hotspot" to "Tap to open tethering settings"
            ScheduleAction.REMIND_DISABLE -> "Time to disable your hotspot" to "Tap to open tethering settings"
        }

        createNotificationChannel()
        showNotification(label = label, title = title, body = body)
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hotspot Scheduler",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders from your hotspot schedule"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun showNotification(label: String, title: String, body: String) {
        val tetherIntent = Intent("com.android.settings.TetherSettings").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val resolvedIntent = if (context.packageManager.resolveActivity(tetherIntent, 0) != null) {
            tetherIntent
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            label.hashCode(),
            resolvedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(label.hashCode(), notification)
    }
}
