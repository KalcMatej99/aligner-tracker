package org.alignertracker.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF006A62),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFA1F2E6),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF49645F),
        background = Color(0xFFF5FAF8),
        surface = Color(0xFFF5FAF8),
        surfaceVariant = Color(0xFFDBE5DF),
    )
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF83D5C9),
        onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF005048),
        onPrimaryContainer = Color(0xFFA1F2E6),
        secondary = Color(0xFFB1CCC5),
        background = Color(0xFF101D1A),
        surface = Color(0xFF101D1A),
        surfaceVariant = Color(0xFF3F4945),
    )

@Composable
fun AlignerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
