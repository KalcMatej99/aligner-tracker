package org.alignertracker.app.data

import java.time.Instant
import org.alignertracker.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class ReportMathTest {
    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun snapshot(start: String = "2026-03-28T23:00:00Z") =
        TrackerSnapshot(
            TreatmentPlan(
                startDate = "2026-03-29",
                totalTrays = 10,
                currentTray = 1,
                daysPerTray = 7,
                currentTrayStartedOn = "2026-03-29",
                dailyGoalMinutes = 1200,
                zoneId = "Europe/Rome",
                trackingStartedAt = at(start),
            ),
            listOf(WearEvent(1, at(start), true)),
        )

    @Test
    fun completeDstDayCanEarnStreakButTodayCannot() {
        val now = Instant.parse("2026-03-30T10:00:00Z")
        assertEquals(1, ReportMath.streak(snapshot(), now))
        assertEquals(0, ReportMath.streak(snapshot(), Instant.parse("2026-03-29T21:59:59Z")))
    }

    @Test
    fun partialFirstDayDoesNotEarnAStreakEvenIfGoalMet() {
        assertEquals(
            0,
            ReportMath.streak(
                snapshot("2026-03-29T00:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z"),
            ),
        )
    }

    @Test
    fun trayIntervalsSplitWithoutDoubleCountingAndUnknownTimeIsExcluded() {
        val current =
            snapshot()
                .copy(
                    events =
                        listOf(
                            WearEvent(1, 1000, true),
                            WearEvent(2, 4000, false),
                            WearEvent(3, 7000, true),
                        )
                )
        val first = ReportMath.interval(current, 0, 5000)
        val second = ReportMath.interval(current, 5000, 10000)
        assertEquals(4000L, first.trackedMillis)
        assertEquals(5000L, second.trackedMillis)
        assertEquals(6000L, first.wornMillis + second.wornMillis)
        assertEquals(3000L, first.removedMillis + second.removedMillis)
    }
}
