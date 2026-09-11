package org.alignertracker.app.ui

import java.time.*
import org.alignertracker.app.data.ReportMath
import org.alignertracker.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProgressSummaryTest {
    @Test
    fun completeCalendarDaysRespectDstGapsAndToday() {
        val zone = ZoneId.of("Europe/Rome")
        for (date in listOf(LocalDate.parse("2026-03-29"), LocalDate.parse("2026-10-25"))) {
            val start = date.atStartOfDay(zone).toInstant()
            val now = date.plusDays(1).atTime(12, 0).atZone(zone).toInstant()
            val snapshot =
                TrackerSnapshot(
                    TreatmentPlan(zoneId = zone.id, trackingStartedAt = start.toEpochMilli()),
                    listOf(WearEvent(1, start.toEpochMilli(), true)),
                )
            val days = ReportMath.days(snapshot, 2, now)
            assertEquals(listOf(days.first()), completeProgressDays(days, date.plusDays(1), zone))
            val gap =
                snapshot.copy(
                    trackingGaps =
                        listOf(
                            TrackingGap(
                                1,
                                start.toEpochMilli(),
                                start.plusSeconds(60).toEpochMilli(),
                            )
                        )
                )
            assertTrue(
                completeProgressDays(ReportMath.days(gap, 2, now), date.plusDays(1), zone).isEmpty()
            )
            val completed =
                snapshot.copy(
                    plan =
                        snapshot.plan!!.copy(
                            completed = true,
                            completedAt = start.plusSeconds(3600).toEpochMilli(),
                        )
                )
            assertTrue(
                completeProgressDays(ReportMath.days(completed, 2, now), date.plusDays(1), zone)
                    .isEmpty()
            )
        }
    }
}
