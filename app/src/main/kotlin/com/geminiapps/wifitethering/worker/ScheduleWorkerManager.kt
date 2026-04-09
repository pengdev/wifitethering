package com.geminiapps.wifitethering.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.geminiapps.wifitethering.data.model.Schedule
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduleWorkerManager {

    fun schedule(context: Context, schedule: Schedule) {
        if (!schedule.enabled) {
            cancel(context, schedule)
            return
        }
        val initialDelay = computeInitialDelayMs(schedule.hourOfDay, schedule.minute, schedule.daysOfWeek)
        val data = Data.Builder()
            .putString(ScheduleNotificationWorker.KEY_LABEL, schedule.label)
            .putString(ScheduleNotificationWorker.KEY_ACTION, schedule.action.name)
            .build()

        val request = PeriodicWorkRequestBuilder<ScheduleNotificationWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tagFor(schedule.id))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            tagFor(schedule.id),
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun cancel(context: Context, schedule: Schedule) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagFor(schedule.id))
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    private fun tagFor(id: Int) = "schedule_$id"

    /**
     * Computes milliseconds until the next occurrence of the given time on any of the
     * specified days (bitmask: bit 0 = Monday … bit 6 = Sunday).
     */
    private fun computeInitialDelayMs(hourOfDay: Int, minute: Int, daysOfWeek: Int): Long {
        val now = Calendar.getInstance()
        // Calendar.DAY_OF_WEEK: Sun=1, Mon=2, … Sat=7
        // Our bitmask: bit 0 = Mon (Calendar 2), …, bit 6 = Sun (Calendar 1)
        fun calendarDayToBit(calDay: Int): Int = when (calDay) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        for (daysAhead in 0..6) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, daysAhead)
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val bit = calendarDayToBit(candidate.get(Calendar.DAY_OF_WEEK))
            if ((daysOfWeek shr bit) and 1 == 1 && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis - now.timeInMillis
            }
        }
        // Fallback: fire in 7 days
        return TimeUnit.DAYS.toMillis(7)
    }
}
