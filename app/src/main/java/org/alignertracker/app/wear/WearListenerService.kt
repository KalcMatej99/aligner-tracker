package org.alignertracker.app.wear

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.alignertracker.app.TrackerApplication

class WearListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRequest(nodeId: String, path: String, request: ByteArray): Task<ByteArray> {
        val completion = TaskCompletionSource<ByteArray>()
        serviceScope.launch {
            val bridge = (application as TrackerApplication).container.wearBridge
            runCatching { bridge.handleRequest(nodeId, path, request) }
                .onSuccess(completion::setResult)
                .onFailure {
                    completion.setException(IllegalArgumentException("Wear request failed."))
                }
        }
        return completion.task
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
