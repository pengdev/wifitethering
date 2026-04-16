package com.geminiapps.wifitethering.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.geminiapps.wifitethering.MainActivity
import com.geminiapps.wifitethering.R
import com.geminiapps.wifitethering.domain.HotspotManager
import com.geminiapps.wifitethering.domain.HotspotState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HotspotWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var hotspotManager: HotspotManager

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            // Need to re-inject manually or use entry points if Hilt fails in onReceive
            // For simplicity in this demo, we'll try to use the injected manager
            hotspotManager.toggleOrOpenSettings()
            
            // Trigger a manual update to reflect 'Enabling' state immediately
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, HotspotWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, ids)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val info = hotspotManager.currentInfo()
        val views = RemoteViews(context.packageName, R.layout.widget_hotspot)

        val statusText = when (info.state) {
            HotspotState.ENABLED -> info.ssid ?: "Active"
            HotspotState.ENABLING -> "Turning on..."
            HotspotState.DISABLING -> "Turning off..."
            else -> "Hotspot Off"
        }

        val iconRes = if (info.state == HotspotState.ENABLED) {
            android.R.drawable.ic_menu_delete // Replace with your wifi icon
        } else {
            android.R.drawable.ic_menu_add
        }

        views.setTextViewText(R.id.widget_status, statusText)
        views.setImageViewResource(R.id.widget_icon, iconRes)

        val intent = Intent(context, HotspotWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        private const val ACTION_TOGGLE = "com.geminiapps.wifitethering.ACTION_TOGGLE_WIDGET"
    }
}
