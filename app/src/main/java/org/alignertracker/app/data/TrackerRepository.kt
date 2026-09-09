package org.alignertracker.app.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.alignertracker.app.domain.Appointment
import org.alignertracker.app.domain.CommandOutcome
import org.alignertracker.app.domain.CommandRejection
import org.alignertracker.app.domain.CommandStatus
import org.alignertracker.app.domain.PhotoMetadata
import org.alignertracker.app.domain.ScheduleRevision
import org.alignertracker.app.domain.StateVersion
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TrackerValidation
import org.alignertracker.app.domain.TrackingGapReason
import org.alignertracker.app.domain.TrayIntervalDraft
import org.alignertracker.app.domain.TreatmentNote
import org.alignertracker.app.domain.TreatmentPhase
import org.alignertracker.app.domain.TreatmentPhaseKind
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearCommand

class TrackerRepository(
    private val database: TrackerDatabase,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.trackerDao()

    val snapshots: Flow<TrackerSnapshot> =
        database.invalidationTracker
            .createFlow(
                "treatment",
                "wear_events",
                "tracking_gaps",
                "treatment_phases",
                "schedule_revisions",
                "tray_intervals",
                "tray_history",
                "target_history",
                "treatment_notes",
                "appointments",
                "photo_metadata",
                "tracker_state",
                emitInitialState = true,
            )
            .map { snapshot() }
            .distinctUntilChanged()

    suspend fun snapshot(): TrackerSnapshot = database.withTransaction { snapshotInside() }

    suspend fun start(plan: TreatmentPlan, wearing: Boolean) {
        database.withTransaction {
            check(dao.plan() == null) {
                "A treatment already exists. Delete it or explicitly replace a backup first."
            }
            TrackerValidation.plan(plan, clock.instant())
            require(!plan.completed) { "A new treatment cannot already be completed." }
            dao.insertPlan(PlanEntity.from(plan))
            dao.insertEvent(EventEntity(at = plan.trackingStartedAt, wearing = wearing))
            if (plan.hasSchedule) attachSchedule(plan, plan.trackingStartedAt)
            plan.dailyGoalMinutes?.let { minutes ->
                val date =
                    Instant.ofEpochMilli(plan.trackingStartedAt)
                        .atZone(ZoneId.of(plan.zoneId))
                        .toLocalDate()
                        .toString()
                dao.insertTarget(TargetHistoryEntity(0, date, minutes))
            }
            dao.putState(StateEntity(generation = UUID.randomUUID().toString(), revision = 1))
        }
    }

    /** The successful transaction, not screen creation, defines the first recorded instant. */
    suspend fun startTracking(wearing: Boolean, zoneId: String = ZoneId.systemDefault().id) {
        database.withTransaction {
            start(TreatmentPlan(zoneId = zoneId, trackingStartedAt = clock.millis()), wearing)
        }
    }

    suspend fun updateTreatmentDetails(
        startDate: String?,
        totalTrays: Int?,
        currentTray: Int?,
        daysPerTray: Int?,
        currentTrayStartedOn: String?,
    ) {
        database.withTransaction {
            val old = activePlan()
            val updated =
                old.copy(
                    startDate = startDate,
                    totalTrays = totalTrays,
                    currentTray = currentTray,
                    daysPerTray = daysPerTray,
                    currentTrayStartedOn = currentTrayStartedOn,
                )
            TrackerValidation.plan(updated, clock.instant())
            if (dao.phases().isNotEmpty()) {
                require(updated.hasSchedule) {
                    "Recorded schedules need current tray, total, interval and tray start. Use corrections instead of removing recorded details."
                }
                require(
                    totalTrays!! >=
                        (dao.trayHistory()
                            .filter {
                                it.phaseId == dao.phases().single { phase -> phase.active }.id
                            }
                            .maxOfOrNull { it.trayNumber } ?: 1)
                ) {
                    "Total trays cannot be below an already recorded tray number."
                }
                val changed =
                    old.totalTrays != totalTrays ||
                        old.currentTray != currentTray ||
                        old.daysPerTray != daysPerTray ||
                        old.currentTrayStartedOn != currentTrayStartedOn
                if (changed) {
                    val now = clock.millis()
                    check(now >= dao.events().last().at) {
                        "The clock precedes existing treatment history. Correct it before changing details."
                    }
                    val phase = dao.phases().single { it.active }
                    val previous = dao.trayHistory().single { it.endedOn == null }
                    check(now >= (previous.startedAt ?: old.trackingStartedAt)) {
                        "The clock precedes existing treatment history. Correct it before changing details."
                    }
                    dao.updatePhase(phase.copy(totalTrays = totalTrays))
                    val revision =
                        dao.insertScheduleRevision(
                            ScheduleRevisionEntity(
                                0,
                                phase.id,
                                now,
                                "Explicit correction of current tray details",
                            )
                        )
                    // Preserve any variable intervals unless the prescribed interval itself
                    // changed.
                    val priorRevision =
                        dao.scheduleRevisions()
                            .filter { it.phaseId == phase.id && it.id != revision }
                            .last()
                    val priorIntervals =
                        dao.trayIntervals().filter { it.scheduleRevisionId == priorRevision.id }
                    val prescribed =
                        (1..totalTrays).map { tray ->
                            if (tray == currentTray) daysPerTray!!
                            else
                                priorIntervals
                                    .singleOrNull { tray in it.firstTray..it.lastTray }
                                    ?.daysPerTray ?: daysPerTray!!
                        }
                    var first = 1
                    for (tray in 1..totalTrays) {
                        if (tray == totalTrays || prescribed[tray] != prescribed[first - 1]) {
                            dao.insertTrayInterval(
                                TrayIntervalEntity(0, revision, first, tray, prescribed[first - 1])
                            )
                            first = tray + 1
                        }
                    }
                    if (
                        old.currentTray != currentTray ||
                            old.currentTrayStartedOn != currentTrayStartedOn
                    ) {
                        val today =
                            Instant.ofEpochMilli(now)
                                .atZone(ZoneId.of(old.zoneId))
                                .toLocalDate()
                                .toString()
                        dao.updateTrayHistory(previous.copy(endedOn = today, endedAt = now))
                        dao.insertTrayHistory(
                            TrayHistoryEntity(
                                0,
                                phase.id,
                                currentTray!!,
                                currentTrayStartedOn!!,
                                null,
                                revision,
                                daysPerTray!!,
                                now,
                                null,
                            )
                        )
                    }
                }
            } else if (updated.hasSchedule) attachSchedule(updated, clock.millis())
            dao.updatePlan(PlanEntity.from(updated))
            bumpState()
        }
    }

    private suspend fun attachSchedule(plan: TreatmentPlan, recordedAt: Long) {
        val phaseId =
            dao.insertPhase(
                PhaseEntity.from(
                    TreatmentPhase(
                        0,
                        TreatmentPhaseKind.ALIGNER,
                        1,
                        "Initial treatment",
                        plan.totalTrays!!,
                        plan.startDate,
                    )
                )
            )
        val revisionId =
            dao.insertScheduleRevision(
                ScheduleRevisionEntity.from(
                    ScheduleRevision(0, phaseId, recordedAt, "Initial prescribed schedule")
                )
            )
        dao.insertTrayInterval(
            TrayIntervalEntity(0, revisionId, 1, plan.totalTrays!!, plan.daysPerTray!!)
        )
        dao.insertTrayHistory(
            TrayHistoryEntity(
                0,
                phaseId,
                plan.currentTray!!,
                plan.currentTrayStartedOn!!,
                null,
                revisionId,
                plan.daysPerTray!!,
                recordedAt,
                null,
            )
        )
    }

    suspend fun setWearing(wearing: Boolean) {
        database.withTransaction {
            val plan = requirePlan()
            if (plan.completed) return@withTransaction
            val events = dao.events()
            val last = events.last()
            if (last.wearing == wearing) return@withTransaction
            val now = clock.millis()
            check(now > last.at) {
                "The clock is at or before the previous switch. Correct the clock or previous switch time, then try again."
            }
            check(events.size < TrackerValidation.MAX_EVENTS) {
                "Event limit reached. Export your history before starting another treatment."
            }
            dao.insertEvent(EventEntity(at = now, wearing = wearing))
            bumpState()
        }
    }

    suspend fun updateEvent(id: Long, at: Long) {
        database.withTransaction {
            val plan = requirePlan()
            val events = dao.events()
            val index = events.indexOfFirst { it.id == id }
            require(index >= 0) { "This switch no longer exists." }
            require(index > 0) { "The initial tracking event cannot be moved." }
            require(at <= minOf(clock.millis(), plan.completedAt ?: Long.MAX_VALUE)) {
                "A switch cannot be in the future or after completion."
            }
            require(
                at > events[index - 1].at &&
                    (index == events.lastIndex || at < events[index + 1].at)
            ) {
                "Choose a time strictly between the adjacent switches."
            }
            require(at >= plan.trackingStartedAt) { "A switch cannot precede tracking." }
            if (events[index].at != at) {
                dao.updateEvent(events[index].copy(at = at))
                bumpState()
            }
        }
    }

    suspend fun insertMissingInterval(startAt: Long, endAt: Long, wearing: Boolean) {
        database.withTransaction {
            val plan = requirePlan()
            require(startAt < endAt) { "Missing interval end must follow its start." }
            val events = dao.events()
            check(events.size <= TrackerValidation.MAX_EVENTS - 2) { "Event limit reached." }
            val ownerIndex = events.indexOfLast { it.at < startAt }
            require(ownerIndex >= 0) { "Missing interval must start inside tracked history." }
            val owner = events[ownerIndex]
            val boundary =
                minOf(
                    events.getOrNull(ownerIndex + 1)?.at ?: Long.MAX_VALUE,
                    plan.completedAt ?: clock.millis(),
                    clock.millis(),
                )
            require(endAt < boundary) {
                "Missing interval must stay strictly inside one existing interval."
            }
            require(owner.wearing != wearing) {
                "Missing interval must differ from the surrounding state."
            }
            require(dao.trackingGaps().none { startAt < it.endAt && endAt > it.startAt }) {
                "Missing interval cannot overlap untracked coverage."
            }
            dao.insertEvent(EventEntity(at = startAt, wearing = wearing))
            dao.insertEvent(EventEntity(at = endAt, wearing = owner.wearing))
            bumpState()
        }
    }

    suspend fun recordTrackingGap(
        startAt: Long,
        endAt: Long,
        reason: TrackingGapReason = TrackingGapReason.CLOCK_DISCONTINUITY,
    ): Long =
        database.withTransaction {
            val plan = requirePlan()
            val upper = minOf(clock.millis(), plan.completedAt ?: Long.MAX_VALUE)
            require(startAt >= plan.trackingStartedAt && startAt < endAt && endAt <= upper) {
                "Tracking gap must be a positive interval within recorded treatment time."
            }
            require(dao.trackingGaps().none { startAt < it.endAt && endAt > it.startAt }) {
                "Tracking gaps cannot overlap."
            }
            val id = dao.insertTrackingGap(TrackingGapEntity(0, startAt, endAt, reason.name))
            bumpState()
            id
        }

    suspend fun advanceTray() {
        database.withTransaction {
            val plan = activePlan()
            check(plan.hasSchedule) { "Add your tray schedule first." }
            val phase = dao.phases().single { it.active }
            check(plan.currentTray!! < phase.totalTrays) {
                "This is the last tray. Complete or begin a new phase explicitly when appropriate."
            }
            val today = LocalDate.now(clock.withZone(ZoneId.of(plan.zoneId)))
            check(today >= LocalDate.parse(plan.currentTrayStartedOn)) {
                "The clock precedes the current tray start. Correct it before advancing."
            }
            check(clock.millis() >= dao.events().last().at) {
                "The clock precedes the last switch. Correct it before advancing."
            }
            dao.trayHistory()
                .single { it.endedOn == null }
                .let {
                    dao.updateTrayHistory(
                        it.copy(endedOn = today.toString(), endedAt = clock.millis())
                    )
                }
            val revision = dao.scheduleRevisions().last { it.phaseId == phase.id }
            val nextTray = plan.currentTray!! + 1
            val days =
                dao.trayIntervals()
                    .single {
                        it.scheduleRevisionId == revision.id &&
                            nextTray in it.firstTray..it.lastTray
                    }
                    .daysPerTray
            dao.insertTrayHistory(
                TrayHistoryEntity(
                    0,
                    phase.id,
                    nextTray,
                    today.toString(),
                    null,
                    revision.id,
                    days,
                    clock.millis(),
                    null,
                )
            )
            dao.updatePlan(
                PlanEntity.from(
                    plan.copy(
                        currentTray = nextTray,
                        daysPerTray = days,
                        currentTrayStartedOn = today.toString(),
                    )
                )
            )
            bumpState()
        }
    }

    suspend fun replaceSchedule(
        phaseId: Long,
        intervals: List<TrayIntervalDraft>,
        reason: String? = null,
    ): Long =
        database.withTransaction {
            activePlan()
            val phase =
                dao.phases().singleOrNull { it.id == phaseId }
                    ?: throw IllegalArgumentException("Phase does not exist.")
            check(phase.active) { "Only the active phase schedule can be changed." }
            require(reason == null || reason.length <= 1000) { "Schedule reason is too long." }
            TrackerValidation.intervals(intervals, phase.totalTrays)
            val revisionId =
                dao.insertScheduleRevision(
                    ScheduleRevisionEntity(0, phase.id, clock.millis(), reason)
                )
            intervals
                .sortedBy { it.firstTray }
                .forEach {
                    dao.insertTrayInterval(
                        TrayIntervalEntity(0, revisionId, it.firstTray, it.lastTray, it.daysPerTray)
                    )
                }
            val plan = requirePlan()
            val currentDays =
                intervals.single { plan.currentTray!! in it.firstTray..it.lastTray }.daysPerTray
            dao.updatePlan(PlanEntity.from(plan.copy(daysPerTray = currentDays)))
            bumpState()
            revisionId
        }

    suspend fun beginPhase(
        kind: TreatmentPhaseKind,
        name: String,
        totalTrays: Int,
        daysPerTray: Int,
        currentWearing: Boolean? = null,
    ) {
        database.withTransaction {
            val plan = requirePlan()
            require(!plan.completed || currentWearing != null) {
                "Choose the current IN or OUT state when resuming a completed treatment."
            }
            if (currentWearing != null && dao.events().last().wearing != currentWearing) {
                check(dao.events().size < TrackerValidation.MAX_EVENTS) {
                    "Event limit reached. Export your history before starting another phase."
                }
                check(clock.millis() > dao.events().last().at) {
                    "The current clock must follow the previous switch."
                }
                dao.insertEvent(EventEntity(at = clock.millis(), wearing = currentWearing))
            }
            require(name.isNotBlank() && name.length <= 200) { "Phase name is required." }
            require(totalTrays in 1..1000) { "Phase tray count is invalid." }
            require(daysPerTray in 1..365) { "Tray interval is invalid." }
            val now = clock.millis()
            check(now >= dao.events().last().at && now >= (plan.completedAt ?: 0)) {
                "The clock precedes existing treatment history. Correct it before starting a phase."
            }
            val today = LocalDate.now(clock.withZone(ZoneId.of(plan.zoneId))).toString()
            val ordinal = (dao.phases().maxOfOrNull { it.ordinal } ?: 0) + 1
            if (plan.completed) {
                val completedAt = checkNotNull(plan.completedAt)
                if (completedAt < now) {
                    dao.insertTrackingGap(
                        TrackingGapEntity(
                            0,
                            completedAt,
                            now,
                            TrackingGapReason.COMPLETED_PHASE_PAUSE.name,
                        )
                    )
                }
            } else if (dao.phases().isNotEmpty()) {
                val oldPhase = dao.phases().single { it.active }
                dao.updatePhase(oldPhase.copy(active = false, completedOn = today))
                dao.trayHistory()
                    .single { it.endedOn == null }
                    .let { dao.updateTrayHistory(it.copy(endedOn = today, endedAt = now)) }
            }
            val phaseId =
                dao.insertPhase(
                    PhaseEntity(0, kind.name, ordinal, name.trim(), totalTrays, today, null, true)
                )
            val revisionId =
                dao.insertScheduleRevision(
                    ScheduleRevisionEntity(0, phaseId, now, "Initial phase schedule")
                )
            dao.insertTrayInterval(TrayIntervalEntity(0, revisionId, 1, totalTrays, daysPerTray))
            dao.insertTrayHistory(
                TrayHistoryEntity(0, phaseId, 1, today, null, revisionId, daysPerTray, now, null)
            )
            dao.updatePlan(
                PlanEntity.from(
                    plan.copy(
                        totalTrays = totalTrays,
                        currentTray = 1,
                        daysPerTray = daysPerTray,
                        currentTrayStartedOn = today,
                        completed = false,
                        completedAt = null,
                    )
                )
            )
            bumpState()
        }
    }

    suspend fun completeTreatment() {
        database.withTransaction {
            val plan = requirePlan()
            if (plan.completed) return@withTransaction
            val now = clock.millis()
            check(now >= dao.events().last().at) {
                "The clock precedes the last switch. Correct it before completing treatment."
            }
            check(plan.hasSchedule) { "Add your tray schedule first." }
            val completed = plan.copy(completed = true, completedAt = now)
            TrackerValidation.plan(completed, Instant.ofEpochMilli(now))
            val today =
                Instant.ofEpochMilli(now).atZone(ZoneId.of(plan.zoneId)).toLocalDate().toString()
            dao.phases()
                .single { it.active }
                .let { dao.updatePhase(it.copy(active = false, completedOn = today)) }
            dao.trayHistory()
                .single { it.endedOn == null }
                .let { dao.updateTrayHistory(it.copy(endedOn = today, endedAt = now)) }
            dao.updatePlan(PlanEntity.from(completed))
            bumpState()
        }
    }

    suspend fun updateGoal(minutes: Int, effectiveFrom: String? = null) {
        require(minutes in 1..1440) { "Prescribed target must be 1 to 1440 minutes." }
        database.withTransaction {
            val plan = activePlan()
            val today = LocalDate.now(clock.withZone(ZoneId.of(plan.zoneId)))
            val first = dao.targetHistory().isEmpty()
            val effective =
                effectiveFrom?.let(TrackerValidation::date)
                    ?: if (first) today else today.plusDays(1)
            require(effective >= today) { "A target change cannot rewrite past target history." }
            val existing =
                dao.targetHistory().singleOrNull { it.effectiveFrom == effective.toString() }
            if (existing == null)
                dao.insertTarget(
                    TargetHistoryEntity(
                        0,
                        effective.toString(),
                        minutes,
                        if (first && effective == today) clock.millis() else null,
                    )
                )
            else dao.updateTarget(existing.copy(goalMinutes = minutes))
            dao.updatePlan(PlanEntity.from(plan.copy(dailyGoalMinutes = minutes)))
            bumpState()
        }
    }

    suspend fun addNote(note: TreatmentNote): Long =
        database.withTransaction {
            requirePlan()
            TrackerValidation.note(note, clock.millis())
            validateReferences(note.phaseId, note.trayHistoryId)
            val now = clock.millis()
            val id =
                dao.insertNote(NoteEntity.from(note.copy(id = 0, createdAt = now, updatedAt = now)))
            bumpState()
            id
        }

    suspend fun updateNote(note: TreatmentNote) {
        database.withTransaction {
            requirePlan()
            val old = dao.note(note.id) ?: throw IllegalArgumentException("Note does not exist.")
            TrackerValidation.note(note, clock.millis())
            validateReferences(note.phaseId, note.trayHistoryId)
            dao.updateNote(
                NoteEntity.from(note.copy(createdAt = old.createdAt, updatedAt = clock.millis()))
            )
            bumpState()
        }
    }

    suspend fun deleteNote(id: Long): Boolean =
        database.withTransaction {
            val changed = dao.deleteNote(id) > 0
            if (changed) bumpState()
            changed
        }

    suspend fun addAppointment(appointment: Appointment): Long =
        database.withTransaction {
            requirePlan()
            TrackerValidation.appointment(appointment)
            val id = dao.insertAppointment(AppointmentEntity.from(appointment.copy(id = 0)))
            bumpState()
            id
        }

    suspend fun updateAppointment(appointment: Appointment) {
        database.withTransaction {
            requirePlan()
            require(dao.appointment(appointment.id) != null) { "Appointment does not exist." }
            TrackerValidation.appointment(appointment)
            dao.updateAppointment(AppointmentEntity.from(appointment))
            bumpState()
        }
    }

    suspend fun deleteAppointment(id: Long): Boolean =
        database.withTransaction {
            val changed = dao.deleteAppointment(id) > 0
            if (changed) bumpState()
            changed
        }

    suspend fun addPhoto(metadata: PhotoMetadata): Long =
        database.withTransaction {
            requirePlan()
            TrackerValidation.photo(metadata, clock.millis(), allowOwnership = true)
            validateReferences(metadata.phaseId, metadata.trayHistoryId)
            val id = dao.insertPhoto(PhotoEntity.from(metadata.copy(id = 0)))
            bumpState()
            id
        }

    suspend fun updatePhoto(metadata: PhotoMetadata): String? =
        database.withTransaction {
            requirePlan()
            val old =
                dao.photo(metadata.id) ?: throw IllegalArgumentException("Photo does not exist.")
            TrackerValidation.photo(metadata, clock.millis(), allowOwnership = true)
            validateReferences(metadata.phaseId, metadata.trayHistoryId)
            dao.updatePhoto(PhotoEntity.from(metadata))
            bumpState()
            old.ownedFileName?.takeIf { it != metadata.ownedFileName }
        }

    suspend fun deletePhoto(id: Long): String? =
        database.withTransaction {
            val old = dao.photo(id) ?: return@withTransaction null
            dao.deletePhoto(id)
            bumpState()
            old.ownedFileName
        }

    suspend fun applyWearCommand(command: WearCommand): CommandOutcome =
        database.withTransaction {
            dao.commandOutcome(command.idempotencyId)?.model()?.let {
                return@withTransaction it
            }
            require(command.idempotencyId.matches(Regex("[A-Za-z0-9._:-]{8,200}"))) {
                "Invalid command idempotency ID."
            }
            val before = dao.state()?.model() ?: StateVersion()
            val now = clock.millis()
            val rejection =
                when {
                    command.expectedGeneration != before.generation ->
                        CommandRejection.STALE_GENERATION
                    command.expectedRevision != before.revision -> CommandRejection.STALE_REVISION
                    dao.plan() == null -> CommandRejection.INVALID
                    dao.plan()!!.completed -> CommandRejection.COMPLETED
                    command.requestedAt < 0 -> CommandRejection.INVALID
                    else -> null
                }
            var after = before
            var finalRejection = rejection
            if (finalRejection == null) {
                val events = dao.events()
                val last = events.last()
                if (last.wearing != command.wearing) {
                    if (now <= last.at || events.size >= TrackerValidation.MAX_EVENTS)
                        finalRejection = CommandRejection.INVALID
                    else {
                        dao.insertEvent(EventEntity(at = now, wearing = command.wearing))
                        after = bumpState()
                    }
                }
            }
            val outcome =
                CommandOutcome(
                    command.idempotencyId,
                    if (finalRejection == null) CommandStatus.ACCEPTED else CommandStatus.REJECTED,
                    finalRejection,
                    now,
                    before,
                    after,
                )
            dao.insertCommandOutcome(CommandOutcomeEntity.from(outcome))
            outcome
        }

    suspend fun clearCommandLedger(before: Long): Int =
        database.withTransaction { dao.clearCommandLedger(before) }

    suspend fun replaceFromBackup(snapshot: TrackerSnapshot) {
        replaceFromBackupWithCleanup(snapshot)
    }

    suspend fun replaceFromBackupWithCleanup(
        snapshot: TrackerSnapshot,
        trustedPhotoFiles: Map<Long, String> = emptyMap(),
    ): List<String> {
        require(snapshot.photos.all { it.ownedFileName == null }) {
            "Portable restore cannot claim local photo ownership."
        }
        require(trustedPhotoFiles.keys == snapshot.photos.mapTo(HashSet()) { it.id }) {
            "Trusted photo files must identify every restored photo exactly once."
        }
        trustedPhotoFiles.values.forEach {
            require(
                it.matches(
                    Regex(
                        "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.jpg"
                    )
                )
            ) {
                "Trusted photo filename is invalid."
            }
        }
        val replacement =
            TrackerValidation.expandLegacy(snapshot.copy(events = snapshot.events.toList()))
                .copy(
                    photos =
                        snapshot.photos.map { it.copy(ownedFileName = trustedPhotoFiles[it.id]) },
                    stateVersion = StateVersion(),
                )
        TrackerValidation.snapshot(replacement, clock.instant())
        return database.withTransaction {
            TrackerValidation.snapshot(replacement, clock.instant())
            val oldFiles = dao.photos().mapNotNull { it.ownedFileName }
            clearTables()
            replacement.plan?.let { dao.insertPlan(PlanEntity.from(it)) }
            dao.insertEvents(replacement.events.map { EventEntity(it.id, it.at, it.wearing) })
            dao.insertTrackingGaps(replacement.trackingGaps.map(TrackingGapEntity::from))
            dao.insertPhases(replacement.phases.map(PhaseEntity::from))
            dao.insertScheduleRevisions(
                replacement.scheduleRevisions.map(ScheduleRevisionEntity::from)
            )
            dao.insertTrayIntervals(replacement.trayIntervals.map(TrayIntervalEntity::from))
            dao.insertTrayHistories(replacement.trayHistory.map(TrayHistoryEntity::from))
            dao.insertTargets(replacement.targetHistory.map(TargetHistoryEntity::from))
            dao.insertNotes(replacement.notes.map(NoteEntity::from))
            dao.insertAppointments(replacement.appointments.map(AppointmentEntity::from))
            dao.insertPhotos(replacement.photos.map(PhotoEntity::from))
            if (replacement.plan != null)
                dao.putState(StateEntity(generation = UUID.randomUUID().toString(), revision = 1))
            oldFiles
        }
    }

    suspend fun clearAll() {
        clearAllWithCleanup()
    }

    suspend fun clearAllWithCleanup(): List<String> =
        database.withTransaction {
            val files = dao.photos().mapNotNull { it.ownedFileName }
            clearTables()
            files
        }

    private suspend fun snapshotInside(): TrackerSnapshot {
        val plan = dao.plan()?.model() ?: return TrackerSnapshot()
        return TrackerSnapshot(
            plan = plan,
            events = dao.events().map { it.model() },
            phases = dao.phases().map { it.model() },
            scheduleRevisions = dao.scheduleRevisions().map { it.model() },
            trayIntervals = dao.trayIntervals().map { it.model() },
            trayHistory = dao.trayHistory().map { it.model() },
            targetHistory = dao.targetHistory().map { it.model() },
            notes = dao.notes().map { it.model() },
            appointments = dao.appointments().map { it.model() },
            photos = dao.photos().map { it.model() },
            trackingGaps = dao.trackingGaps().map { it.model() },
            stateVersion = dao.state()?.model() ?: StateVersion(),
        )
    }

    private suspend fun validateReferences(phaseId: Long?, trayHistoryId: Long?) {
        require(phaseId == null || dao.phases().any { it.id == phaseId }) {
            "Referenced phase does not exist."
        }
        require(trayHistoryId == null || dao.trayHistory().any { it.id == trayHistoryId }) {
            "Referenced tray does not exist."
        }
    }

    private suspend fun bumpState(): StateVersion {
        val old = dao.state()?.model() ?: StateVersion(UUID.randomUUID().toString(), 0)
        val next = old.copy(revision = old.revision + 1)
        dao.putState(StateEntity(generation = next.generation, revision = next.revision))
        return next
    }

    private suspend fun clearTables() {
        dao.deleteCommandOutcomes()
        dao.deleteState()
        dao.deleteTrackingGaps()
        dao.deletePhotos()
        dao.deleteAppointments()
        dao.deleteNotes()
        dao.deleteTargets()
        dao.deleteTrayHistory()
        dao.deleteTrayIntervals()
        dao.deleteScheduleRevisions()
        dao.deletePhases()
        dao.deleteEvents()
        dao.deletePlan()
    }

    private suspend fun requirePlan(): TreatmentPlan =
        checkNotNull(dao.plan()?.model()) { "Set up a treatment first." }

    private suspend fun activePlan(): TreatmentPlan =
        requirePlan().also { check(!it.completed) { "Completed treatment is read-only." } }
}
