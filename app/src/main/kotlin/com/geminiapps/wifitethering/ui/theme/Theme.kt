package com.geminiapps.wifitethering.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush

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

// AppTheme choices stored in DataStore
enum class AppTheme { SYSTEM, DARK, AMOLED, LIGHT, GLASS }

@Composable
fun WifiTetheringTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED, AppTheme.GLASS -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) {
        if (appTheme == AppTheme.AMOLED) {
            DarkColorScheme.copy(background = Color.Black, surface = Color(0xFF0A0A0A))
        } else if (appTheme == AppTheme.GLASS) {
            DarkColorScheme.copy(
                background = Color.Transparent,
                surface = Color.White.copy(alpha = 0.08f),
                surfaceVariant = Color.White.copy(alpha = 0.12f),
                onSurface = Color.White,
                onSurfaceVariant = Color.White.copy(alpha = 0.7f)
            )
        } else {
            DarkColorScheme
        }
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        if (appTheme == AppTheme.GLASS) {
            Box(Modifier.fillMaxSize()) {
                MeshGradientBackground()
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun MeshGradientBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    )
}
