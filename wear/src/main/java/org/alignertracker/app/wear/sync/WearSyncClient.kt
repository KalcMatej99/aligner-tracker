package org.alignertracker.app.wear.sync

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.alignertracker.app.wear.data.LocalWearCommand
import org.alignertracker.app.wear.data.WearRepository
import org.alignertracker.app.wear.surface.WearSurfaceUpdates

internal class WearSyncClient(
    context: Context,
    private val repository: WearRepository,
    private val transport: WatchWearTransport = GmsWatchWearTransport(context.applicationContext),
    private val json: Json = Json { explicitNulls = false },
) {
    private val appContext = context.applicationContext
    private val syncMutex = Mutex()

    suspend fun reconcile(): Boolean =
        syncMutex.withLock {
            val phone =
                runCatching { transport.nearbyPhoneNodes().sorted().firstOrNull() }
                    .getOrElse {
                        repository.markError(null, "Could not check for a nearby phone.")
                        return@withLock false
                    }
            if (phone == null) {
                val message =
                    if (repository.pendingCommands().isEmpty()) {
                        "Phone is not nearby. Bring the paired phone close and sync again."
                    } else {
                        "Phone is not nearby. The pending change is safely stored."
                    }
                repository.markError(null, message)
                return@withLock false
            }

            val pending = repository.pendingCommands()
            if (pending.isEmpty()) {
                return@withLock requestStatus(phone)
            }
            pending.all { sendCommand(phone, it) }
        }

    suspend fun acceptPushedStatus(
        senderNodeId: String,
        path: String,
        payload: ByteArray,
    ): Boolean {
        if (path != STATUS_PATH || payload.size > MAX_PAYLOAD_BYTES) {
            return false
        }
        val nearby =
            runCatching { transport.isNearby(senderNodeId) }
                .getOrElse {
                    repository.markError(null, "Could not verify the nearby phone.")
                    return false
                }
        if (!nearby) return false
        return runCatching {
                val response = decode(payload)
                repository.applyStatus(response)
                requestSurfaceUpdates()
                true
            }
            .getOrElse {
                repository.markError(null, "Nearby phone sent an invalid status update.")
                false
            }
    }

    private suspend fun sendCommand(phoneNodeId: String, command: LocalWearCommand): Boolean =
        runCatching {
                val request = WearBridgeRequest(command = command.wire())
                val response = request(phoneNodeId, COMMAND_PATH, request)
                repository.applyCommandResponse(command.idempotencyId, response)
                requestSurfaceUpdates()
                true
            }
            .getOrElse {
                repository.markError(
                    command.idempotencyId,
                    it.message ?: "Phone did not acknowledge the pending change.",
                )
                false
            }

    private suspend fun requestStatus(phoneNodeId: String): Boolean =
        runCatching {
                repository.applyStatus(request(phoneNodeId, SYNC_PATH, WearBridgeRequest()))
                requestSurfaceUpdates()
                true
            }
            .getOrElse {
                repository.markError(null, it.message ?: "Could not refresh phone status.")
                false
            }

    private suspend fun request(
        nodeId: String,
        path: String,
        request: WearBridgeRequest,
    ): WearBridgeResponse {
        val encoded = json.encodeToString(request).encodeToByteArray()
        check(encoded.size <= MAX_PAYLOAD_BYTES) { "Wear request is too large." }
        val response = withTimeout(20_000) { transport.sendRequest(nodeId, path, encoded) }
        check(response.size <= MAX_PAYLOAD_BYTES) { "Phone response is too large." }
        return decode(response)
    }

    private fun decode(payload: ByteArray): WearBridgeResponse =
        json.decodeFromString(payload.decodeToString())

    private fun requestSurfaceUpdates() {
        runCatching { WearSurfaceUpdates.request(appContext) }
    }
}

internal interface WatchWearTransport {
    suspend fun nearbyPhoneNodes(): Set<String>

    suspend fun isNearby(nodeId: String): Boolean

    suspend fun sendRequest(nodeId: String, path: String, payload: ByteArray): ByteArray
}

private class GmsWatchWearTransport(context: Context) : WatchWearTransport {
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    override suspend fun nearbyPhoneNodes(): Set<String> =
        capabilityClient
            .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes
            .filterTo(linkedSetOf()) { it.isNearby }
            .mapTo(linkedSetOf()) { it.id }

    override suspend fun isNearby(nodeId: String): Boolean =
        nodeClient.connectedNodes.await().any { it.id == nodeId && it.isNearby }

    override suspend fun sendRequest(nodeId: String, path: String, payload: ByteArray): ByteArray =
        messageClient.sendRequest(nodeId, path, payload).await()
}
