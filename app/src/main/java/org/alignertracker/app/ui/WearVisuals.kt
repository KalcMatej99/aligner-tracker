package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import java.time.*
import org.alignertracker.app.R
import org.alignertracker.app.data.ReportMath
import org.alignertracker.app.domain.DaySummary

@Composable
internal fun compactDuration(millis: Long): String =
    stringResource(R.string.short_duration, millis / 3_600_000, millis / 60_000 % 60)

/** Totals, not chronology. Actual day length handles 23/25-hour accounting days. */
internal fun dayParts(day: DaySummary, now: Instant, zone: ZoneId): List<Long> {
    val date = LocalDate.parse(day.date)
    val total = ReportMath.dayMillis(date, zone)
    val elapsed =
        (now.toEpochMilli() - date.atStartOfDay(zone).toInstant().toEpochMilli()).coerceIn(0, total)
    return listOf(
        day.wornMillis,
        day.removedMillis,
        (elapsed - day.trackedMillis).coerceAtLeast(0),
        total - elapsed,
    )
}

@Composable
internal fun BreakdownBar(
    day: DaySummary,
    now: Instant,
    zone: ZoneId,
    scale: Long = ReportMath.dayMillis(LocalDate.parse(day.date), zone),
    target: Boolean = false,
) {
    val parts = dayParts(day, now, zone)
    val colors = MaterialTheme.colorScheme
    val summary =
        readableDate(LocalDate.parse(day.date)) +
            ". " +
            listOf(R.string.worn, R.string.removed, R.string.untracked, R.string.future_time)
                .mapIndexed { i, label ->
                    stringResource(label) +
                        ": " +
                        (if (i < 2 && day.trackedMillis == 0L)
                            stringResource(R.string.report_no_record)
                        else durationLabel(parts[i]))
                }
                .joinToString(". ") +
            if (target)
                ". " +
                    (day.goalMinutes?.let {
                        stringResource(R.string.report_goal_duration, durationLabel(it * 60_000L))
                    } ?: stringResource(R.string.report_goal_unknown))
            else ""
    Canvas(
        Modifier.fillMaxWidth().height(24.dp).clearAndSetSemantics { contentDescription = summary }
    ) {
        var left = 0f
        parts.forEachIndexed { index, value ->
            val width = size.width * value / scale
            if (width > 0) {
                val color =
                    when (index) {
                        0 -> colors.primary
                        1 -> colors.onSurfaceVariant
                        else -> colors.surface
                    }
                drawRect(color, Offset(left, 3.dp.toPx()), Size(width, size.height - 6.dp.toPx()))
                if (index == 1) {
                    var x = left + 3.dp.toPx()
                    while (x < left + width) {
                        drawLine(
                            colors.surface,
                            Offset(x, 4.dp.toPx()),
                            Offset(x, size.height - 4.dp.toPx()),
                            1.dp.toPx(),
                        )
                        x += 6.dp.toPx()
                    }
                }
                if (index == 2) {
                    var x = left + 3.dp.toPx()
                    while (x < left + width) {
                        drawCircle(colors.onSurfaceVariant, 1.dp.toPx(), Offset(x, size.height / 2))
                        x += 6.dp.toPx()
                    }
                }
                drawRect(
                    colors.outline,
                    Offset(left, 3.dp.toPx()),
                    Size(width, size.height - 6.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            }
            left += width
        }
        if (target)
            day.goalMinutes?.let {
                val x = (size.width * it * 60_000L / scale).coerceIn(1f, size.width - 1f)
                drawLine(colors.surface, Offset(x, 0f), Offset(x, size.height), 5.dp.toPx())
                drawLine(colors.onSurface, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
    }
}

@Composable
internal fun DetailsDisclosure(label: Int, content: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) {
        Text(stringResource(label))
        Text(if (open) " −" else " +")
    }
    if (open) content()
}

@Composable
internal fun TrayMilestone(
    label: String,
    date: String,
    estimated: Boolean,
    showStateLabel: Boolean = true,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier.width(16.dp)
                .height(if (showStateLabel) 64.dp else 48.dp)
                .clearAndSetSemantics {}
        ) {
            drawCircle(
                color,
                5.dp.toPx(),
                Offset(size.width / 2, 10.dp.toPx()),
                style = Stroke(if (estimated) 1.dp.toPx() else 4.dp.toPx()),
            )
            drawLine(
                color,
                Offset(size.width / 2, 20.dp.toPx()),
                Offset(size.width / 2, size.height),
                1.dp.toPx(),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(date)
            if (showStateLabel)
                Text(
                    stringResource(
                        if (estimated) R.string.schedule_estimated else R.string.schedule_recorded
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
        }
    }
}

/**
 * Today-only compact values; spoken description retains complete units and missing-target truth.
 */
@Composable
internal fun TodayWearGoal(day: DaySummary) {
    val recorded = day.trackedMillis > 0L
    val worn =
        if (recorded) todayDuration(day.wornMillis / 60_000L)
        else stringResource(R.string.today_value_unknown)
    val goal =
        day.goalMinutes?.let { todayDuration(it.toLong()) }
            ?: stringResource(R.string.today_value_unknown)
    val description =
        stringResource(
            R.string.today_wear_goal_description,
            if (recorded) durationLabel(day.wornMillis)
            else stringResource(R.string.report_no_record),
            day.goalMinutes?.let { durationLabel(it * 60_000L) }
                ?: stringResource(R.string.report_goal_unknown),
        )
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.today_wear_goal_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            stringResource(R.string.today_wear_goal_value, worn, goal),
            style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun todayDuration(minutes: Long): String =
    if (minutes % 60L == 0L) stringResource(R.string.today_hours, minutes / 60L)
    else stringResource(R.string.today_hours_minutes, minutes / 60L, minutes % 60L)
