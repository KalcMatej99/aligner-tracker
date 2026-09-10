package org.alignertracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import org.alignertracker.app.R

/** Picker milliseconds represent a calendar day in UTC, never an accounting-zone instant. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarDialog(
    value: String,
    latest: LocalDate,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val compact =
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp /
            androidx.compose.ui.platform.LocalDensity.current.fontScale < 300
    val state =
        rememberDatePickerState(
            initialDisplayMode = if (compact) DisplayMode.Input else DisplayMode.Picker,
            initialSelectedDateMillis =
                value
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    },
            yearRange = 1970..minOf(2100, latest.year),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        Instant.ofEpochMilli(utcTimeMillis)
                            .atOffset(ZoneOffset.UTC)
                            .toLocalDate() <= latest
                },
        )
    // The shared scrollable dialog keeps native picker controls reachable at 200% text.
    TrackerDialog(
        picker = true,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_date)) },
        text = {
            Column {
                TextButton(
                    onClick = {
                        state.displayMode =
                            if (state.displayMode == DisplayMode.Input) DisplayMode.Picker
                            else DisplayMode.Input
                    }
                ) {
                    Text(
                        stringResource(
                            if (state.displayMode == DisplayMode.Input) R.string.show_calendar
                            else R.string.use_keyboard
                        )
                    )
                }
                if (state.displayMode == DisplayMode.Input) {
                    DatePicker(
                        state,
                        modifier = Modifier.fillMaxWidth(),
                        title = null,
                        headline = null,
                        showModeToggle = false,
                    )
                } else
                    Box(Modifier.horizontalScroll(rememberScrollState())) {
                        DatePicker(
                            state,
                            modifier = Modifier.width(360.dp),
                            title = null,
                            headline = null,
                            showModeToggle = false,
                        )
                    }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onSelect(
                            Instant.ofEpochMilli(it)
                                .atOffset(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
                        )
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun DateField(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    latest: LocalDate = LocalDate.of(2100, 12, 31),
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedButton(
        shape = MaterialTheme.shapes.small,
        onClick = {
            keyboard?.hide()
            open = true
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column {
            Text(stringResource(label))
            Text(
                value.takeIf { it.isNotBlank() }?.let { readableDate(LocalDate.parse(it)) }
                    ?: stringResource(R.string.not_known)
            )
        }
    }
    if (open)
        CalendarDialog(
            value,
            latest,
            {
                onChange(it)
                open = false
            },
            { open = false },
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClockDialog(value: LocalTime, onSelect: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state =
        rememberTimePickerState(
            value.hour,
            value.minute,
            android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
    var input by rememberSaveable { mutableStateOf(false) }
    TrackerDialog(
        picker = true,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_time)) },
        text = {
            Column {
                Box(Modifier.horizontalScroll(rememberScrollState())) {
                    if (input) TimeInput(state) else TimePicker(state)
                }
                TextButton(onClick = { input = !input }) {
                    Text(stringResource(if (input) R.string.show_clock else R.string.use_keyboard))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelect(value.withHour(state.hour).withMinute(state.minute)) }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Internal ISO strings retain seconds/fractions and an explicit overlap choice; none is displayed.
 */
@Composable
internal fun TimeField(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    date: LocalDate?,
    zone: ZoneId,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val offsetTime = runCatching { OffsetTime.parse(value) }.getOrNull()
    val time =
        offsetTime?.toLocalTime()
            ?: runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.NOON)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val pattern =
        android.text.format.DateFormat.getBestDateTimePattern(
            locale,
            if (android.text.format.DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm",
        )
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedButton(
        shape = MaterialTheme.shapes.small,
        onClick = {
            keyboard?.hide()
            open = true
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column {
            Text(stringResource(label))
            Text(time.format(DateTimeFormatter.ofPattern(pattern, locale)))
        }
    }
    if (open)
        ClockDialog(
            time,
            { chosen ->
                val offsets =
                    date?.let { zone.rules.getValidOffsets(LocalDateTime.of(it, chosen)) }.orEmpty()
                val retained = offsetTime?.offset?.takeIf { it in offsets }
                onChange(
                    if (retained != null) chosen.atOffset(retained).toString()
                    else chosen.toString()
                )
                open = false
            },
            { open = false },
        )
    val offsets = date?.let { zone.rules.getValidOffsets(LocalDateTime.of(it, time)) }.orEmpty()
    if (offsets.size == 2) {
        Text(stringResource(R.string.repeated_time))
        offsets.forEachIndexed { index, offset ->
            FilterChip(
                selected = offsetTime?.offset == offset,
                onClick = { onChange(time.atOffset(offset).toString()) },
                enabled = enabled,
                label = {
                    Text(
                        stringResource(
                            if (index == 0) R.string.first_occurrence
                            else R.string.second_occurrence
                        )
                    )
                },
            )
        }
    }
    if (date != null && offsets.isEmpty()) ErrorText(R.string.nonexistent_time)
}

@Composable
internal fun MomentField(value: Long, onChange: (Long?) -> Unit, zone: ZoneId, enabled: Boolean) {
    val original = Instant.ofEpochMilli(value).atZone(zone)
    var date by rememberSaveable { mutableStateOf(original.toLocalDate().toString()) }
    var time by rememberSaveable {
        mutableStateOf(original.toOffsetDateTime().toOffsetTime().toString())
    }
    DateField(date, { date = it }, R.string.picker_date, LocalDate.now(zone), enabled)
    TimeField(time, { time = it }, R.string.choose_time, LocalDate.parse(date), zone, enabled)
    val result = parsePickerTime(LocalDate.parse(date), time, zone)
    LaunchedEffect(result) { onChange(result) }
    if (result == null) ErrorText(R.string.timestamp_invalid)
}
