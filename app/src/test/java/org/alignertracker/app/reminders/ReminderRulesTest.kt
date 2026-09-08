package org.alignertracker.app.reminders

import java.time.Instant
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.junit.Assert.*
import org.junit.Test

class ReminderRulesTest {
    private val start = Instant.parse("2026-03-28T11:00:00Z").toEpochMilli()
    private val plan = TreatmentPlan(startDate = "2026-03-28", totalTrays = 20, currentTray = 1, daysPerTray = 1, currentTrayStartedOn = "2026-03-28", dailyGoalMinutes = 1320, zoneId = "Europe/Rome", trackingStartedAt = start)
    private val snapshot = TrackerSnapshot(plan, listOf(WearEvent(1, start, true), WearEvent(2, start + 60000, false)))
    @Test fun `empty completed disabled and in state produce no break`() {
        assertTrue(ReminderRules.pending(TrackerSnapshot(), ReminderPreferences(true)).isEmpty())
        assertTrue(ReminderRules.pending(snapshot.copy(plan = plan.copy(completed = true, completedAt = start + 120000)), ReminderPreferences(true)).isEmpty())
        assertTrue(ReminderRules.pending(snapshot, ReminderPreferences()).isEmpty())
        assertTrue(ReminderRules.pending(snapshot.copy(events = listOf(WearEvent(1, start, true))), ReminderPreferences(true)).isEmpty())
    }
    @Test fun `break follows out timestamp and delivered identity suppresses repeats`() {
        val candidate = ReminderRules.pending(snapshot, ReminderPreferences(true, 30)).single()
        assertEquals(start + 31 * 60000, candidate.dueAt)
        assertTrue(ReminderRules.pending(snapshot, ReminderPreferences(true), setOf(candidate.key)).isEmpty())
        val edited = snapshot.copy(events = snapshot.events.dropLast(1) + WearEvent(2, start + 120000, false))
        assertNotEquals(candidate.key, ReminderRules.pending(edited, ReminderPreferences(true)).single().key)
    }
    @Test fun `tray due at nine in treatment zone on daylight saving day`() {
        val candidate = ReminderRules.pending(snapshot, ReminderPreferences(trayEnabled = true)).single()
        assertEquals(Instant.parse("2026-03-29T07:00:00Z").toEpochMilli(), candidate.dueAt)
        val advanced = snapshot.copy(plan = plan.copy(currentTray = 2, currentTrayStartedOn = "2026-03-29"))
        assertNotEquals(candidate.key, ReminderRules.pending(advanced, ReminderPreferences(trayEnabled = true)).single().key)
    }
    @Test fun `both types have independent delivery markers`() {
        val prefs = ReminderPreferences(enabled = true, trayEnabled = true)
        val candidates = ReminderRules.pending(snapshot, prefs)
        assertEquals(2, candidates.size)
        assertEquals(listOf("tray"), ReminderRules.pending(snapshot, prefs, setOf(candidates.first().key)).map { it.kind })
    }
}
