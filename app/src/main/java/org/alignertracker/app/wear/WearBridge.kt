package org.alignertracker.app.wear

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.alignertracker.app.data.ClockGuard
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.CommandSource
import org.alignertracker.app.domain.WearCommand
import org.alignertracker.transport.MicrogWearDataLayer

class WearBridge
internal constructor(
    context: Context,
    private val repository: TrackerRepository,
    private val clockGuard: ClockGuard,
    private val transport: PhoneWearTransport = GmsPhoneWearTransport(context.applicationContext),
    private val json: Json = Json { explicitNulls = false },
) {
    suspend fun handleRequest(senderNodeId: String, path: String, payload: ByteArray): ByteArray {
        if (!transport.isNearby(senderNodeId)) {
            return response(error = "A directly connected nearby watch is required.")
        }
        if (payload.size > MAX_WEAR_PAYLOAD_BYTES) {
            return response(error = "Wear request is too large.")
        }
        runCatching { clockGuard.observe() }
            .onFailure {
                return response(
                    error =
                        it.message
                            ?: "The phone clock changed. Open the phone app and review tracking before retrying."
                )
            }

        return when (path) {
            WEAR_SYNC_PATH -> {
                val request =
                    decodeRequest(payload) ?: return response(error = "Wear request is invalid.")
                if (request.protocolVersion != WEAR_PROTOCOL_VERSION || request.command != null) {
                    response(error = "Wear protocol version or request type is unsupported.")
                } else {
                    response(status = currentStatus())
                }
            }

            WEAR_COMMAND_PATH -> {
                val request =
                    decodeRequest(payload) ?: return response(error = "Wear request is invalid.")
                val command = request.command
                if (request.protocolVersion != WEAR_PROTOCOL_VERSION || command == null) {
                    response(error = "Wear protocol version or request type is unsupported.")
                } else {
                    val outcome =
                        repository.applyWearCommand(
                            WearCommand(
                                idempotencyId = command.idempotencyId,
                                expectedGeneration = command.expectedGeneration,
                                expectedRevision = command.expectedRevision,
                                wearing = command.wearing,
                                requestedAt = command.requestedAt,
                                source = CommandSource.WATCH,
                            )
                        )
                    response(
                        outcome =
                            WireCommandOutcome(
                                idempotencyId = outcome.idempotencyId,
                                status = outcome.status.name,
                                rejection = outcome.rejection?.name,
                                receivedAt = outcome.receivedAt,
                                generation = outcome.stateAfter.generation,
                                revision = outcome.stateAfter.revision,
                            ),
                        status = currentStatus(),
                    )
                }
            }

            else -> response(error = "Wear request path is unsupported.")
        }
    }

    suspend fun handleMessage(senderNodeId: String, path: String, payload: ByteArray) {
        val (requestPath, requestId) =
            org.alignertracker.transport.WearMessagePaths.parseRequest(path) ?: return
        if (payload.size > MAX_WEAR_PAYLOAD_BYTES || !transport.isNearby(senderNodeId)) return
        val result = handleRequest(senderNodeId, requestPath, payload)
        transport.sendMessage(
            senderNodeId,
            org.alignertracker.transport.WearMessagePaths.reply(requestId),
            result,
        )
    }

    suspend fun currentStatus(): WearStatus {
        val snapshot = repository.snapshot()
        return WearStatus(
            generation = snapshot.stateVersion.generation,
            revision = snapshot.stateVersion.revision,
            wearing = snapshot.events.lastOrNull()?.wearing ?: false,
            completed = snapshot.plan?.completed == true,
            hasPlan = snapshot.plan != null,
            tray = snapshot.plan?.currentTray,
            lastAt = snapshot.events.lastOrNull()?.at,
        )
    }

    suspend fun publishStatusToNearbyWearNodes(): Int {
        clockGuard.observe()
        val encoded = response(status = currentStatus())
        var delivered = 0
        transport.nearbyNodes(WATCH_CAPABILITY).forEach { nodeId ->
            runCatching { transport.sendMessage(nodeId, WEAR_STATUS_PATH, encoded) }
                .onSuccess { delivered += 1 }
        }
        return delivered
    }

    suspend fun reconcile(): Int = publishStatusToNearbyWearNodes()

    private fun decodeRequest(payload: ByteArray): WearBridgeRequest? =
        runCatching { json.decodeFromString<WearBridgeRequest>(payload.decodeToString()) }
            .getOrNull()

    private fun response(
        outcome: WireCommandOutcome? = null,
        status: WearStatus? = null,
        error: String? = null,
    ): ByteArray =
        json
            .encodeToString(WearBridgeResponse(outcome = outcome, status = status, error = error))
            .encodeToByteArray()
}

internal interface PhoneWearTransport {
    suspend fun isNearby(nodeId: String): Boolean

    suspend fun nearbyNodes(capability: String): Set<String>

    suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray)
}

private class GmsPhoneWearTransport(context: Context) : PhoneWearTransport {
    private val dataLayer = MicrogWearDataLayer(context)

    override suspend fun isNearby(nodeId: String): Boolean = dataLayer.isNearby(nodeId)

    override suspend fun nearbyNodes(capability: String): Set<String> =
        dataLayer.nearbyNodes(capability)

    override suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray) =
        dataLayer.sendMessage(nodeId, path, payload)
}
