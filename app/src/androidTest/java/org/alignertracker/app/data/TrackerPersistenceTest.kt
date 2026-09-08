package org.alignertracker.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerPersistenceTest {
    @Test fun treatmentEventsCorrectionAndCompletionSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "restart-verification.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, TrackerDatabase::class.java, name).build()
        val start = Instant.parse("2025-10-26T12:00:00Z")
        try {
            val initial = TrackerRepository(database, Clock.fixed(start, ZoneOffset.UTC))
            initial.start(TreatmentPlan(startDate = "2025-10-01", totalTrays = 20, currentTray = 1,
                daysPerTray = 7, currentTrayStartedOn = "2025-10-20", dailyGoalMinutes = 1200,
                zoneId = "Europe/Rome", trackingStartedAt = start.toEpochMilli()), true)
            val later = TrackerRepository(database, Clock.fixed(start.plusSeconds(120), ZoneOffset.UTC))
            later.setWearing(false)
            later.updateEvent(later.snapshot().events.last().id, start.plusSeconds(60).toEpochMilli())
            later.completeTreatment()
            val expected = later.snapshot()
            database.close()
            database = Room.databaseBuilder(context, TrackerDatabase::class.java, name).build()
            val reopened = TrackerRepository(database)
            assertEquals(expected, reopened.snapshot())
            assertNotNull(reopened.snapshot().plan!!.completedAt)
            try {
                reopened.replaceFromBackup(expected.copy(events = emptyList()))
                throw AssertionError("Invalid import was accepted")
            } catch (_: IllegalArgumentException) {
                assertEquals(expected, reopened.snapshot())
            }
            reopened.clearAll()
            assertEquals(TrackerSnapshot(), reopened.snapshot())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
