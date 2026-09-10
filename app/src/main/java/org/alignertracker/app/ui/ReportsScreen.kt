package org.alignertracker.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.*
import org.alignertracker.app.R
import org.alignertracker.app.data.ReportMath
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.reminders.ReminderPreferences

@Composable
fun ReportsScreen(
    snapshot: TrackerSnapshot,
    model: TrackerViewModel,
    now: Instant,
    preferences: ReminderPreferences,
) {
    if (snapshot.plan == null) {
        ScreenColumn {
            Heading(R.string.reports_title)
            Text(stringResource(R.string.no_progress))
        }
        return
    }
    var showData by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableStateOf(7) }
    var preview by remember { mutableStateOf(false) }
    val zone = ZoneId.of(snapshot.plan.zoneId)
    val days =
        remember(snapshot, count, now.epochSecond / 60) {
            ReportMath.days(snapshot, if (count == 0) now.atZone(zone).dayOfMonth else count, now)
        }
    val context = LocalContext.current
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            uri?.let { model.export(context.contentResolver, it, true) }
        }
    LazyColumn(
        Modifier.widthIn(max = 680.dp).fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Heading(R.string.reports_title) }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (period in listOf(1, 7, 30, 0)) FilterChip(
                    selected = count == period,
                    onClick = { count = period },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = {
                        Text(
                            if (period == 0) stringResource(R.string.report_month)
                            else stringResource(R.string.report_days, period)
                        )
                    },
                )
            }
        }
        item {
            Text(
                stringResource(
                    R.string.report_range,
                    readableDate(LocalDate.parse(days.first().date)),
                    readableDate(LocalDate.parse(days.last().date)),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            SummaryRow(
                R.string.range_wear,
                if (days.any { it.trackedMillis > 0 }) compactDuration(days.sumOf { it.wornMillis })
                else stringResource(R.string.report_no_record),
            )
            Text(
                stringResource(
                    R.string.report_coverage_count,
                    days.count { it.trackedMillis > 0 },
                    days.size,
                )
            )
            val scale =
                days
                    .maxOf { ReportMath.dayMillis(LocalDate.parse(it.date), zone) }
                    .coerceAtLeast(24 * 3_600_000L)
            Text(
                stringResource(R.string.chart_hours, scale / 3_600_000),
                style = MaterialTheme.typography.labelMedium,
            )
            days.forEach { day ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        readableDate(LocalDate.parse(day.date)),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (day.trackedMillis == 0L) stringResource(R.string.report_no_record)
                        else compactDuration(day.wornMillis),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                BreakdownBar(day, now, zone, scale, target = true)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                stringResource(
                    if (days.any { it.goalMinutes != null }) R.string.chart_target
                    else R.string.report_goal_unknown
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            DetailsDisclosure(R.string.time_details) {
                Text(stringResource(R.string.time_details_body))
                Text(
                    stringResource(
                        R.string.report_out_tracked,
                        durationLabel(days.sumOf { it.removedMillis }),
                        durationLabel(days.sumOf { it.trackedMillis }),
                    )
                )
            }
        }
        item {
            TextButton(onClick = { showData = !showData }) {
                Text(stringResource(R.string.daily_data))
            }
        }
        if (showData)
            items(days.asReversed(), key = { it.date }) { day ->
                Column(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        readableDate(LocalDate.parse(day.date)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (day.trackedMillis == 0L) {
                        Text(
                            stringResource(R.string.report_no_record),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            stringResource(
                                R.string.report_wear_out,
                                durationLabel(day.wornMillis),
                                durationLabel(day.removedMillis),
                            )
                        )
                        Text(
                            stringResource(
                                R.string.report_tracked_duration,
                                durationLabel(day.trackedMillis),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val total = ReportMath.dayMillis(LocalDate.parse(day.date), zone)
                        Text(
                            stringResource(
                                if (day.trackedMillis == total) R.string.report_full
                                else R.string.report_partial
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            day.goalMinutes?.let {
                                stringResource(
                                    R.string.report_goal_duration,
                                    durationLabel(it * 60_000L),
                                )
                            } ?: stringResource(R.string.report_goal_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
        item {
            DetailsDisclosure(R.string.report_reading_help) {
                Text(
                    stringResource(R.string.reports_rules),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (snapshot.targetHistory.isNotEmpty())
            item {
                FilterChip(
                    selected = preferences.streaksEnabled,
                    onClick = {
                        model.updateReminders(
                            preferences.copy(streaksEnabled = !preferences.streaksEnabled)
                        )
                    },
                    label = { Text(stringResource(R.string.optional_streaks)) },
                )
                if (preferences.streaksEnabled) {
                    Text(stringResource(R.string.streak_value, ReportMath.streak(snapshot, now)))
                    Text(stringResource(R.string.streak_rules))
                }
            }
        item {
            Text(
                stringResource(R.string.per_tray_report),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (snapshot.trayHistory.isEmpty())
            item { Text(stringResource(R.string.tray_history_empty)) }
        items(snapshot.trayHistory, key = { "tray${it.id}" }) { tray ->
            val start =
                tray.startedAt
                    ?: LocalDate.parse(tray.startedOn).atStartOfDay(zone).toInstant().toEpochMilli()
            val end =
                tray.endedAt
                    ?: tray.endedOn?.let {
                        LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli()
                    }
                    ?: now.toEpochMilli()
            val total = ReportMath.interval(snapshot, start, end)
            Text(
                stringResource(
                    R.string.per_tray_row,
                    snapshot.phases.firstOrNull { it.id == tray.phaseId }?.name ?: "",
                    tray.trayNumber,
                    total.wornMillis / 60000,
                    total.trackedMillis / 60000,
                )
            )
        }
        item {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = { preview = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.preview_report))
            }
        }
    }
    if (preview)
        TrackerDialog(
            onDismissRequest = { preview = false },
            title = { Text(stringResource(R.string.preview_report)) },
            text = {
                Column {
                    Text(stringResource(R.string.report_export_notice))
                    Text(
                        stringResource(
                            R.string.report_record_count,
                            snapshot.events.size,
                            snapshot.trayHistory.size,
                        )
                    )
                    Text(stringResource(R.string.reports_rules))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preview = false
                        export.launch("aligner-report.csv")
                    }
                ) {
                    Text(stringResource(R.string.choose_location))
                }
            },
            dismissButton = {
                TextButton(onClick = { preview = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
}
