package org.alignertracker.app.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.alignertracker.app.domain.TrackerSnapshot

data class ReminderCandidate(val kind: String, val key: String, val dueAt: Long)

object ReminderRules {
    fun pending(snapshot: TrackerSnapshot, preferences: ReminderPreferences, delivered: Set<String> = emptySet()): List<ReminderCandidate> {
        val plan = snapshot.plan ?: return emptyList()
        if (plan.completed) return emptyList()
        val result = mutableListOf<ReminderCandidate>()
        val last = snapshot.events.lastOrNull()
        if (preferences.enabled && last != null && !last.wearing) {
            result += ReminderCandidate("break", "break:${plan.trackingStartedAt}:${last.id}:${last.at}", Instant.ofEpochMilli(last.at).plusSeconds(preferences.breakMinutes * 60L).toEpochMilli())
        }
        if (preferences.trayEnabled) {
            val due = LocalDate.parse(plan.currentTrayStartedOn).plusDays(plan.daysPerTray.toLong())
            result += ReminderCandidate("tray", "tray:${plan.trackingStartedAt}:${plan.currentTray}:${plan.currentTrayStartedOn}:${plan.daysPerTray}", due.atTime(LocalTime.of(9, 0)).atZone(ZoneId.of(plan.zoneId)).toInstant().toEpochMilli())
        }
        return result.filter { it.key !in delivered }
    }
}
