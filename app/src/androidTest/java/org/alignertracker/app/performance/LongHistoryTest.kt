package org.alignertracker.app.performance

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.data.*
import org.alignertracker.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LongHistoryTest {
    @Test
    fun fiftyThousandEventsRemainPortableAndSummariesMatch() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        try {
            val now = Instant.now()
            val start = now.minusSeconds(50_000 * 60L)
            val date = start.atZone(ZoneOffset.UTC).toLocalDate().toString()
            val snapshot =
                TrackerSnapshot(
                    TreatmentPlan(
                        startDate = date,
                        totalTrays = 20,
                        currentTray = 1,
                        daysPerTray = 7,
                        currentTrayStartedOn = date,
                        dailyGoalMinutes = 1200,
                        zoneId = "UTC",
                        trackingStartedAt = start.toEpochMilli(),
                    ),
                    (0 until 50_000).map {
                        WearEvent(it + 1L, start.toEpochMilli() + it * 60_000L, it % 2 == 0)
                    },
                )
            fun timed(name: String, action: () -> Unit) {
                val before = SystemClock.elapsedRealtime()
                action()
                println("ALIGNER_PERF $name=${SystemClock.elapsedRealtime() - before}ms")
            }
            val repository = TrackerRepository(database)
            val before = SystemClock.elapsedRealtime()
            repository.replaceFromBackup(snapshot)
            println("ALIGNER_PERF restore50000=${SystemClock.elapsedRealtime() - before}ms")
            val stored = repository.snapshot()
            assertEquals(50_000, stored.events.size)
            timed("summarize30days") {
                val days = ReportMath.days(stored, 30, now)
                assertTrue(days.all { it.wornMillis + it.removedMillis == it.trackedMillis })
            }
            timed("jsonRoundTrip50000") {
                assertEquals(50_000, BackupCodec.decode(BackupCodec.encode(stored)).events.size)
            }
            timed("csv50000") {
                val csv = BackupCodec.csv(stored, now)
                assertTrue(csv.contains("tracked_millis"))
                assertTrue(csv.lines().size >= 30)
            }
            timed("encryptedArchiveKdf") {
                val content = ByteArray(1_000_000) { 42 }
                val password = "performance synthetic passphrase".toCharArray()
                assertArrayEquals(
                    content,
                    EncryptedBackup.decrypt(EncryptedBackup.encrypt(content, password), password),
                )
            }
        } finally {
            database.close()
        }
    }
}
