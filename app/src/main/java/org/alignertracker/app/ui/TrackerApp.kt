package org.alignertracker.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import org.alignertracker.app.R
import org.alignertracker.app.reminders.ReminderScheduler

private enum class Destination(val title: Int, val glyph: String) {
    TODAY(R.string.today, "◉"), SCHEDULE(R.string.schedule, "▦"),
    HISTORY(R.string.history, "≡"), PROGRESS(R.string.progress, "↗"), SETTINGS(R.string.settings, "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(model: TrackerViewModel) {
    val snapshot by model.snapshot.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val pendingRestore by model.pendingRestore.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by androidx.compose.runtime.remember { mutableStateOf(Instant.now()) }
    var permissionEpoch by androidx.compose.runtime.remember { mutableIntStateOf(0) }
    var destinationName by rememberSaveable { mutableStateOf(Destination.TODAY.name) }
    val destination = Destination.valueOf(destinationName)
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    val resolver = context.contentResolver
    val pickerFailure = stringResource(R.string.open_settings_failed)
    fun launchIntent(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure { model.reportError(pickerFailure) }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionEpoch++
        model.reconcile()
    }
    val backupExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { model.export(resolver, it, false) }
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { model.export(resolver, it, true) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { model.inspectImport(resolver, it) }
    }
    fun openImport() {
        runCatching { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            .onFailure { model.reportError(pickerFailure) }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionEpoch++
            model.reconcile()
            while (true) { now = Instant.now(); delay(1_000) }
        }
    }
    // Read fresh after permission callbacks and each foreground resume.
    val notificationsAllowed = androidx.compose.runtime.remember(permissionEpoch) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exactAllowed = androidx.compose.runtime.remember(permissionEpoch) {
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
    val breakChannelAllowed = androidx.compose.runtime.remember(permissionEpoch) {
        context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ReminderScheduler.BREAK_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    val trayChannelAllowed = androidx.compose.runtime.remember(permissionEpoch) {
        context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ReminderScheduler.TRAY_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    BackHandler(destination != Destination.TODAY) { destinationName = Destination.TODAY.name }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(if (destination == Destination.SETTINGS) R.string.settings else R.string.app_name)) },
                navigationIcon = {
                    if (destination == Destination.SETTINGS) TextButton(onClick = { destinationName = Destination.TODAY.name }) { Text(stringResource(R.string.back)) }
                },
                actions = {
                    if (snapshot != null && destination != Destination.SETTINGS) TextButton(onClick = { destinationName = Destination.SETTINGS.name }) { Text(stringResource(R.string.settings)) }
                })
        },
        bottomBar = {
            if (snapshot?.plan != null && destination != Destination.SETTINGS) NavigationBar {
                Destination.entries.filter { it != Destination.SETTINGS }.forEach { tab ->
                    NavigationBarItem(selected = destination == tab,
                        onClick = { destinationName = tab.name },
                        icon = { Text(tab.glyph, modifier = Modifier.clearAndSetSemantics {}) },
                        label = { Text(stringResource(tab.title)) })
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (notice != null) {
                val text = when (notice!!) {
                    TrackerViewModel.Notice.SAVED -> R.string.notice_saved
                    TrackerViewModel.Notice.EXPORTED -> R.string.notice_exported
                    TrackerViewModel.Notice.RESTORED -> R.string.notice_restored
                    TrackerViewModel.Notice.DELETED -> R.string.notice_deleted
                    TrackerViewModel.Notice.REMINDER_RETRY -> R.string.notice_reminder_retry
                }
                TextButton(onClick = model::dismissMessage) { Text(stringResource(text)) }
            }
            val state = snapshot
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (state == null) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading))
                    }
                } else if (destination == Destination.SETTINGS) {
                    SettingsScreen(state.plan, preferences, busy, notificationsAllowed, exactAllowed, breakChannelAllowed, trayChannelAllowed,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else launchIntent(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        },
                        onOpenNotificationSettings = { launchIntent(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) },
                        onRequestExact = {
                            if (Build.VERSION.SDK_INT >= 31) launchIntent(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        },
                        onGoal = model::updateGoal, onReminders = model::updateReminders,
                        onExport = { confirmation = if (it) "exportCsv" else "exportBackup" },
                        onImport = ::openImport, onDelete = { confirmation = "delete" })
                } else if (state.plan == null) {
                    OnboardingScreen(busy, model::start, ::openImport)
                } else when (destination) {
                    Destination.TODAY -> TodayScreen(state, now, busy, model::setWearing)
                    Destination.SCHEDULE -> ScheduleScreen(state, busy, { confirmation = "advance" }, { confirmation = "complete" })
                    Destination.HISTORY -> HistoryScreen(state, now.truncatedTo(ChronoUnit.MINUTES), busy) { editId = it }
                    Destination.PROGRESS -> ProgressScreen(state, now.truncatedTo(ChronoUnit.MINUTES))
                    Destination.SETTINGS -> Unit
                }
            }
        }
    }
    val activeSnapshot = snapshot
    activeSnapshot?.events?.firstOrNull { it.id == editId }?.let { event ->
        EditEventDialog(activeSnapshot, event, busy, { editId = null }) { at ->
            model.updateEvent(event.id, at) { editId = null }
        }
    }
    pendingRestore?.let { restored ->
        AlertDialog(onDismissRequest = { if (!busy) model.cancelImport() },
            title = { Text(stringResource(R.string.restore_heading)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val plan = restored.plan
                    val description = if (plan == null) stringResource(R.string.restore_empty) else
                        stringResource(R.string.restore_plan, plan.currentTray, plan.totalTrays, plan.zoneId)
                    Text(stringResource(R.string.restore_summary, restored.events.size, description))
                    Text(stringResource(R.string.restore_warning))
                }
            },
            confirmButton = { TextButton(onClick = { model.confirmImport(); destinationName = Destination.TODAY.name }, enabled = !busy) { Text(stringResource(R.string.confirm_restore)) } },
            dismissButton = { TextButton(onClick = model::cancelImport, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
    }
    confirmation?.let { action ->
        val title = when (action) {
            "advance" -> stringResource(R.string.advance_title, (snapshot?.plan?.currentTray ?: 0) + 1)
            "complete" -> stringResource(R.string.complete_confirm)
            "delete" -> stringResource(R.string.delete_title)
            else -> stringResource(R.string.export_confirm)
        }
        val body = when (action) {
            "advance" -> R.string.advance_body
            "complete" -> R.string.complete_warning
            "delete" -> R.string.delete_warning
            else -> R.string.export_warning
        }
        val confirmText = when (action) {
            "advance" -> R.string.confirm_advance
            "complete" -> R.string.confirm_complete
            "delete" -> R.string.confirm_delete
            else -> R.string.choose_location
        }
        AlertDialog(onDismissRequest = { if (!busy) confirmation = null }, title = { Text(title) },
            text = { Text(stringResource(body), Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    confirmation = null
                    when (action) {
                        "advance" -> model.advance()
                        "complete" -> model.complete()
                        "delete" -> { model.clearAll(); destinationName = Destination.TODAY.name }
                        else -> runCatching {
                            if (action == "exportCsv") csvExport.launch("aligner-report.csv")
                            else backupExport.launch("aligner-backup.json")
                        }.onFailure { model.reportError(pickerFailure) }
                    }
                }) { Text(stringResource(confirmText)) }
            }, dismissButton = { TextButton(enabled = !busy, onClick = { confirmation = null }) { Text(stringResource(R.string.cancel)) } })
    }
    if (error != null) AlertDialog(onDismissRequest = model::dismissMessage,
        title = { Text(stringResource(R.string.error_title)) },
        text = { Text(stringResource(R.string.error_detail, error!!), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = model::dismissMessage) { Text(stringResource(R.string.close)) } })

}
