package com.geminiapps.wifitethering.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTracker @Inject constructor() {

    private val _sessionStartMs = MutableStateFlow<Long?>(null)
    val sessionStartMs: StateFlow<Long?> = _sessionStartMs.asStateFlow()

    fun onHotspotEnabled() {
        if (_sessionStartMs.value == null) {
            _sessionStartMs.value = System.currentTimeMillis()
        }
    }

    fun onHotspotDisabled() {
        _sessionStartMs.value = null
    }

    fun elapsedSeconds(): Long {
        val start = _sessionStartMs.value ?: return 0L
        return (System.currentTimeMillis() - start) / 1000
    }
}
