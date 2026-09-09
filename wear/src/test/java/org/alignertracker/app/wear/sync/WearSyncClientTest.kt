package org.alignertracker.app.wear.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.alignertracker.app.wear.data.EnqueueResult
import org.alignertracker.app.wear.data.WearDatabase
import org.alignertracker.app.wear.data.WearRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearSyncClientTest {
    private lateinit var context: Context
    private lateinit var database: WearDatabase
    private lateinit var repository: WearRepository
    private lateinit var transport: FakeTransport
    private lateinit var client: WearSyncClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, WearDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository =
            WearRepository(database, Clock.fixed(Instant.ofEpochMilli(90_000), ZoneOffset.UTC))
        transport = FakeTransport()
        client = WearSyncClient(context, repository, transport)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun versionOneStatusWithUnknownTraySupportsTrackingAndRejectsFutureProtocol() = runTest {
        val partial = phoneStatus(revision = 10, wearing = false).copy(tray = null)
        val json = Json { explicitNulls = false }
        val decoded =
            json.decodeFromString<WearBridgeResponse>(
                json.encodeToString(WearBridgeResponse(status = partial))
            )
        repository.applyStatus(decoded)
        assertNull(repository.currentView().acknowledged!!.tray)
        assertTrue(repository.enqueue(true) is EnqueueResult.Enqueued)
        assertTrue(
            runCatching {
                    repository.applyStatus(
                        WearBridgeResponse(
                            protocolVersion = 99,
                            status = partial.copy(revision = 100),
                        )
                    )
                }
                .isFailure
        )
        assertEquals(10L, repository.currentView().acknowledged!!.revision)
    }

    @Test
    fun offlineCommandRemainsPendingAndRetriesWhenPhoneReconnects() = runTest {
        repository.applyStatus(
            WearBridgeResponse(status = phoneStatus(revision = 10, wearing = false))
        )
        val command = (repository.enqueue(wearing = true) as EnqueueResult.Enqueued).command

        assertFalse(client.reconcile())
        assertEquals(command.idempotencyId, repository.currentView().pending?.idempotencyId)
        assertFalse(repository.currentView().acknowledged!!.wearing)

        transport.nearbyNodes += "phone"
        transport.response = { _, path, payload ->
            assertEquals(COMMAND_PATH, path)
            val request = Json.decodeFromString<WearBridgeRequest>(payload.decodeToString())
            assertEquals(command.idempotencyId, request.command!!.idempotencyId)
            Json.encodeToString(
                    WearBridgeResponse(
                        outcome =
                            WireCommandOutcome(
                                idempotencyId = command.idempotencyId,
                                status = "ACCEPTED",
                                receivedAt = 100_000,
                                generation = "generation",
                                revision = 11,
                            ),
                        status = phoneStatus(revision = 11, wearing = true),
                    )
                )
                .encodeToByteArray()
        }

        assertTrue(client.reconcile())
        assertNull(repository.currentView().pending)
        assertTrue(repository.currentView().acknowledged!!.wearing)
        assertEquals(1, transport.requestCount)
    }

    @Test
    fun statusPushFromNonNearbyNodeIsIgnored() = runTest {
        val payload =
            Json.encodeToString(
                    WearBridgeResponse(status = phoneStatus(revision = 3, wearing = true))
                )
                .encodeToByteArray()

        assertFalse(client.acceptPushedStatus("cloud-node", STATUS_PATH, payload))
        assertNull(repository.currentView().acknowledged)

        transport.nearbyNodes += "nearby-phone"
        assertTrue(client.acceptPushedStatus("nearby-phone", STATUS_PATH, payload))
        assertTrue(repository.currentView().acknowledged!!.wearing)
    }

    private fun phoneStatus(revision: Long, wearing: Boolean) =
        WearStatus(
            generation = "generation",
            revision = revision,
            wearing = wearing,
            completed = false,
            hasPlan = true,
            tray = 4,
            lastAt = 80_000,
        )

    private class FakeTransport : WatchWearTransport {
        val nearbyNodes = linkedSetOf<String>()
        var requestCount = 0
        var response: suspend (String, String, ByteArray) -> ByteArray = { _, _, _ ->
            error("No phone response configured")
        }

        override suspend fun nearbyPhoneNodes(): Set<String> = nearbyNodes.toSet()

        override suspend fun isNearby(nodeId: String): Boolean = nodeId in nearbyNodes

        override suspend fun sendRequest(
            nodeId: String,
            path: String,
            payload: ByteArray,
        ): ByteArray {
            requestCount += 1
            return response(nodeId, path, payload)
        }
    }
}
