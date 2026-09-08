package org.alignertracker.app.wear

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.wear.data.EnqueueResult
import org.alignertracker.app.wear.data.LocalCommandState
import org.alignertracker.app.wear.data.WearDatabase
import org.alignertracker.app.wear.data.WearRepository
import org.alignertracker.app.wear.sync.WearBridgeResponse
import org.alignertracker.app.wear.sync.WearStatus
import org.alignertracker.app.wear.sync.WireCommandOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearCompanionInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun durableOutboxSurvivesDatabaseReopenAndStoresStaleOutcome() = runBlocking {
        val databaseName = "wear-instrumented-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)
        try {
            val firstDatabase = database(databaseName)
            val firstRepository = repository(firstDatabase)
            firstRepository.applyStatus(
                WearBridgeResponse(status = phoneStatus(12, wearing = false))
            )
            val enqueued = firstRepository.enqueue(wearing = true) as EnqueueResult.Enqueued
            firstDatabase.close()

            val reopenedDatabase = database(databaseName)
            val reopenedRepository = repository(reopenedDatabase)
            val restored = reopenedRepository.currentView().pending
            assertNotNull(restored)
            assertEquals(enqueued.command.idempotencyId, restored!!.idempotencyId)
            assertFalse(reopenedRepository.currentView().acknowledged!!.wearing)

            reopenedRepository.applyCommandResponse(
                restored.idempotencyId,
                WearBridgeResponse(
                    outcome =
                        WireCommandOutcome(
                            idempotencyId = restored.idempotencyId,
                            status = "REJECTED",
                            rejection = "STALE_REVISION",
                            receivedAt = 125_000,
                            generation = "phone-generation",
                            revision = 13,
                        ),
                    status = phoneStatus(13, wearing = false),
                ),
            )

            assertNull(reopenedRepository.currentView().pending)
            val stored = reopenedDatabase.wearDao().command(restored.idempotencyId)
            assertEquals(LocalCommandState.REJECTED.name, stored!!.state)
            assertEquals("STALE_REVISION", stored.rejection)
            reopenedDatabase.close()
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun mainActivityLaunchesWithoutNearbyPhone() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent =
            Intent(context, WearMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        val activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
        assertTrue(activity is WearMainActivity)
        assertFalse(activity.isFinishing)
        activity.finish()
    }

    private fun database(name: String): WearDatabase =
        Room.databaseBuilder(context, WearDatabase::class.java, name).build()

    private fun repository(database: WearDatabase): WearRepository =
        WearRepository(database, Clock.fixed(Instant.ofEpochMilli(120_000), ZoneOffset.UTC))

    private fun phoneStatus(revision: Long, wearing: Boolean) =
        WearStatus(
            generation = "phone-generation",
            revision = revision,
            wearing = wearing,
            completed = false,
            hasPlan = true,
            tray = 5,
            lastAt = 110_000,
        )
}
