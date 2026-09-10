package org.alignertracker.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.alignertracker.app.R

/** Keeps the native control role and action while identifying the affected record. */
internal fun Modifier.recordAction(description: String): Modifier = semantics {
    contentDescription = description
}

@Composable
internal fun Modifier.fieldError(invalid: Boolean, @StringRes message: Int): Modifier {
    val description = stringResource(message)
    return semantics { if (invalid) error(description) }
}

@Composable
internal fun OperationNotice(message: String, onDismiss: () -> Unit) {
    val dismiss = stringResource(R.string.dismiss_notice)
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(
        onClick = onDismiss,
        modifier =
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                onClick(label = dismiss, action = null)
            },
    ) {
        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.Canvas(Modifier.padding(start = 12.dp).size(20.dp)) {
            drawLine(
                ink,
                androidx.compose.ui.geometry.Offset(size.width * .3f, size.height * .3f),
                androidx.compose.ui.geometry.Offset(size.width * .7f, size.height * .7f),
                1.8.dp.toPx(),
            )
            drawLine(
                ink,
                androidx.compose.ui.geometry.Offset(size.width * .7f, size.height * .3f),
                androidx.compose.ui.geometry.Offset(size.width * .3f, size.height * .7f),
                1.8.dp.toPx(),
            )
        }
    }
}
