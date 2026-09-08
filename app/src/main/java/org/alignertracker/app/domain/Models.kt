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
    val phases: List<TreatmentPhase> = emptyList(),
    val scheduleRevisions: List<ScheduleRevision> = emptyList(),
    val trayIntervals: List<TrayInterval> = emptyList(),
    val trayHistory: List<TrayHistoryEntry> = emptyList(),
    val targetHistory: List<TargetHistoryEntry> = emptyList(),
    val notes: List<TreatmentNote> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val photos: List<PhotoMetadata> = emptyList(),
    val trackingGaps: List<TrackingGap> = emptyList(),
    val stateVersion: StateVersion = StateVersion(),
)

@Serializable
enum class TrackingGapReason {
    CLOCK_DISCONTINUITY,
    MANUAL_CORRECTION,
    COMPLETED_PHASE_PAUSE,
}

@Serializable
data class TrackingGap(
    val id: Long = 0,
    val startAt: Long,
    val endAt: Long,
    val reason: TrackingGapReason = TrackingGapReason.CLOCK_DISCONTINUITY,
)

@Serializable
enum class TreatmentPhaseKind {
    ALIGNER,
    REFINEMENT,
    RETENTION,
}

@Serializable
data class TreatmentPhase(
    val id: Long = 0,
    val kind: TreatmentPhaseKind,
    val ordinal: Int,
    val name: String,
    val totalTrays: Int,
    val startedOn: String,
    val completedOn: String? = null,
    val active: Boolean = true,
)

@Serializable
data class ScheduleRevision(
    val id: Long = 0,
    val phaseId: Long,
    val createdAt: Long,
    val reason: String? = null,
)

@Serializable
data class TrayInterval(
    val id: Long = 0,
    val scheduleRevisionId: Long,
    val firstTray: Int,
    val lastTray: Int,
    val daysPerTray: Int,
)

data class TrayIntervalDraft(val firstTray: Int, val lastTray: Int, val daysPerTray: Int)

@Serializable
data class TrayHistoryEntry(
    val id: Long = 0,
    val phaseId: Long,
    val trayNumber: Int,
    val startedOn: String,
    val endedOn: String? = null,
    val scheduleRevisionId: Long,
    val prescribedDays: Int,
    /** Exact user action time when known; null for imported history that only has a date. */
    val startedAt: Long? = null,
    /** Exact user action time when known; null for open or date-only imported history. */
    val endedAt: Long? = null,
)

@Serializable
data class TargetHistoryEntry(val id: Long = 0, val effectiveFrom: String, val goalMinutes: Int)

@Serializable
data class TreatmentNote(
    val id: Long = 0,
    val occurredAt: Long,
    val text: String,
    val phaseId: Long? = null,
    val trayHistoryId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class Appointment(
    val id: Long = 0,
    val startsAt: Long,
    val durationMinutes: Int,
    val title: String,
    val note: String = "",
    val reminderMinutesBefore: Int? = null,
    val completed: Boolean = false,
)

@Serializable
data class PhotoMetadata(
    val id: Long = 0,
    val capturedAt: Long,
    val mimeType: String,
    val byteSize: Long,
    val sha256: String,
    val caption: String = "",
    val phaseId: Long? = null,
    val trayHistoryId: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val ownedFileName: String? = null,
)

@Serializable data class StateVersion(val generation: String = "", val revision: Long = 0)

@Serializable
enum class CommandSource {
    PHONE,
    WATCH,
}

@Serializable
enum class CommandStatus {
    ACCEPTED,
    REJECTED,
}

@Serializable
enum class CommandRejection {
    STALE_GENERATION,
    STALE_REVISION,
    INVALID,
    COMPLETED,
}

@Serializable
data class WearCommand(
    val idempotencyId: String,
    val expectedGeneration: String,
    val expectedRevision: Long,
    val wearing: Boolean,
    val requestedAt: Long,
    val source: CommandSource = CommandSource.WATCH,
)

@Serializable
data class CommandOutcome(
    val idempotencyId: String,
    val status: CommandStatus,
    val rejection: CommandRejection? = null,
    val receivedAt: Long,
    val stateBefore: StateVersion,
    val stateAfter: StateVersion,
)

data class DaySummary(
    val date: String,
    val wornMillis: Long,
    val removedMillis: Long,
    val trackedMillis: Long,
    val goalMinutes: Int,
)
