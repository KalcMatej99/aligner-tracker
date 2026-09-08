package org.alignertracker.transport

import java.util.UUID

/** Correlation lives in the path; the versioned, size-bounded domain DTO stays unchanged. */
internal object WearMessagePaths {
    const val MAX_BYTES = 8 * 1024
    private const val REPLY_PREFIX = "/aligner/v1/reply/"
    private val requestBases = setOf("/aligner/v1/sync", "/aligner/v1/command")

    fun request(base: String, id: String): String {
        require(base in requestBases && validId(id))
        return "$base/$id"
    }

    fun parseRequest(path: String): Pair<String, String>? {
        val base = path.substringBeforeLast('/', "")
        val id = path.substringAfterLast('/')
        return if (base in requestBases && validId(id)) base to id else null
    }

    fun reply(id: String): String {
        require(validId(id))
        return REPLY_PREFIX + id
    }

    fun parseReply(path: String): String? =
        path.takeIf { it.startsWith(REPLY_PREFIX) }?.removePrefix(REPLY_PREFIX)?.takeIf(::validId)

    private fun validId(id: String): Boolean =
        runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
}
