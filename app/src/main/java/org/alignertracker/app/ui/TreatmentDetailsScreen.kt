package org.alignertracker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.alignertracker.app.R
import org.alignertracker.app.domain.*

@Composable
fun TreatmentDetailsScreen(snapshot: TrackerSnapshot, model: TrackerViewModel, busy: Boolean) {
    val active = snapshot.phases.firstOrNull { it.active }
    var intervals by
        rememberSaveable(active?.id) {
            mutableStateOf("1-${active?.totalTrays ?: 1}:${snapshot.plan?.daysPerTray ?: 7}")
        }
    var reason by rememberSaveable { mutableStateOf("") }
    var phaseName by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("1") }
    var days by rememberSaveable { mutableStateOf("7") }
    var phaseWearing by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var kind by rememberSaveable { mutableStateOf(TreatmentPhaseKind.REFINEMENT.name) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val parsed =
        runCatching {
                intervals
                    .split(',')
                    .map { part ->
                        val sides = part.trim().split(':')
                        require(sides.size == 2)
                        val range = sides[0].split('-')
                        require(range.size in 1..2)
                        TrayIntervalDraft(
                            range[0].trim().toInt(),
                            range.last().trim().toInt(),
                            sides[1].trim().toInt(),
                        )
                    }
                    .also { require(it.isNotEmpty()) }
            }
            .getOrNull()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.treatment_details),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.schedule_instructions))
        if (active != null && snapshot.plan?.completed == false) {
            OutlinedTextField(
                intervals,
                { intervals = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.variable_intervals)) },
                supportingText = { Text(stringResource(R.string.interval_example)) },
                isError = parsed == null,
            )
            if (parsed == null) FormFeedback(R.string.schedule_format_invalid)
            OutlinedTextField(
                reason,
                { reason = it.take(1000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.adjustment_reason)) },
            )
            Button(
                onClick = { confirm = "schedule" },
                enabled = !busy && parsed != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_schedule))
            }
        }
        Text(
            stringResource(R.string.new_phase),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.phase_instructions))
        OutlinedTextField(
            phaseName,
            { phaseName = it.take(100) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.phase_name)) },
        )
        for (phaseKind in
            listOf(TreatmentPhaseKind.REFINEMENT, TreatmentPhaseKind.RETENTION)) FilterChip(
            selected = kind == phaseKind.name,
            onClick = { kind = phaseKind.name },
            label = {
                Text(
                    stringResource(
                        if (phaseKind == TreatmentPhaseKind.REFINEMENT) R.string.phase_refinement
                        else R.string.phase_retention
                    )
                )
            },
        )
        OutlinedTextField(
            count,
            { count = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.phase_count)) },
        )
        OutlinedTextField(
            days,
            { days = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.phase_days)) },
        )
        Text(stringResource(R.string.phase_choose_state))
        for (state in listOf(true, false)) FilterChip(
            selected = phaseWearing == state,
            onClick = { phaseWearing = state },
            label = { Text(stringResource(if (state) R.string.widget_in else R.string.widget_out)) },
        )
        if (phaseName.isBlank() || count.toIntOrNull() !in 1..1000 || days.toIntOrNull() !in 1..365)
            FormFeedback(R.string.phase_form_invalid)
        Button(
            onClick = { confirm = "phase" },
            enabled =
                !busy &&
                    phaseWearing != null &&
                    phaseName.isNotBlank() &&
                    count.toIntOrNull() in 1..1000 &&
                    days.toIntOrNull() in 1..365,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.begin_phase))
        }
        Text(
            stringResource(R.string.actual_tray_history),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (snapshot.trayHistory.isEmpty()) Text(stringResource(R.string.tray_history_empty))
        snapshot.trayHistory.asReversed().forEach { entry ->
            val name = snapshot.phases.firstOrNull { it.id == entry.phaseId }?.name ?: ""
            Text(
                stringResource(
                    R.string.tray_history_row,
                    name,
                    entry.trayNumber,
                    entry.startedOn,
                    entry.endedOn ?: stringResource(R.string.ongoing),
                    entry.prescribedDays,
                )
            )
        }
    }
    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(
                    stringResource(
                        if (action == "phase") R.string.begin_phase else R.string.save_schedule
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (action == "phase") R.string.phase_confirm else R.string.schedule_confirm
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (action == "phase")
                            model.beginPhase(
                                TreatmentPhaseKind.valueOf(kind),
                                phaseName,
                                count.toInt(),
                                days.toInt(),
                                phaseWearing!!,
                            )
                        else if (active != null && parsed != null)
                            model.replaceSchedule(active.id, parsed, reason)
                        confirm = null
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.apply_changes))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
