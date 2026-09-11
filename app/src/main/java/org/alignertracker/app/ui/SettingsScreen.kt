package org.alignertracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import org.alignertracker.app.R
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.reminders.ReminderPreferences

@Composable
fun SettingsScreen(
    plan: TreatmentPlan?,
    preferences: ReminderPreferences,
    busy: Boolean,
    notificationsAllowed: Boolean,
    exactAllowed: Boolean,
    breakChannelAllowed: Boolean,
    trayChannelAllowed: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestExact: () -> Unit,
    onGoal: (Int) -> Unit,
    onReminders: (ReminderPreferences) -> Unit,
    onExport: (Boolean) -> Unit,
    onImport: () -> Unit,
    onDelete: () -> Unit,
    onEncryptedExport: () -> Unit = {},
    onEncryptedImport: () -> Unit = {},
    appointmentChannelAllowed: Boolean = true,
    onDetails: () -> Unit = {},
    startupZone: String = java.time.ZoneId.systemDefault().id,
    onStartupZone: (String) -> Unit = {},
) {
    var choosingZone by rememberSaveable { mutableStateOf(false) }
    var zoneQuery by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable(preferences.enabled) { mutableStateOf(preferences.enabled) }
    var trayEnabled by
        rememberSaveable(preferences.trayEnabled) { mutableStateOf(preferences.trayEnabled) }
    var precise by rememberSaveable(preferences.precise) { mutableStateOf(preferences.precise) }
    var delay by
        rememberSaveable(preferences.breakMinutes) {
            mutableStateOf(preferences.breakMinutes.toString())
        }
    var delayError by rememberSaveable { mutableStateOf(false) }
    ScreenColumn {
        Heading(R.string.settings_heading)
        if (plan != null) TreatmentSettingsLink(onDetails)
        if (plan != null && !plan.completed)
            ExpandableBlock(
                R.string.reminder_heading,
                stringResource(
                    if (notificationsAllowed) R.string.notifications_allowed
                    else R.string.notifications_blocked
                ),
                Destination.TODAY,
            ) {
                Text(stringResource(R.string.reminder_staged))
                ToggleRow(stringResource(R.string.break_reminder), enabled, !busy) {
                    enabled = it
                    if (it && !notificationsAllowed) onRequestNotifications()
                }
                Entry(
                    delay,
                    {
                        delay = it
                        delayError = false
                    },
                    R.string.break_delay,
                    numeric = true,
                    error = delayError,
                    errorMessage = if (delayError) R.string.reminder_delay_invalid else null,
                    enabled = !busy,
                )
                ToggleRow(stringResource(R.string.tray_reminder), trayEnabled, !busy) {
                    trayEnabled = it
                    if (it && !notificationsAllowed) onRequestNotifications()
                }
                Text(stringResource(R.string.tray_reminder_help))
                ToggleRow(stringResource(R.string.precise_reminders), precise, !busy) { value ->
                    precise = value
                    if (value && !exactAllowed) onRequestExact()
                }
                Text(stringResource(R.string.precise_help))
                Button(
                    shape = androidx.compose.material3.MaterialTheme.shapes.small,
                    onClick = {
                        val minutes = parseUserInteger(delay)
                        delayError = minutes == null || minutes !in 1..240
                        if (!delayError)
                            onReminders(
                                preferences.copy(
                                    enabled = enabled,
                                    breakMinutes = minutes!!,
                                    trayEnabled = trayEnabled,
                                    precise = precise,
                                    streaksEnabled = preferences.streaksEnabled,
                                )
                            )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.reminder_save))
                }
                Text(
                    stringResource(
                        if (notificationsAllowed) R.string.notifications_allowed
                        else R.string.notifications_blocked
                    )
                )
                if (enabled)
                    Text(
                        stringResource(
                            if (breakChannelAllowed) R.string.break_channel_allowed
                            else R.string.break_channel_blocked
                        )
                    )
                if (trayEnabled)
                    Text(
                        stringResource(
                            if (trayChannelAllowed) R.string.tray_channel_allowed
                            else R.string.tray_channel_blocked
                        )
                    )
                Text(
                    stringResource(
                        if (appointmentChannelAllowed) R.string.appointment_channel_allowed
                        else R.string.appointment_channel_blocked
                    )
                )
                if (!notificationsAllowed)
                    TextButton(onClick = onRequestNotifications) {
                        Text(stringResource(R.string.enable_notifications))
                    }
                TextButton(onClick = onOpenNotificationSettings) {
                    Text(stringResource(R.string.notifications_settings))
                }
                if (precise) {
                    Text(
                        stringResource(
                            if (exactAllowed) R.string.precise_available
                            else R.string.precise_unavailable
                        )
                    )
                    if (!exactAllowed)
                        TextButton(onClick = onRequestExact) {
                            Text(stringResource(R.string.precise_settings))
                        }
                }
                Text(
                    stringResource(R.string.reminder_limits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        ExpandableBlock(
            R.string.treatment_zone,
            readableZone(plan?.zoneId ?: startupZone),
            Destination.HISTORY,
        ) {
            Text(stringResource(R.string.zone_value, readableZone(plan?.zoneId ?: startupZone)))
            Text(stringResource(R.string.fixed_zone_help))
            if (plan == null) {
                TextButton(onClick = { choosingZone = !choosingZone }) {
                    Text(stringResource(R.string.change_accounting_zone))
                }
                if (choosingZone) {
                    Entry(zoneQuery, { zoneQuery = it }, R.string.search_zones)
                    java.time.ZoneId.getAvailableZoneIds()
                        .sorted()
                        .filter { it.replace('_', ' ').contains(zoneQuery, ignoreCase = true) }
                        .take(40)
                        .forEach { id ->
                            TextButton(
                                onClick = {
                                    onStartupZone(id)
                                    choosingZone = false
                                }
                            ) {
                                Text(readableZone(id))
                            }
                        }
                }
            }
        }
        ExpandableBlock(
            R.string.data_heading,
            stringResource(R.string.support_data_hint),
            Destination.SETTINGS,
        ) {
            Text(stringResource(R.string.data_help))
            if (plan != null) {
                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { onExport(false) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.export_backup))
                }
                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = onEncryptedExport,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.export_encrypted_backup))
                }
                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = { onExport(true) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.export_csv))
                }
            }
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = onImport,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.import_backup))
            }
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = onEncryptedImport,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.import_encrypted_backup))
            }
            if (plan != null)
                TextButton(
                    onClick = onDelete,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(R.string.delete_data),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
        }
        ExpandableBlock(
            R.string.watch_information,
            stringResource(R.string.support_watch_hint),
            Destination.SETTINGS,
        ) {
            Text(
                stringResource(R.string.watch_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(stringResource(R.string.privacy_footer), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChecked,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun TreatmentSettingsLink(onDetails: () -> Unit) {
    androidx.compose.material3.OutlinedCard(
        onClick = onDetails,
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.treatment_details),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.support_plan_edit_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
