package com.geminiapps.wifitethering.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HotspotConfigRoute(
    onNavigateBack: () -> Unit,
    viewModel: HotspotConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HotspotConfigScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSsidChange = viewModel::onSsidChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSave = viewModel::saveConfig,
        onClearResult = viewModel::clearSaveResult,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotConfigScreen(
    uiState: HotspotConfigUiState,
    onNavigateBack: () -> Unit,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearResult: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveResult) {
        when (uiState.saveResult) {
            SaveResult.SUCCESS -> snackbarHostState.showSnackbar("Hotspot config saved")
            SaveResult.FAILED -> snackbarHostState.showSnackbar("Failed to save — hotspot must be off")
            SaveResult.OPEN_SETTINGS -> snackbarHostState.showSnackbar("Opening settings to edit config")
            null -> Unit
        }
        if (uiState.saveResult != null) onClearResult()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hotspot Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.canEditProgrammatically) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        text = "On Android 8+, hotspot name and password can only be changed through system settings. Tap Save to open tethering settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = uiState.ssid,
                onValueChange = onSsidChange,
                label = { Text("Network Name (SSID)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState.canEditProgrammatically,
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState.canEditProgrammatically,
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.canEditProgrammatically) "Save" else "Open Settings to Edit")
            }
        }
    }
}
