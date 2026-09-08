package org.alignertracker.app.wear.sync

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.alignertracker.transport.WearMessagePaths

/** Only in-flight replies live here. The Room outbox owns retries across process death. */
internal object WearMessageRequests {
    private data class Pending(val nodeId: String, val reply: CompletableDeferred<ByteArray>)

    private val pending = ConcurrentHashMap<String, Pending>()

    suspend fun request(
        nodeId: String,
        path: String,
        payload: ByteArray,
        send: suspend (String, String, ByteArray) -> Unit,
    ): ByteArray =
        withTimeout(20_000) {
            require(payload.size <= WearMessagePaths.MAX_BYTES)
            val id = UUID.randomUUID().toString()
            val entry = Pending(nodeId, CompletableDeferred())
            check(pending.putIfAbsent(id, entry) == null)
            try {
                send(nodeId, WearMessagePaths.request(path, id), payload)
                entry.reply.await()
            } finally {
                pending.remove(id, entry)
                entry.reply.cancel()
            }
        }

    /**
     * Caller first verifies the sending node is nearby. Wrong-node/late/duplicate replies are
     * ignored.
     */
    fun accept(nodeId: String, path: String, payload: ByteArray): Boolean {
        if (payload.size > WearMessagePaths.MAX_BYTES) return false
        val id = WearMessagePaths.parseReply(path) ?: return false
        val entry = pending[id] ?: return false
        if (entry.nodeId != nodeId) return false
        return entry.reply.complete(payload.copyOf())
    }
}
