package org.alignertracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.*
import org.alignertracker.app.R
import org.alignertracker.app.domain.*

@Composable
fun JournalScreen(snapshot: TrackerSnapshot, model: TrackerViewModel, busy: Boolean) {
    val zone = ZoneId.of(snapshot.plan?.zoneId ?: "UTC")
    var date by rememberSaveable { mutableStateOf(LocalDate.now(zone).toString()) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteId by rememberSaveable { mutableStateOf(0L) }
    var appointmentId by rememberSaveable { mutableStateOf(0L) }
    var title by rememberSaveable { mutableStateOf("") }
    var appointmentTime by rememberSaveable { mutableStateOf("09:00") }
    var reminder by rememberSaveable { mutableStateOf("60") }
    var duration by rememberSaveable { mutableStateOf("30") }
    var appointmentNote by rememberSaveable { mutableStateOf("") }
    var missingStart by rememberSaveable { mutableStateOf("12:00") }
    var missingEndDate by rememberSaveable(date) { mutableStateOf(date) }
    var missingEnd by rememberSaveable { mutableStateOf("12:30") }
    var missingWearing by rememberSaveable { mutableStateOf(false) }
    var delete by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var correctionConfirm by remember { mutableStateOf(false) }
    val parsed =
        runCatching { LocalDate.parse(date).also { require(it.year in 1970..2100) } }.getOrNull()
    fun at(time: String): Long? = parseTreatmentTime(parsed, time, zone)
    val appointmentAt = at(appointmentTime)
    val start = at(missingStart)
    val end =
        parseTreatmentTime(
            runCatching { LocalDate.parse(missingEndDate) }.getOrNull(),
            missingEnd,
            zone,
        )
    val notes =
        snapshot.notes.filter {
            Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() == parsed
        }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.journal_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            TrackerTextField(
                date,
                { date = it },
                label = { Text(stringResource(R.string.journal_date)) },
                isError = parsed == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (parsed == null) item { FormFeedback(R.string.photo_date_invalid) }
        item {
            FilterChip(
                selected = calendar,
                onClick = { calendar = !calendar },
                label = { Text(stringResource(R.string.calendar_toggle)) },
            )
        }
        if (calendar && parsed != null)
            item {
                val locale = java.util.Locale.getDefault()
                val month = YearMonth.from(parsed)
                val firstWeekday = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
                val offset = (month.atDay(1).dayOfWeek.value - firstWeekday.value + 7) % 7
                Column {
                    JournalMonthNavigation(
                        previousLabel = stringResource(R.string.previous_month),
                        nextLabel = stringResource(R.string.next_month),
                        onPrevious = { date = parsed.minusMonths(1).withDayOfMonth(1).toString() },
                        onNext = { date = parsed.plusMonths(1).withDayOfMonth(1).toString() },
                    )
                    Text(
                        parsed.month.getDisplayName(java.time.format.TextStyle.FULL, locale) +
                            " " +
                            parsed.year
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Column(Modifier.width(448.dp)) {
                            Row {
                                for (index in 0..6) Text(
                                    firstWeekday
                                        .plus(index.toLong())
                                        .getDisplayName(java.time.format.TextStyle.SHORT, locale),
                                    modifier = Modifier.width(64.dp),
                                )
                            }
                            (0 until offset + month.lengthOfMonth()).toList().chunked(7).forEach {
                                row ->
                                Row {
                                    row.forEach { cell ->
                                        val day = cell - offset + 1
                                        if (day < 1) Spacer(Modifier.width(64.dp))
                                        else {
                                            val cellDate = month.atDay(day)
                                            val label =
                                                cellDate.format(
                                                    java.time.format.DateTimeFormatter
                                                        .ofLocalizedDate(
                                                            java.time.format.FormatStyle.FULL
                                                        )
                                                )
                                            TextButton(
                                                onClick = { date = cellDate.toString() },
                                                modifier =
                                                    Modifier.width(64.dp)
                                                        .heightIn(min = 56.dp)
                                                        .semantics {
                                                            contentDescription = label
                                                            selected = cellDate == parsed
                                                        },
                                            ) {
                                                Text(
                                                    java.text.NumberFormat.getIntegerInstance(
                                                            locale
                                                        )
                                                        .format(day)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        if (parsed != null)
            item {
                val summary = WearMath.summarize(snapshot, parsed, Instant.now())
                Text(
                    stringResource(
                        R.string.journal_summary,
                        summary.wornMillis / 60000,
                        summary.removedMillis / 60000,
                        summary.trackedMillis / 60000,
                    )
                )
            }
        if (snapshot.trackingGaps.isNotEmpty())
            item { Text(stringResource(R.string.clock_gap_notice)) }
        item {
            Text(
                stringResource(R.string.notes_heading),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (notes.isEmpty()) item { Text(stringResource(R.string.notes_empty)) }
        items(notes, key = { "note${it.id}" }) { note ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(note.text)
                    TextButton(
                        onClick = {
                            noteId = note.id
                            noteText = note.text
                        }
                    ) {
                        Text(stringResource(R.string.edit_record))
                    }
                    TextButton(onClick = { delete = "note" to note.id }, enabled = !busy) {
                        Text(stringResource(R.string.delete_record))
                    }
                }
            }
        }
        item {
            TrackerTextField(
                noteText,
                { noteText = it.take(10000) },
                label = { Text(stringResource(R.string.note_text)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = {
                    val existing = snapshot.notes.firstOrNull { it.id == noteId }
                    model.saveNote(
                        existing?.copy(text = noteText)
                            ?: TreatmentNote(
                                occurredAt = parsed!!.atStartOfDay(zone).toInstant().toEpochMilli(),
                                text = noteText,
                            )
                    ) {
                        noteId = 0
                        noteText = ""
                    }
                },
                enabled =
                    !busy &&
                        parsed != null &&
                        parsed <= LocalDate.now(zone) &&
                        noteText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_note))
            }
        }
        item { Text(stringResource(R.string.appointment_permission_hint)) }
        item {
            Text(
                stringResource(R.string.appointments_heading),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (snapshot.appointments.isEmpty())
            item { Text(stringResource(R.string.appointments_empty)) }
        items(snapshot.appointments.sortedBy { it.startsAt }, key = { "appointment${it.id}" }) {
            appointment ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(appointment.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Instant.ofEpochMilli(appointment.startsAt)
                            .atZone(zone)
                            .format(
                                java.time.format.DateTimeFormatter.ofLocalizedDateTime(
                                    java.time.format.FormatStyle.MEDIUM
                                )
                            )
                    )
                    if (appointment.note.isNotBlank()) Text(appointment.note)
                    TextButton(
                        onClick = {
                            appointmentId = appointment.id
                            title = appointment.title
                            date =
                                Instant.ofEpochMilli(appointment.startsAt)
                                    .atZone(zone)
                                    .toLocalDate()
                                    .toString()
                            appointmentTime =
                                Instant.ofEpochMilli(appointment.startsAt)
                                    .atZone(zone)
                                    .toLocalTime()
                                    .toString()
                            duration = appointment.durationMinutes.toString()
                            reminder = appointment.reminderMinutesBefore?.toString() ?: ""
                            appointmentNote = appointment.note
                        }
                    ) {
                        Text(stringResource(R.string.edit_record))
                    }
                    TextButton(
                        onClick = {
                            model.saveAppointment(
                                appointment.copy(completed = !appointment.completed)
                            )
                        },
                        enabled = !busy,
                    ) {
                        Text(
                            stringResource(
                                if (appointment.completed) R.string.appointment_reopen
                                else R.string.appointment_complete
                            )
                        )
                    }
                    TextButton(
                        onClick = { delete = "appointment" to appointment.id },
                        enabled = !busy,
                    ) {
                        Text(stringResource(R.string.delete_record))
                    }
                }
            }
        }
        item {
            TrackerTextField(
                title,
                { title = it.take(200) },
                label = { Text(stringResource(R.string.appointment_title)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TrackerTextField(
                appointmentTime,
                { appointmentTime = it },
                label = { Text(stringResource(R.string.appointment_time)) },
                supportingText = { Text(stringResource(R.string.local_time_hint, zone.id)) },
                isError = appointmentAt == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TrackerTextField(
                duration,
                { duration = it },
                label = { Text(stringResource(R.string.appointment_duration)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TrackerTextField(
                reminder,
                { reminder = it },
                label = { Text(stringResource(R.string.appointment_reminder)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TrackerTextField(
                appointmentNote,
                { appointmentNote = it.take(10000) },
                label = { Text(stringResource(R.string.note_text)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (
            appointmentAt == null ||
                duration.toIntOrNull() !in 1..1440 ||
                title.isBlank() ||
                (reminder.isNotBlank() && reminder.toIntOrNull() !in 0..10080)
        )
            item { FormFeedback(R.string.appointment_form_invalid) }
        item {
            Button(
                onClick = {
                    val existing = snapshot.appointments.firstOrNull { it.id == appointmentId }
                    model.saveAppointment(
                        (existing
                                ?: Appointment(
                                    startsAt = appointmentAt!!,
                                    durationMinutes = duration.toInt(),
                                    title = title,
                                ))
                            .copy(
                                startsAt = appointmentAt!!,
                                durationMinutes = duration.toInt(),
                                title = title,
                                note = appointmentNote,
                                reminderMinutesBefore = reminder.toIntOrNull(),
                            )
                    ) {
                        appointmentId = 0
                        title = ""
                        appointmentNote = ""
                    }
                },
                enabled =
                    !busy &&
                        appointmentAt != null &&
                        duration.toIntOrNull() in 1..1440 &&
                        title.isNotBlank() &&
                        (reminder.isBlank() || reminder.toIntOrNull() in 0..10080),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_appointment))
            }
        }
        item {
            Text(
                stringResource(R.string.missing_interval),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        item { Text(stringResource(R.string.missing_interval_hint)) }
        item {
            TrackerTextField(
                missingStart,
                { missingStart = it },
                label = { Text(stringResource(R.string.interval_start)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TrackerTextField(
                missingEndDate,
                { missingEndDate = it },
                label = { Text(stringResource(R.string.interval_end_date)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TrackerTextField(
                missingEnd,
                { missingEnd = it },
                label = { Text(stringResource(R.string.interval_end)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            FilterChip(
                selected = missingWearing,
                onClick = { missingWearing = !missingWearing },
                label = {
                    Text(
                        stringResource(
                            if (missingWearing) R.string.widget_in else R.string.widget_out
                        )
                    )
                },
            )
        }
        if (start == null || end == null || start >= end || end > System.currentTimeMillis())
            item { FormFeedback(R.string.correction_form_invalid) }
        item {
            Button(
                onClick = { correctionConfirm = true },
                enabled =
                    !busy &&
                        start != null &&
                        end != null &&
                        start < end &&
                        end <= System.currentTimeMillis(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.review_correction))
            }
        }
    }
    delete?.let { (kind, id) ->
        TrackerDialog(
            onDismissRequest = { delete = null },
            title = { Text(stringResource(R.string.delete_record)) },
            text = { Text(stringResource(R.string.delete_record_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (kind == "note") model.deleteNote(id) else model.deleteAppointment(id)
                        delete = null
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { delete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (correctionConfirm)
        TrackerDialog(
            onDismissRequest = { correctionConfirm = false },
            title = { Text(stringResource(R.string.review_correction)) },
            text = {
                Text(
                    "$date $missingStart – $missingEndDate $missingEnd\n" +
                        stringResource(R.string.missing_interval_hint)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (start != null && end != null)
                            model.insertMissingInterval(start, end, missingWearing)
                        correctionConfirm = false
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.save_note))
                }
            },
            dismissButton = {
                TextButton(onClick = { correctionConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}

@Composable
internal fun JournalMonthNavigation(
    previousLabel: String,
    nextLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / fontScale < 300.dp) {
            Column(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onPrevious,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(previousLabel)
                }
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(nextLabel)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(previousLabel)
                }
                TextButton(onClick = onNext, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(nextLabel)
                }
            }
        }
    }
}
