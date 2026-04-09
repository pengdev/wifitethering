package com.geminiapps.wifitethering.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiapps.wifitethering.data.PreferencesRepository
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val hotspotInfo: HotspotInfo = HotspotInfo(HotspotState.UNKNOWN, null),
    val canToggleProgrammatically: Boolean = false,
    val sessionElapsedSeconds: Long = 0L,
    val isPremium: Boolean = false,
    val showRatingPrompt: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val hotspotManager: HotspotManager,
    private val sessionTracker: SessionTracker,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _sessionTick = MutableStateFlow(0L)

    init {
        // Tick every second to update elapsed session time in UI
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _sessionTick.value = sessionTracker.elapsedSeconds()
            }
        }
        // Track usage for rating prompt
        viewModelScope.launch {
            preferencesRepository.incrementUsageCount()
        }
        // Observe hotspot state to drive session tracker
        viewModelScope.launch {
            hotspotManager.hotspotInfo.collect { info ->
                when (info.state) {
                    HotspotState.ENABLED -> sessionTracker.onHotspotEnabled()
                    HotspotState.DISABLED -> sessionTracker.onHotspotDisabled()
                    else -> Unit
                }
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        hotspotManager.hotspotInfo,
        _sessionTick,
        preferencesRepository.isPremium,
        preferencesRepository.usageCount,
        preferencesRepository.hasRated,
    ) { hotspotInfo, elapsed, isPremium, usageCount, hasRated ->
        HomeUiState(
            hotspotInfo = hotspotInfo,
            canToggleProgrammatically = hotspotManager.canToggleProgrammatically(),
            sessionElapsedSeconds = elapsed,
            isPremium = isPremium,
            showRatingPrompt = !hasRated && usageCount >= 20,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            canToggleProgrammatically = hotspotManager.canToggleProgrammatically()
        ),
    )

    fun onToggleOrOpenSettings() {
        hotspotManager.toggleOrOpenSettings()
    }

    fun onOpenSettings() {
        hotspotManager.openTetheringSettings()
    }

    fun onDismissRatingPrompt() {
        viewModelScope.launch {
            preferencesRepository.setHasRated(true)
        }
    }
}
