package com.geminiapps.wifitethering.domain

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

enum class HotspotState { ENABLED, DISABLED, ENABLING, DISABLING, UNKNOWN }

data class HotspotInfo(
    val state: HotspotState,
    val ssid: String?,
)

@Singleton
class HotspotManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * Polls hotspot state every 2 seconds and emits updates.
     */
    val hotspotInfo: Flow<HotspotInfo> = flow {
        while (true) {
            emit(currentInfo())
            delay(2_000)
        }
    }

    fun currentInfo(): HotspotInfo {
        val state = getApState()
        val ssid = if (state == HotspotState.ENABLED) readSsid() else null
        return HotspotInfo(state = state, ssid = ssid)
    }

    /**
     * On API < 26: toggle the hotspot programmatically via reflection.
     * On API 26+: open system tethering settings — programmatic toggle is blocked.
     * Returns true if a programmatic toggle was attempted (API < 26), false if settings were opened.
     */
    fun toggleOrOpenSettings(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val currentlyEnabled = isApEnabled()
            setApEnabled(!currentlyEnabled)
            true
        } else {
            openTetheringSettings()
            false
        }
    }

    fun openTetheringSettings() {
        val intent = Intent("com.android.settings.TetherSettings").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        // Fallback to generic wireless settings if TetherSettings intent is not available
        val resolvedIntent = if (context.packageManager.resolveActivity(intent, 0) != null) {
            intent
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(resolvedIntent)
    }

    fun canToggleProgrammatically(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    // --- Reflection helpers (API 21-25 only) ---

    @Suppress("DEPRECATION")
    private fun setApEnabled(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return
        try {
            // Disable WiFi client mode before enabling AP
            if (enable) wifiManager.isWifiEnabled.let { if (it) wifiManager.isWifiEnabled = false }
            val method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.java
            )
            method.invoke(wifiManager, null, enable)
        } catch (_: Exception) {
            // Reflection failed — open settings as fallback
            openTetheringSettings()
        }
    }

    private fun isApEnabled(): Boolean {
        return try {
            val method = wifiManager.javaClass.getMethod("isWifiApEnabled")
            method.invoke(wifiManager) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getApState(): HotspotState {
        return try {
            val method = wifiManager.javaClass.getMethod("getWifiApState")
            when (method.invoke(wifiManager) as? Int) {
                10 -> HotspotState.DISABLING  // WIFI_AP_STATE_DISABLING
                11 -> HotspotState.DISABLED   // WIFI_AP_STATE_DISABLED
                12 -> HotspotState.ENABLING   // WIFI_AP_STATE_ENABLING
                13 -> HotspotState.ENABLED    // WIFI_AP_STATE_ENABLED
                else -> HotspotState.UNKNOWN
            }
        } catch (_: Exception) {
            if (isApEnabled()) HotspotState.ENABLED else HotspotState.DISABLED
        }
    }

    @Suppress("DEPRECATION")
    fun readSsid(): String? {
        return try {
            val method = wifiManager.javaClass.getMethod("getWifiApConfiguration")
            val config = method.invoke(wifiManager) as? android.net.wifi.WifiConfiguration
            config?.SSID
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    fun readPassword(): String? {
        return try {
            val method = wifiManager.javaClass.getMethod("getWifiApConfiguration")
            val config = method.invoke(wifiManager) as? android.net.wifi.WifiConfiguration
            config?.preSharedKey
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    fun applyConfig(ssid: String, password: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return false
        return try {
            val config = android.net.wifi.WifiConfiguration().apply {
                SSID = ssid
                preSharedKey = password
                allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK)
            }
            val method = wifiManager.javaClass.getMethod(
                "setWifiApConfiguration",
                android.net.wifi.WifiConfiguration::class.java
            )
            method.invoke(wifiManager, config) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }
}
