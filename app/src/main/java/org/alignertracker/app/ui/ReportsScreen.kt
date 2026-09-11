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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.*
import org.alignertracker.app.R
import org.alignertracker.app.data.ReportMath
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.reminders.ReminderPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    snapshot: TrackerSnapshot,
    model: TrackerViewModel,
    now: Instant,
    preferences: ReminderPreferences,
    busy: Boolean = false,
) {
    if (snapshot.plan == null) {
        ScreenColumn {
            Heading(R.string.reports_title)
            Text(stringResource(R.string.no_progress))
        }
        return
    }
    var showTools by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableStateOf(7) }
    var preview by remember { mutableStateOf(false) }
    val zone = ZoneId.of(snapshot.plan.zoneId)
    val days =
        remember(snapshot, count, now.epochSecond / 60) { ReportMath.days(snapshot, count, now) }
    val context = LocalContext.current
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            uri?.let { model.export(context.contentResolver, it, true) }
        }
    ProgressOverview(snapshot, now) {
        count = it
        showTools = true
    }
    if (showTools)
        ModalBottomSheet(onDismissRequest = { showTools = false }) {
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.progress_tools),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    OutlinedButton(
                        onClick = { preview = true },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.progress_export))
                    }
                    Text(
                        stringResource(R.string.progress_chart_help),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (snapshot.targetHistory.isNotEmpty())
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.optional_streaks), Modifier.weight(1f))
                            Switch(
                                checked = preferences.streaksEnabled,
                                enabled = !busy,
                                modifier =
                                    Modifier.semantics {
                                        contentDescription =
                                            context.getString(R.string.optional_streaks)
                                    },
                                onCheckedChange = {
                                    model.updateReminders(preferences.copy(streaksEnabled = it))
                                },
                            )
                        }
                        if (preferences.streaksEnabled) {
                            Text(
                                stringResource(
                                    R.string.streak_value,
                                    ReportMath.streak(snapshot, now),
                                )
                            )
                            Text(stringResource(R.string.streak_rules))
                        }
                    }
                if (snapshot.trayHistory.isNotEmpty())
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
                            ?: LocalDate.parse(tray.startedOn)
                                .atStartOfDay(zone)
                                .toInstant()
                                .toEpochMilli()
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
