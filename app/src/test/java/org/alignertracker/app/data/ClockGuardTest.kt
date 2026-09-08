package org.alignertracker.app.data

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.room.Room
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.domain.TreatmentPlan
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ClockGuardTest {
    @Test
    fun sameBootDiscontinuityExcludesCoverageAndBackwardClockCannotMutateHistory() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        val repository = TrackerRepository(db)
        val prefs = context.getSharedPreferences("clock-observation", Context.MODE_PRIVATE)
        try {
            val now = Instant.now()
            val started = now.minusSeconds(3600)
            val date = started.atZone(ZoneOffset.UTC).toLocalDate().toString()
            repository.start(
                TreatmentPlan(
                    startDate = date,
                    totalTrays = 2,
                    currentTray = 1,
                    daysPerTray = 7,
                    currentTrayStartedOn = date,
                    dailyGoalMinutes = 1200,
                    zoneId = "UTC",
                    trackingStartedAt = started.toEpochMilli(),
                ),
                true,
            )
            Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 42)
            val generation = repository.snapshot().stateVersion.generation
            fun anchor(wall: Long) {
                prefs
                    .edit()
                    .putString("generation", generation)
                    .putInt("boot", 42)
                    .putLong("elapsed", SystemClock.elapsedRealtime())
                    .putLong("wall", wall)
                    .commit()
            }
            anchor(now.minusSeconds(600).toEpochMilli())
            ClockGuard(context, repository).observe()
            assertEquals(1, repository.snapshot().trackingGaps.size)
            // Reopening the guard reads the persisted anchor, with no duplicate gap.
            ClockGuard(context, repository).observe()
            assertEquals(1, repository.snapshot().trackingGaps.size)
            val before = repository.snapshot()
            anchor(now.plusSeconds(600).toEpochMilli())
            assertTrue(runCatching { ClockGuard(context, repository).observe() }.isFailure)
            assertEquals(before, repository.snapshot())
            // A different boot has no trustworthy monotonic comparison; no fabricated gap.
            Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 43)
            ClockGuard(context, repository).observe()
            assertEquals(before, repository.snapshot())
            repository.clearAll()
            ClockGuard(context, repository).observe()
            assertTrue(prefs.all.isEmpty())
        } finally {
            prefs.edit().clear().commit()
            db.close()
        }
    }
}
