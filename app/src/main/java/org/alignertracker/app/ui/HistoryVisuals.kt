package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import org.alignertracker.app.R
import org.alignertracker.app.domain.WearEvent

/**
 * Original code-drawn legend; matches the chronological day timeline, without hiding its meaning.
 */
@Composable
internal fun HistoryBarLegend() {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
                R.string.history_legend_in,
                R.string.history_legend_out,
                R.string.history_legend_unknown,
                R.string.history_legend_future,
            )
            .forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Canvas(Modifier.size(18.dp, 12.dp)) {
                        drawRect(
                            if (index == 0) colors.primary
                            else if (index == 1) colors.surfaceContainerHigh else colors.surface
                        )
                        if (index == 2)
                            for (x in listOf(4f, 9f, 14f)) drawCircle(
                                colors.onSurfaceVariant,
                                1.dp.toPx(),
                                Offset(x.dp.toPx(), size.height / 2),
                            )
                        if (index == 3) {
                            drawLine(
                                colors.outline,
                                Offset(0f, size.height / 2),
                                Offset(size.width, size.height / 2),
                                1.dp.toPx(),
                            )
                        } else {
                            val stroke = if (index == 1) 1.5.dp.toPx() else 1.dp.toPx()
                            drawRect(
                                if (index == 1) colors.onSurfaceVariant else colors.outline,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(stroke),
                            )
                        }
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
                }
            }
    }
    Text(
        stringResource(R.string.history_bar_note),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
    )
}

/** Equal-spaced recorded events, not duration segments or inferred continuous coverage. */
@Composable
internal fun HistoryEventRow(
    event: WearEvent,
    zone: ZoneId,
    first: Boolean,
    last: Boolean,
    initial: Boolean,
    completed: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val state = stringResource(if (event.wearing) R.string.state_in else R.string.state_out)
    val editLabel =
        stringResource(R.string.edit_transition_accessibility, state, readableTime(event.at, zone))
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(Modifier.width(20.dp).fillMaxHeight()) {
            val x = size.width / 2
            val y = 24.dp.toPx()
            if (!first) drawLine(colors.outlineVariant, Offset(x, 0f), Offset(x, y), 1.dp.toPx())
            if (!last)
                drawLine(colors.outlineVariant, Offset(x, y), Offset(x, size.height), 1.dp.toPx())
            drawCircle(colors.surface, 7.dp.toPx(), Offset(x, y))
            if (event.wearing) drawCircle(colors.primary, 6.dp.toPx(), Offset(x, y))
            else
                drawCircle(
                    colors.onSurfaceVariant,
                    6.dp.toPx(),
                    Offset(x, y),
                    style = Stroke(2.dp.toPx()),
                )
        }
        Column(
            Modifier.weight(1f).padding(vertical = 12.dp).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                state,
                style = MaterialTheme.typography.titleMedium,
                color = if (event.wearing) colors.primary else colors.onSurface,
            )
            Text(
                readableTime(event.at, zone, includeDate = false),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            if (initial)
                Text(
                    stringResource(R.string.initial_event),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
        }
        if (!initial && !completed)
            IconButton(
                onClick = onEdit,
                enabled = !busy,
                modifier =
                    Modifier.padding(top = 4.dp).heightIn(min = 48.dp).recordAction(editLabel),
            ) {
                TrackerUtilityIcon(UtilityIcon.EDIT)
            }
    }
}
