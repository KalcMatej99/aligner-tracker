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
import org.alignertracker.app.domain.Appointment
import org.alignertracker.app.domain.PhotoMetadata
import org.alignertracker.app.domain.TreatmentNote
import org.alignertracker.app.domain.TrayIntervalDraft
import org.alignertracker.app.domain.TreatmentPhaseKind
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearCommand
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

    private fun plan() =
        TreatmentPlan(
            startDate = "2025-10-01",
            totalTrays = 3,
            currentTray = 1,
            daysPerTray = 7,
            currentTrayStartedOn = "2025-10-20",
            dailyGoalMinutes = 1200,
            zoneId = "Europe/Rome",
            trackingStartedAt = clock.millis(),
        )

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    TrackerDatabase::class.java,
                )
                .build()
        repository = TrackerRepository(database, clock)
    }

    @After fun close() = database.close()

    @Test
    fun `repeated and concurrent same state transitions stay idempotent`() = runBlocking {
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

    @Test
    fun `clock rollback refuses transition and preserves old snapshot`() = runBlocking {
        repository.start(plan(), true)
        val before = repository.snapshot()
        clock.value = clock.value.minusSeconds(1)
        rejected { repository.setWearing(false) }
        rejected { repository.completeTreatment() }
        assertEquals(before, repository.snapshot())
    }

    @Test
    fun `correction respects neighbors initial event and future boundary`() = runBlocking {
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

    @Test
    fun `invalid imports leave all prior records untouched`() = runBlocking {
        repository.start(plan(), true)
        val before = repository.snapshot()
        rejected {
            repository.replaceFromBackup(
                before.copy(events = listOf(WearEvent(5, clock.millis() + 1, true)))
            )
        }
        rejected {
            repository.replaceFromBackup(before.copy(plan = before.plan!!.copy(currentTray = 9)))
        }
        assertEquals(before, repository.snapshot())
        val replacement = before.copy(plan = before.plan!!.copy(dailyGoalMinutes = 1320))
        repository.replaceFromBackup(replacement)
        val restored = repository.snapshot()
        assertEquals(replacement.copy(stateVersion = restored.stateVersion), restored)
    }

    @Test
    fun `explicit progression refuses last tray and completion freezes writes`() = runBlocking {
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

    @Test
    fun `setup refuses replacing treatment and invalid plan`() = runBlocking {
        rejected { repository.start(plan().copy(dailyGoalMinutes = 0), true) }
        assertEquals(TrackerSnapshot(), repository.snapshot())
        repository.start(plan(), true)
        rejected { repository.start(plan(), false) }
        assertEquals(1, repository.snapshot().events.size)
    }

    @Test
    fun `schedule revisions preserve prior prescription and exact tray actions`() = runBlocking {
        repository.start(plan(), true)
        val phase = repository.snapshot().phases.single()
        rejected {
            repository.replaceSchedule(
                phase.id,
                listOf(TrayIntervalDraft(1, 1, 5), TrayIntervalDraft(3, 3, 10)),
            )
        }
        repository.replaceSchedule(
            phase.id,
            listOf(TrayIntervalDraft(1, 1, 5), TrayIntervalDraft(2, 3, 10)),
            "Clinician changed later trays",
        )
        assertEquals(2, repository.snapshot().scheduleRevisions.size)
        assertEquals(5, repository.snapshot().plan!!.daysPerTray)
        assertEquals(
            java.time.LocalDate.parse("2025-10-25"),
            WearMath.nextChangeDate(repository.snapshot()),
        )
        repository.advanceTray()
        val state = repository.snapshot()
        assertEquals(10, state.plan!!.daysPerTray)
        assertEquals(2, state.trayHistory.size)
        assertEquals(clock.millis(), state.trayHistory[0].endedAt)
        assertEquals(clock.millis(), state.trayHistory[1].startedAt)
    }

    @Test
    fun `goal changes default to tomorrow and preserve today's prescribed target`() = runBlocking {
        repository.start(plan(), true)
        repository.updateGoal(1320)
        val state = repository.snapshot()
        assertEquals("2025-10-27", state.targetHistory.last().effectiveFrom)
        assertEquals(
            1200,
            WearMath.summarize(state, java.time.LocalDate.parse("2025-10-26"), clock.instant())
                .goalMinutes,
        )
        assertEquals(
            1320,
            WearMath.targetForDate(state, java.time.LocalDate.parse("2025-10-27")),
        )
    }

    @Test
    fun `missing interval splits one known interval and remains correctable after completion`() =
        runBlocking {
            repository.start(plan(), true)
            val start = clock.millis()
            clock.value = clock.value.plusSeconds(600)
            repository.completeTreatment()
            repository.insertMissingInterval(start + 60_000, start + 120_000, false)
            val corrected = repository.snapshot()
            assertEquals(listOf(true, false, true), corrected.events.map { it.wearing })
            rejected {
                repository.insertMissingInterval(start + 60_000, start + 90_000, false)
            }
            repository.updateEvent(corrected.events[1].id, start + 70_000)
            assertEquals(start + 70_000, repository.snapshot().events[1].at)
        }

    @Test
    fun `stale and duplicate watch commands have durable deterministic outcomes`() = runBlocking {
        repository.start(plan(), true)
        val initial = repository.snapshot().stateVersion
        val stale = WearCommand("watch-stale-0001", initial.generation, initial.revision - 1, false, clock.millis())
        val firstRejection = repository.applyWearCommand(stale)
        assertEquals(firstRejection, repository.applyWearCommand(stale.copy(wearing = true)))
        assertEquals("STALE_REVISION", firstRejection.rejection!!.name)

        clock.value = clock.value.plusSeconds(60)
        val accepted =
            repository.applyWearCommand(
                WearCommand("watch-valid-0001", initial.generation, initial.revision, false, clock.millis() - 30_000)
            )
        assertEquals("ACCEPTED", accepted.status.name)
        assertEquals(clock.millis(), repository.snapshot().events.last().at)

        val beforeRestore = repository.snapshot()
        repository.replaceFromBackup(beforeRestore.copy(stateVersion = org.alignertracker.app.domain.StateVersion()))
        val restored = repository.snapshot().stateVersion
        assertTrue(restored.generation != initial.generation)
        val replay = repository.applyWearCommand(
            WearCommand("watch-after-restore", initial.generation, accepted.stateAfter.revision, true, clock.millis())
        )
        assertEquals("STALE_GENERATION", replay.rejection!!.name)
    }

    @Test
    fun `completed phase pause is untracked when refinement begins`() = runBlocking {
        repository.start(plan(), true)
        clock.value = clock.value.plusSeconds(60)
        repository.completeTreatment()
        val completedAt = clock.millis()
        clock.value = clock.value.plusSeconds(3600)
        repository.beginPhase(TreatmentPhaseKind.REFINEMENT, "Refinement 1", 2, 5)
        val state = repository.snapshot()
        assertFalse(state.plan!!.completed)
        assertEquals(2, state.phases.size)
        assertEquals(completedAt, state.trackingGaps.single().startAt)
        assertEquals(clock.millis(), state.trackingGaps.single().endAt)
    }

    @Test
    fun `journal records and trusted photo ownership restore atomically`() = runBlocking {
        repository.start(plan(), true)
        val state = repository.snapshot()
        val phaseId = state.phases.single().id
        val trayId = state.trayHistory.single().id
        val noteId =
            repository.addNote(
                TreatmentNote(occurredAt = clock.millis(), text = "Attachment changed", phaseId = phaseId, trayHistoryId = trayId)
            )
        val appointmentId =
            repository.addAppointment(
                Appointment(startsAt = clock.millis() + 86_400_000, durationMinutes = 30, title = "Check-up")
            )
        val oldFile = "11111111-1111-4111-8111-111111111111.jpg"
        val photoId =
            repository.addPhoto(
                PhotoMetadata(
                    capturedAt = clock.millis(),
                    mimeType = "image/jpeg",
                    byteSize = 123,
                    sha256 = "a".repeat(64),
                    phaseId = phaseId,
                    trayHistoryId = trayId,
                    width = 100,
                    height = 80,
                    ownedFileName = oldFile,
                )
            )
        assertEquals(noteId, repository.snapshot().notes.single().id)
        assertEquals(appointmentId, repository.snapshot().appointments.single().id)
        val portable = repository.snapshot().copy(
            photos = repository.snapshot().photos.map { it.copy(ownedFileName = null) }
        )
        val newFile = "22222222-2222-4222-8222-222222222222.jpg"
        assertEquals(listOf(oldFile), repository.replaceFromBackupWithCleanup(portable, mapOf(photoId to newFile)))
        assertEquals(newFile, repository.snapshot().photos.single().ownedFileName)
    }

    private suspend fun rejected(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        } catch (_: IllegalStateException) {
            return
        }
        throw AssertionError("Expected operation to reject invalid state")
    }

    private class MutableClock(var value: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = fixed(value, zone)

        override fun instant(): Instant = value
    }
}
