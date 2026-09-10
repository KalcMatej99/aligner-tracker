package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Original 24-unit pictograms; adjacent native tab labels provide semantics. */
@Composable
internal fun TrackerNavIcon(destination: Destination) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        fun line(x: Float, y: Float, xx: Float, yy: Float) =
            drawLine(ink, Offset(x * u, y * u), Offset(xx * u, yy * u), 1.8f * u, StrokeCap.Round)
        when (destination) {
            Destination.TODAY -> {
                drawCircle(ink, 8.5f * u, style = Stroke(1.8f * u))
                line(12f, 7f, 12f, 12f)
                line(12f, 12f, 15f, 14f)
            }
            Destination.SCHEDULE -> {
                drawRoundRect(
                    ink,
                    Offset(4 * u, 5 * u),
                    Size(16 * u, 16 * u),
                    androidx.compose.ui.geometry.CornerRadius(2 * u),
                    style = Stroke(1.8f * u),
                )
                line(8f, 3f, 8f, 7f)
                line(16f, 3f, 16f, 7f)
                line(4f, 10f, 20f, 10f)
                line(8f, 14f, 10f, 14f)
                line(14f, 14f, 16f, 14f)
                line(8f, 17f, 10f, 17f)
            }
            Destination.HISTORY -> {
                for (y in listOf(6f, 12f, 18f)) {
                    drawCircle(ink, 1.3f * u, Offset(5 * u, y * u))
                    line(10f, y, 20f, y)
                }
            }
            Destination.PROGRESS -> {
                line(5f, 20f, 5f, 12f)
                line(12f, 20f, 12f, 5f)
                line(19f, 20f, 19f, 9f)
            }
            Destination.SETTINGS -> {
                val gear = androidx.compose.ui.graphics.Path()
                for (i in 0..31) {
                    val angle = i * Math.PI / 16
                    val radius = if (i % 4 < 2) 10f else 7.5f
                    val x = (12 + radius * kotlin.math.cos(angle)).toFloat() * u
                    val y = (12 + radius * kotlin.math.sin(angle)).toFloat() * u
                    if (i == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
                }
                gear.close()
                drawPath(gear, ink, style = Stroke(1.6f * u))
                drawCircle(ink, 3f * u, style = Stroke(1.6f * u))
            }
            else -> Unit
        }
    }
}

@Composable
internal fun TrackerMoreIcon() {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        for (y in listOf(5f, 12f, 19f)) drawCircle(ink, 1.8f * u, Offset(12f * u, y * u))
    }
}

@Composable
internal fun TrackerChevron(forward: Boolean, description: String) {
    val ink = androidx.compose.material3.LocalContentColor.current
    val rtl =
        androidx.compose.ui.platform.LocalLayoutDirection.current ==
            androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(Modifier.size(24.dp).then(Modifier.semantics { contentDescription = description })) {
        val u = size.width / 24f
        val pointsRight = forward != rtl
        val x = if (pointsRight) 9f else 15f
        val tip = if (pointsRight) 15f else 9f
        drawLine(ink, Offset(x * u, 6 * u), Offset(tip * u, 12 * u), 1.8f * u, StrokeCap.Round)
        drawLine(ink, Offset(tip * u, 12 * u), Offset(x * u, 18 * u), 1.8f * u, StrokeCap.Round)
    }
}

internal enum class UtilityIcon {
    BACK,
    EDIT,
    DELETE,
}

/** Original utility vectors; the containing button supplies its accessible name. */
@Composable
internal fun TrackerUtilityIcon(icon: UtilityIcon) {
    val ink = androidx.compose.material3.LocalContentColor.current
    val rtl =
        androidx.compose.ui.platform.LocalLayoutDirection.current ==
            androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        fun line(x: Float, y: Float, xx: Float, yy: Float) =
            drawLine(ink, Offset(x * u, y * u), Offset(xx * u, yy * u), 1.8f * u, StrokeCap.Round)
        when (icon) {
            UtilityIcon.BACK -> {
                val tip = if (rtl) 19f else 5f
                val tail = 24f - tip
                val bend = 12f
                line(tail, 12f, tip, 12f)
                line(bend, 5f, tip, 12f)
                line(tip, 12f, bend, 19f)
            }
            UtilityIcon.EDIT -> {
                val path =
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(4 * u, 20 * u)
                        lineTo(5 * u, 15 * u)
                        lineTo(16 * u, 4 * u)
                        lineTo(20 * u, 8 * u)
                        lineTo(9 * u, 19 * u)
                        close()
                    }
                drawPath(path, ink, style = Stroke(1.8f * u))
                line(14f, 6f, 18f, 10f)
            }
            UtilityIcon.DELETE -> {
                line(4f, 6f, 20f, 6f)
                line(9f, 3f, 15f, 3f)
                line(6f, 6f, 7f, 21f)
                line(7f, 21f, 17f, 21f)
                line(17f, 21f, 18f, 6f)
                line(10f, 10f, 10f, 17f)
                line(14f, 10f, 14f, 17f)
            }
        }
    }
}
