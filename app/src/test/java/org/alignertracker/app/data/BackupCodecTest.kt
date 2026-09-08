package org.alignertracker.app.data

import java.time.Instant
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TrackerValidation
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val start = Instant.parse("2025-10-26T00:00:00Z").toEpochMilli()
    private val now = Instant.parse("2025-10-26T02:00:00Z")
    private val state =
        TrackerSnapshot(
            TreatmentPlan(
                startDate = "2025-10-25",
                totalTrays = 20,
                currentTray = 2,
                daysPerTray = 10,
                currentTrayStartedOn = "2025-10-25",
                dailyGoalMinutes = 1200,
                zoneId = "Europe/Rome",
                trackingStartedAt = start,
            ),
            listOf(WearEvent(1, start, true), WearEvent(2, start + 3_600_000, false)),
        )

    @Test
    fun `round trip preserves timestamps IDs zone and completion`() {
        val completed =
            state.copy(plan = state.plan!!.copy(completed = true, completedAt = now.toEpochMilli()))
        assertEquals(completed, BackupCodec.decode(BackupCodec.encode(completed)))
        assertEquals(TrackerSnapshot(), BackupCodec.decode(BackupCodec.encode(TrackerSnapshot())))
    }

    @Test
    fun `unknown schema unknown fields duplicate fields and nested bombs reject`() {
        val encoded = BackupCodec.encode(state)
        rejects(encoded.replace("\"schemaVersion\":1", "\"schemaVersion\":2"))
        rejects(encoded.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"extra\":true"))
        rejects(encoded.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"))
        rejects("[[[[[[]]]]]]")
        rejects("not JSON")
    }

    @Test
    fun `future timestamps missing initial coverage and nonalternating events reject`() {
        val encoded = BackupCodec.encode(state)
        rejects(
            encoded.replace(
                (start + 3_600_000).toString(),
                (Instant.now().toEpochMilli() + 86_400_000).toString(),
            )
        )
        rejects(encoded.replace("\"wearing\":false", "\"wearing\":true"))
        rejects(encoded.replace("\"at\":$start", "\"at\":${start + 1}"))
        rejects(encoded.replace("\"id\":2", "\"id\":1"))
        rejects(encoded.replace("\"dailyGoalMinutes\":1200", "\"dailyGoalMinutes\":1441"))
        rejects(encoded.replace("\"completed\":false", "\"completed\":true"))
    }

    @Test
    fun `size limit counts UTF8 bytes and event limit is bounded`() {
        rejects(" ".repeat(TrackerValidation.MAX_BACKUP_BYTES + 1))
        rejects("é".repeat(TrackerValidation.MAX_BACKUP_BYTES / 2 + 1))
        assertThrows(IllegalArgumentException::class.java) {
            TrackerValidation.snapshot(
                state.copy(events = List(50_001) { WearEvent(it + 1L, start + it, it % 2 == 0) }),
                now,
            )
        }
    }

    @Test
    fun `strict numerical and boolean types reject coercion`() {
        val encoded = BackupCodec.encode(state)
        rejects(encoded.replace("\"totalTrays\":20", "\"totalTrays\":20.5"))
        rejects(encoded.replace("\"wearing\":false", "\"wearing\":\"false\""))
        rejects(encoded.replace("\"at\":$start", "\"at\":999999999999999999999999"))
    }

    @Test
    fun `CSV describes exact covered durations timezone DST and current goal`() {
        val csv = BackupCodec.csv(state, now)
        assertTrue(
            csv.startsWith(
                "date,treatment_timezone,worn_millis,removed_millis,tracked_millis,day_millis,current_goal_minutes,coverage\r\n"
            )
        )
        assertTrue(
            csv.contains(
                "2025-10-26,\"Europe/Rome\",3600000,3600000,7200000,90000000,1200,partial\r\n"
            )
        )
    }

    @Test
    fun `CSV stops at completion and initial state is never projected earlier`() {
        val completed =
            state.copy(plan = state.plan!!.copy(completed = true, completedAt = now.toEpochMilli()))
        assertEquals(
            BackupCodec.csv(completed, now),
            BackupCodec.csv(completed, now.plusSeconds(86400 * 10)),
        )
    }

    private fun rejects(source: String) {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(source) }
    }
}
