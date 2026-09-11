package org.alignertracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.temporal.ChronoUnit
import org.alignertracker.app.R
import org.alignertracker.app.domain.TreatmentPlan

/** Calendar-time position in the current tray; never a wear score or automatic advancement. */
@Composable
internal fun ScheduleNextChange(
    plan: TreatmentPlan,
    due: LocalDate,
    now: Instant,
    lastTray: Boolean,
) {
    val today = now.atZone(ZoneId.of(plan.zoneId)).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due)
    val remaining =
        when {
            days > 0 -> pluralStringResource(R.plurals.schedule_days_until, days.toInt(), days)
            days == 0L -> stringResource(R.string.schedule_planned_today)
            else -> stringResource(R.string.schedule_date_passed)
        }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(
                if (lastTray) R.string.schedule_planned_finish else R.string.schedule_next_change
            ),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Text(
            readableDate(due),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            remaining,
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary,
            textAlign = TextAlign.Center,
        )
        if (!lastTray && plan.currentTray != null)
            Text(
                stringResource(R.string.schedule_next_tray, plan.currentTray + 1),
                style = MaterialTheme.typography.bodyLarge,
            )
        Spacer(Modifier.height(12.dp))
        Text(
            trayLabel(plan),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        plan.currentTrayStartedOn?.let { value ->
            val start = LocalDate.parse(value)
            val interval = ChronoUnit.DAYS.between(start, due).coerceAtLeast(1)
            val elapsed = ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)
            val description = stringResource(R.string.schedule_elapsed, elapsed, interval)
            Canvas(Modifier.fillMaxWidth().height(40.dp).clearAndSetSemantics {}) {
                val left = 9.dp.toPx()
                val right = size.width - left
                val y = size.height / 2
                val x = left + (right - left) * elapsed.coerceAtMost(interval).toFloat() / interval
                drawLine(
                    colors.outlineVariant,
                    Offset(left, y),
                    Offset(right, y),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    colors.primary,
                    Offset(left, y),
                    Offset(x, y),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawCircle(colors.primary, 5.dp.toPx(), Offset(left, y))
                drawCircle(colors.surface, 7.dp.toPx(), Offset(right, y))
                drawCircle(
                    colors.onSurfaceVariant,
                    7.dp.toPx(),
                    Offset(right, y),
                    style = Stroke(2.dp.toPx()),
                )
                drawCircle(colors.primary, 7.dp.toPx(), Offset(x, y))
                drawCircle(colors.surface, 3.dp.toPx(), Offset(x, y))
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.current_started, readableDate(start)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
