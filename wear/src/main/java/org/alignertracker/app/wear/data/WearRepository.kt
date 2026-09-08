package org.alignertracker.app.wear.data

import androidx.room.withTransaction
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.alignertracker.app.wear.sync.WearBridgeResponse
import org.alignertracker.app.wear.sync.WearStatus
import org.alignertracker.app.wear.sync.WireWearCommand

internal enum class LocalCommandState {
    PENDING,
    ACKNOWLEDGED,
    REJECTED,
}

internal data class AcknowledgedStatus(
    val generation: String,
    val revision: Long,
    val wearing: Boolean,
    val completed: Boolean,
    val hasPlan: Boolean,
    val tray: Int?,
    val lastAt: Long?,
    val acknowledgedAt: Long,
)

internal data class LocalWearCommand(
    val idempotencyId: String,
    val expectedGeneration: String,
    val expectedRevision: Long,
    val wearing: Boolean,
    val requestedAt: Long,
    val state: LocalCommandState,
    val rejection: String?,
    val lastError: String?,
) {
    fun wire(): WireWearCommand =
        WireWearCommand(
            idempotencyId = idempotencyId,
            expectedGeneration = expectedGeneration,
            expectedRevision = expectedRevision,
            wearing = wearing,
            requestedAt = requestedAt,
        )
}

internal data class WearViewState(
    val acknowledged: AcknowledgedStatus? = null,
    val pending: LocalWearCommand? = null,
    val latestResolved: LocalWearCommand? = null,
    val syncError: String? = null,
)

internal sealed interface EnqueueResult {
    data class Enqueued(val command: LocalWearCommand) : EnqueueResult

    data class Blocked(val reason: String) : EnqueueResult
}

internal class WearRepository(
    private val database: WearDatabase,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.wearDao()

    val viewState: Flow<WearViewState> =
        combine(dao.observeStatus(), dao.observeCommands()) { status, commands ->
            WearViewState(
                acknowledged = status?.takeIf { it.available }?.model(),
                pending =
                    commands.firstOrNull { it.state == LocalCommandState.PENDING.name }?.model(),
                latestResolved =
                    commands.firstOrNull { it.state != LocalCommandState.PENDING.name }?.model(),
                syncError = status?.syncError,
            )
        }

    suspend fun currentView(): WearViewState =
        database.withTransaction {
            val status = dao.status()
            val commands = dao.pendingCommands()
            WearViewState(
                acknowledged = status?.takeIf { it.available }?.model(),
                pending = commands.firstOrNull()?.model(),
                latestResolved = dao.latestResolvedCommand()?.model(),
                syncError = status?.syncError,
            )
        }

    suspend fun enqueue(wearing: Boolean): EnqueueResult =
        database.withTransaction {
            if (dao.pendingCommands().isNotEmpty()) {
                return@withTransaction EnqueueResult.Blocked(
                    "Wait for the pending phone acknowledgement."
                )
            }
            val status =
                dao.status()?.takeIf { it.available }
                    ?: return@withTransaction EnqueueResult.Blocked(
                        "Sync with the nearby phone before recording a change."
                    )
            if (!status.hasPlan) {
                return@withTransaction EnqueueResult.Blocked("Set up treatment on the phone first.")
            }
            if (status.completed) {
                return@withTransaction EnqueueResult.Blocked("Treatment is completed on the phone.")
            }
            if (status.wearing == wearing) {
                return@withTransaction EnqueueResult.Blocked(
                    "The acknowledged phone state already matches."
                )
            }
            val command =
                WearCommandEntity(
                    idempotencyId = "watch:${UUID.randomUUID()}",
                    expectedGeneration = status.generation,
                    expectedRevision = status.revision,
                    wearing = wearing,
                    requestedAt = clock.millis(),
                    state = LocalCommandState.PENDING.name,
                )
            dao.insertCommand(command)
            EnqueueResult.Enqueued(command.model())
        }

    suspend fun pendingCommands(): List<LocalWearCommand> = dao.pendingCommands().map { it.model() }

    suspend fun applyCommandResponse(commandId: String, response: WearBridgeResponse) {
        database.withTransaction {
            val command =
                checkNotNull(dao.command(commandId)) { "Pending watch command is missing." }
            check(command.state == LocalCommandState.PENDING.name) {
                "Watch command is already resolved."
            }
            check(response.protocolVersion == 1) { "Phone protocol version is unsupported." }
            response.error?.let { throw IllegalArgumentException(it) }
            val outcome =
                checkNotNull(response.outcome) { "Phone response omitted the command outcome." }
            check(outcome.idempotencyId == commandId) {
                "Phone acknowledged a different watch command."
            }
            val status =
                checkNotNull(response.status) { "Phone response omitted acknowledged state." }
            check(outcome.generation == status.generation && outcome.revision == status.revision) {
                "Phone outcome and status versions disagree."
            }
            if (outcome.status == "ACCEPTED") {
                check(outcome.rejection == null && status.wearing == command.wearing) {
                    "Phone acceptance did not confirm the requested state."
                }
            } else {
                check(outcome.status == "REJECTED" && outcome.rejection != null) {
                    "Phone rejection is incomplete."
                }
            }
            putStatus(status)
            dao.putCommand(
                command.copy(
                    state =
                        if (outcome.status == "ACCEPTED") LocalCommandState.ACKNOWLEDGED.name
                        else LocalCommandState.REJECTED.name,
                    rejection = outcome.rejection,
                    receivedAt = outcome.receivedAt,
                    resultingGeneration = outcome.generation,
                    resultingRevision = outcome.revision,
                    lastError = null,
                )
            )
        }
    }

    suspend fun applyStatus(response: WearBridgeResponse) {
        database.withTransaction {
            check(response.protocolVersion == 1) { "Phone protocol version is unsupported." }
            response.error?.let { throw IllegalArgumentException(it) }
            check(response.outcome == null) { "Unexpected command outcome in status response." }
            putStatus(
                checkNotNull(response.status) { "Phone response omitted acknowledged state." }
            )
        }
    }

    suspend fun markError(commandId: String?, message: String) {
        val safeMessage = message.take(240).ifBlank { "Could not sync with the nearby phone." }
        database.withTransaction {
            if (commandId != null) {
                dao.command(commandId)
                    ?.takeIf { it.state == LocalCommandState.PENDING.name }
                    ?.let { dao.putCommand(it.copy(lastError = safeMessage)) }
            }
            val current = dao.status()
            dao.putStatus((current ?: emptyStatus()).copy(syncError = safeMessage))
        }
    }

    suspend fun clearResolved() = database.withTransaction { dao.clearResolvedCommands() }

    private suspend fun putStatus(status: WearStatus) {
        dao.putStatus(
            AcknowledgedStatusEntity(
                available = true,
                generation = status.generation,
                revision = status.revision,
                wearing = status.wearing,
                completed = status.completed,
                hasPlan = status.hasPlan,
                tray = status.tray,
                lastAt = status.lastAt,
                acknowledgedAt = clock.millis(),
                syncError = null,
            )
        )
    }

    private fun emptyStatus() =
        AcknowledgedStatusEntity(
            available = false,
            generation = "",
            revision = 0,
            wearing = false,
            completed = false,
            hasPlan = false,
            tray = null,
            lastAt = null,
            acknowledgedAt = 0,
        )
}

private fun AcknowledgedStatusEntity.model() =
    AcknowledgedStatus(
        generation = generation,
        revision = revision,
        wearing = wearing,
        completed = completed,
        hasPlan = hasPlan,
        tray = tray,
        lastAt = lastAt,
        acknowledgedAt = acknowledgedAt,
    )

private fun WearCommandEntity.model() =
    LocalWearCommand(
        idempotencyId = idempotencyId,
        expectedGeneration = expectedGeneration,
        expectedRevision = expectedRevision,
        wearing = wearing,
        requestedAt = requestedAt,
        state = LocalCommandState.valueOf(state),
        rejection = rejection,
        lastError = lastError,
    )
