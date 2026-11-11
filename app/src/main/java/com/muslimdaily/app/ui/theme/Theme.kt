package com.muslimdaily.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenSoft,
    onPrimaryContainer = GreenDark,
    secondary = Gold,
    background = SurfaceLight,
    surface = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextMuted,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDarkMode,
    onPrimary = SurfaceDark,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenSoft,
    secondary = Gold,
    background = SurfaceDark,
    surface = CardDark,
    onBackground = Color(0xFFE6F0EA),
    onSurface = Color(0xFFE6F0EA),
    onSurfaceVariant = Color(0xFF9DB0A5),
)

@Composable
fun MuslimDailyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
