package org.alignertracker.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF006A62),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFA1F2E6),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF49645F),
        onSecondary = Color.White,
        background = Color(0xFFF5FAF8),
        onBackground = Color(0xFF19201D),
        surface = Color(0xFFF5FAF8),
        onSurface = Color(0xFF19201D),
        surfaceVariant = Color(0xFFDBE5DF),
        onSurfaceVariant = Color(0xFF3F4945),
        surfaceContainer = Color(0xFFEFF6F2),
        surfaceContainerLow = Color(0xFFF1F8F4),
        outline = Color(0xFF6F7975),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )
internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFF83D5C9),
        onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF005048),
        onPrimaryContainer = Color(0xFFA1F2E6),
        secondary = Color(0xFFB1CCC5),
        onSecondary = Color(0xFF1C3530),
        background = Color(0xFF101D1A),
        onBackground = Color(0xFFDEE5E1),
        surface = Color(0xFF101D1A),
        onSurface = Color(0xFFDEE5E1),
        surfaceVariant = Color(0xFF3F4945),
        onSurfaceVariant = Color(0xFFBEC9C5),
        surfaceContainer = Color(0xFF1A2723),
        surfaceContainerLow = Color(0xFF16211E),
        outline = Color(0xFF89938F),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

@Composable
fun AlignerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
