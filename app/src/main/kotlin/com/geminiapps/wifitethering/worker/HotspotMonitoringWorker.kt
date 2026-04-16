package com.geminiapps.wifitethering.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.geminiapps.wifitethering.data.PreferencesRepository
import com.geminiapps.wifitethering.domain.HotspotManager
import com.geminiapps.wifitethering.domain.HotspotState
import com.geminiapps.wifitethering.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class HotspotMonitoringWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val hotspotManager: HotspotManager,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val info = hotspotManager.currentInfo()
        
        // Only monitor if hotspot is actually active
        if (info.state != HotspotState.ENABLED) {
            return Result.success()
        }

        val isPremium = preferencesRepository.isPremium.first()
        if (!isPremium) return Result.success()

        // 🔋 Check Battery Limit
        val battEnabled = preferencesRepository.batteryLimitEnabled.first()
        val battPercent = preferencesRepository.batteryLimitPercent.first()
        if (battEnabled && info.batteryLevel != -1 && info.batteryLevel <= battPercent) {
            NotificationHelper.showLimitReachedNotification(
                context,
                "Battery Limit Reached",
                "Hotspot is draining your battery (Current: ${info.batteryLevel}%). Tap to turn off."
            )
            // Note: We don't auto-stop the worker yet so the notification stays or re-fires if dismissed
            return Result.success()
        }

        // 📊 Check Data Limit
        val dataEnabled = preferencesRepository.dataLimitEnabled.first()
        val dataLimitMb = preferencesRepository.dataLimitMb.first()
        // Here we'd use NetworkStatsManager for a real data check. 
        // For now, we'll monitor the session time as a proxy or just the battery.
        
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "HotspotMonitoringWork"

        fun start(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val request = PeriodicWorkRequestBuilder<HotspotMonitoringWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
