package com.geminiapps.wifitethering.ui.config

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiapps.wifitethering.domain.HotspotManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HotspotConfigUiState(
    val ssid: String = "",
    val password: String = "",
    val canEditProgrammatically: Boolean = false,
    val saveResult: SaveResult? = null,
)

enum class SaveResult { SUCCESS, FAILED, OPEN_SETTINGS }

@HiltViewModel
class HotspotConfigViewModel @Inject constructor(
    private val hotspotManager: HotspotManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HotspotConfigUiState(
            canEditProgrammatically = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        )
    )
    val uiState: StateFlow<HotspotConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    ssid = hotspotManager.readSsid() ?: "",
                    password = hotspotManager.readPassword() ?: "",
                )
            }
        }
    }

    fun onSsidChange(value: String) = _uiState.update { it.copy(ssid = value) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun saveConfig() {
        val state = _uiState.value
        if (!state.canEditProgrammatically) {
            _uiState.update { it.copy(saveResult = SaveResult.OPEN_SETTINGS) }
            hotspotManager.openTetheringSettings()
            return
        }
        val success = hotspotManager.applyConfig(state.ssid, state.password)
        _uiState.update { it.copy(saveResult = if (success) SaveResult.SUCCESS else SaveResult.FAILED) }
    }

    fun clearSaveResult() = _uiState.update { it.copy(saveResult = null) }
}
