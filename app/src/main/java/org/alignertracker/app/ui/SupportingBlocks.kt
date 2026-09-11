package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.alignertracker.app.R
import org.alignertracker.app.domain.TreatmentPlan

/** Original code-native information blocks; icons reuse the app's original vector system. */
@Composable
internal fun ExpandableBlock(
    title: Int,
    summary: String,
    icon: Destination,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val holder = rememberSaveableStateHolder()
    val state =
        stringResource(if (expanded) R.string.support_expanded else R.string.support_collapsed)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .semantics { stateDescription = state }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TrackerNavIcon(icon)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(if (expanded) "−" else "+", Modifier.clearAndSetSemantics {})
            }
            if (expanded)
                holder.SaveableStateProvider("content") {
                    Column(
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
        }
    }
}

@Composable
internal fun TreatmentPlanOverview(plan: TreatmentPlan?) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.support_saved_plan),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (plan?.currentTray != null && plan.totalTrays != null)
                    stringResource(
                        R.string.support_tray_position,
                        plan.currentTray,
                        plan.totalTrays,
                    )
                else stringResource(R.string.not_known),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (plan?.currentTray != null && plan.totalTrays != null) {
                val primary = MaterialTheme.colorScheme.primary
                val outline = MaterialTheme.colorScheme.outline
                Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                    val count = plan.totalTrays.coerceAtMost(30)
                    val current =
                        ((plan.currentTray - 1).toFloat() / plan.totalTrays * count)
                            .toInt()
                            .coerceIn(0, count - 1)
                    val step = size.width / count
                    repeat(count) { i ->
                        drawCircle(
                            if (i == current) primary else outline,
                            if (i == current) 6.dp.toPx() else 2.dp.toPx(),
                            Offset(step * (i + .5f), size.height / 2),
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    plan?.dailyGoalMinutes?.let {
                        stringResource(R.string.support_daily_target, compactDuration(it * 60000L))
                    } ?: stringResource(R.string.report_goal_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                )
                plan?.daysPerTray?.let {
                    Text(
                        stringResource(R.string.support_tray_interval, it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (plan?.completed == true) Text(stringResource(R.string.completed_title))
        }
    }
}

@Composable
internal fun PhotoProgressEntry(count: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrackerNavIcon(Destination.PHOTOS)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.support_photo_progress),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        if (count == 0) R.string.support_photo_empty
                        else R.string.support_photo_saved,
                        count,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
