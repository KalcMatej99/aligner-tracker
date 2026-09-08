package org.alignertracker.app.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.alignertracker.app.wear.data.LocalCommandState
import org.alignertracker.app.wear.data.WearViewState
import org.alignertracker.app.wear.surface.WearSurfaceUpdates

internal const val EXTRA_RECORD_NEXT_CHANGE = "record_next_change"

class WearMainActivity : ComponentActivity() {
    private val trackerApplication: WearTrackerApplication
        get() = application as WearTrackerApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_RECORD_NEXT_CHANGE, false)) {
            lifecycleScope.launch { enqueueNextAndSync() }
        } else {
            lifecycleScope.launch { trackerApplication.syncClient.reconcile() }
        }
        setContent {
            val state by
                trackerApplication.repository.viewState.collectAsStateWithLifecycle(
                    initialValue = WearViewState()
                )
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WearHome(
                        state = state,
                        onRecord = { wearing -> lifecycleScope.launch { enqueueAndSync(wearing) } },
                        onRetry = {
                            lifecycleScope.launch { trackerApplication.syncClient.reconcile() }
                        },
                        onDismiss = {
                            lifecycleScope.launch {
                                trackerApplication.repository.clearResolved()
                                runCatching { WearSurfaceUpdates.request(this@WearMainActivity) }
                            }
                        },
                    )
                }
            }
        }
    }

    private suspend fun enqueueNextAndSync() {
        val acknowledged = trackerApplication.repository.currentView().acknowledged
        if (acknowledged != null) enqueueAndSync(!acknowledged.wearing)
        else trackerApplication.syncClient.reconcile()
    }

    private suspend fun enqueueAndSync(wearing: Boolean) {
        trackerApplication.repository.enqueue(wearing)
        runCatching { WearSurfaceUpdates.request(this) }
        trackerApplication.syncClient.reconcile()
    }
}

@androidx.compose.runtime.Composable
private fun WearHome(
    state: WearViewState,
    onRecord: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val status = state.acknowledged
    val pending = state.pending
    val stateText =
        when {
            status == null -> "Phone status unknown"
            !status.hasPlan -> "Set up treatment on phone"
            status.completed -> "Treatment completed"
            status.wearing -> "Acknowledged: IN"
            else -> "Acknowledged: OUT"
        }
    val desired = status?.let { !it.wearing }
    val actionText = if (desired == true) "Record IN" else "Record OUT"
    val canRecord = status != null && status.hasPlan && !status.completed && pending == null

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .semantics { stateDescription = stateText },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Aligner Tracker", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(stateText, textAlign = TextAlign.Center)
        status?.tray?.let { Text("Tray $it", style = MaterialTheme.typography.bodySmall) }
        pending?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                if (it.wearing) "Pending phone acknowledgement: IN"
                else "Pending phone acknowledgement: OUT",
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
            it.lastError?.let { error ->
                Text(
                    syncErrorText(error),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        state.latestResolved
            ?.takeIf { it.state == LocalCommandState.REJECTED }
            ?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    rejectionMessage(it.rejection),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Dismiss") }
            }
        state.syncError
            ?.takeIf { pending == null }
            ?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    syncErrorText(it),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        Spacer(Modifier.height(12.dp))
        if (canRecord && desired != null) {
            Button(onClick = { onRecord(desired) }, modifier = Modifier.fillMaxWidth()) {
                Text(actionText)
            }
        }
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Sync phone") }
    }
}

private fun rejectionMessage(rejection: String?): String =
    when (rejection) {
        "STALE_GENERATION",
        "STALE_REVISION" ->
            "Phone state changed while the watch was offline. Nothing was recorded; review the phone and try again."
        "COMPLETED" -> "The phone reports that treatment is completed. Nothing was recorded."
        else -> "The phone rejected this change. Nothing was recorded."
    }

private fun syncErrorText(error: String): String =
    when {
        error.contains("not nearby", ignoreCase = true) -> "Phone not nearby"
        error.contains("invalid", ignoreCase = true) -> "Phone response was invalid"
        error.contains("clock", ignoreCase = true) -> "Review the phone clock, then sync"
        else -> "Could not sync with phone"
    }
