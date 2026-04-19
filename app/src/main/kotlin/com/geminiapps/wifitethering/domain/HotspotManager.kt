package com.geminiapps.wifitethering.domain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

enum class HotspotState { ENABLED, DISABLED, ENABLING, DISABLING, UNKNOWN }

data class HotspotInfo(
    val state: HotspotState,
    val ssid: String?,
    val password: String? = null,
    val batteryLevel: Int = -1,
)

/**
 * Describes what this device can actually do based on its API level.
 * Used to adapt the UI so we only show features that work.
 */
data class DeviceCapabilities(
    val canToggleProgrammatically: Boolean,   // API < 26
    val canReadSsidAndPassword: Boolean,       // API < 28 (reliable)
    val canEditConfig: Boolean,                // API < 26
    val canScanConnectedDevices: Boolean,      // API < 29 via ARP; API 30+ via TetheringManager
    val canUseTile: Boolean,                   // API >= 24
    val needsNotificationPermission: Boolean,  // API >= 33
    val canUseSoftApCallback: Boolean,         // API >= 27
)

@Singleton
class HotspotManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * On API 27+: emits state changes via SoftApCallback (no polling, no reflection for state).
     * On API <27: polls every 2 seconds via reflection.
     */
    val hotspotInfo: Flow<HotspotInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            softApCallbackFlow()
        } else {
            pollingFlow()
        }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun softApCallbackFlow(): Flow<HotspotInfo> = callbackFlow {
        // Emit current state immediately so collectors don't wait for the first callback
        trySend(currentInfo())

        val callback = object : WifiManager.SoftApCallback {
            override fun onStateChanged(state: Int, failureReason: Int) {
                val hotspotState = hotspotStateFromApState(state)
                val ssid = if (hotspotState == HotspotState.ENABLED) readSsid() else null
                val password = if (hotspotState == HotspotState.ENABLED) readPassword() else null
                trySend(
                    HotspotInfo(
                        state = hotspotState,
                        ssid = ssid,
                        password = password,
                        batteryLevel = getBatteryLevel(),
                    )
                )
            }
        }
        wifiManager.registerSoftApCallback(ContextCompat.getMainExecutor(context), callback)
        awaitClose { wifiManager.unregisterSoftApCallback(callback) }
    }

    private fun pollingFlow(): Flow<HotspotInfo> = flow {
        while (true) {
            emit(currentInfo())
            delay(2_000)
        }
    }

    private fun hotspotStateFromApState(state: Int): HotspotState = when (state) {
        WifiManager.WIFI_AP_STATE_DISABLING -> HotspotState.DISABLING
        WifiManager.WIFI_AP_STATE_DISABLED  -> HotspotState.DISABLED
        WifiManager.WIFI_AP_STATE_ENABLING  -> HotspotState.ENABLING
        WifiManager.WIFI_AP_STATE_ENABLED   -> HotspotState.ENABLED
        else -> HotspotState.UNKNOWN
    }

    fun currentInfo(): HotspotInfo {
        val state = getApState()
        val ssid = if (state == HotspotState.ENABLED) readSsid() else null
        val password = if (state == HotspotState.ENABLED) readPassword() else null
        return HotspotInfo(state = state, ssid = ssid, password = password, batteryLevel = getBatteryLevel())
    }

    fun capabilities(): DeviceCapabilities = DeviceCapabilities(
        canToggleProgrammatically = Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
        canReadSsidAndPassword = Build.VERSION.SDK_INT < Build.VERSION_CODES.P,
        canEditConfig = Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
        canScanConnectedDevices = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        canUseTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
        needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        canUseSoftApCallback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1,
    )

    fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        return batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        } ?: -1
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
            if (enable) wifiManager.isWifiEnabled.let { if (it) wifiManager.isWifiEnabled = false }
            val method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.java
            )
            method.invoke(wifiManager, null, enable)
        } catch (_: Exception) {
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
            hotspotStateFromApState(method.invoke(wifiManager) as? Int ?: -1)
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
