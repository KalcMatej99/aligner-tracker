package org.alignertracker.app.wear

import kotlinx.serialization.Serializable

internal const val WEAR_PROTOCOL_VERSION = 1
internal const val WEAR_PATH_PREFIX = "/aligner/v1"
internal const val WEAR_COMMAND_PATH = "$WEAR_PATH_PREFIX/command"
internal const val WEAR_SYNC_PATH = "$WEAR_PATH_PREFIX/sync"
internal const val WEAR_STATUS_PATH = "$WEAR_PATH_PREFIX/status"
internal const val PHONE_CAPABILITY = "aligner_phone_v1"
internal const val WATCH_CAPABILITY = "aligner_watch_v1"
internal const val MAX_WEAR_PAYLOAD_BYTES = 8 * 1024

@Serializable
internal data class WireWearCommand(
    val idempotencyId: String,
    val expectedGeneration: String,
    val expectedRevision: Long,
    val wearing: Boolean,
    val requestedAt: Long,
)

@Serializable
internal data class WearBridgeRequest(
    val protocolVersion: Int = WEAR_PROTOCOL_VERSION,
    val command: WireWearCommand? = null,
)

@Serializable
data class WearStatus(
    val generation: String,
    val revision: Long,
    val wearing: Boolean,
    val completed: Boolean,
    val hasPlan: Boolean,
    val tray: Int?,
    val lastAt: Long?,
)

@Serializable
internal data class WireCommandOutcome(
    val idempotencyId: String,
    val status: String,
    val rejection: String? = null,
    val receivedAt: Long,
    val generation: String,
    val revision: Long,
)

@Serializable
internal data class WearBridgeResponse(
    val protocolVersion: Int = WEAR_PROTOCOL_VERSION,
    val outcome: WireCommandOutcome? = null,
    val status: WearStatus? = null,
    val error: String? = null,
)
