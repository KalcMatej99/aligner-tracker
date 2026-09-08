package org.alignertracker.app.reminders

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.alignertracker.app.domain.TrackerSnapshot

data class ReminderCandidate(val kind: String, val key: String, val dueAt: Long)

object ReminderRules {
    fun pending(
        snapshot: TrackerSnapshot,
        preferences: ReminderPreferences,
        delivered: Set<String> = emptySet(),
    ): List<ReminderCandidate> {
        val plan = snapshot.plan ?: return emptyList()

        val result = mutableListOf<ReminderCandidate>()
        val last = snapshot.events.lastOrNull()
        if (!plan.completed && preferences.enabled && last != null && !last.wearing) {
            result +=
                ReminderCandidate(
                    "break",
                    "break:${plan.trackingStartedAt}:${last.id}:${last.at}",
                    Instant.ofEpochMilli(last.at)
                        .plusSeconds(preferences.breakMinutes * 60L)
                        .toEpochMilli(),
                )
        }
        if (
            !plan.completed &&
                preferences.trayEnabled &&
                snapshot.phases.firstOrNull { it.active }?.kind !=
                    org.alignertracker.app.domain.TreatmentPhaseKind.RETENTION
        ) {
            val due = org.alignertracker.app.domain.WearMath.nextChangeDate(snapshot)
            result +=
                ReminderCandidate(
                    "tray",
                    "tray:${plan.trackingStartedAt}:${plan.currentTray}:${plan.currentTrayStartedOn}:${plan.daysPerTray}:${snapshot.scheduleRevisions.lastOrNull()?.id ?: 0}",
                    due.atTime(LocalTime.of(9, 0))
                        .atZone(ZoneId.of(plan.zoneId))
                        .toInstant()
                        .toEpochMilli(),
                )
        }
        snapshot.appointments
            .filter { !it.completed && it.reminderMinutesBefore != null }
            .forEach { appointment ->
                result +=
                    ReminderCandidate(
                        "appointment",
                        "appointment:${appointment.id}:${appointment.startsAt}:${appointment.reminderMinutesBefore}",
                        appointment.startsAt - appointment.reminderMinutesBefore!! * 60_000L,
                    )
            }
        return result.filter { it.key !in delivered }.sortedBy { it.dueAt }
    }
}
