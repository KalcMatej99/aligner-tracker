package org.alignertracker.app.wear

import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.alignertracker.app.TrackerApplication
import org.alignertracker.transport.CompatibleWearListenerService

class WearListenerService : CompatibleWearListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        if (event.data.size > MAX_WEAR_PAYLOAD_BYTES) return
        val payload = event.data.copyOf()
        val source = event.sourceNodeId
        val path = event.path
        serviceScope.launch {
            val bridge = (application as TrackerApplication).container.wearBridge
            runCatching { bridge.handleMessage(source, path, payload) }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
