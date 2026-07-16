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
    primaryContainer = Color(0xFFD9EAFF),
    onPrimaryContainer = Color(0xFF072A52),
    secondary = Color(0xFF0A7F83),
    secondaryContainer = Color(0xFFD9F4F3),
    onSecondaryContainer = Color(0xFF043D40),
    tertiary = Color(0xFFE06D2F),
    tertiaryContainer = Color(0xFFFFE4D3),
    onTertiaryContainer = Color(0xFF5A2106),
    background = Color(0xFFF7FAFF),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE6EDF7),
    onSurfaceVariant = Color(0xFF48576B),
    error = Color(0xFFB42318)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF062748),
    primaryContainer = Color(0xFF123E68),
    onPrimaryContainer = Color(0xFFD8EAFF),
    secondary = Color(0xFF7FD8D2),
    secondaryContainer = Color(0xFF124A4D),
    onSecondaryContainer = Color(0xFFCBF4F1),
    tertiary = Color(0xFFFFB088),
    tertiaryContainer = Color(0xFF71330F),
    onTertiaryContainer = Color(0xFFFFE4D3),
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
