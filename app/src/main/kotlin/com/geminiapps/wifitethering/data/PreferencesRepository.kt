package com.geminiapps.wifitethering.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.geminiapps.wifitethering.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserPreferences(
    val isPremium: Boolean,
    val appTheme: AppTheme,
    val hasRated: Boolean,
    val usageCount: Int,
    val batteryThreshold: Int,
    val dataLimitEnabled: Boolean,
    val dataLimitMb: Int,
    val batteryLimitEnabled: Boolean,
    val batteryLimitPercent: Int,
    val hotspotOnCount: Int,
    val batteryTrialSessionsUsed: Int,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext val context: Context,
) {
    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val APP_THEME = intPreferencesKey("app_theme")
        val USAGE_COUNT = intPreferencesKey("usage_count")
        val HAS_RATED = booleanPreferencesKey("has_rated")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold")
        val DATA_LIMIT_ENABLED = booleanPreferencesKey("data_limit_enabled")
        val DATA_LIMIT_MB = intPreferencesKey("data_limit_mb")
        val BATTERY_LIMIT_ENABLED = booleanPreferencesKey("battery_limit_enabled")
        val BATTERY_LIMIT_PERCENT = intPreferencesKey("battery_limit_percent")
        val HOTSPOT_ON_COUNT = intPreferencesKey("hotspot_on_count")
        val BATTERY_TRIAL_SESSIONS = intPreferencesKey("battery_trial_sessions")
    }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            if (com.geminiapps.wifitethering.BuildConfig.DEBUG) true
            else prefs[Keys.IS_PREMIUM] ?: false
        }

    val appTheme: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            AppTheme.entries.getOrNull(prefs[Keys.APP_THEME] ?: 0) ?: AppTheme.SYSTEM
        }

    val hasRated: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.HAS_RATED] ?: false }

    val usageCount: Flow<Int> = context.dataStore.data
        .map { it[Keys.USAGE_COUNT] ?: 0 }

    val batteryThreshold: Flow<Int> = context.dataStore.data
        .map { it[Keys.BATTERY_THRESHOLD] ?: 20 }

    val dataLimitEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.DATA_LIMIT_ENABLED] ?: false }

    val dataLimitMb: Flow<Int> = context.dataStore.data
        .map { it[Keys.DATA_LIMIT_MB] ?: 1000 }

    val batteryLimitEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.BATTERY_LIMIT_ENABLED] ?: false }

    val batteryLimitPercent: Flow<Int> = context.dataStore.data
        .map { it[Keys.BATTERY_LIMIT_PERCENT] ?: 20 }

    val hotspotOnCount: Flow<Int> = context.dataStore.data
        .map { it[Keys.HOTSPOT_ON_COUNT] ?: 0 }

    val batteryTrialSessionsUsed: Flow<Int> = context.dataStore.data
        .map { it[Keys.BATTERY_TRIAL_SESSIONS] ?: 0 }

    val userPreferences: Flow<UserPreferences> = combine(
        isPremium,
        appTheme,
        hasRated,
        usageCount,
        batteryThreshold,
        dataLimitEnabled,
        dataLimitMb,
        batteryLimitEnabled,
        batteryLimitPercent,
        hotspotOnCount,
        batteryTrialSessionsUsed,
    ) { args ->
        UserPreferences(
            isPremium = args[0] as Boolean,
            appTheme = args[1] as AppTheme,
            hasRated = args[2] as Boolean,
            usageCount = args[3] as Int,
            batteryThreshold = args[4] as Int,
            dataLimitEnabled = args[5] as Boolean,
            dataLimitMb = args[6] as Int,
            batteryLimitEnabled = args[7] as Boolean,
            batteryLimitPercent = args[8] as Int,
            hotspotOnCount = args[9] as Int,
            batteryTrialSessionsUsed = args[10] as Int,
        )
    }

    suspend fun setPremium(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_PREMIUM] = value }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.ordinal }
    }

    suspend fun incrementUsageCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.USAGE_COUNT] = (prefs[Keys.USAGE_COUNT] ?: 0) + 1
        }
    }

    suspend fun setHasRated(value: Boolean) {
        context.dataStore.edit { it[Keys.HAS_RATED] = value }
    }

    suspend fun setBatteryThreshold(value: Int) {
        context.dataStore.edit { it[Keys.BATTERY_THRESHOLD] = value }
    }

    suspend fun setDataLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DATA_LIMIT_ENABLED] = enabled }
    }

    suspend fun setDataLimitMb(mb: Int) {
        context.dataStore.edit { it[Keys.DATA_LIMIT_MB] = mb }
    }

    suspend fun setBatteryLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BATTERY_LIMIT_ENABLED] = enabled }
    }

    suspend fun setBatteryLimitPercent(percent: Int) {
        context.dataStore.edit { it[Keys.BATTERY_LIMIT_PERCENT] = percent }
    }

    suspend fun incrementHotspotOnCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOTSPOT_ON_COUNT] = (prefs[Keys.HOTSPOT_ON_COUNT] ?: 0) + 1
        }
    }

    suspend fun incrementBatteryTrialSessions() {
        context.dataStore.edit { prefs ->
            prefs[Keys.BATTERY_TRIAL_SESSIONS] = (prefs[Keys.BATTERY_TRIAL_SESSIONS] ?: 0) + 1
        }
    }
}
