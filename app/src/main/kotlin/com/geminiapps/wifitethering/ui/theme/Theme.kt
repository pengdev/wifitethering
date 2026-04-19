package com.geminiapps.wifitethering.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val primaryGreen = Color(0xFF00E676)
private val onPrimary = Color(0xFF003916)
private val background = Color(0xFF121212)
private val surface = Color(0xFF1E1E1E)
private val onSurface = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = primaryGreen,
    onPrimary = onPrimary,
    background = background,
    surface = surface,
    onSurface = onSurface,
    onBackground = onSurface,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006D36),
    onPrimary = Color.White,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    onBackground = Color(0xFF1A1A1A),
)

enum class AppTheme { SYSTEM, DARK, LIGHT }

@Composable
fun WifiTetheringTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // API 31+: use wallpaper-derived Material You colors
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (useDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
