package com.geminiapps.wifitethering.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geminiapps.wifitethering.data.db.AppDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: AppDatabase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val schedules = database.scheduleDao().getEnabledSchedules()
                schedules.forEach { schedule ->
                    ScheduleWorkerManager.schedule(context, schedule)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
