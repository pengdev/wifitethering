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
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val APP_THEME = intPreferencesKey("app_theme")
        val USAGE_COUNT = intPreferencesKey("usage_count")
        val HAS_RATED = booleanPreferencesKey("has_rated")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold")
    }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.IS_PREMIUM] ?: false }

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
}
