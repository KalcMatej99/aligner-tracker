package org.alignertracker.app.ui

import java.time.*
import org.alignertracker.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class VisualTimeTest {
    private val zone = ZoneId.of("Europe/Rome")

    @Test
    fun pickerPreservesMillisecondsAndOverlapOccurrence() {
        val first = parsePickerTime(LocalDate.parse("2026-10-25"), "02:30:17.123+02:00", zone)!!
        val second = parsePickerTime(LocalDate.parse("2026-10-25"), "02:30:17.123+01:00", zone)!!
        assertEquals(3_600_000L, second - first)
        assertEquals(123L, first % 1000)
        assertNull(parsePickerTime(LocalDate.parse("2026-10-25"), "02:30", zone))
        assertNull(parsePickerTime(LocalDate.parse("2026-03-29"), "02:30", zone))
        assertEquals(
            Instant.parse("2026-07-01T07:30:17.123Z").toEpochMilli(),
            parsePickerTime(LocalDate.parse("2026-07-01"), "09:30:17.123+01:00", zone),
        )
    }

    @Test
    fun breakdownSeparatesMissingAndFutureAcrossShortAndLongDays() {
        for ((date, hours) in listOf("2026-03-29" to 23, "2026-10-25" to 25)) {
            val start = LocalDate.parse(date).atStartOfDay(zone).toInstant()
            val snapshot =
                TrackerSnapshot(
                    plan =
                        TreatmentPlan(
                            zoneId = zone.id,
                            trackingStartedAt = start.plusSeconds(3600).toEpochMilli(),
                        ),
                    events = listOf(WearEvent(1, start.plusSeconds(3600).toEpochMilli(), true)),
                    trackingGaps =
                        listOf(
                            TrackingGap(
                                1,
                                start.plusSeconds(7200).toEpochMilli(),
                                start.plusSeconds(10800).toEpochMilli(),
                            )
                        ),
                )
            val now = start.plusSeconds(14400)
            val summary = WearMath.summarize(snapshot, LocalDate.parse(date), now)
            assertEquals(
                listOf(7_200_000L, 0L, 7_200_000L, (hours - 4) * 3_600_000L),
                dayParts(summary, now, zone),
            )
            assertNull(summary.goalMinutes)
        }
    }
}
