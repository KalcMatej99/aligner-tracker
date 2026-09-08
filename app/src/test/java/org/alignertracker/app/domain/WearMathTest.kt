package org.alignertracker.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WearMathTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun at(date: String, hour: Int): Long = LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun snapshot(date: String, hour: Int = 0): TrackerSnapshot {
        val start = at(date, hour)
        return TrackerSnapshot(TreatmentPlan(startDate = date, totalTrays = 20, currentTray = 1,
            daysPerTray = 7, currentTrayStartedOn = date, dailyGoalMinutes = 1200,
            zoneId = zone.id, trackingStartedAt = start), listOf(WearEvent(1, start, true)))
    }

    @Test fun `spring DST has 23 tracked hours`() {
        val state = snapshot("2026-03-29")
        val summary = WearMath.summarize(state, LocalDate.parse("2026-03-29"), Instant.ofEpochMilli(at("2026-03-30", 0)))
        assertEquals(23 * 3_600_000L, summary.wornMillis)
        assertEquals(summary.wornMillis, summary.trackedMillis)
    }

    @Test fun `autumn DST has 25 tracked hours`() {
        val state = snapshot("2025-10-26")
        val summary = WearMath.summarize(state, LocalDate.parse("2025-10-26"), Instant.ofEpochMilli(at("2025-10-27", 0)))
        assertEquals(25 * 3_600_000L, summary.wornMillis)
    }

    @Test fun `partial first day never invents earlier wear and clips now`() {
        val state = snapshot("2026-03-28", 12)
        val summary = WearMath.summarize(state, LocalDate.parse("2026-03-28"), Instant.ofEpochMilli(at("2026-03-28", 15)))
        assertEquals(3 * 3_600_000L, summary.trackedMillis)
        assertEquals(0, summary.removedMillis)
    }

    @Test fun `intervals spanning midnight split wearing and removed`() {
        val state = snapshot("2026-03-27", 23).let {
            it.copy(events = it.events + WearEvent(2, at("2026-03-28", 1), false) + WearEvent(3, at("2026-03-28", 3), true))
        }
        val summary = WearMath.summarize(state, LocalDate.parse("2026-03-28"), Instant.ofEpochMilli(at("2026-03-28", 5)))
        assertEquals(3 * 3_600_000L, summary.wornMillis)
        assertEquals(2 * 3_600_000L, summary.removedMillis)
        assertEquals(5 * 3_600_000L, summary.trackedMillis)
    }

    @Test fun `completion freezes totals including the last active interval`() {
        val state = snapshot("2026-03-28", 12).let {
            it.copy(plan = it.plan!!.copy(completed = true, completedAt = at("2026-03-28", 14)))
        }
        val summary = WearMath.summarize(state, LocalDate.parse("2026-03-28"), Instant.ofEpochMilli(at("2026-04-01", 0)))
        assertEquals(2 * 3_600_000L, summary.wornMillis)
        assertEquals(0, WearMath.summarize(state, LocalDate.parse("2026-03-29"), Instant.ofEpochMilli(at("2026-04-01", 0))).trackedMillis)
    }

    @Test fun `future day and clock before initial event have zero coverage`() {
        val state = snapshot("2026-03-28", 12)
        assertEquals(0, WearMath.summarize(state, LocalDate.parse("2026-03-29"), Instant.ofEpochMilli(at("2026-03-28", 15))).trackedMillis)
        assertEquals(0, WearMath.summarize(state, LocalDate.parse("2026-03-28"), Instant.ofEpochMilli(at("2026-03-28", 11))).trackedMillis)
    }

    @Test fun `schedule uses calendar dates and current goal is consistent historically`() {
        val state = snapshot("2026-03-28").let { it.copy(plan = it.plan!!.copy(dailyGoalMinutes = 1320)) }
        assertEquals(LocalDate.parse("2026-04-04"), WearMath.nextChangeDate(state.plan!!))
        assertEquals(1320, WearMath.summarize(state, LocalDate.parse("2026-03-28"), Instant.ofEpochMilli(at("2026-03-29", 0))).goalMinutes)
    }
}
