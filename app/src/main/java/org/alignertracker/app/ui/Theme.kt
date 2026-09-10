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
        primary = Color(0xFF245BC3),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE9EFFC),
        onPrimaryContainer = Color(0xFF173C86),
        secondary = Color(0xFF555D6B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE9EDF3),
        onSecondaryContainer = Color(0xFF252C38),
        tertiary = Color(0xFF555D6B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFECEEF2),
        onTertiaryContainer = Color(0xFF303641),
        surfaceTint = Color(0xFF245BC3),
        surfaceContainerLowest = Color.White,
        surfaceContainerHighest = Color(0xFFE3E7ED),
        inverseSurface = Color(0xFF292E38),
        inverseOnSurface = Color(0xFFF0F2F6),
        inversePrimary = Color(0xFFAFC6FF),
        outlineVariant = Color(0xFFD9DDE4),
        surfaceContainerHigh = Color(0xFFE9ECF1),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1C2028),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1C2028),
        surfaceVariant = Color(0xFFE9ECF1),
        onSurfaceVariant = Color(0xFF555D69),
        surfaceContainer = Color(0xFFF0F2F6),
        surfaceContainerLow = Color(0xFFF3F5F8),
        outline = Color(0xFF747C8A),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )
internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFFAFC6FF),
        onPrimary = Color(0xFF102E66),
        primaryContainer = Color(0xFF263D65),
        onPrimaryContainer = Color(0xFFDCE6FF),
        secondary = Color(0xFFC2C8D3),
        onSecondary = Color(0xFF242B36),
        secondaryContainer = Color(0xFF323944),
        onSecondaryContainer = Color(0xFFE2E6ED),
        tertiary = Color(0xFFC2C8D3),
        onTertiary = Color(0xFF242B36),
        tertiaryContainer = Color(0xFF323944),
        onTertiaryContainer = Color(0xFFE2E6ED),
        surfaceTint = Color(0xFFAFC6FF),
        surfaceContainerLowest = Color(0xFF0D1015),
        surfaceContainerHighest = Color(0xFF353B46),
        inverseSurface = Color(0xFFE8EBF1),
        inverseOnSurface = Color(0xFF292E38),
        inversePrimary = Color(0xFF245BC3),
        outlineVariant = Color(0xFF3C424D),
        surfaceContainerHigh = Color(0xFF2B303A),
        background = Color(0xFF12151B),
        onBackground = Color(0xFFE8EBF1),
        surface = Color(0xFF12151B),
        onSurface = Color(0xFFE8EBF1),
        surfaceVariant = Color(0xFF3A414D),
        onSurfaceVariant = Color(0xFFBAC1CD),
        surfaceContainer = Color(0xFF20252E),
        surfaceContainerLow = Color(0xFF1A1E26),
        outline = Color(0xFF8993A2),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

internal val TrackerTypography =
    Typography(
        headlineLarge =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    )
internal val TrackerShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
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
