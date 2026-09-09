package org.alignertracker.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        content()
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
    modifier: Modifier = Modifier,
) {
    val message = errorMessage?.let { stringResource(it) }
    TrackerTextField(
        value,
        onChange,
        modifier.fillMaxWidth().semantics { if (error && message != null) this.error(message) },
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
    BidiFormatter.getInstance(currentLocale())
        .unicodeWrap(
            ZoneId.of(zoneId).getDisplayName(java.time.format.TextStyle.FULL, currentLocale()) +
                " · " +
                zoneId.replace('_', ' ')
        )

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
fun OnboardingScreen(busy: Boolean, onStart: (Boolean, String) -> Unit, onImport: () -> Unit) {
    var zone by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }
    var wearing by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var chooseZone by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    ScreenColumn {
        Heading(R.string.quick_start_title)
        Text(stringResource(R.string.quick_start_body))
        Section(R.string.setup_state) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (state in listOf(true, false)) {
                    val selected = wearing == state
                    Surface(
                        color =
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        border =
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected,
                                    enabled = !busy,
                                    role = Role.RadioButton,
                                    onClick = { wearing = state },
                                )
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = selected, onClick = null, enabled = !busy)
                            Text(
                                stringResource(
                                    if (state) R.string.state_in else R.string.state_out
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
            if (wearing == null)
                Text(
                    stringResource(R.string.choose_tracking_state),
                    style = MaterialTheme.typography.bodyMedium,
                )
            Text(
                stringResource(R.string.setup_coverage),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { wearing?.let { onStart(it, zone) } },
                enabled = !busy && wearing != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(if (busy) R.string.saving else R.string.setup_start))
            }
        }
        Text(stringResource(R.string.zone_value, readableZone(zone)))
        TextButton(onClick = { chooseZone = true }, enabled = !busy) {
            Text(stringResource(R.string.change_accounting_zone))
        }
        OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_import))
        }
    }
    if (chooseZone)
        TrackerDialog(
            onDismissRequest = { chooseZone = false },
            title = { Text(stringResource(R.string.treatment_zone)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.fixed_zone_help))
                    Entry(query, { query = it }, R.string.search_zones)
                    ZoneId.getAvailableZoneIds()
                        .sorted()
                        .filter { it.replace('_', ' ').contains(query.trim(), ignoreCase = true) }
                        .take(40)
                        .forEach { id ->
                            TextButton(
                                onClick = {
                                    zone = id
                                    chooseZone = false
                                    query = ""
                                }
                            ) {
                                Text(readableZone(id))
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { chooseZone = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}

@Composable
internal fun trayLabel(plan: TreatmentPlan): String =
    when {
        plan.currentTray == null -> stringResource(R.string.tray_unknown)
        plan.totalTrays == null -> stringResource(R.string.tray_number_only, plan.currentTray)
        else -> stringResource(R.string.tray_fraction, plan.currentTray, plan.totalTrays)
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
    MetricPair(
        R.string.worn,
        durationLabel(summary.wornMillis),
        R.string.removed,
        durationLabel(summary.removedMillis),
    )
    MetricPair(
        R.string.covered,
        durationLabel(summary.trackedMillis),
        R.string.untracked,
        durationLabel((elapsed - summary.trackedMillis).coerceAtLeast(0)),
    )
    if (showCurrentNote)
        Text(stringResource(R.string.coverage_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun MetricPair(
    @StringRes first: Int,
    firstValue: String,
    @StringRes second: Int,
    secondValue: String,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / LocalDensity.current.fontScale < 280.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(first, firstValue)
                Metric(second, secondValue)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) { Metric(first, firstValue) }
                Column(Modifier.weight(1f)) { Metric(second, secondValue) }
            }
        }
    }
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
) = TodayScreen(snapshot, now, busy, {}, onToggle, {})

@Composable
fun TodayScreen(
    snapshot: TrackerSnapshot,
    now: Instant,
    busy: Boolean,
    onDetails: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onCorrect: () -> Unit = {},
) {
    val plan = snapshot.plan ?: return
    val zone = ZoneId.of(plan.zoneId)
    val wearing = WearMath.isWearing(snapshot)
    val currentState = stringResource(if (wearing) R.string.state_in else R.string.state_out)
    val summary = WearMath.summarize(snapshot, now.atZone(zone).toLocalDate(), now)
    val uiPreferences =
        androidx.compose.ui.platform.LocalContext.current.getSharedPreferences(
            "presentation",
            android.content.Context.MODE_PRIVATE,
        )
    var detailsDismissed by
        rememberSaveable(plan.trackingStartedAt) {
            mutableStateOf(
                uiPreferences.getLong("detailsDismissedFor", -1L) == plan.trackingStartedAt
            )
        }
    BoxWithConstraints(Modifier.widthIn(max = 680.dp).fillMaxSize()) {
        val compact = maxHeight / LocalDensity.current.fontScale < 250.dp
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (!compact)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Heading(R.string.wear_heading)
                        Text(
                            readableDate(now.atZone(zone).toLocalDate()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color =
                        if (plan.completed) MaterialTheme.colorScheme.surfaceContainer
                        else if (wearing) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor =
                        if (plan.completed) MaterialTheme.colorScheme.onSurface
                        else if (wearing) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(
                                if (plan.completed) R.string.completed_title
                                else if (wearing) R.string.state_in else R.string.state_out
                            ),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        if (plan.completed) Text(stringResource(R.string.completed_body))
                        else
                            snapshot.events.lastOrNull()?.let { event ->
                                Text(
                                    stringResource(
                                        if (wearing) R.string.current_wear_session
                                        else R.string.current_break_session
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    if (snapshot.trackingGaps.any { it.endAt > event.at })
                                        stringResource(R.string.clock_duration_uncertain)
                                    else durationLabel(now.toEpochMilli() - event.at),
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                Text(
                                    stringResource(
                                        R.string.state_since,
                                        readableTime(event.at, zone),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                    }
                }
                if (compact && !plan.completed)
                    TextButton(onClick = onCorrect, enabled = !busy) {
                        Text(stringResource(R.string.correct_missed_transition))
                    }
                Section(R.string.today_summary) {
                    Totals(summary, now, zone, !plan.completed)
                    Text(
                        summary.goalMinutes?.let {
                            stringResource(R.string.goal_value, durationLabel(it * 60_000L))
                        }
                            ?: stringResource(
                                if (plan.dailyGoalMinutes == null) R.string.goal_unknown
                                else R.string.goal_comparison_unavailable
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (plan.currentTray != null)
                    Text(trayLabel(plan), style = MaterialTheme.typography.titleMedium)
                if (
                    !detailsDismissed &&
                        !plan.completed &&
                        (plan.dailyGoalMinutes == null || !plan.hasSchedule)
                ) {
                    HorizontalDivider()
                    Section(R.string.optional_details_invitation) {
                        Text(
                            stringResource(R.string.optional_details_help),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = onDetails) {
                            Text(stringResource(R.string.add_treatment_details))
                        }
                        TextButton(
                            onClick = {
                                detailsDismissed = true
                                uiPreferences
                                    .edit()
                                    .putLong("detailsDismissedFor", plan.trackingStartedAt)
                                    .apply()
                            }
                        ) {
                            Text(stringResource(R.string.not_now))
                        }
                    }
                }
                Text(
                    stringResource(R.string.zone_value, readableZone(plan.zoneId)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!plan.completed)
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
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
                        if (!compact)
                            TextButton(
                                onClick = onCorrect,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.correct_missed_transition))
                            }
                    }
                }
        }
    }
}

@Composable
fun ScheduleScreen(
    snapshot: TrackerSnapshot,
    busy: Boolean,
    onAdvance: () -> Unit,
    onComplete: () -> Unit,
    onDetails: () -> Unit = {},
) {
    val plan = snapshot.plan ?: return
    val due = WearMath.nextChangeDate(snapshot)
    ScreenColumn {
        Heading(R.string.plan_heading)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(trayLabel(plan), style = MaterialTheme.typography.headlineSmall)
            Text(
                plan.currentTrayStartedOn?.let {
                    stringResource(R.string.current_started, readableDate(LocalDate.parse(it)))
                } ?: stringResource(R.string.tray_date_unknown)
            )
            if (plan.completed) Text(stringResource(R.string.completed_body))
            else {
                Text(
                    due?.let { stringResource(R.string.next_change, readableDate(it)) }
                        ?: stringResource(R.string.schedule_missing_details),
                    style =
                        if (due != null) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.schedule_note),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (plan.hasSchedule && plan.currentTray!! < plan.totalTrays!!)
                    Button(
                        onClick = onAdvance,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.advance_tray))
                    }
                else if (plan.hasSchedule)
                    Button(
                        onClick = onComplete,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.complete_action))
                    }
            }
        }
        if (!plan.completed)
            TextButton(onClick = onDetails) {
                Text(stringResource(R.string.edit_treatment_details))
            }
        if (!plan.completed && plan.hasSchedule && plan.currentTray!! < plan.totalTrays!!)
            Section(R.string.future_schedule) {
                WearMath.futureTrayStarts(snapshot).forEach { (tray, startDate) ->
                    Text(stringResource(R.string.future_tray, tray, readableDate(startDate)))
                }
                if (plan.totalTrays!! > plan.currentTray!! + 12)
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
    var dateEntry by rememberSaveable { mutableStateOf(false) }
    var dateDraft by rememberSaveable { mutableStateOf("") }
    var dateInvalid by rememberSaveable { mutableStateOf(false) }
    val compactDate =
        LocalConfiguration.current.screenWidthDp / LocalDensity.current.fontScale < 360
    val dateContext = androidx.compose.ui.platform.LocalContext.current
    val date = LocalDate.parse(selected)
    val summary = WearMath.summarize(snapshot, date, now)
    val events =
        snapshot.events.filter { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == date }
    ScreenColumn {
        Heading(R.string.history_heading)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(readableDate(date), style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = date > LocalDate.of(1970, 1, 1),
                    onClick = { selected = date.minusDays(1).toString() },
                ) {
                    Text(stringResource(R.string.previous_day))
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { selected = date.plusDays(1).toString() },
                    enabled = date < today,
                ) {
                    Text(stringResource(R.string.next_day))
                }
            }
            TextButton(
                onClick = {
                    if (compactDate) {
                        dateDraft = selected
                        dateInvalid = false
                        dateEntry = true
                    } else
                        android.app
                            .DatePickerDialog(
                                dateContext,
                                R.style.TrackerDatePicker,
                                { _, year, month, day ->
                                    selected = LocalDate.of(year, month + 1, day).toString()
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth,
                            )
                            .apply {
                                datePicker.maxDate =
                                    today
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                                datePicker.minDate =
                                    LocalDate.of(1970, 1, 1)
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                            }
                            .show()
                }
            ) {
                Text(stringResource(R.string.choose_date))
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
    if (dateEntry)
        TrackerDialog(
            onDismissRequest = { dateEntry = false },
            title = { Text(stringResource(R.string.choose_date)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Entry(
                        dateDraft,
                        {
                            dateDraft = it
                            dateInvalid = false
                        },
                        R.string.history_date,
                        error = dateInvalid,
                        errorMessage = if (dateInvalid) R.string.date_invalid else null,
                    )
                    val preview = runCatching { LocalDate.parse(dateDraft.trim()) }.getOrNull()
                    if (preview != null && preview >= LocalDate.of(1970, 1, 1) && preview <= today)
                        Text(readableDate(preview))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = runCatching { LocalDate.parse(dateDraft.trim()) }.getOrNull()
                        dateInvalid =
                            parsed == null || parsed < LocalDate.of(1970, 1, 1) || parsed > today
                        if (!dateInvalid) {
                            selected = parsed.toString()
                            dateEntry = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.go_to_date))
                }
            },
            dismissButton = {
                TextButton(onClick = { dateEntry = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}

@Composable
fun ProgressScreen(snapshot: TrackerSnapshot, now: Instant) {
    val plan = snapshot.plan ?: return
    val zone = ZoneId.of(plan.zoneId)
    val today = now.atZone(zone).toLocalDate()
    ScreenColumn {
        Heading(R.string.progress_heading)
        Text(trayLabel(plan), style = MaterialTheme.typography.titleLarge)
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
                    date < today && it.trackedMillis == fullDay && it.goalMinutes != null
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
                                fullyTracked.count {
                                    it.goalMinutes?.let { goal ->
                                        it.wornMillis >= goal * 60_000L
                                    } == true
                                },
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
