package org.alignertracker.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.core.text.BidiFormatter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max
import org.alignertracker.app.R
import org.alignertracker.app.domain.DaySummary
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.domain.WearMath

@Composable
internal fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.widthIn(max = 680.dp)
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
internal fun Heading(@StringRes title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

@Composable
internal fun Entry(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    numeric: Boolean = false,
    error: Boolean = false,
    @StringRes errorMessage: Int? = null,
    enabled: Boolean = true,
) {
    val message = errorMessage?.let { stringResource(it) }
    TrackerTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().semantics { if (error && message != null) this.error(message) },
        label = { Text(stringResource(label)) },
        supportingText = message?.let { { Text(it) } },
        singleLine = true,
        isError = error,
        enabled = enabled,
        keyboardOptions =
            KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
    )
}

@Composable
internal fun ErrorText(@StringRes error: Int) {
    Text(
        stringResource(error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
internal fun durationLabel(millis: Long): String {
    val minutes = max(0L, millis) / 60_000
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours == 0L) {
        androidx.compose.ui.res.pluralStringResource(
            R.plurals.duration_minutes,
            remainingMinutes.toInt(),
            remainingMinutes,
        )
    } else {
        stringResource(
            R.string.duration_hours_minutes,
            androidx.compose.ui.res.pluralStringResource(
                R.plurals.duration_hours,
                hours.toInt(),
                hours,
            ),
            androidx.compose.ui.res.pluralStringResource(
                R.plurals.duration_minutes,
                remainingMinutes.toInt(),
                remainingMinutes,
            ),
        )
    }
}

internal fun formatDateForLocale(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

@Composable
private fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

@Composable
internal fun readableDate(date: LocalDate): String {
    val locale = currentLocale()
    return BidiFormatter.getInstance(locale).unicodeWrap(formatDateForLocale(date, locale))
}

@Composable
internal fun readableTime(at: Long, zone: ZoneId): String {
    val locale = currentLocale()
    val zoned = Instant.ofEpochMilli(at).atZone(zone)
    val dateTime =
        zoned.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale))
    val offset =
        if (zoned.offset == ZoneOffset.UTC) stringResource(R.string.utc_abbreviation)
        else stringResource(R.string.utc_offset, zoned.offset.id)
    val bidi = BidiFormatter.getInstance(locale)
    return stringResource(
        R.string.date_time_with_offset,
        bidi.unicodeWrap(dateTime),
        bidi.unicodeWrap(offset),
    )
}

@Composable
internal fun readableZone(zoneId: String): String =
    BidiFormatter.getInstance(currentLocale()).unicodeWrap(zoneId)

internal fun parseUserInteger(value: String): Int? {
    val input = value.trim()
    if (input.isEmpty()) return null
    var result = 0
    input.forEach { character ->
        val digit = Character.digit(character, 10)
        if (digit < 0 || result > (Int.MAX_VALUE - digit) / 10) return null
        result = result * 10 + digit
    }
    return result
}

internal fun parseUserHours(value: String): Int? {
    val normalized = StringBuilder()
    var separatorSeen = false
    value.trim().forEach { character ->
        val digit = Character.digit(character, 10)
        when {
            digit >= 0 -> normalized.append(digit)
            (character == '.' || character == ',' || character == '\u066B') && !separatorSeen -> {
                normalized.append('.')
                separatorSeen = true
            }
            else -> return null
        }
    }
    if (normalized.isEmpty() || normalized.length > 8) return null
    return runCatching {
            (normalized.toString().toBigDecimal() * 60.toBigDecimal()).intValueExact()
        }
        .getOrNull()
}

@Composable
fun OnboardingScreen(
    busy: Boolean,
    onStart: (TreatmentPlan, Boolean) -> Unit,
    onImport: () -> Unit,
) {
    var total by rememberSaveable { mutableStateOf("") }
    var current by rememberSaveable { mutableStateOf("1") }
    var interval by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var zone by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }
    var start by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var currentStart by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var wearing by rememberSaveable { mutableStateOf(true) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    fun plan(): TreatmentPlan? =
        runCatching {
                val zoneId = ZoneId.of(zone.trim())
                val today = LocalDate.now(zoneId)
                val first = LocalDate.parse(start.trim())
                val trayDate = LocalDate.parse(currentStart.trim())
                val trays = requireNotNull(parseUserInteger(total))
                val tray = requireNotNull(parseUserInteger(current))
                val days = requireNotNull(parseUserInteger(interval))
                val minutes = requireNotNull(parseUserHours(goal))
                require(
                    trays in 1..1000 && tray in 1..trays && days in 1..365 && minutes in 1..1440
                )
                require(
                    first <= trayDate &&
                        trayDate <= today &&
                        first.year in 1970..2100 &&
                        trayDate.year in 1970..2100
                )
                TreatmentPlan(
                    startDate = first.toString(),
                    totalTrays = trays,
                    currentTray = tray,
                    daysPerTray = days,
                    currentTrayStartedOn = trayDate.toString(),
                    dailyGoalMinutes = minutes,
                    zoneId = zoneId.id,
                    trackingStartedAt = Instant.now().toEpochMilli(),
                )
            }
            .getOrNull()
    val candidate = plan()
    ScreenColumn {
        Heading(R.string.welcome_title)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)
        Section(R.string.setup_title) {
            Text(stringResource(R.string.setup_body))
            Entry(
                total,
                { total = it },
                R.string.total_trays,
                numeric = true,
                enabled = !busy,
                error = attempted && parseUserInteger(total) !in 1..1000,
                errorMessage =
                    if (attempted && parseUserInteger(total) !in 1..1000)
                        R.string.tray_count_invalid
                    else null,
            )
            Entry(
                current,
                { current = it },
                R.string.current_tray,
                numeric = true,
                enabled = !busy,
                error =
                    attempted && parseUserInteger(current) !in 1..(parseUserInteger(total) ?: 0),
                errorMessage =
                    if (
                        attempted && parseUserInteger(current) !in 1..(parseUserInteger(total) ?: 0)
                    )
                        R.string.current_tray_invalid
                    else null,
            )
            Entry(
                interval,
                { interval = it },
                R.string.days_per_tray,
                error = attempted && parseUserInteger(interval) !in 1..365,
                errorMessage =
                    if (attempted && parseUserInteger(interval) !in 1..365)
                        R.string.tray_days_invalid
                    else null,
                numeric = true,
                enabled = !busy,
            )
            Entry(
                goal,
                { goal = it },
                R.string.goal_hours,
                numeric = true,
                enabled = !busy,
                error = attempted && parseUserHours(goal) !in 1..1440,
                errorMessage =
                    if (attempted && parseUserHours(goal) !in 1..1440) R.string.goal_hours_invalid
                    else null,
            )
            Entry(start, { start = it }, R.string.treatment_start, enabled = !busy)
            Entry(currentStart, { currentStart = it }, R.string.tray_start, enabled = !busy)
            Entry(zone, { zone = it }, R.string.treatment_zone, enabled = !busy)
            Text(stringResource(R.string.zone_helper), style = MaterialTheme.typography.bodySmall)
            if (candidate != null)
                Text(
                    stringResource(
                        R.string.setup_date_preview,
                        readableDate(WearMath.nextChangeDate(candidate)),
                    )
                )
            if (attempted && candidate == null) ErrorText(R.string.setup_invalid)
        }
        Section(R.string.setup_state) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = wearing,
                    onClick = { wearing = true },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.state_in)) },
                )
                FilterChip(
                    selected = !wearing,
                    onClick = { wearing = false },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.state_out)) },
                )
            }
            Text(stringResource(R.string.setup_coverage))
            Button(
                onClick = {
                    attempted = true
                    plan()?.let { onStart(it, wearing) }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(if (busy) R.string.saving else R.string.setup_start))
            }
        }
        OutlinedButton(
            onClick = onImport,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.setup_import))
        }
    }
}

@Composable
internal fun Totals(
    summary: DaySummary,
    now: Instant,
    zone: ZoneId,
    showCurrentNote: Boolean = false,
) {
    val date = LocalDate.parse(summary.date)
    val elapsed =
        Duration.between(
                date.atStartOfDay(zone).toInstant(),
                minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), now),
            )
            .toMillis()
            .coerceAtLeast(0)
    Metric(R.string.worn, durationLabel(summary.wornMillis))
    Metric(R.string.removed, durationLabel(summary.removedMillis))
    Metric(R.string.covered, durationLabel(summary.trackedMillis))
    Metric(R.string.untracked, durationLabel((elapsed - summary.trackedMillis).coerceAtLeast(0)))
    if (showCurrentNote)
        Text(stringResource(R.string.coverage_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun Metric(@StringRes title: Int, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun TodayScreen(
    snapshot: TrackerSnapshot,
    now: Instant,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val plan = snapshot.plan ?: return
    val zone = ZoneId.of(plan.zoneId)
    val wearing = WearMath.isWearing(snapshot)
    val currentState = stringResource(if (wearing) R.string.state_in else R.string.state_out)
    val summary = WearMath.summarize(snapshot, now.atZone(zone).toLocalDate(), now)
    ScreenColumn {
        Heading(R.string.wear_heading)
        Text(
            stringResource(R.string.tray_fraction, plan.currentTray, plan.totalTrays),
            style = MaterialTheme.typography.titleMedium,
        )
        Section(
            if (plan.completed) R.string.completed_title
            else if (wearing) R.string.state_in else R.string.state_out
        ) {
            if (plan.completed) Text(stringResource(R.string.completed_body))
            else {
                snapshot.events.lastOrNull()?.let { event ->
                    Text(
                        if (snapshot.trackingGaps.any { it.endAt > event.at })
                            stringResource(R.string.clock_duration_uncertain)
                        else
                            stringResource(
                                R.string.current_duration,
                                durationLabel(now.toEpochMilli() - event.at),
                            ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.state_since, readableTime(event.at, zone)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = { onToggle(!wearing) },
                    enabled = !busy,
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics {
                            stateDescription = currentState
                            liveRegion = LiveRegionMode.Polite
                        },
                ) {
                    Text(
                        stringResource(
                            if (busy) R.string.saving
                            else if (wearing) R.string.take_out else R.string.put_in
                        )
                    )
                }
            }
        }
        Section(R.string.today_summary) {
            Totals(summary, now, zone, !plan.completed)
            HorizontalDivider()
            Text(stringResource(R.string.goal_value, durationLabel(summary.goalMinutes * 60_000L)))
        }
        Text(stringResource(R.string.edit_hint))
        Text(
            stringResource(R.string.zone_value, readableZone(plan.zoneId)),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ScheduleScreen(
    snapshot: TrackerSnapshot,
    busy: Boolean,
    onAdvance: () -> Unit,
    onComplete: () -> Unit,
) {
    val plan = snapshot.plan ?: return
    val due = WearMath.nextChangeDate(snapshot)
    ScreenColumn {
        Heading(R.string.plan_heading)
        Section(R.string.schedule) {
            Text(
                stringResource(R.string.tray_fraction, plan.currentTray, plan.totalTrays),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(
                    R.string.current_started,
                    readableDate(LocalDate.parse(plan.currentTrayStartedOn)),
                )
            )
            if (plan.completed) Text(stringResource(R.string.completed_body))
            else {
                Text(
                    stringResource(R.string.next_change, readableDate(due)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.schedule_note))
                if (plan.currentTray < plan.totalTrays)
                    Button(
                        onClick = onAdvance,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.advance_tray))
                    }
                else
                    Button(
                        onClick = onComplete,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.complete_action))
                    }
            }
        }
        if (!plan.completed && plan.currentTray < plan.totalTrays)
            Section(R.string.future_schedule) {
                WearMath.futureTrayStarts(snapshot).forEach { (tray, startDate) ->
                    Text(stringResource(R.string.future_tray, tray, readableDate(startDate)))
                }
                if (plan.totalTrays > plan.currentTray + 12)
                    Text(stringResource(R.string.future_more))
            }
        Text(
            stringResource(R.string.zone_value, readableZone(plan.zoneId)),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun HistoryScreen(snapshot: TrackerSnapshot, now: Instant, busy: Boolean, onEdit: (Long) -> Unit) {
    val plan = snapshot.plan ?: return
    val zone = ZoneId.of(plan.zoneId)
    val today = now.atZone(zone).toLocalDate()
    var selected by rememberSaveable(plan.trackingStartedAt) { mutableStateOf(today.toString()) }
    var draft by rememberSaveable(plan.trackingStartedAt) { mutableStateOf(today.toString()) }
    var dateError by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.parse(selected)
    val summary = WearMath.summarize(snapshot, date, now)
    val events =
        snapshot.events.filter { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == date }
    ScreenColumn {
        Heading(R.string.history_heading)
        Section(R.string.history) {
            Text(readableDate(date), style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = date > LocalDate.of(1970, 1, 1),
                    onClick = {
                        selected = date.minusDays(1).toString()
                        draft = selected
                    },
                ) {
                    Text(stringResource(R.string.previous_day))
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selected = date.plusDays(1).toString()
                        draft = selected
                    },
                    enabled = date < today,
                ) {
                    Text(stringResource(R.string.next_day))
                }
            }
            Entry(
                draft,
                {
                    draft = it
                    dateError = false
                },
                R.string.history_date,
                error = dateError,
                errorMessage = if (dateError) R.string.date_invalid else null,
            )
            OutlinedButton(
                onClick = {
                    val parsed = runCatching { LocalDate.parse(draft.trim()) }.getOrNull()
                    dateError =
                        parsed == null || parsed > today || parsed < LocalDate.of(1970, 1, 1)
                    if (!dateError) selected = parsed.toString()
                }
            ) {
                Text(stringResource(R.string.go_to_date))
            }
            if (summary.trackedMillis == 0L) Text(stringResource(R.string.no_tracking))
            Totals(summary, now, zone, date == today && !plan.completed)
        }
        Section(R.string.events_heading) {
            if (events.isEmpty()) Text(stringResource(R.string.no_changes))
            events.forEach { event ->
                val editDescription =
                    stringResource(
                        R.string.edit_transition_accessibility,
                        stringResource(
                            if (event.wearing) R.string.state_in else R.string.state_out
                        ),
                        readableTime(event.at, zone),
                    )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(
                            if (event.wearing) R.string.state_in else R.string.state_out
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(readableTime(event.at, zone))
                    if (event.id == snapshot.events.firstOrNull()?.id)
                        Text(
                            stringResource(R.string.initial_event),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    else if (!plan.completed)
                        OutlinedButton(
                            onClick = { onEdit(event.id) },
                            enabled = !busy,
                            modifier = Modifier.heightIn(min = 48.dp).recordAction(editDescription),
                        ) {
                            Text(stringResource(R.string.edit_time))
                        }
                }
                HorizontalDivider()
            }
        }
        Text(
            stringResource(R.string.zone_value, readableZone(plan.zoneId)),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ProgressScreen(snapshot: TrackerSnapshot, now: Instant) {
    val plan = snapshot.plan ?: return
    val zone = ZoneId.of(plan.zoneId)
    val today = now.atZone(zone).toLocalDate()
    ScreenColumn {
        Heading(R.string.progress_heading)
        Text(
            stringResource(R.string.treatment_fraction, plan.currentTray, plan.totalTrays),
            style = MaterialTheme.typography.titleLarge,
        )
        listOf(7, 30).forEach { period ->
            val summaries =
                (0 until period).map {
                    WearMath.summarize(snapshot, today.minusDays(it.toLong()), now)
                }
            val recorded = summaries.filter { it.trackedMillis > 0 }
            val fullyTracked =
                summaries.filter {
                    val date = LocalDate.parse(it.date)
                    val fullDay =
                        Duration.between(
                                date.atStartOfDay(zone),
                                date.plusDays(1).atStartOfDay(zone),
                            )
                            .toMillis()
                    date < today && it.trackedMillis == fullDay
                }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        stringResource(R.string.progress_period, period),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (recorded.isEmpty()) Text(stringResource(R.string.no_progress))
                    else {
                        Metric(
                            R.string.average_recorded,
                            durationLabel(recorded.sumOf { it.wornMillis } / recorded.size),
                        )
                        Text(stringResource(R.string.days_with_data, recorded.size, period))
                        Text(
                            stringResource(
                                R.string.full_days,
                                fullyTracked.count { it.wornMillis >= it.goalMinutes * 60_000L },
                                fullyTracked.size,
                            )
                        )
                    }
                    Text(
                        stringResource(R.string.progress_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Text(stringResource(R.string.progress_goal_policy))
        Text(
            stringResource(R.string.zone_value, readableZone(plan.zoneId)),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun EditEventDialog(
    snapshot: TrackerSnapshot,
    event: WearEvent,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val zone = ZoneId.of(snapshot.plan!!.zoneId)
    val original = Instant.ofEpochMilli(event.at).atZone(zone)
    var value by
        rememberSaveable(event.id) {
            mutableStateOf(original.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        }
    var offset by rememberSaveable(event.id) { mutableStateOf(original.offset.id) }
    var invalid by rememberSaveable(event.id) { mutableStateOf(false) }
    val index = snapshot.events.indexOfFirst { it.id == event.id }
    val previous = snapshot.events.getOrNull(index - 1)
    val following = snapshot.events.getOrNull(index + 1)
    TrackerDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.edit_event_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.edit_event_body, readableZone(zone.id)))
                previous?.let {
                    Text(stringResource(R.string.previous_event, readableTime(it.at, zone)))
                }
                Text(
                    if (following == null) stringResource(R.string.no_following_event)
                    else stringResource(R.string.following_event, readableTime(following.at, zone))
                )
                Entry(
                    value,
                    {
                        value = it
                        invalid = false
                    },
                    R.string.event_timestamp,
                    error = invalid,
                    errorMessage = if (invalid) R.string.timestamp_invalid else null,
                    enabled = !busy,
                )
                Entry(
                    offset,
                    {
                        offset = it
                        invalid = false
                    },
                    R.string.event_offset,
                    error = invalid,
                    enabled = !busy,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val result =
                        runCatching {
                                val local =
                                    LocalDateTime.parse(
                                        value.trim(),
                                        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                                            .withResolverStyle(
                                                java.time.format.ResolverStyle.STRICT
                                            ),
                                    )
                                val offsets = zone.rules.getValidOffsets(local)
                                val chosen =
                                    if (offset.isBlank()) {
                                        require(offsets.size == 1)
                                        offsets.single()
                                    } else ZoneOffset.of(offset.trim())
                                require(chosen in offsets)
                                val instant = local.toInstant(chosen).toEpochMilli()
                                require(
                                    previous != null &&
                                        instant > previous.at &&
                                        instant <= Instant.now().toEpochMilli()
                                )
                                require(following == null || instant < following.at)
                                instant
                            }
                            .getOrNull()
                    invalid = result == null
                    result?.let(onSave)
                },
            ) {
                Text(stringResource(if (busy) R.string.saving else R.string.save))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
