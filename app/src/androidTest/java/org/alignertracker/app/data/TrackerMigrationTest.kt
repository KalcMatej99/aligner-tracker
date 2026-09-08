package org.alignertracker.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            TrackerDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun actualVersion1SchemaMigratesTreatmentEventsAndHistoricalTargets() = runBlocking {
        val name = "tracker-migration-1-2"
        helper.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO treatment VALUES (1, '2025-10-01', 12, 4, 7, '2025-10-22', 1200, 'Europe/Rome', 1761134400000, 0, NULL)"
            )
            execSQL("INSERT INTO wear_events (id, at, wearing) VALUES (1, 1761134400000, 1)")
            execSQL("INSERT INTO wear_events (id, at, wearing) VALUES (2, 1761138000000, 0)")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, TrackerDatabase.MIGRATION_1_2).close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database =
            Room.databaseBuilder(context, TrackerDatabase::class.java, name)
                .addMigrations(TrackerDatabase.MIGRATION_1_2)
                .build()
        try {
            val state = TrackerRepository(database).snapshot()
            assertEquals(listOf(true, false), state.events.map { it.wearing })
            assertEquals(4, state.trayHistory.single().trayNumber)
            assertEquals(1761134400000, state.trayHistory.single().startedAt)
            assertEquals(1200, state.targetHistory.single().goalMinutes)
            assertEquals(1, state.scheduleRevisions.size)
            assertEquals(1, state.trayIntervals.size)
            assertTrue(state.stateVersion.generation.isNotBlank())
            assertFalse(state.plan!!.completed)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
