package org.alignertracker.app.wear.sync

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.alignertracker.app.wear.WearTrackerApplication

class WearDataListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val syncClient = (application as WearTrackerApplication).syncClient
        val payload = messageEvent.data.copyOf()
        serviceScope.launch {
            syncClient.acceptPushedStatus(messageEvent.sourceNodeId, messageEvent.path, payload)
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
