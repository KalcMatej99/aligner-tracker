package org.alignertracker.app.reminders

import java.time.Instant
import org.alignertracker.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class ExpandedReminderRulesTest {
    private val plan =
        TreatmentPlan(
            startDate = "2026-03-20",
            totalTrays = 2,
            currentTray = 1,
            daysPerTray = 7,
            currentTrayStartedOn = "2026-03-20",
            dailyGoalMinutes = 1200,
            zoneId = "Europe/Rome",
            trackingStartedAt = Instant.parse("2026-03-20T10:00:00Z").toEpochMilli(),
        )

    @Test
    fun latestScheduleRevisionChangesReminderAndDstBoundary() {
        val base =
            TrackerSnapshot(
                plan,
                listOf(WearEvent(1, plan.trackingStartedAt, true)),
                phases =
                    listOf(
                        TreatmentPhase(1, TreatmentPhaseKind.ALIGNER, 1, "Plan", 2, "2026-03-20")
                    ),
                scheduleRevisions =
                    listOf(
                        ScheduleRevision(1, 1, plan.trackingStartedAt),
                        ScheduleRevision(2, 1, plan.trackingStartedAt + 1),
                    ),
                trayIntervals = listOf(TrayInterval(1, 1, 1, 2, 7), TrayInterval(2, 2, 1, 2, 10)),
            )
        val due = ReminderRules.pending(base, ReminderPreferences(trayEnabled = true)).single()
        assertEquals(Instant.parse("2026-03-30T07:00:00Z").toEpochMilli(), due.dueAt)
    }

    @Test
    fun appointmentsRemainAvailableAfterTreatmentCompletionAndRespectCompletion() {
        val state =
            TrackerSnapshot(
                plan.copy(completed = true, completedAt = plan.trackingStartedAt),
                listOf(WearEvent(1, plan.trackingStartedAt, true)),
                appointments =
                    listOf(
                        Appointment(
                            1,
                            plan.trackingStartedAt + 3600000,
                            30,
                            "Visit",
                            reminderMinutesBefore = 30,
                        ),
                        Appointment(
                            2,
                            plan.trackingStartedAt + 7200000,
                            30,
                            "Done",
                            reminderMinutesBefore = 30,
                            completed = true,
                        ),
                    ),
            )
        val candidates =
            ReminderRules.pending(state, ReminderPreferences(enabled = true, trayEnabled = true))
        assertEquals(1, candidates.size)
        assertEquals("appointment", candidates.single().kind)
        assertTrue(
            ReminderRules.pending(state, ReminderPreferences(), setOf(candidates.single().key))
                .isEmpty()
        )
    }
}
