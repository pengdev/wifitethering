package com.geminiapps.wifitethering.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geminiapps.wifitethering.ui.config.HotspotConfigRoute
import com.geminiapps.wifitethering.ui.devices.DevicesRoute
import com.geminiapps.wifitethering.ui.home.HomeRoute
import com.geminiapps.wifitethering.ui.scheduler.SchedulerRoute
import com.geminiapps.wifitethering.ui.settings.SettingsRoute
import com.geminiapps.wifitethering.ui.upgrade.UpgradeBottomSheet

object Routes {
    const val HOME = "home"
    const val DEVICES = "devices"
    const val SCHEDULER = "scheduler"
    const val CONFIG = "config"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(onRequestUpgrade: () -> Unit) {
    val navController = rememberNavController()
    var showUpgradeSheet by remember { mutableStateOf(false) }

    val triggerUpgrade = { showUpgradeSheet = true }

    if (showUpgradeSheet) {
        UpgradeBottomSheet(
            onDismiss = { showUpgradeSheet = false },
            onConfirmUpgrade = {
                showUpgradeSheet = false
                onRequestUpgrade()
            }
        )
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                onNavigateToDevices = { navController.navigate(Routes.DEVICES) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onRequestUpgrade = triggerUpgrade,
            )
        }
        composable(Routes.DEVICES) {
            DevicesRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SCHEDULER) {
            SchedulerRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.CONFIG) {
            HotspotConfigRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScheduler = { navController.navigate(Routes.SCHEDULER) },
                onNavigateToConfig = { navController.navigate(Routes.CONFIG) },
                onRequestUpgrade = triggerUpgrade,
            )
        }
    }
}
