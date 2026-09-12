package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.*
import org.alignertracker.app.R
import org.alignertracker.app.domain.TrackerSnapshot

internal enum class HistoryTimeState {
    IN,
    OUT,
    UNTRACKED,
    FUTURE,
}

internal data class HistoryTimeSpan(val start: Long, val end: Long, val state: HistoryTimeState)

/** Presentation projection only. Boundaries use instants, including 23/25-hour local days. */
internal fun historyTimeSpans(
    snapshot: TrackerSnapshot,
    date: LocalDate,
    now: Instant,
    zone: ZoneId,
): List<HistoryTimeSpan> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val plan = snapshot.plan
    val gaps =
        snapshot.trackingGaps
            .filter { it.endAt > start && it.startAt < end }
            .sortedBy { it.startAt }
    val bounds =
        (listOf(start, end, now.toEpochMilli()) +
                listOfNotNull(plan?.trackingStartedAt, plan?.completedAt) +
                snapshot.events.map { it.at } +
                gaps.flatMap { listOf(it.startAt, it.endAt) })
            .filter { it in start..end }
            .distinct()
            .sorted()
    var eventIndex = -1
    var gapIndex = 0
    val result = mutableListOf<HistoryTimeSpan>()
    bounds.zipWithNext().forEach { (a, b) ->
        while (
            eventIndex + 1 < snapshot.events.size && snapshot.events[eventIndex + 1].at <= a
        ) eventIndex++
        while (gapIndex < gaps.size && gaps[gapIndex].endAt <= a) gapIndex++
        val event = snapshot.events.getOrNull(eventIndex)
        val state =
            when {
                a >= now.toEpochMilli() -> HistoryTimeState.FUTURE
                plan == null ||
                    a < plan.trackingStartedAt ||
                    a >= (plan.completedAt ?: Long.MAX_VALUE) ||
                    event == null ||
                    gaps.getOrNull(gapIndex)?.let { a >= it.startAt && a < it.endAt } == true ->
                    HistoryTimeState.UNTRACKED
                event.wearing -> HistoryTimeState.IN
                else -> HistoryTimeState.OUT
            }
        if (result.lastOrNull()?.state == state)
            result[result.lastIndex] = result.last().copy(end = b)
        else result += HistoryTimeSpan(a, b, state)
    }
    return result
}

@Composable
internal fun HistoryDayTimeline(
    snapshot: TrackerSnapshot,
    date: LocalDate,
    now: Instant,
    zone: ZoneId,
) {
    val spans = historyTimeSpans(snapshot, date, now, zone)
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val colors = MaterialTheme.colorScheme
    val labels =
        listOf(R.string.state_in, R.string.state_out, R.string.untracked, R.string.future_time)
            .map { stringResource(it) }
    val textMeasurer = rememberTextMeasurer()
    val nowText =
        textMeasurer.measure(
            stringResource(R.string.timeline_now),
            MaterialTheme.typography.labelSmall.copy(color = colors.onSurface),
        )
    val isToday = date == now.atZone(zone).toLocalDate()
    val labelSpace = with(LocalDensity.current) { nowText.size.height.toDp() } + 6.dp
    val barHeight = maxOf(28.dp, with(LocalDensity.current) { nowText.size.height.toDp() } + 12.dp)
    val description = stringResource(R.string.history_timeline)
    val intervalDescriptions =
        spans.map { span ->
            stringResource(
                R.string.history_time_interval,
                labels[span.state.ordinal],
                readableTime(span.start, zone),
                readableTime(span.end, zone),
            )
        }
    Column(
        Modifier.clearAndSetSemantics {
            contentDescription = description + ". " + intervalDescriptions.joinToString(". ")
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.history_timeline),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Canvas(Modifier.fillMaxWidth().height(barHeight + if (isToday) labelSpace else 0.dp)) {
            val top = if (isToday) labelSpace.toPx() else 3.dp.toPx()
            val bottom = size.height - 3.dp.toPx()
            spans.forEach { span ->
                val left = size.width * (span.start - start).toFloat() / (end - start)
                val width = size.width * (span.end - span.start).toFloat() / (end - start)
                clipRect(left, top, left + width, bottom) {
                    when (span.state) {
                        HistoryTimeState.IN ->
                            drawRect(colors.primary, Offset(left, top), Size(width, bottom - top))
                        HistoryTimeState.OUT -> {
                            drawRect(
                                colors.surfaceContainerHigh,
                                Offset(left, top),
                                Size(width, bottom - top),
                            )
                            val stroke = minOf(1.5.dp.toPx(), width)
                            drawRect(
                                colors.onSurfaceVariant,
                                Offset(left + stroke / 2, top + stroke / 2),
                                Size((width - stroke).coerceAtLeast(0f), bottom - top - stroke),
                                style = Stroke(stroke),
                            )
                        }
                        HistoryTimeState.UNTRACKED -> {
                            drawRect(colors.surface, Offset(left, top), Size(width, bottom - top))
                            var x = left + 3.dp.toPx()
                            while (x < left + width) {
                                drawCircle(
                                    colors.onSurfaceVariant,
                                    1.dp.toPx(),
                                    Offset(x, (top + bottom) / 2),
                                )
                                x += 6.dp.toPx()
                            }
                            drawRect(
                                colors.outline,
                                Offset(left, top),
                                Size(width, bottom - top),
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                        HistoryTimeState.FUTURE -> {
                            drawLine(
                                colors.outline,
                                Offset(left, (top + bottom) / 2),
                                Offset(left + width, (top + bottom) / 2),
                                1.dp.toPx(),
                            )
                        }
                    }
                }
            }
            if (isToday) {
                val x = size.width * (now.toEpochMilli() - start).toFloat() / (end - start)
                drawText(
                    nowText,
                    topLeft =
                        Offset(
                            (x - nowText.size.width / 2).coerceIn(
                                0f,
                                (size.width - nowText.size.width).coerceAtLeast(0f),
                            ),
                            0f,
                        ),
                )
                drawLine(
                    colors.onSurface,
                    Offset(x, nowText.size.height + 2.dp.toPx()),
                    Offset(x, bottom),
                    1.dp.toPx(),
                )
            }
        }
        BoxWithConstraints {
            val hours =
                if (maxWidth / LocalDensity.current.fontScale < 280.dp) listOf(0, 12, 24)
                else listOf(0, 6, 12, 18, 24)
            val positions =
                hours.map { hour ->
                    (date
                            .atStartOfDay()
                            .plusHours(hour.toLong())
                            .atZone(zone)
                            .toInstant()
                            .toEpochMilli() - start)
                        .toFloat() / (end - start)
                }
            Layout(
                content = {
                    hours.forEach {
                        Text(
                            stringResource(R.string.history_axis_hour, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { measurables, constraints ->
                val children =
                    measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
                layout(constraints.maxWidth, children.maxOf { it.height }) {
                    children.forEachIndexed { i, child ->
                        child.place(
                            (positions[i] * constraints.maxWidth - child.width / 2)
                                .toInt()
                                .coerceIn(0, constraints.maxWidth - child.width),
                            0,
                        )
                    }
                }
            }
        }
    }
}
