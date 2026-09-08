package org.alignertracker.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WearMath {
    fun isWearing(snapshot: TrackerSnapshot): Boolean = snapshot.events.lastOrNull()?.wearing ?: false

    fun nextChangeDate(plan: TreatmentPlan): LocalDate =
        LocalDate.parse(plan.currentTrayStartedOn).plusDays(plan.daysPerTray.toLong())

    fun summarize(snapshot: TrackerSnapshot, date: LocalDate, now: Instant): DaySummary {
        val plan = snapshot.plan
            ?: return DaySummary(date.toString(), 0, 0, 0, 0)
        val zone = ZoneId.of(plan.zoneId)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            now.toEpochMilli(),
            plan.completedAt ?: Long.MAX_VALUE,
        )
        var worn = 0L
        var removed = 0L
        val events = snapshot.events
        // Find the interval covering midnight without scanning all preceding history.
        var low = 0
        var high = events.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (events[mid].at <= start) low = mid + 1 else high = mid
        }
        var index = (low - 1).coerceAtLeast(0)
        while (index < events.size && events[index].at < end) {
            val event = events[index]
            val intervalStart = maxOf(start, event.at, plan.trackingStartedAt)
            val intervalEnd = minOf(end, events.getOrNull(index + 1)?.at ?: end)
            val duration = (intervalEnd - intervalStart).coerceAtLeast(0)
            if (event.wearing) worn += duration else removed += duration
            index++
        }
        return DaySummary(date.toString(), worn, removed, worn + removed, plan.dailyGoalMinutes)
    }
}
