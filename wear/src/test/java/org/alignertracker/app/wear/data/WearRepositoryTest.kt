package org.alignertracker.app.wear.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.alignertracker.app.wear.sync.WearBridgeResponse
import org.alignertracker.app.wear.sync.WearStatus
import org.alignertracker.app.wear.sync.WireCommandOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearRepositoryTest {
    private lateinit var database: WearDatabase
    private lateinit var repository: WearRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, WearDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository =
            WearRepository(database, Clock.fixed(Instant.ofEpochMilli(50_000), ZoneOffset.UTC))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pendingCommandSurvivesSyncFailureUntilPhoneAcknowledgesIt() = runTest {
        repository.applyStatus(statusResponse(revision = 4, wearing = false))

        val enqueued = repository.enqueue(wearing = true)
        assertTrue(enqueued is EnqueueResult.Enqueued)
        val command = (enqueued as EnqueueResult.Enqueued).command

        repository.markError(command.idempotencyId, "Phone is not nearby.")
        val offline = repository.currentView()
        assertEquals(command.idempotencyId, offline.pending?.idempotencyId)
        assertEquals("Phone is not nearby.", offline.pending?.lastError)
        assertFalse(offline.acknowledged!!.wearing)

        repository.applyCommandResponse(
            command.idempotencyId,
            commandResponse(command.idempotencyId, revision = 5, wearing = true),
        )

        val reconciled = repository.viewState.first { it.latestResolved != null }
        assertNull(reconciled.pending)
        assertTrue(reconciled.acknowledged!!.wearing)
        assertEquals(LocalCommandState.ACKNOWLEDGED, reconciled.latestResolved!!.state)
        assertNull(reconciled.syncError)
    }

    @Test
    fun staleRejectionStoresOutcomeAndAdoptsPhoneSnapshot() = runTest {
        repository.applyStatus(statusResponse(revision = 7, wearing = true))
        val command = (repository.enqueue(wearing = false) as EnqueueResult.Enqueued).command

        repository.applyCommandResponse(
            command.idempotencyId,
            WearBridgeResponse(
                outcome =
                    WireCommandOutcome(
                        idempotencyId = command.idempotencyId,
                        status = "REJECTED",
                        rejection = "STALE_REVISION",
                        receivedAt = 70_000,
                        generation = "phone-generation",
                        revision = 8,
                    ),
                status = phoneStatus(revision = 8, wearing = true),
            ),
        )

        val reconciled = repository.viewState.first { it.latestResolved != null }
        val acknowledged = checkNotNull(reconciled.acknowledged)
        val resolved = checkNotNull(reconciled.latestResolved)
        assertNull(reconciled.pending)
        assertTrue(acknowledged.wearing)
        assertEquals(8, acknowledged.revision)
        assertEquals(LocalCommandState.REJECTED, resolved.state)
        assertEquals("STALE_REVISION", resolved.rejection)
    }

    @Test
    fun enqueueRequiresAcknowledgedActivePlanAndAllowsOnlyOnePendingCommand() = runTest {
        assertTrue(repository.enqueue(wearing = true) is EnqueueResult.Blocked)

        repository.applyStatus(statusResponse(revision = 1, wearing = false, hasPlan = false))
        assertTrue(repository.enqueue(wearing = true) is EnqueueResult.Blocked)

        repository.applyStatus(statusResponse(revision = 2, wearing = false))
        assertTrue(repository.enqueue(wearing = false) is EnqueueResult.Blocked)
        assertTrue(repository.enqueue(wearing = true) is EnqueueResult.Enqueued)
        assertTrue(repository.enqueue(wearing = true) is EnqueueResult.Blocked)
        assertNotNull(repository.currentView().pending)
    }

    private fun statusResponse(revision: Long, wearing: Boolean, hasPlan: Boolean = true) =
        WearBridgeResponse(status = phoneStatus(revision, wearing, hasPlan))

    private fun commandResponse(commandId: String, revision: Long, wearing: Boolean) =
        WearBridgeResponse(
            outcome =
                WireCommandOutcome(
                    idempotencyId = commandId,
                    status = "ACCEPTED",
                    receivedAt = 60_000,
                    generation = "phone-generation",
                    revision = revision,
                ),
            status = phoneStatus(revision, wearing),
        )

    private fun phoneStatus(revision: Long, wearing: Boolean, hasPlan: Boolean = true) =
        WearStatus(
            generation = "phone-generation",
            revision = revision,
            wearing = wearing,
            completed = false,
            hasPlan = hasPlan,
            tray = if (hasPlan) 3 else null,
            lastAt = 40_000,
        )
}
