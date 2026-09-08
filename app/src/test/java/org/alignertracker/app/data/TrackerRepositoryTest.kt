package org.alignertracker.app.data

import androidx.room.Room
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.domain.WearMath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class TrackerRepositoryTest {
    private lateinit var database: TrackerDatabase
    private lateinit var repository: TrackerRepository
    private val clock = MutableClock(Instant.parse("2025-10-26T12:00:00Z"))
    private fun plan() = TreatmentPlan(startDate = "2025-10-01", totalTrays = 3, currentTray = 1,
        daysPerTray = 7, currentTrayStartedOn = "2025-10-20", dailyGoalMinutes = 1200,
        zoneId = "Europe/Rome", trackingStartedAt = clock.millis())

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrackerDatabase::class.java).build()
        repository = TrackerRepository(database, clock)
    }

    @After fun close() = database.close()

    @Test fun `repeated and concurrent same state transitions stay idempotent`() = runBlocking {
        repository.start(plan(), true)
        clock.value = clock.value.plusSeconds(60)
        val first = async { repository.setWearing(false) }
        val second = async { repository.setWearing(false) }
        first.await()
        second.await()
        repository.setWearing(false)
        assertEquals(2, repository.snapshot().events.size)
        assertFalse(WearMath.isWearing(repository.snapshot()))
        assertEquals(repository.snapshot(), repository.snapshots.first())
    }

    @Test fun `clock rollback refuses transition and preserves old snapshot`() = runBlocking {
        repository.start(plan(), true)
        val before = repository.snapshot()
        clock.value = clock.value.minusSeconds(1)
        rejected { repository.setWearing(false) }
        rejected { repository.completeTreatment() }
        assertEquals(before, repository.snapshot())
    }

    @Test fun `correction respects neighbors initial event and future boundary`() = runBlocking {
        repository.start(plan(), true)
        clock.value = clock.value.plusSeconds(60)
        repository.setWearing(false)
        clock.value = clock.value.plusSeconds(60)
        repository.setWearing(true)
        val events = repository.snapshot().events
        rejected { repository.updateEvent(events.first().id, events.first().at + 1) }
        rejected { repository.updateEvent(events[1].id, events.first().at) }
        rejected { repository.updateEvent(events[1].id, events.last().at) }
        rejected { repository.updateEvent(events.last().id, clock.millis() + 1) }
        repository.updateEvent(events[1].id, events[1].at - 30_000)
        assertEquals(events[1].at - 30_000, repository.snapshot().events[1].at)
    }

    @Test fun `invalid imports leave all prior records untouched`() = runBlocking {
        repository.start(plan(), true)
        val before = repository.snapshot()
        rejected { repository.replaceFromBackup(before.copy(events = listOf(WearEvent(5, clock.millis() + 1, true)))) }
        rejected { repository.replaceFromBackup(before.copy(plan = before.plan!!.copy(currentTray = 9))) }
        assertEquals(before, repository.snapshot())
        val replacement = before.copy(plan = before.plan!!.copy(dailyGoalMinutes = 1320))
        repository.replaceFromBackup(replacement)
        assertEquals(replacement, repository.snapshot())
    }

    @Test fun `explicit progression refuses last tray and completion freezes writes`() = runBlocking {
        repository.start(plan(), true)
        repository.advanceTray()
        assertEquals(2, repository.snapshot().plan!!.currentTray)
        assertEquals("2025-10-26", repository.snapshot().plan!!.currentTrayStartedOn)
        repository.advanceTray()
        rejected { repository.advanceTray() }
        clock.value = clock.value.plusSeconds(60)
        repository.completeTreatment()
        val completed = repository.snapshot()
        assertTrue(completed.plan!!.completed)
        assertEquals(clock.millis(), completed.plan.completedAt)
        clock.value = clock.value.plusSeconds(60)
        repository.setWearing(false)
        repository.completeTreatment()
        rejected { repository.updateGoal(1300) }
        assertEquals(completed, repository.snapshot())
        repository.clearAll()
        assertEquals(TrackerSnapshot(), repository.snapshot())
    }

    @Test fun `setup refuses replacing treatment and invalid plan`() = runBlocking {
        rejected { repository.start(plan().copy(dailyGoalMinutes = 0), true) }
        assertEquals(TrackerSnapshot(), repository.snapshot())
        repository.start(plan(), true)
        rejected { repository.start(plan(), false) }
        assertEquals(1, repository.snapshot().events.size)
    }

    private suspend fun rejected(block: suspend () -> Unit) {
        try { block() } catch (_: IllegalArgumentException) { return }
        catch (_: IllegalStateException) { return }
        throw AssertionError("Expected operation to reject invalid state")
    }

    private class MutableClock(var value: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = fixed(value, zone)
        override fun instant(): Instant = value
    }
}
