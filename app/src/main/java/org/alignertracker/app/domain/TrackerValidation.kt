package org.alignertracker.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Shared validation for setup and backups, before any database mutation. */
object TrackerValidation {
    const val MAX_EVENTS = 50_000
    const val MAX_BACKUP_BYTES = 5 * 1024 * 1024

    fun plan(plan: TreatmentPlan, now: Instant) {
        require(plan.id == 1L) { "A backup must contain one treatment with ID 1." }
        require(plan.totalTrays in 1..1000) { "Total trays must be between 1 and 1000." }
        require(plan.currentTray in 1..plan.totalTrays) {
            "Current tray must be within the treatment."
        }
        require(plan.daysPerTray in 1..365) { "Days per tray must be between 1 and 365." }
        require(plan.dailyGoalMinutes in 1..1440) { "Prescribed target must be 1 to 1440 minutes." }
        val zone =
            try {
                ZoneId.of(plan.zoneId)
            } catch (_: Exception) {
                throw IllegalArgumentException("Choose a valid treatment time zone.")
            }
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
            require(snapshot.events.isEmpty()) { "Events require a treatment plan." }
            return
        }
        plan(plan, now)
        require(snapshot.events.isNotEmpty()) { "Treatment needs an initial tracking event." }
        require(snapshot.events.first().at == plan.trackingStartedAt) {
            "Initial event must match tracking start."
        }
        val ids = HashSet<Long>()
        var previous: WearEvent? = null
        for (event in snapshot.events) {
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

    private fun date(value: String): LocalDate {
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
}
