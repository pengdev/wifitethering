package com.geminiapps.wifitethering.ui.home

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
import kotlinx.coroutines.flow.flow
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

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _sessionTick.value = sessionTracker.elapsedSeconds()
            }
        }
        viewModelScope.launch {
            preferencesRepository.incrementUsageCount()
        }
        viewModelScope.launch {
            hotspotManager.hotspotInfo.collect { info ->
                when (info.state) {
                    HotspotState.ENABLED -> {
                        sessionTracker.onHotspotEnabled()
                        com.geminiapps.wifitethering.worker.HotspotMonitoringWorker.start(preferencesRepository.context)
                    }
                    HotspotState.DISABLED -> {
                        sessionTracker.onHotspotDisabled()
                        com.geminiapps.wifitethering.worker.HotspotMonitoringWorker.stop(preferencesRepository.context)
                    }
                    else -> Unit
                }
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        hotspotManager.hotspotInfo,
        _sessionTick,
        preferencesRepository.userPreferences,
    ) { hotspotInfo, elapsed, prefs ->
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
            currentUsageMb = (elapsed.toDouble() * 0.05).toInt()
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
