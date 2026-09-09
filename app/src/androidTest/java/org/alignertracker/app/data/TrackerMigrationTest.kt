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
    fun versionTwoUpgradePreservesEveryExistingColumnAndAllowsUnknownSetup(): Unit = runBlocking {
        val name = "tracker-migration-2-3"
        helper.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO treatment VALUES (1, '2025-10-01', 12, 4, 7, '2025-10-22', 1200, 'Europe/Rome', 1761134400000, 0, NULL)"
            )
            execSQL("INSERT INTO wear_events VALUES (1, 1761134400000, 1)")
            execSQL("INSERT INTO wear_events VALUES (2, 1761138000000, 0)")
            close()
        }
        val old = helper.runMigrationsAndValidate(name, 2, true, TrackerDatabase.MIGRATION_1_2)
        old.execSQL("INSERT INTO target_history VALUES (2, '2025-10-24', 1260)")
        old.execSQL("UPDATE treatment SET dailyGoalMinutes=1260")
        old.execSQL(
            "INSERT INTO treatment_notes VALUES (1, 1761134400000, 'Synthetic migration note', 1, 1, 1761134400000, 1761134400000)"
        )
        old.execSQL(
            "INSERT INTO tracking_gaps VALUES (1, 1761134500000, 1761134600000, 'MANUAL_CORRECTION')"
        )
        val tables =
            listOf(
                "treatment",
                "wear_events",
                "treatment_phases",
                "schedule_revisions",
                "tray_intervals",
                "tray_history",
                "target_history",
                "treatment_notes",
                "tracking_gaps",
                "tracker_state",
            )
        fun rows(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            columns: List<String>? = null,
        ): Pair<List<String>, List<List<String?>>> {
            db.query("SELECT ${columns?.joinToString() ?: "*"} FROM $table ORDER BY id").use { c ->
                val names = c.columnNames.toList()
                val data = mutableListOf<List<String?>>()
                while (c.moveToNext()) data +=
                    names.indices.map { if (c.isNull(it)) null else c.getString(it) }
                return names to data
            }
        }
        val expected = tables.associateWith { rows(old, it) }
        old.close()
        val migrated = helper.runMigrationsAndValidate(name, 3, true, TrackerDatabase.MIGRATION_2_3)
        expected.forEach { (table, before) ->
            assertEquals(table, before, rows(migrated, table, before.first))
        }
        migrated.query("SELECT effectiveAt FROM target_history").use { c ->
            while (c.moveToNext()) assertTrue(c.isNull(0))
        }
        migrated.close()
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(name)
    }

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
        helper
            .runMigrationsAndValidate(
                name,
                3,
                true,
                TrackerDatabase.MIGRATION_1_2,
                TrackerDatabase.MIGRATION_2_3,
            )
            .close()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database =
            Room.databaseBuilder(context, TrackerDatabase::class.java, name)
                .addMigrations(TrackerDatabase.MIGRATION_1_2, TrackerDatabase.MIGRATION_2_3)
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
