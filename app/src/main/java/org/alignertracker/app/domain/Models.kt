package org.alignertracker.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class TreatmentPlan(
    val id: Long = 1,
    val startDate: String,
    val totalTrays: Int,
    val currentTray: Int,
    val daysPerTray: Int,
    val currentTrayStartedOn: String,
    val dailyGoalMinutes: Int,
    val zoneId: String,
    val trackingStartedAt: Long,
    val completed: Boolean = false,
    val completedAt: Long? = null,
)

@Serializable data class WearEvent(val id: Long = 0, val at: Long, val wearing: Boolean)

@Serializable
data class TrackerSnapshot(
    val plan: TreatmentPlan? = null,
    val events: List<WearEvent> = emptyList(),
)

data class DaySummary(
    val date: String,
    val wornMillis: Long,
    val removedMillis: Long,
    val trackedMillis: Long,
    val goalMinutes: Int,
)
