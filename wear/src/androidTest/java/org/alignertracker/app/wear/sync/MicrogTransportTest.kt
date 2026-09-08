package org.alignertracker.app.wear.sync

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.alignertracker.transport.CompatibleWearListenerService
import org.alignertracker.transport.MicrogWearDataLayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MicrogTransportTest {
    @Test
    fun apacheAdapterQueriesInstalledWearServiceAndRefusesUnknownNode() = runBlocking {
        val layer = MicrogWearDataLayer(ApplicationProvider.getApplicationContext())
        assertFalse(layer.isNearby("nonexistent-aligner-node"))
        layer.nearbyNodes(PHONE_CAPABILITY).forEach { assertTrue(layer.isNearby(it)) }
        assertTrue(
            runCatching { layer.sendMessage("nonexistent-aligner-node", SYNC_PATH, byteArrayOf()) }
                .isFailure
        )
    }

    @Test
    fun modernEventBindActionsUseMicrogListenerAndUnknownActionsFailClosed() {
        val service = object : CompatibleWearListenerService() {}
        service.onCreate()
        try {
            assertNotNull(
                service.onBind(Intent("com.google.android.gms.wearable.MESSAGE_RECEIVED"))
            )
            assertNotNull(
                service.onBind(Intent("com.google.android.gms.wearable.CAPABILITY_CHANGED"))
            )
            assertNull(service.onBind(Intent("unrelated.action")))
        } finally {
            service.onDestroy()
        }
    }
}
