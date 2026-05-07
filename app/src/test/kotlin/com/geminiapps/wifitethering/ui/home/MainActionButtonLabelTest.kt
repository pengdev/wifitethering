package com.geminiapps.wifitethering.ui.home

import com.geminiapps.wifitethering.domain.DeviceCapabilities
import com.geminiapps.wifitethering.domain.HotspotInfo
import com.geminiapps.wifitethering.domain.HotspotState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActionButtonLabelTest {

    private fun labelFor(canToggle: Boolean, state: HotspotState): String {
        val isEnabled = state == HotspotState.ENABLED
        return when {
            canToggle -> if (isEnabled) "Turn Off Hotspot" else "Turn On Hotspot"
            else -> if (isEnabled) "Hotspot is Active" else "Open Settings to Enable"
        }
    }

    @Test
    fun `label is Turn On Hotspot when canToggle and hotspot off`() {
        assertEquals("Turn On Hotspot", labelFor(canToggle = true, state = HotspotState.DISABLED))
    }

    @Test
    fun `label is Turn Off Hotspot when canToggle and hotspot on`() {
        assertEquals("Turn Off Hotspot", labelFor(canToggle = true, state = HotspotState.ENABLED))
    }

    @Test
    fun `label is Hotspot is Active when cannot toggle and hotspot on`() {
        assertEquals("Hotspot is Active", labelFor(canToggle = false, state = HotspotState.ENABLED))
    }

    @Test
    fun `label is Open Settings to Enable when cannot toggle and hotspot off`() {
        assertEquals("Open Settings to Enable", labelFor(canToggle = false, state = HotspotState.DISABLED))
    }
}
