package org.alignertracker.app.ui

import java.time.*
import org.alignertracker.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class HistoryTimeSpansTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun at(date: LocalDate, hour: Int) =
        date.atStartOfDay().plusHours(hour.toLong()).atZone(zone).toInstant().toEpochMilli()

    private fun snapshot(date: LocalDate) =
        TrackerSnapshot(
            plan = TreatmentPlan(zoneId = zone.id, trackingStartedAt = at(date.minusDays(1), 20)),
            events =
                listOf(
                    WearEvent(1, at(date.minusDays(1), 20), true),
                    WearEvent(2, at(date, 8), false),
                    WearEvent(3, at(date, 9), true),
                    WearEvent(4, at(date, 14), false),
                    WearEvent(5, at(date, 15), true),
                ),
        )

    @Test
    fun chronologicalBreaksAndOvernightCarryAgreeWithAccounting() {
        val date = LocalDate.parse("2026-09-10")
        val snapshot = snapshot(date)
        val now = Instant.ofEpochMilli(at(date, 18))
        val spans = historyTimeSpans(snapshot, date, now, zone)
        assertEquals(
            listOf(
                HistoryTimeState.IN,
                HistoryTimeState.OUT,
                HistoryTimeState.IN,
                HistoryTimeState.OUT,
                HistoryTimeState.IN,
                HistoryTimeState.FUTURE,
            ),
            spans.map { it.state },
        )
        assertEquals(
            listOf(at(date, 8), at(date, 14)),
            spans.filter { it.state == HistoryTimeState.OUT }.map { it.start },
        )
        val summary = WearMath.summarize(snapshot, date, now)
        assertEquals(
            summary.wornMillis,
            spans.filter { it.state == HistoryTimeState.IN }.sumOf { it.end - it.start },
        )
        assertEquals(
            summary.removedMillis,
            spans.filter { it.state == HistoryTimeState.OUT }.sumOf { it.end - it.start },
        )
        assertEquals(at(date.plusDays(1), 0) - at(date, 0), spans.sumOf { it.end - it.start })
    }

    @Test
    fun gapsCompletionAndFirstRecordStayUntracked() {
        val date = LocalDate.parse("2026-09-10")
        val snapshot =
            snapshot(date)
                .copy(
                    plan =
                        snapshot(date)
                            .plan!!
                            .copy(
                                trackingStartedAt = at(date, 4),
                                completed = true,
                                completedAt = at(date, 16),
                            ),
                    trackingGaps = listOf(TrackingGap(startAt = at(date, 10), endAt = at(date, 11))),
                )
        val now = Instant.ofEpochMilli(at(date, 18))
        val spans = historyTimeSpans(snapshot, date, now, zone)
        assertEquals(
            listOf(at(date, 0), at(date, 10), at(date, 16)),
            spans.filter { it.state == HistoryTimeState.UNTRACKED }.map { it.start },
        )
        assertEquals(
            WearMath.summarize(snapshot, date, now).wornMillis,
            spans.filter { it.state == HistoryTimeState.IN }.sumOf { it.end - it.start },
        )
        val empty = historyTimeSpans(snapshot.copy(events = emptyList()), date, now, zone)
        assertEquals(
            listOf(HistoryTimeState.UNTRACKED, HistoryTimeState.FUTURE),
            empty.map { it.state },
        )
    }

    @Test
    fun dstDaysCoverActualElapsedTimeWithoutLosingRepeatedHour() {
        for ((text, hours) in listOf("2026-03-29" to 23, "2026-10-25" to 25)) {
            val date = LocalDate.parse(text)
            val snapshot = snapshot(date)
            val now = Instant.ofEpochMilli(at(date.plusDays(1), 0))
            val spans = historyTimeSpans(snapshot, date, now, zone)
            assertEquals(hours * 3600000L, spans.sumOf { it.end - it.start })
            assertEquals(
                WearMath.summarize(snapshot, date, now).trackedMillis,
                spans
                    .filter { it.state == HistoryTimeState.IN || it.state == HistoryTimeState.OUT }
                    .sumOf { it.end - it.start },
            )
            assertTrue(spans.zipWithNext().all { (a, b) -> a.end == b.start })
        }
    }
}
