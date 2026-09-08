package org.alignertracker.app.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TrackerValidation
import org.alignertracker.app.domain.TreatmentPlan

class TrackerRepository(private val database: TrackerDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = database.trackerDao()

    val snapshots: Flow<TrackerSnapshot> = database.invalidationTracker
        .createFlow("treatment", "wear_events", emitInitialState = true)
        .map { snapshot() }
        .distinctUntilChanged()

    suspend fun snapshot(): TrackerSnapshot = database.withTransaction {
        TrackerSnapshot(dao.plan()?.model(), dao.events().map { it.model() })
    }

    suspend fun start(plan: TreatmentPlan, wearing: Boolean) {
        database.withTransaction {
            check(dao.plan() == null) { "A treatment already exists. Delete it or explicitly replace a backup first." }
            TrackerValidation.plan(plan, clock.instant())
            require(!plan.completed) { "A new treatment cannot already be completed." }
            dao.insertPlan(PlanEntity.from(plan))
            dao.insertEvent(EventEntity(at = plan.trackingStartedAt, wearing = wearing))
        }
    }

    suspend fun setWearing(wearing: Boolean) {
        database.withTransaction {
            val plan = requirePlan()
            if (plan.completed) return@withTransaction
            val events = dao.events()
            val last = events.last()
            if (last.wearing == wearing) return@withTransaction
            val now = clock.millis()
            check(now > last.at) { "The clock is at or before the previous switch. Correct the clock or previous switch time, then try again." }
            check(events.size < TrackerValidation.MAX_EVENTS) { "Event limit reached. Export your history before starting another treatment." }
            dao.insertEvent(EventEntity(at = now, wearing = wearing))
        }
    }

    suspend fun updateEvent(id: Long, at: Long) {
        database.withTransaction {
            val plan = activePlan()
            val events = dao.events()
            val index = events.indexOfFirst { it.id == id }
            require(index >= 0) { "This switch no longer exists." }
            require(index > 0) { "The initial tracking event cannot be moved." }
            require(at <= clock.millis()) { "A switch cannot be in the future." }
            require(at > events[index - 1].at && (index == events.lastIndex || at < events[index + 1].at)) {
                "Choose a time strictly between the adjacent switches."
            }
            require(at >= plan.trackingStartedAt) { "A switch cannot precede tracking." }
            dao.updateEvent(events[index].copy(at = at))
        }
    }

    suspend fun advanceTray() {
        database.withTransaction {
            val plan = activePlan()
            check(plan.currentTray < plan.totalTrays) { "This is the last tray. Complete treatment explicitly when appropriate." }
            val today = LocalDate.now(clock.withZone(ZoneId.of(plan.zoneId)))
            check(today >= LocalDate.parse(plan.currentTrayStartedOn)) { "The clock precedes the current tray start. Correct it before advancing." }
            check(clock.millis() >= dao.events().last().at) { "The clock precedes the last switch. Correct it before advancing." }
            dao.updatePlan(PlanEntity.from(plan.copy(currentTray = plan.currentTray + 1, currentTrayStartedOn = today.toString())))
        }
    }

    suspend fun completeTreatment() {
        database.withTransaction {
            val plan = requirePlan()
            if (plan.completed) return@withTransaction
            val now = clock.millis()
            check(now >= dao.events().last().at) { "The clock precedes the last switch. Correct it before completing treatment." }
            val completed = plan.copy(completed = true, completedAt = now)
            TrackerValidation.plan(completed, Instant.ofEpochMilli(now))
            dao.updatePlan(PlanEntity.from(completed))
        }
    }

    suspend fun updateGoal(minutes: Int) {
        require(minutes in 1..1440) { "Prescribed target must be 1 to 1440 minutes." }
        database.withTransaction {
            dao.updatePlan(PlanEntity.from(activePlan().copy(dailyGoalMinutes = minutes)))
        }
    }

    suspend fun replaceFromBackup(snapshot: TrackerSnapshot) {
        // Copy caller-owned collections so validation and insertion use the same frozen input.
        val replacement = snapshot.copy(events = snapshot.events.toList())
        TrackerValidation.snapshot(replacement, clock.instant())
        database.withTransaction {
            TrackerValidation.snapshot(replacement, clock.instant())
            dao.deleteEvents()
            dao.deletePlan()
            replacement.plan?.let { dao.insertPlan(PlanEntity.from(it)) }
            dao.insertEvents(replacement.events.map { EventEntity(it.id, it.at, it.wearing) })
        }
    }

    suspend fun clearAll() {
        database.withTransaction {
            dao.deleteEvents()
            dao.deletePlan()
        }
    }

    private suspend fun requirePlan(): TreatmentPlan =
        checkNotNull(dao.plan()?.model()) { "Set up a treatment first." }

    private suspend fun activePlan(): TreatmentPlan = requirePlan().also {
        check(!it.completed) { "Completed treatment is read-only." }
    }
}
