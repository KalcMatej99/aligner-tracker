package org.alignertracker.app.data

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.alignertracker.app.domain.WearMath
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks the exact disposable files delivered for owner acceptance against the real importer. */
class PixelAcceptanceFixtureTest {
    private val now = Instant.parse("2026-09-09T10:00:00Z")

    @Test
    fun `portable fixture preserves history and independently calculated daily totals`() {
        val snapshot = BackupCodec.decode(resource("history-schema2.json"))
        assertEquals(snapshot, BackupCodec.decode(BackupCodec.encode(snapshot)))
        val expected = Json.parseToJsonElement(resource("expected-days.json")).jsonArray
        expected.forEach {
            val row = it.jsonObject
            val day =
                WearMath.summarize(
                    snapshot,
                    LocalDate.parse(row.getValue("date").jsonPrimitive.content),
                    now,
                )
            assertEquals(row.getValue("wornMillis").jsonPrimitive.long, day.wornMillis)
            assertEquals(row.getValue("removedMillis").jsonPrimitive.long, day.removedMillis)
            assertEquals(row.getValue("trackedMillis").jsonPrimitive.long, day.trackedMillis)
            assertEquals(row.getValue("goalMinutes").jsonPrimitive.long, day.goalMinutes!!.toLong())
        }
        assertEquals(2, ReportMath.streak(snapshot, now))
        assertEquals(listOf(7, 10), snapshot.trayHistory.map { it.prescribedDays })
        assertEquals(1, snapshot.notes.size)
        assertEquals(1, snapshot.appointments.size)
    }

    @Test
    fun `legacy fixture imports known records without invented historical trays`() {
        val snapshot = BackupCodec.decode(resource("legacy-schema1.json"))
        assertEquals(3, snapshot.events.size)
        assertEquals(1, snapshot.trayHistory.size)
        assertEquals(1320, snapshot.targetHistory.single().goalMinutes)
    }

    private fun resource(name: String): String =
        checkNotNull(javaClass.getResource("/pixel/$name")).readText()
}
