package com.geminiapps.wifitethering.domain

import android.os.Build
import com.geminiapps.wifitethering.data.model.ConnectedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceScanner @Inject constructor() {

    /**
     * Returns connected devices using a tiered approach based on API level:
     *  - API < 29: Read /proc/net/arp (reliable)
     *  - API 29+:  Try `ip neigh` command (works on some devices)
     *  - Fallback: Returns empty list — caller should show a graceful message
     */
    suspend fun scanDevices(): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            scanViaArpFile()
        } else {
            // Try `ip neigh` as a fallback for restricted /proc/net/arp
            val ipNeighResult = scanViaIpNeigh()
            ipNeighResult
        }
    }

    /**
     * Returns true if scanning is expected to work on this device.
     * On API 29+ the result may always be empty.
     */
    val canScanReliably: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

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
                // Skip stale/failed entries
                if (state == "FAILED" || state == "INCOMPLETE") return@mapNotNull null
                ConnectedDevice(ip = ip, mac = mac, iface = iface)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
