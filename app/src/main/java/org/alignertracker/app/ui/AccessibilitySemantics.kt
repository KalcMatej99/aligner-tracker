package org.alignertracker.app.ui

import androidx.annotation.StringRes
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
    TextButton(
        onClick = onDismiss,
        modifier =
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                onClick(label = dismiss, action = null)
            },
    ) {
        Text(message)
    }
}
