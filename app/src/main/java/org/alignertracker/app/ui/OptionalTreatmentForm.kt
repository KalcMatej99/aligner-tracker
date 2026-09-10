package org.alignertracker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.*
import org.alignertracker.app.R
import org.alignertracker.app.domain.TrackerSnapshot

@Composable
internal fun OptionalTreatmentForm(
    snapshot: TrackerSnapshot,
    model: TrackerViewModel,
    busy: Boolean,
) {
    val plan = snapshot.plan ?: return
    var total by
        rememberSaveable(plan.trackingStartedAt, plan.totalTrays) {
            mutableStateOf(plan.totalTrays?.toString().orEmpty())
        }
    var current by
        rememberSaveable(plan.trackingStartedAt, plan.currentTray) {
            mutableStateOf(plan.currentTray?.toString().orEmpty())
        }
    var interval by
        rememberSaveable(plan.trackingStartedAt, plan.daysPerTray) {
            mutableStateOf(plan.daysPerTray?.toString().orEmpty())
        }
    var start by
        rememberSaveable(plan.trackingStartedAt, plan.startDate) {
            mutableStateOf(plan.startDate.orEmpty())
        }
    var trayStart by
        rememberSaveable(plan.trackingStartedAt, plan.currentTrayStartedOn) {
            mutableStateOf(plan.currentTrayStartedOn.orEmpty())
        }
    var goal by rememberSaveable(plan.dailyGoalMinutes) { mutableStateOf("") }
    var goalError by rememberSaveable { mutableStateOf(false) }
    val goalFeedback = remember { BringIntoViewRequester() }
    LaunchedEffect(goalError) { if (goalError) goalFeedback.bringIntoView() }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var confirmCorrection by rememberSaveable { mutableStateOf(false) }
    val established = snapshot.phases.isNotEmpty()
    val activePhase = snapshot.phases.firstOrNull { it.active }
    val highestRecordedTray =
        snapshot.trayHistory
            .filter { activePhase == null || it.phaseId == activePhase.id }
            .maxOfOrNull { it.trayNumber } ?: 0
    val editable = !busy && !plan.completed
    val totalNumber = parseUserInteger(total)
    val currentNumber = parseUserInteger(current)
    val intervalNumber = parseUserInteger(interval)
    val totalBelowHistory = totalNumber != null && totalNumber < highestRecordedTray
    val totalError =
        (total.isNotBlank() && totalNumber !in 1..1000) ||
            (established && total.isBlank()) ||
            totalBelowHistory
    val currentError =
        (current.isNotBlank() &&
            (currentNumber !in 1..1000 ||
                (totalNumber != null && currentNumber != null && currentNumber > totalNumber))) ||
            (established && current.isBlank())
    val intervalError =
        (interval.isNotBlank() && intervalNumber !in 1..365) || (established && interval.isBlank())
    val dateError = start.isNotBlank() && trayStart.isNotBlank() && start > trayStart
    val trayDateMissing = established && trayStart.isBlank()
    val missingEstablishedField =
        established &&
            (current.isBlank() || total.isBlank() || interval.isBlank() || trayStart.isBlank())
    val valid = !totalError && !currentError && !intervalError && !dateError && !trayDateMissing
    val scheduleChanged =
        totalNumber != plan.totalTrays ||
            currentNumber != plan.currentTray ||
            intervalNumber != plan.daysPerTray ||
            trayStart.ifBlank { null } != plan.currentTrayStartedOn
    fun saveDetails() {
        model.updateTreatmentDetails(
            start.ifBlank { null },
            totalNumber,
            currentNumber,
            intervalNumber,
            trayStart.ifBlank { null },
        )
    }
    Section(R.string.optional_prescription) {
        Text(stringResource(R.string.optional_target_help))
        plan.dailyGoalMinutes?.let {
            Text(stringResource(R.string.goal_value, durationLabel(it * 60_000L)))
        }
        Entry(
            goal,
            {
                goal = it
                goalError = false
            },
            R.string.optional_goal_hours,
            numeric = true,
            error = goalError,
            errorMessage = if (goalError) R.string.goal_hours_invalid else null,
            enabled = editable,
            modifier = Modifier.bringIntoViewRequester(goalFeedback),
        )
        Text(
            stringResource(
                if (plan.dailyGoalMinutes == null) R.string.initial_target_help
                else R.string.target_help
            )
        )
        Button(
            shape = androidx.compose.material3.MaterialTheme.shapes.small,
            onClick = {
                val minutes = parseUserHours(goal)
                goalError = minutes !in 1..1440
                if (!goalError) model.updateGoal(minutes!!)
            },
            enabled = editable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.save_target))
        }
    }
    Section(R.string.optional_tray_details) {
        Text(stringResource(R.string.optional_tray_help))
        Entry(
            current,
            { current = it },
            R.string.optional_current_tray,
            true,
            attempted && currentError,
            if (attempted && currentError) R.string.current_tray_invalid else null,
            editable,
        )
        Entry(
            total,
            { total = it },
            R.string.optional_total_trays,
            true,
            attempted && totalError,
            if (attempted && totalBelowHistory) R.string.total_preserves_history
            else if (attempted && totalError) R.string.tray_count_invalid else null,
            editable,
        )
        Entry(
            interval,
            { interval = it },
            R.string.optional_interval,
            true,
            attempted && intervalError,
            if (attempted && intervalError) R.string.tray_days_invalid else null,
            editable,
        )
        OptionalDate(
            trayStart,
            { trayStart = it },
            R.string.optional_tray_date,
            plan.zoneId,
            editable,
        )
        OptionalDate(start, { start = it }, R.string.optional_treatment_date, plan.zoneId, editable)
        if (attempted && dateError) ErrorText(R.string.optional_date_order)
        if (attempted && missingEstablishedField) ErrorText(R.string.established_schedule_required)
        if (established) Text(stringResource(R.string.existing_schedule_edit_help))
        Button(
            shape = androidx.compose.material3.MaterialTheme.shapes.small,
            onClick = {
                attempted = true
                if (valid) {
                    if (established && scheduleChanged) confirmCorrection = true else saveDetails()
                }
            },
            enabled = editable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.save_treatment_details))
        }
    }
    if (confirmCorrection)
        TrackerDialog(
            onDismissRequest = { if (!busy) confirmCorrection = false },
            title = { Text(stringResource(R.string.confirm_treatment_correction)) },
            text = { Text(stringResource(R.string.treatment_correction_notice)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveDetails()
                        confirmCorrection = false
                    },
                    enabled = editable && valid,
                ) {
                    Text(stringResource(R.string.apply_treatment_correction))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCorrection = false }, enabled = !busy) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalDate(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    zone: String,
    enabled: Boolean,
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now(ZoneId.of(zone))
    val dateLabel = stringResource(label)
    Text(dateLabel, style = MaterialTheme.typography.labelLarge)
    Text(
        value.takeIf { it.isNotBlank() }?.let { readableDate(LocalDate.parse(it)) }
            ?: stringResource(R.string.not_known)
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = { onChange(today.toString()) },
            enabled = enabled,
            modifier = Modifier.recordAction(stringResource(R.string.date_today_action, dateLabel)),
        ) {
            Text(stringResource(R.string.use_today))
        }
        TextButton(
            onClick = { choosing = true },
            enabled = enabled,
            modifier = Modifier.recordAction(stringResource(R.string.date_choose_action, dateLabel)),
        ) {
            Text(stringResource(R.string.choose_date))
        }
        TextButton(
            onClick = { onChange("") },
            enabled = enabled,
            modifier =
                Modifier.recordAction(stringResource(R.string.date_unknown_action, dateLabel)),
        ) {
            Text(stringResource(R.string.not_sure))
        }
    }
    if (choosing)
        CalendarDialog(
            value,
            today,
            {
                onChange(it)
                choosing = false
            },
            { choosing = false },
        )
}
