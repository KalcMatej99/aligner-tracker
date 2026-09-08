package org.alignertracker.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WearMath {
    fun isWearing(snapshot: TrackerSnapshot): Boolean =
        snapshot.events.lastOrNull()?.wearing ?: false

    fun nextChangeDate(plan: TreatmentPlan): LocalDate =
        LocalDate.parse(plan.currentTrayStartedOn).plusDays(plan.daysPerTray.toLong())

    fun nextChangeDate(snapshot: TrackerSnapshot): LocalDate {
        val plan = checkNotNull(snapshot.plan) { "Set up a treatment first." }
        val openTray = snapshot.trayHistory.singleOrNull { it.endedOn == null }
        val activePhase = snapshot.phases.singleOrNull { it.active }
        val revision =
            activePhase?.let { phase ->
                snapshot.scheduleRevisions
                    .filter { it.phaseId == phase.id }
                    .maxWithOrNull(compareBy<ScheduleRevision> { it.createdAt }.thenBy { it.id })
            }
        val currentDays =
            revision?.let { current ->
                snapshot.trayIntervals.singleOrNull {
                    it.scheduleRevisionId == current.id && plan.currentTray in it.firstTray..it.lastTray
                }
            }?.daysPerTray ?: plan.daysPerTray
        return LocalDate.parse(openTray?.startedOn ?: plan.currentTrayStartedOn)
            .plusDays(currentDays.toLong())
    }

    fun targetForDate(snapshot: TrackerSnapshot, date: LocalDate): Int {
        val plan = snapshot.plan ?: return 0
        return snapshot.targetHistory
            .asSequence()
            .filter { LocalDate.parse(it.effectiveFrom) <= date }
            .maxByOrNull { it.effectiveFrom }
            ?.goalMinutes ?: plan.dailyGoalMinutes
    }

    fun summarize(snapshot: TrackerSnapshot, date: LocalDate, now: Instant): DaySummary {
        val plan = snapshot.plan ?: return DaySummary(date.toString(), 0, 0, 0, 0)
        val zone = ZoneId.of(plan.zoneId)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end =
            minOf(
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
            val obscured =
                snapshot.trackingGaps.sumOf { gap ->
                    (minOf(intervalEnd, gap.endAt) - maxOf(intervalStart, gap.startAt))
                        .coerceAtLeast(0)
                }
            val duration = ((intervalEnd - intervalStart).coerceAtLeast(0) - obscured).coerceAtLeast(0)
            if (event.wearing) worn += duration else removed += duration
            index++
        }
        return DaySummary(date.toString(), worn, removed, worn + removed, targetForDate(snapshot, date))
    }
}
