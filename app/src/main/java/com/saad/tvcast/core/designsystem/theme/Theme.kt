package com.saad.tvcast.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B4D91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F0FA),
    onPrimaryContainer = Color(0xFF072A52),
    secondary = Color(0xFF0B7A75),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFFFBFCFF),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = Color(0xFF4B5563),
    error = Color(0xFFB42318)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF062748),
    primaryContainer = Color(0xFF123E68),
    onPrimaryContainer = Color(0xFFD8EAFF),
    secondary = Color(0xFF7FD8D2),
    tertiary = Color(0xFFFFC766),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF151B23),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF243244),
    onSurfaceVariant = Color(0xFFC7D2E2),
    error = Color(0xFFFFB4AB)
)

@Composable
fun TVCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TVCastTypography,
        shapes = TVCastShapes,
        content = content
    )
}
