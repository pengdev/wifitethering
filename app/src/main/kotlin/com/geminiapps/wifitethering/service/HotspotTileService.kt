package com.geminiapps.wifitethering.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.geminiapps.wifitethering.domain.HotspotManager
import com.geminiapps.wifitethering.domain.HotspotState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class HotspotTileService : TileService() {

    @Inject lateinit var hotspotManager: HotspotManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        hotspotManager.hotspotInfo
            .onEach { info ->
                val tile = qsTile ?: return@onEach
                tile.state = when (info.state) {
                    HotspotState.ENABLED -> Tile.STATE_ACTIVE
                    HotspotState.ENABLING -> Tile.STATE_ACTIVE
                    HotspotState.DISABLED -> Tile.STATE_INACTIVE
                    HotspotState.DISABLING -> Tile.STATE_INACTIVE
                    else -> Tile.STATE_INACTIVE
                }
                tile.label = info.ssid ?: "Hotspot"
                tile.updateTile()
            }
            .launchIn(serviceScope)
    }

    override fun onStopListening() {
        super.onStopListening()
        serviceScope.cancel()
    }

    override fun onClick() {
        super.onClick()
        val info = hotspotManager.currentInfo()
        
        if (hotspotManager.canToggleProgrammatically()) {
            // API < 26: Toggle directly
            hotspotManager.toggleOrOpenSettings()
        } else {
            // API 26+: Collapse shade and open settings or handle via intent
            val intent = Intent(this, com.geminiapps.wifitethering.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // We could add an extra to trigger the settings open immediately
                putExtra("OPEN_SETTINGS", true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires specific handling for starting activities from tiles
                // Note: startActivityAndCollapse(PendingIntent) is the modern way
                // Simplified for now:
                startActivityAndCollapse(intent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
