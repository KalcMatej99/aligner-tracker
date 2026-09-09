package org.alignertracker.app.data

import androidx.room.Room
import java.time.*
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.domain.*
import org.alignertracker.app.photos.PhotoStore
import org.alignertracker.app.reminders.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class QuickStartTest {
    private val context = RuntimeEnvironment.getApplication()
    private lateinit var db: TrackerDatabase
    private lateinit var repo: TrackerRepository
    private var instant = Instant.parse("2025-10-26T00:30:00Z")
    private val clock =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant, zone)

            override fun instant(): Instant = instant
        }

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        repo = TrackerRepository(db, clock)
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun zeroFieldsStartIsAtomicAndOnlyRecordsKnownState() = runBlocking {
        repo.startTracking(false, "Europe/Rome")
        val state = repo.snapshot()
        assertEquals(instant.toEpochMilli(), state.plan!!.trackingStartedAt)
        assertEquals(false, state.events.single().wearing)
        assertNull(state.plan.dailyGoalMinutes)
        assertNull(state.plan.startDate)
        assertNull(state.plan.currentTray)
        assertNull(state.plan.totalTrays)
        assertTrue(state.phases.isEmpty())
        assertTrue(state.targetHistory.isEmpty())
        assertNull(WearMath.nextChangeDate(state))
        assertEquals(
            listOf("break"),
            ReminderRules.pending(state, ReminderPreferences(enabled = true, trayEnabled = true))
                .map { it.kind },
        )
        assertTrue(runCatching { repo.startTracking(true, "UTC") }.isFailure)
        assertEquals(state, repo.snapshot())
        TrackerValidation.snapshot(state, instant)
    }

    @Test
    fun laterMetadataAndTargetNeverRewriteEventsOrEarlierTargets() = runBlocking {
        repo.startTracking(true, "Europe/Rome")
        val events = repo.snapshot().events
        instant = instant.plusSeconds(3600)
        repo.updateTreatmentDetails(null, null, 4, 7, null)
        assertNull(WearMath.nextChangeDate(repo.snapshot()))
        repo.updateTreatmentDetails(null, null, 4, 7, "2025-10-20")
        assertEquals(LocalDate.parse("2025-10-27"), WearMath.nextChangeDate(repo.snapshot()))
        assertTrue(repo.snapshot().phases.isEmpty())
        repo.updateGoal(1260)
        val target = repo.snapshot().targetHistory.single()
        assertEquals(instant.toEpochMilli(), target.effectiveAt)
        assertNull(WearMath.targetForDate(repo.snapshot(), LocalDate.parse("2025-10-25")))
        assertNull(WearMath.targetForDate(repo.snapshot(), LocalDate.parse("2025-10-26")))
        assertEquals(1260, WearMath.targetForDate(repo.snapshot(), LocalDate.parse("2025-10-27")))
        assertEquals(0, ReportMath.streak(repo.snapshot(), instant.plusSeconds(86400)))
        repo.updateTreatmentDetails(null, 20, 4, 7, "2025-10-20")
        assertNull(repo.snapshot().phases.single().startedOn)
        assertEquals(events, repo.snapshot().events)
        assertEquals(instant.toEpochMilli(), repo.snapshot().trayHistory.single().startedAt)
        TrackerValidation.snapshot(repo.snapshot(), instant)
    }

    @Test
    fun partialPlainAndEncryptedArchivesPreserveAbsenceAndRestoreAtomically() = runBlocking {
        repo.startTracking(false, "America/New_York")
        repo.addNote(TreatmentNote(occurredAt = instant.toEpochMilli(), text = "Synthetic note"))
        val original = repo.snapshot().copy(stateVersion = StateVersion())
        val encoded = BackupCodec.encode(original)
        assertTrue(encoded.contains("\"schemaVersion\":3"))
        assertEquals(original, BackupCodec.decode(encoded))
        assertTrue(BackupCodec.csv(original, instant).lines()[1].endsWith(",,untracked"))
        val bytes = PortableArchive.encode(original, PhotoStore(context, repo))
        val password = "synthetic test passphrase".toCharArray()
        val encrypted = EncryptedBackup.encrypt(bytes, password)
        val restored =
            PortableArchive.inspect(EncryptedBackup.decrypt(encrypted, password)).snapshot
        repo.replaceFromBackup(restored)
        assertEquals(original, repo.snapshot().copy(stateVersion = StateVersion()))
        val before = repo.snapshot()
        assertTrue(
            runCatching {
                    repo.replaceFromBackup(
                        restored.copy(plan = restored.plan!!.copy(currentTray = 0))
                    )
                }
                .isFailure
        )
        assertEquals(before, repo.snapshot())
    }

    @Test
    fun correctingEstablishedDetailsPreservesHistoryAndEvents() = runBlocking {
        repo.startTracking(true, "UTC")
        repo.updateTreatmentDetails(null, 20, 2, 7, "2025-10-25")
        repo.replaceSchedule(
            repo.snapshot().phases.single().id,
            listOf(TrayIntervalDraft(1, 10, 7), TrayIntervalDraft(11, 20, 10)),
        )
        val before = repo.snapshot()
        instant = instant.plusSeconds(3600)
        repo.updateTreatmentDetails(null, 21, 2, 7, "2025-10-24")
        val after = repo.snapshot()
        assertEquals(before.events, after.events)
        assertEquals(before.targetHistory, after.targetHistory)
        assertEquals(
            before.scheduleRevisions,
            after.scheduleRevisions.take(before.scheduleRevisions.size),
        )
        assertEquals(before.trayIntervals, after.trayIntervals.take(before.trayIntervals.size))
        assertEquals(
            before.trayHistory
                .single()
                .copy(endedOn = "2025-10-26", endedAt = instant.toEpochMilli()),
            after.trayHistory.first(),
        )
        assertEquals("2025-10-24", after.trayHistory.last().startedOn)
        assertEquals(instant.toEpochMilli(), after.trayHistory.last().startedAt)
        assertEquals(10, after.trayIntervals.last { 15 in it.firstTray..it.lastTray }.daysPerTray)
        assertEquals(
            after.copy(stateVersion = StateVersion()),
            BackupCodec.decode(BackupCodec.encode(after)),
        )
        assertTrue(
            runCatching { repo.updateTreatmentDetails(null, 1, 1, 7, "2025-10-24") }.isFailure
        )
        assertEquals(after, repo.snapshot())
    }

    @Test
    fun malformedSchemaThreeCannotInventTargetsOrEnableUnbackedSchedule() = runBlocking {
        repo.startTracking(true, "UTC")
        val json = BackupCodec.encode(repo.snapshot())
        assertTrue(
            runCatching {
                    BackupCodec.decode(
                        json.replace("\"dailyGoalMinutes\":null", "\"dailyGoalMinutes\":1200")
                    )
                }
                .isFailure
        )
        val forged =
            json
                .replace("\"currentTray\":null", "\"currentTray\":2")
                .replace("\"totalTrays\":null", "\"totalTrays\":20")
                .replace("\"daysPerTray\":null", "\"daysPerTray\":7")
                .replace("\"currentTrayStartedOn\":null", "\"currentTrayStartedOn\":\"2025-10-20\"")
        assertTrue(runCatching { BackupCodec.decode(forged) }.isFailure)
    }

    @Test
    fun dstCoverageAndUnknownTargetsStayHonest() = runBlocking {
        repo.startTracking(true, "Europe/Rome")
        instant = Instant.parse("2025-10-27T00:00:00Z")
        val day = WearMath.summarize(repo.snapshot(), LocalDate.parse("2025-10-26"), instant)
        assertEquals(Duration.ofMinutes(1350).toMillis(), day.wornMillis)
        assertNull(day.goalMinutes)
        assertEquals(0, ReportMath.streak(repo.snapshot(), instant))
    }
}
