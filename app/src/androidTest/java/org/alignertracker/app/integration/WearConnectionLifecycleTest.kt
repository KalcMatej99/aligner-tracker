package org.alignertracker.app.integration

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.alignertracker.transport.MicrogWearDataLayer
import org.junit.Assert.assertFalse
import org.junit.Test

class WearConnectionLifecycleTest {
    private suspend fun probe() {
        try {
            assertFalse(
                MicrogWearDataLayer(ApplicationProvider.getApplicationContext())
                    .isNearby("nonexistent-aligner-node")
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: IllegalStateException) {
            // A phone without a paired Wear service must fail normally, never crash its callback.
            org.junit.Assert.assertEquals("Wear connection is unavailable.", error.message)
        }
    }

    @Test
    fun concurrentIoCallersAndCancellationKeepConnectionCallbacksSafe() = runBlocking {
        repeat(12) {
            coroutineScope {
                val cancelled = launch(Dispatchers.IO) { probe() }
                val checks = (1..8).map { async(Dispatchers.IO) { probe() } }
                delay(2)
                cancelled.cancel()
                checks.awaitAll()
                cancelled.join()
            }
        }
    }
}
