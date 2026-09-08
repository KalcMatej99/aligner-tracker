package org.alignertracker.app.wear.sync

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.alignertracker.transport.WearMessagePaths
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WearMessageRequestsTest {
    @Test
    fun repliesAreCorrelatedByUuidAndNodeAndDuplicatesAreIgnored() = runTest {
        val paths = mutableListOf<String>()
        val first = async {
            WearMessageRequests.request("phone", "/aligner/v1/sync", byteArrayOf()) { _, path, _ ->
                paths += path
            }
        }
        val second = async {
            WearMessageRequests.request("phone", "/aligner/v1/sync", byteArrayOf()) { _, path, _ ->
                paths += path
            }
        }
        runCurrent()
        val replies =
            paths.map { WearMessagePaths.reply(WearMessagePaths.parseRequest(it)!!.second) }
        assertNotEquals(replies[0], replies[1])
        assertFalse(WearMessageRequests.accept("other-phone", replies[0], byteArrayOf(1)))
        assertFalse(WearMessageRequests.accept("phone", replies[0], ByteArray(8193)))
        assertTrue(WearMessageRequests.accept("phone", replies[1], byteArrayOf(2)))
        assertTrue(WearMessageRequests.accept("phone", replies[0], byteArrayOf(1)))
        assertFalse(WearMessageRequests.accept("phone", replies[0], byteArrayOf(3)))
        assertArrayEquals(byteArrayOf(1), first.await())
        assertArrayEquals(byteArrayOf(2), second.await())
        assertFalse(WearMessageRequests.accept("phone", replies[1], byteArrayOf(2)))
    }

    @Test
    fun timeoutRemovesCorrelationSoLateReplyCannotAcknowledgeAnotherRequest() = runTest {
        var reply = ""
        val request = async {
            runCatching {
                WearMessageRequests.request("phone", "/aligner/v1/command", byteArrayOf()) {
                    _,
                    path,
                    _ ->
                    reply = WearMessagePaths.reply(WearMessagePaths.parseRequest(path)!!.second)
                }
            }
        }
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        assertTrue(request.await().exceptionOrNull() is TimeoutCancellationException)
        assertFalse(WearMessageRequests.accept("phone", reply, byteArrayOf(1)))
    }

    @Test
    fun malformedAndUnsupportedRequestPathsAreRejected() {
        assertNull(WearMessagePaths.parseRequest("/aligner/v1/command"))
        assertNull(WearMessagePaths.parseRequest("/aligner/v1/command/1-1-1-1-1"))
        assertNull(
            WearMessagePaths.parseRequest("/aligner/v1/delete/00000000-0000-0000-0000-000000000000")
        )
        assertNull(WearMessagePaths.parseReply("/aligner/v1/reply/../../command"))
    }
}
