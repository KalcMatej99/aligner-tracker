package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import java.time.*
import java.time.format.DateTimeFormatter
import org.alignertracker.app.R
import org.alignertracker.app.data.ReportMath
import org.alignertracker.app.domain.DaySummary
import org.alignertracker.app.domain.TrackerSnapshot

/**
 * Closed, fully recorded calendar days only; neither today's partial total nor gaps lower the
 * average.
 */
internal fun completeProgressDays(days: List<DaySummary>, today: LocalDate, zone: ZoneId) =
    days.filter {
        LocalDate.parse(it.date) < today &&
            it.trackedMillis == ReportMath.dayMillis(LocalDate.parse(it.date), zone)
    }

@Composable
internal fun ProgressOverview(
    snapshot: TrackerSnapshot,
    now: Instant,
    onPhotos: (() -> Unit)? = null,
    onTools: (Int) -> Unit,
) {
    var count by rememberSaveable { mutableStateOf(7) }
    val zone = ZoneId.of(snapshot.plan?.zoneId ?: "UTC")
    val today = now.atZone(zone).toLocalDate()
    val days =
        remember(snapshot, count, now.epochSecond / 60) { ReportMath.days(snapshot, count, now) }
    val complete = completeProgressDays(days, today, zone)
    val eligible = complete.filter { it.goalMinutes != null }
    LazyColumn(
        Modifier.widthIn(max = 680.dp).fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Heading(R.string.progress) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30).forEach { period ->
                    FilterChip(
                        selected = count == period,
                        onClick = { count = period },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(stringResource(R.string.progress_window, period)) },
                    )
                }
            }
        }
        if (days.isEmpty()) item { Text(stringResource(R.string.no_progress)) }
        else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.progress_average),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (complete.isEmpty()) stringResource(R.string.progress_unavailable)
                        else compactDuration(complete.sumOf { it.wornMillis } / complete.size),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        if (complete.isEmpty()) stringResource(R.string.progress_wait_complete)
                        else
                            pluralStringResource(
                                R.plurals.progress_average_basis,
                                complete.size,
                                complete.size,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.progress_daily_wear),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(
                                R.string.report_range,
                                readableDate(LocalDate.parse(days.first().date)),
                                readableDate(LocalDate.parse(days.last().date)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(R.string.progress_hours),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        DailyWearChart(days, today, zone)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ProgressKey(0, R.string.progress_recorded)
                            ProgressKey(1, R.string.progress_incomplete)
                            ProgressKey(3, R.string.progress_missing)
                            if (days.any { it.goalMinutes != null })
                                ProgressKey(2, R.string.progress_target)
                        }
                        if (days.none { it.trackedMillis > 0 })
                            Text(
                                stringResource(R.string.no_progress),
                                style = MaterialTheme.typography.bodySmall,
                            )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.progress_coverage),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    CoverageStrip(days, today, zone)
                    Text(
                        stringResource(
                            R.string.progress_coverage_counts,
                            complete.size,
                            days.count { it.trackedMillis > 0 } - complete.size,
                            days.count { it.trackedMillis == 0L },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (eligible.isNotEmpty())
                        Text(
                            stringResource(
                                R.string.progress_target_days,
                                eligible.count { it.wornMillis >= it.goalMinutes!! * 60_000L },
                                eligible.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    else if (days.none { it.goalMinutes != null })
                        Text(
                            stringResource(R.string.report_goal_unknown),
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
            }
        }
        if (onPhotos != null) item { PhotoProgressEntry(snapshot.photos.size, onPhotos) }
        item {
            OutlinedButton(
                onClick = { onTools(count) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.progress_tools))
            }
        }
    }
}

@Composable
private fun DailyWearChart(days: List<DaySummary>, today: LocalDate, zone: ZoneId) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val scale =
        days
            .maxOf { ReportMath.dayMillis(LocalDate.parse(it.date), zone) }
            .coerceAtLeast(24 * 3600000L)
    val locale = LocalConfiguration.current.locales[0]
    val labels =
        if (days.size <= 7 && LocalDensity.current.fontScale < 1.5f) days.indices.toList()
        else listOf(0, days.lastIndex / 2, days.lastIndex)
    // Time runs chronologically left-to-right, including in an RTL interface.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    java.text.NumberFormat.getIntegerInstance(locale).format(scale / 3600000),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    java.text.NumberFormat.getNumberInstance(locale).format(scale / 7200000.0),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    java.text.NumberFormat.getIntegerInstance(locale).format(0),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        listOf(0f, .5f, 1f).forEach {
                            drawLine(
                                grid,
                                Offset(0f, it * size.height),
                                Offset(size.width, it * size.height),
                                1.dp.toPx(),
                            )
                        }
                        val slot = size.width / days.size
                        days.forEachIndexed { i, day ->
                            val x = slot * (i + .17f)
                            val w = slot * .66f
                            val h = size.height * day.wornMillis / scale
                            if (day.trackedMillis == 0L) {
                                drawLine(
                                    ink,
                                    Offset(x, size.height - 3.dp.toPx()),
                                    Offset(x + w, size.height - 3.dp.toPx()),
                                    2.dp.toPx(),
                                )
                            } else {
                                drawRect(primary, Offset(x, size.height - h), Size(w, h))
                                if (day !in completeProgressDays(listOf(day), today, zone)) {
                                    clipRect(x, size.height - h, x + w, size.height) {
                                        var y = size.height - h - w
                                        while (y < size.height) {
                                            drawLine(
                                                surface,
                                                Offset(x, y),
                                                Offset(x + w, y + w),
                                                2.dp.toPx(),
                                            )
                                            y += 7.dp.toPx()
                                        }
                                    }
                                }
                                if (h == 0f)
                                    drawCircle(
                                        primary,
                                        2.dp.toPx(),
                                        Offset(x + w / 2, size.height - 3.dp.toPx()),
                                    )
                            }
                            day.goalMinutes?.let { target ->
                                val y = size.height * (1 - target * 60000f / scale)
                                drawLine(
                                    surface,
                                    Offset(x - 1.dp.toPx(), y),
                                    Offset(x + w + 1.dp.toPx(), y),
                                    5.dp.toPx(),
                                )
                                drawLine(ink, Offset(x, y), Offset(x + w, y), 2.dp.toPx())
                            }
                        }
                    }
                    Row(Modifier.fillMaxSize()) {
                        days.forEach { day ->
                            val status =
                                stringResource(
                                    when {
                                        day.trackedMillis == 0L -> R.string.progress_missing
                                        day in completeProgressDays(listOf(day), today, zone) ->
                                            R.string.progress_complete
                                        else -> R.string.progress_incomplete
                                    }
                                )
                            val target =
                                day.goalMinutes?.let { durationLabel(it * 60000L) }
                                    ?: stringResource(R.string.report_goal_unknown)
                            val description =
                                stringResource(
                                    R.string.progress_day_description,
                                    readableDate(LocalDate.parse(day.date)),
                                    if (day.trackedMillis == 0L)
                                        stringResource(R.string.progress_missing)
                                    else durationLabel(day.wornMillis),
                                    if (day.trackedMillis == 0L)
                                        stringResource(R.string.progress_missing)
                                    else durationLabel(day.removedMillis),
                                    status,
                                    target,
                                )
                            Box(
                                Modifier.weight(1f).fillMaxHeight().semantics {
                                    contentDescription = description
                                }
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    labels.forEach { index ->
                        Text(
                            if (index in labels)
                                LocalDate.parse(days[index].date)
                                    .format(DateTimeFormatter.ofPattern("d", locale))
                            else "",
                            modifier =
                                if (labels.size == days.size) Modifier.weight(1f) else Modifier,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageStrip(days: List<DaySummary>, today: LocalDate, zone: ZoneId) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val complete = completeProgressDays(days, today, zone)
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val slot = size.width / days.size
        days.forEachIndexed { i, day ->
            val x = slot * i
            val w = slot - 3.dp.toPx()
            if (day in complete) drawRect(primary, Offset(x, 0f), Size(w, size.height))
            else {
                drawRect(
                    outline,
                    Offset(x, 0f),
                    Size(w, size.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
                if (day.trackedMillis > 0)
                    drawRect(primary, Offset(x, size.height / 2), Size(w, size.height / 2))
            }
        }
    }
}

@Composable
private fun ProgressKey(kind: Int, label: Int) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp, 12.dp)) {
            if (kind == 2 || kind == 3)
                drawLine(
                    ink,
                    Offset(0f, if (kind == 3) size.height else size.height / 2),
                    Offset(size.width, if (kind == 3) size.height else size.height / 2),
                    2.dp.toPx(),
                )
            else {
                drawRect(primary)
                if (kind == 1)
                    clipRect {
                        for (i in -2..4) drawLine(
                            surface,
                            Offset(i * 6.dp.toPx(), 0f),
                            Offset(i * 6.dp.toPx() + size.height, size.height),
                            2.dp.toPx(),
                        )
                    }
            }
        }
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
    }
}
