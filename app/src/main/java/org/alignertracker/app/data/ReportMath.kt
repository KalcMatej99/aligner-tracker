package org.alignertracker.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.alignertracker.app.domain.DaySummary
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.WearMath

object ReportMath {
    data class Period(
        val label: String,
        val wornMillis: Long,
        val removedMillis: Long,
        val trackedMillis: Long,
    )

    fun days(snapshot: TrackerSnapshot, count: Int, now: Instant): List<DaySummary> {
        require(count in 1..366)
        val plan = snapshot.plan ?: return emptyList()
        val today = now.atZone(ZoneId.of(plan.zoneId)).toLocalDate()
        return (0 until count).map {
            WearMath.summarize(snapshot, today.minusDays((count - it - 1).toLong()), now)
        }
    }

    /** Only complete closed calendar days qualify. Unknown and partial days break a run. */
    fun streak(snapshot: TrackerSnapshot, now: Instant): Int {
        val plan = snapshot.plan ?: return 0
        val zone = ZoneId.of(plan.zoneId)
        var date = now.atZone(zone).toLocalDate().minusDays(1)
        val first = Instant.ofEpochMilli(plan.trackingStartedAt).atZone(zone).toLocalDate()
        var count = 0
        while (date >= first) {
            val summary = WearMath.summarize(snapshot, date, now)
            if (
                summary.trackedMillis != dayMillis(date, zone) ||
                    summary.wornMillis < summary.goalMinutes * 60_000L
            )
                break
            count++
            date = date.minusDays(1)
        }
        return count
    }

    fun dayMillis(date: LocalDate, zone: ZoneId): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() -
            date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun interval(snapshot: TrackerSnapshot, start: Long, end: Long): Period {
        var worn = 0L
        var removed = 0L
        snapshot.events.forEachIndexed { index, event ->
            val left = maxOf(start, event.at)
            val right =
                minOf(
                    end,
                    snapshot.events.getOrNull(index + 1)?.at ?: end,
                    snapshot.plan?.completedAt ?: Long.MAX_VALUE,
                )
            val excluded =
                snapshot.trackingGaps.sumOf { gap ->
                    (minOf(right, gap.endAt) - maxOf(left, gap.startAt)).coerceAtLeast(0)
                }
            val duration = ((right - left).coerceAtLeast(0) - excluded).coerceAtLeast(0)
            if (event.wearing) worn += duration else removed += duration
        }
        return Period("", worn, removed, worn + removed)
    }
}
