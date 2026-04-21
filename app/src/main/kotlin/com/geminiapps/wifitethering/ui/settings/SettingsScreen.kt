package com.geminiapps.wifitethering.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geminiapps.wifitethering.ui.theme.AppTheme

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToScheduler: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onRequestUpgrade: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToScheduler = onNavigateToScheduler,
        onNavigateToConfig = onNavigateToConfig,
        onUpgrade = onRequestUpgrade,
        onSetTheme = viewModel::setTheme,
        onRateApp = viewModel::rateApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToScheduler: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onUpgrade: () -> Unit,
    onSetTheme: (AppTheme) -> Unit,
    onRateApp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isPremium) {
                PremiumUpgradeCard(uiState = uiState, onUpgrade = onUpgrade)
            }

            if (uiState.isPremium) {
                SectionLabel("Premium Features")
                if (uiState.canEditConfig) {
                    Button(onClick = onNavigateToConfig, modifier = Modifier.fillMaxWidth()) {
                        Text("Hotspot Configuration")
                    }
                }
                Button(onClick = onNavigateToScheduler, modifier = Modifier.fillMaxWidth()) {
                    Text("Hotspot Scheduler")
                }
                HorizontalDivider()
            } else {
                SectionLabel("Premium Features")
                LockedFeatureButton(label = "Hotspot Scheduler", onClick = onUpgrade)
                if (uiState.canEditConfig) {
                    LockedFeatureButton(label = "Hotspot Configuration", onClick = onUpgrade)
                }
                HorizontalDivider()
            }

            SectionLabel("Appearance")
            ThemePicker(currentTheme = uiState.appTheme, onSelect = onSetTheme)

            HorizontalDivider()
            SectionLabel("About")
            OutlinedButton(onClick = onRateApp, modifier = Modifier.fillMaxWidth()) {
                Text("Rate this App")
            }
            Text(
                text = "Version 2.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LockedFeatureButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumUpgradeCard(uiState: SettingsUiState, onUpgrade: () -> Unit) {
    val description = buildString {
        append("Remove ads, schedule hotspot reminders")
        if (uiState.canEditConfig) append(", edit hotspot name/password")
        append(", and battery-aware alerts.")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Go Premium", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                Text("Upgrade — One-time Purchase")
            }
        }
    }
}

@Composable
private fun ThemePicker(currentTheme: AppTheme, onSelect: (AppTheme) -> Unit) {
    val options = listOf(
        AppTheme.SYSTEM to "System",
        AppTheme.DARK to "Dark",
        AppTheme.LIGHT to "Light",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (theme, label) ->
            FilterChip(
                selected = currentTheme == theme,
                onClick = { onSelect(theme) },
                label = { Text(label) },
            )
        }
    }
}
