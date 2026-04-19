package com.geminiapps.wifitethering.ui.home

import android.net.TrafficStats
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiapps.wifitethering.data.PreferencesRepository
import com.geminiapps.wifitethering.domain.DeviceCapabilities
import com.geminiapps.wifitethering.domain.HotspotInfo
import com.geminiapps.wifitethering.domain.HotspotManager
import com.geminiapps.wifitethering.domain.HotspotState
import com.geminiapps.wifitethering.domain.SessionTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val hotspotInfo: HotspotInfo = HotspotInfo(HotspotState.UNKNOWN, null),
    val capabilities: DeviceCapabilities = DeviceCapabilities(
        canToggleProgrammatically = false,
        canReadSsidAndPassword = false,
        canEditConfig = false,
        canScanConnectedDevices = false,
        canUseTile = false,
        needsNotificationPermission = false,
    ),
    val canToggleProgrammatically: Boolean = false,
    val sessionElapsedSeconds: Long = 0L,
    val isPremium: Boolean = false,
    val isTrialActive: Boolean = false,
    val batteryTrialUsed: Int = 0,
    val showRatingPrompt: Boolean = false,
    val showUpgradePrompt: Boolean = false,
    val showOnboarding: Boolean = false,
    val dataLimitEnabled: Boolean = false,
    val dataLimitMb: Int = 1000,
    val batteryLimitEnabled: Boolean = false,
    val batteryLimitPercent: Int = 20,
    val currentUsageMb: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val hotspotManager: HotspotManager,
    private val sessionTracker: SessionTracker,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _sessionTick = MutableStateFlow(0L)
    private val _currentUsageMb = MutableStateFlow(0)

    /**
     * Baseline cumulative bytes at hotspot session start.
     * TrafficStats.getTotalRxBytes/TxBytes() requires no permission and is
     * available on all API levels. We capture a baseline when the hotspot turns on
     * and compute delta each tick. Value of UNSUPPORTED (-1) means unavailable.
     */
    private var trafficBaselineBytes: Long = TrafficStats.UNSUPPORTED.toLong()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _sessionTick.value = sessionTracker.elapsedSeconds()
                updateDataUsage()
            }
        }
        viewModelScope.launch {
            preferencesRepository.incrementUsageCount()
        }
        viewModelScope.launch {
            hotspotManager.hotspotInfo.collect { info ->
                when (info.state) {
                    HotspotState.ENABLED -> {
                        val wasOff = sessionTracker.sessionStartMs.value == null
                        sessionTracker.onHotspotEnabled()
                        if (wasOff) {
                            // Capture traffic baseline once at session start
                            val rx = TrafficStats.getTotalRxBytes()
                            val tx = TrafficStats.getTotalTxBytes()
                            trafficBaselineBytes = if (rx == TrafficStats.UNSUPPORTED.toLong()
                                || tx == TrafficStats.UNSUPPORTED.toLong()
                            ) {
                                TrafficStats.UNSUPPORTED.toLong()
                            } else {
                                rx + tx
                            }
                        }
                        com.geminiapps.wifitethering.worker.HotspotMonitoringWorker.start(preferencesRepository.context)
                    }
                    HotspotState.DISABLED -> {
                        sessionTracker.onHotspotDisabled()
                        trafficBaselineBytes = TrafficStats.UNSUPPORTED.toLong()
                        com.geminiapps.wifitethering.worker.HotspotMonitoringWorker.stop(preferencesRepository.context)
                        _currentUsageMb.value = 0
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun updateDataUsage() {
        if (sessionTracker.sessionStartMs.value == null) return
        val baseline = trafficBaselineBytes
        if (baseline == TrafficStats.UNSUPPORTED.toLong()) return

        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return

        val sessionBytes = (rx + tx - baseline).coerceAtLeast(0)
        _currentUsageMb.value = (sessionBytes / (1024L * 1024L)).toInt()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        hotspotManager.hotspotInfo,
        _sessionTick,
        preferencesRepository.userPreferences,
        _currentUsageMb,
    ) { hotspotInfo, elapsed, prefs, usageMb ->
        HomeUiState(
            hotspotInfo = hotspotInfo,
            capabilities = hotspotManager.capabilities(),
            canToggleProgrammatically = hotspotManager.canToggleProgrammatically(),
            sessionElapsedSeconds = elapsed,
            isPremium = prefs.isPremium,
            isTrialActive = !prefs.isPremium && prefs.batteryTrialSessionsUsed < 3,
            batteryTrialUsed = prefs.batteryTrialSessionsUsed,
            showRatingPrompt = !prefs.hasRated && prefs.hotspotOnCount >= 5,
            showUpgradePrompt = !prefs.isPremium && prefs.hotspotOnCount >= 3 && prefs.hotspotOnCount % 5 == 0,
            showOnboarding = !prefs.hasSeenOnboarding,
            dataLimitEnabled = prefs.dataLimitEnabled,
            dataLimitMb = prefs.dataLimitMb,
            batteryLimitEnabled = prefs.batteryLimitEnabled,
            batteryLimitPercent = prefs.batteryLimitPercent,
            currentUsageMb = usageMb,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            canToggleProgrammatically = hotspotManager.canToggleProgrammatically()
        ),
    )

    fun onToggleOrOpenSettings() {
        if (hotspotManager.toggleOrOpenSettings()) {
            viewModelScope.launch {
                preferencesRepository.incrementHotspotOnCount()
            }
        }
    }

    fun onOpenSettings() {
        hotspotManager.openTetheringSettings()
    }

    fun onDismissRatingPrompt() {
        viewModelScope.launch {
            preferencesRepository.setHasRated(true)
        }
    }

    fun onDismissOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setHasSeenOnboarding(true)
        }
    }

    fun onToggleDataLimit() {
        viewModelScope.launch {
            preferencesRepository.setDataLimitEnabled(!uiState.value.dataLimitEnabled)
        }
    }

    fun onToggleBatteryLimit() {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            if (!prefs.isPremium) {
                preferencesRepository.incrementBatteryTrialSessions()
            }
            preferencesRepository.setBatteryLimitEnabled(!uiState.value.batteryLimitEnabled)
        }
    }

    fun updateDataLimit(mb: Int) {
        viewModelScope.launch {
            preferencesRepository.setDataLimitMb(mb)
        }
    }

    fun updateBatteryLimit(percent: Int) {
        viewModelScope.launch {
            preferencesRepository.setBatteryLimitPercent(percent)
        }
    }
}
