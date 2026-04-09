package com.geminiapps.wifitethering.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geminiapps.wifitethering.domain.HotspotState
import com.geminiapps.wifitethering.ui.ads.BannerAdView

@Composable
fun HomeRoute(
    onNavigateToDevices: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onToggleOrOpenSettings = viewModel::onToggleOrOpenSettings,
        onOpenSettings = viewModel::onOpenSettings,
        onNavigateToDevices = onNavigateToDevices,
        onNavigateToSettings = onNavigateToSettings,
        onDismissRatingPrompt = viewModel::onDismissRatingPrompt,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleOrOpenSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismissRatingPrompt: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tethering Shortcut") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isPremium) {
                BannerAdView(modifier = Modifier.fillMaxWidth())
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HotspotStatusCard(uiState = uiState)

            MainActionButton(
                uiState = uiState,
                onToggleOrOpenSettings = onToggleOrOpenSettings,
            )

            if (!uiState.canToggleProgrammatically) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Tethering Settings")
                }
            }

            if (uiState.hotspotInfo.state == HotspotState.ENABLED) {
                DevicesChip(onNavigateToDevices = onNavigateToDevices)
                SessionTimerCard(elapsedSeconds = uiState.sessionElapsedSeconds)
            }

            if (uiState.showRatingPrompt) {
                RatingPrompt(onDismiss = onDismissRatingPrompt)
            }
        }
    }
}

@Composable
private fun HotspotStatusCard(uiState: HomeUiState) {
    val (icon, label, color) = when (uiState.hotspotInfo.state) {
        HotspotState.ENABLED -> Triple(Icons.Default.Wifi, "Hotspot Active", MaterialTheme.colorScheme.primary)
        HotspotState.DISABLED -> Triple(Icons.Default.WifiOff, "Hotspot Off", MaterialTheme.colorScheme.onSurfaceVariant)
        HotspotState.ENABLING -> Triple(Icons.Default.Wifi, "Turning On...", MaterialTheme.colorScheme.primary)
        HotspotState.DISABLING -> Triple(Icons.Default.WifiOff, "Turning Off...", MaterialTheme.colorScheme.onSurfaceVariant)
        HotspotState.UNKNOWN -> Triple(Icons.Default.WifiOff, "Unknown", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
            )
            uiState.hotspotInfo.ssid?.let { ssid ->
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MainActionButton(
    uiState: HomeUiState,
    onToggleOrOpenSettings: () -> Unit,
) {
    val isEnabled = uiState.hotspotInfo.state == HotspotState.ENABLED
    val label = when {
        uiState.canToggleProgrammatically -> if (isEnabled) "Turn Off Hotspot" else "Turn On Hotspot"
        else -> if (isEnabled) "Hotspot is Active" else "Enable Hotspot"
    }

    Button(
        onClick = onToggleOrOpenSettings,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun DevicesChip(onNavigateToDevices: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        SuggestionChip(
            onClick = onNavigateToDevices,
            label = { Text("View Connected Devices") },
            icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun SessionTimerCard(elapsedSeconds: Long) {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeString = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Session time", style = MaterialTheme.typography.bodyMedium)
            Text(timeString, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RatingPrompt(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Enjoying the app?",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "A quick rating helps other users find us and keeps the app free.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Not now") }
                // Actual Play Store rating intent would be triggered here
                TextButton(onClick = onDismiss) { Text("Rate") }
            }
        }
    }
}
