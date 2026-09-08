package org.alignertracker.transport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.internal.GetAllCapabilitiesResponse
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse
import com.google.android.gms.wearable.internal.SendMessageResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.microg.gms.wearable.BaseWearableCallbacks
import org.microg.gms.wearable.WearableClientImpl

/** Apache-licensed microG client talking to the installed Wear Data Layer service. */
internal class MicrogWearDataLayer(private val context: Context) {
    suspend fun isNearby(nodeId: String): Boolean = withClient { client ->
        connectedNodes(client).any { it.id == nodeId && it.isNearby }
    }

    suspend fun nearbyNodes(capability: String): Set<String> = withClient { client ->
        val capable =
            suspendCancellableCoroutine<Set<String>> { continuation ->
                client.serviceInterface.getAllCapabilities(
                    object : BaseWearableCallbacks() {
                        override fun onGetAllCapabilitiesResponse(
                            response: GetAllCapabilitiesResponse
                        ) {
                            if (response.statusCode != 0) {
                                continuation.resumeWithException(
                                    IllegalStateException("Could not discover the paired device.")
                                )
                            } else {
                                continuation.resume(
                                    response.capabilities
                                        .orEmpty()
                                        .filter { it.name == capability }
                                        .flatMap { it.nodes }
                                        .filter { it.isNearby }
                                        .map { it.id }
                                        .toSet()
                                )
                            }
                        }
                    },
                    1,
                ) // FILTER_REACHABLE
            }
        connectedNodes(client).filter { it.isNearby && it.id in capable }.map { it.id }.toSet()
    }

    suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray) {
        require(payload.size <= WearMessagePaths.MAX_BYTES) { "Wear message is too large." }
        withClient { client ->
            check(connectedNodes(client).any { it.id == nodeId && it.isNearby }) {
                "The paired device is no longer nearby."
            }
            suspendCancellableCoroutine<Unit> { continuation ->
                client.serviceInterface.sendMessage(
                    object : BaseWearableCallbacks() {
                        override fun onSendMessageResponse(response: SendMessageResponse) {
                            if (response.statusCode == 0) continuation.resume(Unit)
                            else
                                continuation.resumeWithException(
                                    IllegalStateException(
                                        "Could not reach the nearby paired device."
                                    )
                                )
                        }
                    },
                    nodeId,
                    path,
                    payload,
                )
            }
        }
    }

    private suspend fun connectedNodes(client: WearableClientImpl): List<Node> =
        suspendCancellableCoroutine { continuation ->
            client.serviceInterface.getConnectedNodes(
                object : BaseWearableCallbacks() {
                    override fun onGetConnectedNodesResponse(response: GetConnectedNodesResponse) {
                        if (response.statusCode == 0) continuation.resume(response.nodes.orEmpty())
                        else
                            continuation.resumeWithException(
                                IllegalStateException("Could not verify a nearby paired device.")
                            )
                    }
                }
            )
        }

    private suspend fun <T> withClient(block: suspend (WearableClientImpl) -> T): T =
        withTimeout(10_000) {
            var apiClient: GoogleApiClient? = null
            try {
                val connection = CompletableDeferred<WearableClientImpl>()
                val client =
                    GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(
                            object : GoogleApiClient.ConnectionCallbacks {
                                override fun onConnected(bundle: Bundle?) {
                                    val connected = WearableClientImpl.get(apiClient)
                                    if (connected != null) connection.complete(connected)
                                    else
                                        connection.completeExceptionally(
                                            IllegalStateException("Wear connection is unavailable.")
                                        )
                                }

                                override fun onConnectionSuspended(cause: Int) = Unit
                            }
                        )
                        .addOnConnectionFailedListener {
                            connection.completeExceptionally(
                                IllegalStateException("Wear connection is unavailable.")
                            )
                        }
                        .build()
                apiClient = client
                client.connect()
                val connected = connection.await()
                block(connected)
            } finally {
                apiClient?.disconnect()
            }
        }
}

/** Modern GMS binds event actions; microG 0.3.14 accepts only the legacy binding action. */
abstract class CompatibleWearListenerService : WearableListenerService() {
    override fun onBind(intent: Intent): IBinder? =
        when (intent.action) {
            "com.google.android.gms.wearable.MESSAGE_RECEIVED",
            "com.google.android.gms.wearable.CAPABILITY_CHANGED",
            "com.google.android.gms.wearable.BIND_LISTENER" ->
                super.onBind(Intent("com.google.android.gms.wearable.BIND_LISTENER"))
            else -> null
        }
}
