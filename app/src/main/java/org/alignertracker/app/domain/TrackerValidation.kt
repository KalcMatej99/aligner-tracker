package org.alignertracker.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Shared validation for setup, migrations, repository writes and portable backups. */
object TrackerValidation {
    const val MAX_EVENTS = 50_000
    const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    const val MAX_TEXT_LENGTH = 20_000
    const val MAX_AUXILIARY_RECORDS = 20_000

    fun plan(plan: TreatmentPlan, now: Instant) {
        require(plan.id == 1L) { "A backup must contain one treatment with ID 1." }
        require(plan.totalTrays in 1..1000) { "Total trays must be between 1 and 1000." }
        require(plan.currentTray in 1..plan.totalTrays) {
            "Current tray must be within the treatment."
        }
        require(plan.daysPerTray in 1..365) { "Days per tray must be between 1 and 365." }
        require(plan.dailyGoalMinutes in 1..1440) { "Prescribed target must be 1 to 1440 minutes." }
        val zone = zone(plan.zoneId)
        val start = date(plan.startDate)
        val current = date(plan.currentTrayStartedOn)
        val today = now.atZone(zone).toLocalDate()
        require(start <= current && current <= today) {
            "Tray start must be between treatment start and today."
        }
        require(plan.trackingStartedAt in 0..now.toEpochMilli()) {
            "Tracking start cannot be in the future."
        }
        require(start <= Instant.ofEpochMilli(plan.trackingStartedAt).atZone(zone).toLocalDate()) {
            "Treatment cannot start after tracking begins."
        }
        require(plan.completed == (plan.completedAt != null)) {
            "Completed treatment needs a completion timestamp."
        }
        plan.completedAt?.let {
            require(it in plan.trackingStartedAt..now.toEpochMilli()) {
                "Completion must be within recorded treatment time."
            }
            require(current <= Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) {
                "Completion cannot precede the current tray."
            }
        }
    }

    fun snapshot(snapshot: TrackerSnapshot, now: Instant) {
        require(snapshot.events.size <= MAX_EVENTS) {
            "A backup can contain at most 50,000 events."
        }
        val plan = snapshot.plan
        if (plan == null) {
            require(snapshot.events.isEmpty() && !hasExpandedRecords(snapshot)) {
                "Treatment records require a treatment plan."
            }
            return
        }
        plan(plan, now)
        validateEvents(snapshot.events, plan, now)
        if (hasExpandedRecords(snapshot)) validateExpanded(snapshot, now)
    }

    /** Converts schema-1/legacy in-memory state without inventing earlier tray transitions. */
    fun expandLegacy(snapshot: TrackerSnapshot): TrackerSnapshot {
        val plan = snapshot.plan ?: return snapshot
        if (hasExpandedTreatmentRecords(snapshot)) return snapshot
        val zone = zone(plan.zoneId)
        val completedOn =
            plan.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toString() }
        return snapshot.copy(
            phases =
                listOf(
                    TreatmentPhase(
                        id = 1,
                        kind = TreatmentPhaseKind.ALIGNER,
                        ordinal = 1,
                        name = "Initial treatment",
                        totalTrays = plan.totalTrays,
                        startedOn = plan.startDate,
                        completedOn = completedOn,
                        active = !plan.completed,
                    )
                ),
            scheduleRevisions =
                listOf(ScheduleRevision(1, 1, plan.trackingStartedAt, "Imported fixed schedule")),
            trayIntervals = listOf(TrayInterval(1, 1, 1, plan.totalTrays, plan.daysPerTray)),
            trayHistory =
                listOf(
                    TrayHistoryEntry(
                        id = 1,
                        phaseId = 1,
                        trayNumber = plan.currentTray,
                        startedOn = plan.currentTrayStartedOn,
                        endedOn = completedOn,
                        scheduleRevisionId = 1,
                        prescribedDays = plan.daysPerTray,
                        startedAt = plan.trackingStartedAt,
                        endedAt = plan.completedAt,
                    )
                ),
            targetHistory =
                listOf(
                    TargetHistoryEntry(
                        id = 1,
                        effectiveFrom =
                            Instant.ofEpochMilli(plan.trackingStartedAt)
                                .atZone(zone)
                                .toLocalDate()
                                .toString(),
                        goalMinutes = plan.dailyGoalMinutes,
                    )
                ),
        )
    }

    fun intervals(intervals: List<TrayIntervalDraft>, totalTrays: Int) {
        require(intervals.isNotEmpty()) { "A schedule must contain at least one interval." }
        var next = 1
        intervals
            .sortedBy { it.firstTray }
            .forEach {
                require(it.firstTray == next) {
                    "Schedule intervals must have no gaps or overlaps."
                }
                require(it.lastTray in it.firstTray..totalTrays) {
                    "Schedule interval is outside the phase."
                }
                require(it.daysPerTray in 1..365) {
                    "Tray interval must be between 1 and 365 days."
                }
                next = it.lastTray + 1
            }
        require(next == totalTrays + 1) { "Schedule must cover every tray in the phase." }
    }

    fun note(note: TreatmentNote, now: Long) {
        require(note.occurredAt in 0..now) { "Note time cannot be in the future." }
        require(note.text.isNotBlank() && note.text.length <= MAX_TEXT_LENGTH) {
            "Note text must be between 1 and $MAX_TEXT_LENGTH characters."
        }
    }

    fun appointment(appointment: Appointment) {
        require(appointment.startsAt >= 0) { "Appointment time is invalid." }
        require(appointment.durationMinutes in 1..1440) { "Appointment duration is invalid." }
        require(appointment.title.isNotBlank() && appointment.title.length <= 500) {
            "Appointment title is required."
        }
        require(appointment.note.length <= MAX_TEXT_LENGTH) { "Appointment note is too long." }
        appointment.reminderMinutesBefore?.let {
            require(it in 0..525_600) { "Appointment reminder is invalid." }
        }
    }

    fun photo(photo: PhotoMetadata, now: Long, allowOwnership: Boolean) {
        require(photo.capturedAt in 0..now) { "Photo time cannot be in the future." }
        require(photo.mimeType.matches(Regex("image/[A-Za-z0-9.+-]{1,64}"))) {
            "Photo MIME type is invalid."
        }
        require(photo.byteSize in 1..10L * 1024 * 1024) { "Photo size is invalid." }
        require(photo.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Photo checksum is invalid." }
        require(photo.caption.length <= 2000) { "Photo caption is too long." }
        require(photo.width == null || photo.width in 1..50_000) { "Photo width is invalid." }
        require(photo.height == null || photo.height in 1..50_000) { "Photo height is invalid." }
        require(allowOwnership || photo.ownedFileName == null) {
            "Portable photo metadata cannot claim ownership of a local file."
        }
        photo.ownedFileName?.let {
            require(it.isNotBlank() && it.length <= 255 && '/' !in it && '\\' !in it) {
                "Photo file ownership name is invalid."
            }
        }
    }

    fun date(value: String): LocalDate {
        val parsed =
            try {
                LocalDate.parse(value)
            } catch (_: Exception) {
                throw IllegalArgumentException("Dates must use YYYY-MM-DD format.")
            }
        require(value == parsed.toString() && parsed.year in 1970..2100) {
            "Dates must use YYYY-MM-DD between 1970 and 2100."
        }
        return parsed
    }

    private fun validateEvents(events: List<WearEvent>, plan: TreatmentPlan, now: Instant) {
        require(events.isNotEmpty()) { "Treatment needs an initial tracking event." }
        require(events.first().at == plan.trackingStartedAt) {
            "Initial event must match tracking start."
        }
        val ids = HashSet<Long>()
        var previous: WearEvent? = null
        for (event in events) {
            require(event.id > 0 && ids.add(event.id)) { "Event IDs must be positive and unique." }
            require(
                event.at in
                    plan.trackingStartedAt..minOf(
                            now.toEpochMilli(),
                            plan.completedAt ?: Long.MAX_VALUE,
                        )
            ) {
                "An event is outside the tracked period or in the future."
            }
            previous?.let {
                require(event.at > it.at) { "Event timestamps must increase strictly." }
                require(event.wearing != it.wearing) { "Events must alternate between in and out." }
            }
            previous = event
        }
    }

    private fun validateExpanded(snapshot: TrackerSnapshot, now: Instant) {
        val plan = checkNotNull(snapshot.plan)
        require(snapshot.phases.isNotEmpty()) { "Expanded treatment needs a phase." }
        val phaseIds = positiveUnique(snapshot.phases.map { it.id }, "phase")
        val active = snapshot.phases.filter { it.active }
        require(active.size == if (plan.completed) 0 else 1) {
            "Treatment must have exactly one active phase."
        }
        var expectedOrdinal = 1
        snapshot.phases
            .sortedBy { it.ordinal }
            .forEach { phase ->
                require(phase.ordinal == expectedOrdinal++) { "Phase order must be contiguous." }
                require(phase.name.isNotBlank() && phase.name.length <= 200) {
                    "Phase name is invalid."
                }
                require(phase.totalTrays in 1..1000) { "Phase tray count is invalid." }
                val started = date(phase.startedOn)
                phase.completedOn?.let {
                    require(date(it) >= started) { "Phase completion precedes its start." }
                }
            }
        active.singleOrNull()?.let {
            require(it.totalTrays == plan.totalTrays) {
                "Active phase must match the treatment tray count."
            }
        }

        val revisions = snapshot.scheduleRevisions.associateBy { it.id }
        positiveUnique(snapshot.scheduleRevisions.map { it.id }, "schedule revision")
        snapshot.scheduleRevisions.forEach {
            require(it.phaseId in phaseIds && it.createdAt in 0..now.toEpochMilli()) {
                "Schedule revision is invalid."
            }
            require(it.reason == null || it.reason.length <= 1000) {
                "Schedule reason is too long."
            }
            val rows = snapshot.trayIntervals.filter { row -> row.scheduleRevisionId == it.id }
            val phase = snapshot.phases.single { phase -> phase.id == it.phaseId }
            intervals(
                rows.map { row -> TrayIntervalDraft(row.firstTray, row.lastTray, row.daysPerTray) },
                phase.totalTrays,
            )
        }
        positiveUnique(snapshot.trayIntervals.map { it.id }, "tray interval")
        require(snapshot.trayIntervals.all { it.scheduleRevisionId in revisions }) {
            "Tray interval references a missing schedule revision."
        }

        positiveUnique(snapshot.trayHistory.map { it.id }, "tray history")
        require(snapshot.trayHistory.count { it.endedOn == null } == if (plan.completed) 0 else 1) {
            "Treatment must have exactly one open tray history record."
        }
        snapshot.trayHistory.forEach { tray ->
            val phase =
                snapshot.phases.singleOrNull { it.id == tray.phaseId }
                    ?: throw IllegalArgumentException("Tray history references a missing phase.")
            require(tray.trayNumber in 1..phase.totalTrays) { "Tray history number is invalid." }
            val revision = revisions[tray.scheduleRevisionId]
            require(revision?.phaseId == phase.id) { "Tray history references the wrong schedule." }
            require(tray.prescribedDays in 1..365) { "Tray history interval is invalid." }
            val started = date(tray.startedOn)
            tray.endedOn?.let {
                require(date(it) >= started) { "Tray history ends before it starts." }
            }
            tray.startedAt?.let {
                require(it in plan.trackingStartedAt..now.toEpochMilli()) {
                    "Tray history start time is invalid."
                }
            }
            tray.endedAt?.let { ended ->
                require(ended <= minOf(now.toEpochMilli(), plan.completedAt ?: Long.MAX_VALUE)) {
                    "Tray history end time is invalid."
                }
                tray.startedAt?.let {
                    require(ended >= it) { "Tray history end precedes its start." }
                }
            }
        }

        positiveUnique(snapshot.targetHistory.map { it.id }, "target history")
        require(snapshot.targetHistory.isNotEmpty()) { "Expanded treatment needs target history." }
        var priorTargetDate: LocalDate? = null
        snapshot.targetHistory
            .sortedBy { it.effectiveFrom }
            .forEach {
                val effective = date(it.effectiveFrom)
                require(priorTargetDate == null || effective > priorTargetDate) {
                    "Target dates must increase without duplicates."
                }
                require(it.goalMinutes in 1..1440) { "Historical target is invalid." }
                priorTargetDate = effective
            }

        val trayIds = snapshot.trayHistory.mapTo(HashSet()) { it.id }
        positiveUnique(snapshot.trackingGaps.map { it.id }, "tracking gap")
        var priorGapEnd: Long? = null
        snapshot.trackingGaps
            .sortedBy { it.startAt }
            .forEach { gap ->
                require(gap.startAt >= plan.trackingStartedAt && gap.startAt < gap.endAt) {
                    "Tracking gap is outside treatment history."
                }
                require(
                    gap.endAt <= minOf(now.toEpochMilli(), plan.completedAt ?: Long.MAX_VALUE)
                ) {
                    "Tracking gap is outside treatment history."
                }
                require(priorGapEnd == null || gap.startAt >= priorGapEnd!!) {
                    "Tracking gaps cannot overlap."
                }
                priorGapEnd = gap.endAt
            }
        require(
            snapshot.notes.size + snapshot.appointments.size + snapshot.photos.size <=
                MAX_AUXILIARY_RECORDS
        ) {
            "Too many treatment records."
        }
        positiveUnique(snapshot.notes.map { it.id }, "note")
        snapshot.notes.forEach {
            note(it, now.toEpochMilli())
            require(it.phaseId == null || it.phaseId in phaseIds) {
                "Note references a missing phase."
            }
            require(it.trayHistoryId == null || it.trayHistoryId in trayIds) {
                "Note references a missing tray."
            }
        }
        positiveUnique(snapshot.appointments.map { it.id }, "appointment")
        snapshot.appointments.forEach(::appointment)
        positiveUnique(snapshot.photos.map { it.id }, "photo")
        snapshot.photos.forEach {
            photo(it, now.toEpochMilli(), allowOwnership = true)
            require(it.phaseId == null || it.phaseId in phaseIds) {
                "Photo references a missing phase."
            }
            require(it.trayHistoryId == null || it.trayHistoryId in trayIds) {
                "Photo references a missing tray."
            }
        }
        require(snapshot.stateVersion.revision >= 0) { "State revision is invalid." }
        require(snapshot.stateVersion.generation.length <= 200) { "State generation is invalid." }
    }

    private fun positiveUnique(ids: List<Long>, label: String): Set<Long> {
        val values = ids.toSet()
        require(ids.all { it > 0 } && values.size == ids.size) {
            "${label.replaceFirstChar { it.uppercase() }} IDs must be positive and unique."
        }
        return values
    }

    private fun hasExpandedRecords(snapshot: TrackerSnapshot): Boolean =
        hasExpandedTreatmentRecords(snapshot) || snapshot.stateVersion != StateVersion()

    private fun hasExpandedTreatmentRecords(snapshot: TrackerSnapshot): Boolean =
        snapshot.phases.isNotEmpty() ||
            snapshot.scheduleRevisions.isNotEmpty() ||
            snapshot.trayIntervals.isNotEmpty() ||
            snapshot.trayHistory.isNotEmpty() ||
            snapshot.targetHistory.isNotEmpty() ||
            snapshot.notes.isNotEmpty() ||
            snapshot.appointments.isNotEmpty() ||
            snapshot.photos.isNotEmpty() ||
            snapshot.trackingGaps.isNotEmpty()

    private fun zone(value: String): ZoneId =
        try {
            ZoneId.of(value)
        } catch (_: Exception) {
            throw IllegalArgumentException("Choose a valid treatment time zone.")
        }
}
