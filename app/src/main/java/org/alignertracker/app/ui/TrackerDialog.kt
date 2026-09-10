package org.alignertracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** One scroll region keeps expanded titles, explanations and actions reachable together. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    picker: Boolean = false,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = !picker),
        modifier = if (picker) Modifier.padding(8.dp).then(Modifier.fillMaxWidth()) else Modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(
                        max =
                            (LocalConfiguration.current.screenHeightDp.dp - 48.dp).coerceAtLeast(
                                48.dp
                            )
                    )
                    .semantics { isTraversalGroup = true }
                    .verticalScroll(rememberScrollState())
                    .padding(if (picker) 12.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall, title)
                }
                ProvideTextStyle(MaterialTheme.typography.bodyMedium, text)
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    confirmButton()
                    dismissButton?.invoke()
                }
            }
        }
    }
}
