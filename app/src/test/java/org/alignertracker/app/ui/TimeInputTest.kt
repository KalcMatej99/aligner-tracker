package org.alignertracker.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TimeInputTest {
    private val zone = ZoneId.of("Europe/Rome")

    @Test
    fun daylightSavingGapAndAmbiguousTimeAreNotSilentlyAdjusted() {
        assertNull(parseTreatmentTime(LocalDate.parse("2026-03-29"), "02:30", zone))
        assertNull(parseTreatmentTime(LocalDate.parse("2026-10-25"), "02:30", zone))
        assertEquals(
            Instant.parse("2026-10-25T00:30:00Z").toEpochMilli(),
            parseTreatmentTime(LocalDate.parse("2026-10-25"), "02:30+02:00", zone),
        )
        assertEquals(
            Instant.parse("2026-10-25T01:30:00Z").toEpochMilli(),
            parseTreatmentTime(LocalDate.parse("2026-10-25"), "02:30+01:00", zone),
        )
        assertNull(parseTreatmentTime(LocalDate.parse("2026-10-25"), "02:30+03:00", zone))
    }
}
