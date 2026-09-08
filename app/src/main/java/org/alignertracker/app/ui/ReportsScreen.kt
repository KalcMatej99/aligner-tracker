package org.alignertracker.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var count by rememberSaveable { mutableStateOf(7) }
    var preview by remember { mutableStateOf(false) }
    val zone = ZoneId.of(snapshot.plan?.zoneId ?: "UTC")
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
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.reports_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        item { Text(stringResource(R.string.reports_rules)) }
        item {
            Column {
                for (row in listOf(1, 7, 30, 0).chunked(2)) Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (period in row) FilterChip(
                        selected = count == period,
                        onClick = { count = period },
                        label = {
                            Text(
                                if (period == 0) stringResource(R.string.report_month)
                                else stringResource(R.string.report_days, period)
                            )
                        },
                    )
                }
            }
        }
        item {
            Text(
                stringResource(
                    R.string.report_totals,
                    days.sumOf { it.wornMillis } / 60000,
                    days.sumOf { it.removedMillis } / 60000,
                    days.sumOf { it.trackedMillis } / 60000,
                )
            )
        }
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
        }
        if (preferences.streaksEnabled)
            item {
                Text(stringResource(R.string.streak_value, ReportMath.streak(snapshot, now)))
                Text(stringResource(R.string.streak_rules))
            }
        items(days, key = { it.date }) { day ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        LocalDate.parse(day.date)
                            .format(
                                java.time.format.DateTimeFormatter.ofLocalizedDate(
                                    java.time.format.FormatStyle.MEDIUM
                                )
                            )
                    )
                    Text(
                        stringResource(
                            R.string.daily_report,
                            day.wornMillis / 60000,
                            day.removedMillis / 60000,
                            day.trackedMillis / 60000,
                            day.goalMinutes,
                        )
                    )
                    val total = ReportMath.dayMillis(LocalDate.parse(day.date), zone)
                    LinearProgressIndicator(
                        progress = { (day.wornMillis.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(
                            if (day.trackedMillis == total) R.string.report_full
                            else if (day.trackedMillis == 0L) R.string.report_untracked
                            else R.string.report_partial
                        )
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.per_tray_report),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
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
            OutlinedButton(onClick = { preview = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.preview_report))
            }
        }
    }
    if (preview)
        AlertDialog(
            onDismissRequest = { preview = false },
            title = { Text(stringResource(R.string.preview_report)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
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
