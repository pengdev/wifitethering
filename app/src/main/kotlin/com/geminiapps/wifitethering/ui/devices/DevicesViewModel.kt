package com.geminiapps.wifitethering.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiapps.wifitethering.data.model.ConnectedDevice
import com.geminiapps.wifitethering.domain.DeviceScanner
import com.geminiapps.wifitethering.domain.HotspotManager
import com.geminiapps.wifitethering.domain.HotspotState
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

sealed interface DevicesUiState {
    data object HotspotOff : DevicesUiState
    data object Loading : DevicesUiState
    data class Success(val devices: List<ConnectedDevice>) : DevicesUiState
}

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceScanner: DeviceScanner,
    private val hotspotManager: HotspotManager,
) : ViewModel() {

    private val _devices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<DevicesUiState> = combine(
        hotspotManager.hotspotInfo,
        _devices,
        _isLoading,
    ) { hotspotInfo, devices, loading ->
        when {
            hotspotInfo.state != HotspotState.ENABLED -> DevicesUiState.HotspotOff
            loading -> DevicesUiState.Loading
            else -> DevicesUiState.Success(devices)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DevicesUiState.Loading,
    )

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                _isLoading.value = _devices.value.isEmpty()
                _devices.value = deviceScanner.scanDevices()
                _isLoading.value = false
                delay(30_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _devices.value = deviceScanner.scanDevices()
            _isLoading.value = false
        }
    }
}
