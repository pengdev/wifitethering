package com.geminiapps.wifitethering.domain

import android.content.Context
import android.os.Build
import com.geminiapps.wifitethering.data.model.ConnectedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * True when device enumeration is expected to produce reliable results.
     * /proc/net/arp is readable on API < 29. On API 29+, the file is restricted
     * and ip neigh often fails on stock Android — not reliable.
     * Note: android.net.TetheringManager (the proper API 30+ solution) is @SystemApi
     * and not accessible to regular apps.
     */
    val canScanReliably: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /**
     * A live Flow of connected devices, polling every 30 seconds.
     */
    fun deviceFlow(): Flow<List<ConnectedDevice>> = flow {
        while (true) {
            emit(scanDevices())
            delay(30_000)
        }
    }

    /**
     * One-shot scan — used for manual refresh.
     */
    suspend fun scanDevices(): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            scanViaArpFile()
        } else {
            scanViaIpNeigh()
        }
    }

    private fun scanViaArpFile(): List<ConnectedDevice> {
        return try {
            val lines = java.io.File("/proc/net/arp").readLines()
            // Format: IP HW Flags HWaddress Mask Device
            lines.drop(1).mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 6) return@mapNotNull null
                val ip = parts[0]
                val flags = parts[2]
                val mac = parts[3]
                val iface = parts[5]
                if (flags == "0x0" || mac == "00:00:00:00:00:00") return@mapNotNull null
                ConnectedDevice(ip = ip, mac = mac, iface = iface)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun scanViaIpNeigh(): List<ConnectedDevice> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("ip", "neigh", "show"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Format: IP dev IFACE lladdr MAC STATEINFO
            output.lines().mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 5) return@mapNotNull null
                val ip = parts[0]
                val iface = if (parts.size > 2 && parts[1] == "dev") parts[2] else "unknown"
                val mac = parts.firstOrNull {
                    it.matches(Regex("[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}"))
                } ?: return@mapNotNull null
                val state = parts.last()
                if (state == "FAILED" || state == "INCOMPLETE") return@mapNotNull null
                ConnectedDevice(ip = ip, mac = mac, iface = iface)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
