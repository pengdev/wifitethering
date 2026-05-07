package com.geminiapps.wifitethering.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
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
    onRequestUpgrade: () -> Unit,
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
        onToggleDataLimit = viewModel::onToggleDataLimit,
        onToggleBatteryLimit = viewModel::onToggleBatteryLimit,
        onUpdateDataLimit = viewModel::updateDataLimit,
        onUpdateBatteryLimit = viewModel::updateBatteryLimit,
        onRequestUpgrade = onRequestUpgrade,
        onDismissOnboarding = viewModel::onDismissOnboarding,
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
    onToggleDataLimit: () -> Unit,
    onToggleBatteryLimit: () -> Unit,
    onUpdateDataLimit: (Int) -> Unit,
    onUpdateBatteryLimit: (Int) -> Unit,
    onRequestUpgrade: () -> Unit,
    onDismissOnboarding: () -> Unit,
) {
    var showQrSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Hotspot") },
                actions = {
                    if (uiState.dataLimitEnabled || uiState.batteryLimitEnabled) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Monitoring Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp).size(20.dp)
                        )
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                HotspotStatusCard(
                    uiState = uiState,
                    onShareClick = { showQrSheet = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.capabilities.canScanConnectedDevices) {
                        SuggestionChip(
                            onClick = onNavigateToDevices,
                            label = { Text("Devices") },
                            icon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SuggestionChip(
                        onClick = onNavigateToSettings,
                        label = { Text("Schedules") },
                        icon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                MainActionButton(
                    uiState = uiState,
                    onToggleOrOpenSettings = onToggleOrOpenSettings,
                )
            }

            if (!uiState.capabilities.canToggleProgrammatically) {
                item {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("System Tethering Settings")
                    }
                }
            }

            if (uiState.hotspotInfo.state == HotspotState.ENABLED) {
                item {
                    SessionTimerCard(elapsedSeconds = uiState.sessionElapsedSeconds)
                }
            }

            if (uiState.hotspotInfo.state == HotspotState.ENABLED || uiState.dataLimitEnabled || uiState.batteryLimitEnabled) {
                item {
                    Text(
                        "Smart Management",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    ManagementCard(
                        title = "Data Cap",
                        info = "${uiState.currentUsageMb} MB used · Limit: ${uiState.dataLimitMb} MB",
                        progress = (uiState.currentUsageMb.toFloat() / uiState.dataLimitMb).coerceIn(0f, 1f),
                        sliderValue = uiState.dataLimitMb.toFloat(),
                        sliderRange = 100f..10000f,
                        sliderLabel = { "Limit: ${it.toInt()} MB" },
                        enabled = uiState.dataLimitEnabled,
                        onToggle = onToggleDataLimit,
                        onSliderChange = { onUpdateDataLimit(it.toInt()) },
                        isPremium = uiState.isPremium,
                        onRequestUpgrade = onRequestUpgrade,
                    )
                }

                item {
                    ManagementCard(
                        title = "Battery Protector",
                        info = if (uiState.isPremium) {
                            "Current: ${uiState.hotspotInfo.batteryLevel}% · Alert at ${uiState.batteryLimitPercent}%"
                        } else if (uiState.isTrialActive) {
                            "Free Trial: ${3 - uiState.batteryTrialUsed} sessions left"
                        } else {
                            "Current: ${uiState.hotspotInfo.batteryLevel}% · Premium feature"
                        },
                        progress = (uiState.hotspotInfo.batteryLevel.toFloat() / 100).coerceIn(0f, 1f),
                        sliderValue = uiState.batteryLimitPercent.toFloat(),
                        sliderRange = 5f..50f,
                        sliderLabel = { "Alert at ${it.toInt()}%" },
                        enabled = uiState.batteryLimitEnabled,
                        onToggle = onToggleBatteryLimit,
                        onSliderChange = { onUpdateBatteryLimit(it.toInt()) },
                        isPremium = uiState.isPremium || uiState.isTrialActive,
                        onRequestUpgrade = onRequestUpgrade,
                    )
                }
            }

            if (uiState.showUpgradePrompt) {
                item {
                    ContextualUpgradePrompt(onRequestUpgrade = onRequestUpgrade)
                }
            }

        }

        if (uiState.showRatingPrompt) {
            RatingPromptDialog(onDismiss = onDismissRatingPrompt)
        }

        if (showQrSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQrSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                QrSharingBottomSheet(
                    ssid = uiState.hotspotInfo.ssid ?: "",
                    password = uiState.hotspotInfo.password,
                )
            }
        }

        if (uiState.showOnboarding) {
            WelcomeBottomSheet(onDismiss = onDismissOnboarding)
        }
    }
}

@Composable
private fun HotspotStatusCard(uiState: HomeUiState, onShareClick: () -> Unit) {
    val (icon, label, color) = when (uiState.hotspotInfo.state) {
        HotspotState.ENABLED -> Triple(Icons.Default.Wifi, "Hotspot Active", MaterialTheme.colorScheme.primary)
        HotspotState.DISABLED -> Triple(Icons.Default.WifiOff, "Hotspot Off", MaterialTheme.colorScheme.onSurfaceVariant)
        HotspotState.ENABLING -> Triple(Icons.Default.Wifi, "Turning On...", MaterialTheme.colorScheme.primary)
        HotspotState.DISABLING -> Triple(Icons.Default.WifiOff, "Turning Off...", MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple(Icons.Default.WifiOff, "Unknown", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (uiState.hotspotInfo.state == HotspotState.ENABLED && uiState.capabilities.canReadSsidAndPassword) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "Share QR", tint = color)
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(48.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = MaterialTheme.typography.headlineMedium, color = color)
                    if (uiState.isPremium) {
                        Spacer(Modifier.width(8.dp))
                        ProBadge()
                    }
                }
                uiState.hotspotInfo.ssid?.let { ssid ->
                    Text(ssid, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrSharingBottomSheet(ssid: String, password: String?) {
    val qrBitmap = remember(ssid, password) { QrGenerator.generateWifiQrCode(ssid, password) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Scan to Connect", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.size(260.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                qrBitmap?.let {
                    androidx.compose.foundation.Image(it.asImageBitmap(), "QR", modifier = Modifier.fillMaxSize())
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ssid, style = MaterialTheme.typography.titleLarge)
            Text("Sharing secure connection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MainActionButton(uiState: HomeUiState, onToggleOrOpenSettings: () -> Unit) {
    val isEnabled = uiState.hotspotInfo.state == HotspotState.ENABLED
    val label = when {
        uiState.capabilities.canToggleProgrammatically -> if (isEnabled) "Turn Off Hotspot" else "Turn On Hotspot"
        else -> if (isEnabled) "Hotspot is Active" else "Open Settings to Enable"
    }

    Button(
        onClick = onToggleOrOpenSettings,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SessionTimerCard(elapsedSeconds: Long) {
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val time = "%02d:%02d:%02d".format(h, m, s)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Session time", style = MaterialTheme.typography.bodyMedium)
            Text(time, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ProBadge() {
    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) {
        Text("PRO", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun ContextualUpgradePrompt(onRequestUpgrade: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), onClick = onRequestUpgrade) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Enjoying Smart Hotspot?", style = MaterialTheme.typography.titleMedium)
                Text("Unlock Battery Protector & Data monitoring to automate your sharing.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}

@Composable
private fun ManagementCard(title: String, info: String, progress: Float, sliderValue: Float, sliderRange: ClosedFloatingPointRange<Float>, sliderLabel: (Float) -> String, enabled: Boolean, onToggle: () -> Unit, onSliderChange: (Float) -> Unit, isPremium: Boolean, onRequestUpgrade: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), enabled = isPremium, onClick = { if (!isPremium) onRequestUpgrade() }, colors = CardDefaults.cardColors(containerColor = if (enabled && isPremium) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Switch(checked = enabled, onCheckedChange = { if (isPremium) onToggle() else onRequestUpgrade() }, thumbContent = if (!isPremium) { { Icon(Icons.Default.Settings, null, modifier = Modifier.size(12.dp)) } } else null)
            }
            Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = if (enabled && isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            if (isPremium) {
                Text(sliderLabel(sliderValue), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                androidx.compose.material3.Slider(value = sliderValue, onValueChange = onSliderChange, valueRange = sliderRange, modifier = Modifier.fillMaxWidth(), enabled = enabled)
            }
        }
    }
}

@Composable
private fun RatingPromptDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enjoying the app?") },
        text = { Text("A quick rating helps other users find us and keeps the app free.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Rate") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("Welcome to Smart Hotspot!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("Discover modern tethering tools designed to automate and protect your mobile sharing.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                FeatureRow(icon = Icons.Default.Wifi, title = "Easy Sharing", desc = "Toggle hotspot or scan QR code to connect instantly.")
                FeatureRow(icon = Icons.Default.Star, title = "Smart Protection", desc = "Set data caps and battery alerts to stay in control.")
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Get Started") }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
