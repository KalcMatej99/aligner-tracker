package org.alignertracker.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF245C4B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7ECDD),
        onPrimaryContainer = Color(0xFF153C2E),
        secondary = Color(0xFF526459),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0E9DA),
        onSecondaryContainer = Color(0xFF29372C),
        tertiary = Color(0xFF80523D),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF5DFD1),
        onTertiaryContainer = Color(0xFF573724),
        outlineVariant = Color(0xFFC3CBC0),
        surfaceContainerHigh = Color(0xFFE3E7DF),
        background = Color(0xFFFAF9F5),
        onBackground = Color(0xFF202923),
        surface = Color(0xFFFAF9F5),
        onSurface = Color(0xFF202923),
        surfaceVariant = Color(0xFFE3E7DF),
        onSurfaceVariant = Color(0xFF444D46),
        surfaceContainer = Color(0xFFEBEEE6),
        surfaceContainerLow = Color(0xFFF0F2EB),
        outline = Color(0xFF707A70),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )
internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFFA2D3B5),
        onPrimary = Color(0xFF103828),
        primaryContainer = Color(0xFF254F3C),
        onPrimaryContainer = Color(0xFFD7ECDD),
        secondary = Color(0xFFBDCDBD),
        onSecondary = Color(0xFF29372C),
        secondaryContainer = Color(0xFF36483A),
        onSecondaryContainer = Color(0xFFDBE8D8),
        tertiary = Color(0xFFEAB99E),
        onTertiary = Color(0xFF482B1B),
        tertiaryContainer = Color(0xFF543B2D),
        onTertiaryContainer = Color(0xFFFFDBC5),
        outlineVariant = Color(0xFF444F44),
        surfaceContainerHigh = Color(0xFF2D372F),
        background = Color(0xFF131A16),
        onBackground = Color(0xFFE3E8DF),
        surface = Color(0xFF131A16),
        onSurface = Color(0xFFE3E8DF),
        surfaceVariant = Color(0xFF444D46),
        onSurfaceVariant = Color(0xFFC2CCC0),
        surfaceContainer = Color(0xFF222B24),
        surfaceContainerLow = Color(0xFF1B231D),
        outline = Color(0xFF8D988C),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

internal val TrackerTypography =
    Typography(
        headlineLarge =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    )
internal val TrackerShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

@Composable
fun AlignerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = TrackerTypography,
        shapes = TrackerShapes,
        content = content,
    )
}
