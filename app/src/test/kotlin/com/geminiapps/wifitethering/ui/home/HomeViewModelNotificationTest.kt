package com.geminiapps.wifitethering.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelNotificationTest {

    @Test
    fun `HomeUiState has shouldRequestNotificationPermission flag defaulting to false`() {
        val state = HomeUiState()
        assertFalse(state.shouldRequestNotificationPermission)
    }

    @Test
    fun `HomeUiState shouldRequestNotificationPermission can be set to true`() {
        val state = HomeUiState(shouldRequestNotificationPermission = true)
        assertTrue(state.shouldRequestNotificationPermission)
    }
}
