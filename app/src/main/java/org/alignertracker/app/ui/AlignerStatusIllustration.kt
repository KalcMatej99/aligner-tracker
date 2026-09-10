package org.alignertracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.alignertracker.app.R

/** Original editable VectorDrawables; geometry conveys state even in monochrome. */
@Composable
internal fun AlignerStatusIllustration(wearing: Boolean) {
    Image(
        painterResource(
            if (wearing) R.drawable.aligner_status_in else R.drawable.aligner_status_out
        ),
        contentDescription = stringResource(if (wearing) R.string.state_in else R.string.state_out),
        modifier = Modifier.size(width = 176.dp, height = 132.dp),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
    )
}
