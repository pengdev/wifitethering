package com.geminiapps.wifitethering.domain

import com.geminiapps.wifitethering.data.model.ConnectedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceScanner @Inject constructor() {

    /**
     * Reads /proc/net/arp and returns currently connected devices.
     * Works on all API levels without special permissions.
     */
    suspend fun scanDevices(): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        try {
            val lines = java.io.File("/proc/net/arp").readLines()
            // Skip header line; format: IP HW Flags HWaddress Mask Device
            lines.drop(1)
                .mapNotNull { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size < 6) return@mapNotNull null
                    val ip = parts[0]
                    val flags = parts[2]
                    val mac = parts[3]
                    val iface = parts[5]
                    // Skip incomplete/empty entries (flags 0x0 means incomplete)
                    if (flags == "0x0" || mac == "00:00:00:00:00:00") return@mapNotNull null
                    ConnectedDevice(ip = ip, mac = mac, iface = iface)
                }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
