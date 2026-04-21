package com.geminiapps.wifitethering.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiapps.wifitethering.data.PreferencesRepository
import com.geminiapps.wifitethering.domain.BillingManager
import com.geminiapps.wifitethering.domain.HotspotManager
import com.geminiapps.wifitethering.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isPremium: Boolean = false,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val canEditConfig: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    val billingManager: BillingManager,
    private val hotspotManager: HotspotManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.isPremium,
        preferencesRepository.appTheme,
    ) { isPremium, theme ->
        SettingsUiState(
            isPremium = isPremium,
            appTheme = theme,
            canEditConfig = hotspotManager.capabilities().canEditConfig,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesRepository.setAppTheme(theme) }
    }

    fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.geminiapps.wifitethering")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }
}
