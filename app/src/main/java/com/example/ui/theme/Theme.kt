package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0F3044),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = VocalPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B1E54),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = PeakAmber,
    onTertiary = Color.Black,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    outline = StudioCardBorder,
    error = PeakRed,
    onError = Color.White
)

@Composable
fun VoxEngineTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VoxEngineTheme(content = content)
}
