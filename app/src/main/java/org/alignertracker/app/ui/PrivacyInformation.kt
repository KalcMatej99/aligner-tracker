package org.alignertracker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.alignertracker.app.R

/** Offline information: no browser, permission request or treatment mutation. */
@Composable
internal fun PrivacyInformation() {
    var page by rememberSaveable { mutableStateOf("") }
    OutlinedButton(onClick = { page = "privacy" }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.publishing_privacy))
    }
    TextButton(onClick = { page = "health" }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.publishing_health))
    }
    if (page.isNotEmpty()) {
        val privacy = page == "privacy"
        Dialog(
            onDismissRequest = { page = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.safeDrawingPadding().padding(horizontal = 20.dp)) {
                    TextButton(onClick = { page = "" }) {
                        Text(stringResource(R.string.publishing_close))
                    }
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Text(
                                stringResource(
                                    if (privacy) R.string.publishing_privacy
                                    else R.string.publishing_health
                                ),
                                Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        if (privacy) {
                            item { Text(stringResource(R.string.publishing_updated)) }
                            val sections =
                                listOf(
                                    R.string.policy_owner_title to R.string.policy_owner_body,
                                    R.string.policy_records_title to R.string.policy_records_body,
                                    R.string.policy_photos_title to R.string.policy_photos_body,
                                    R.string.policy_exports_title to R.string.policy_exports_body,
                                    R.string.policy_watch_title to R.string.policy_watch_body,
                                    R.string.policy_permissions_title to
                                        R.string.policy_permissions_body,
                                    R.string.policy_deletion_title to R.string.policy_deletion_body,
                                    R.string.policy_contact_title to R.string.policy_contact_body,
                                )
                            sections.forEach { (title, body) ->
                                item {
                                    Text(
                                        stringResource(title),
                                        Modifier.semantics { heading() },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                item { Text(stringResource(body)) }
                            }
                        } else {
                            item { Text(stringResource(R.string.publishing_health_body)) }
                            item { Text(stringResource(R.string.publishing_health_advice)) }
                        }
                    }
                }
            }
        }
    }
}
