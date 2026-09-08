package org.alignertracker.app.wear.sync

import kotlinx.serialization.Serializable

internal const val PROTOCOL_VERSION = 1
internal const val PATH_PREFIX = "/aligner/v1"
internal const val COMMAND_PATH = "$PATH_PREFIX/command"
internal const val SYNC_PATH = "$PATH_PREFIX/sync"
internal const val STATUS_PATH = "$PATH_PREFIX/status"
internal const val PHONE_CAPABILITY = "aligner_phone_v1"
internal const val MAX_PAYLOAD_BYTES = 8 * 1024

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
    val protocolVersion: Int = PROTOCOL_VERSION,
    val command: WireWearCommand? = null,
)

@Serializable
internal data class WearStatus(
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
    val protocolVersion: Int = PROTOCOL_VERSION,
    val outcome: WireCommandOutcome? = null,
    val status: WearStatus? = null,
    val error: String? = null,
)
