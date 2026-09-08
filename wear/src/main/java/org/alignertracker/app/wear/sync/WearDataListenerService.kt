package org.alignertracker.app.wear.sync

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.alignertracker.app.wear.WearTrackerApplication
import org.alignertracker.transport.CompatibleWearListenerService
import org.alignertracker.transport.MicrogWearDataLayer
import org.alignertracker.transport.WearMessagePaths

class WearDataListenerService : CompatibleWearListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.data.size > MAX_PAYLOAD_BYTES) return
        val syncClient = (application as WearTrackerApplication).syncClient
        val payload = messageEvent.data.copyOf()
        serviceScope.launch {
            if (WearMessagePaths.parseReply(messageEvent.path) != null) {
                val nearby =
                    runCatching {
                            MicrogWearDataLayer(applicationContext)
                                .isNearby(messageEvent.sourceNodeId)
                        }
                        .getOrDefault(false)
                if (nearby)
                    WearMessageRequests.accept(
                        messageEvent.sourceNodeId,
                        messageEvent.path,
                        payload,
                    )
            } else {
                syncClient.acceptPushedStatus(messageEvent.sourceNodeId, messageEvent.path, payload)
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        if (capabilityInfo.name != PHONE_CAPABILITY || capabilityInfo.nodes.none { it.isNearby })
            return
        serviceScope.launch { (application as WearTrackerApplication).syncClient.reconcile() }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
