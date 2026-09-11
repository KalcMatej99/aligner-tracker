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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.time.Instant
import kotlinx.coroutines.delay
import org.alignertracker.app.R
import org.alignertracker.app.reminders.ReminderScheduler

internal enum class Destination(val title: Int, val glyph: String) {
    TODAY(R.string.today, "◉"),
    SCHEDULE(R.string.schedule, "▦"),
    HISTORY(R.string.history, "≡"),
    PROGRESS(R.string.progress, "↗"),
    SETTINGS(R.string.settings, ""),
    PHOTOS(R.string.photos_title, ""),
    DETAILS(R.string.treatment_details, ""),
    JOURNAL(R.string.journal_title, ""),
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
    var startupZone by rememberSaveable { mutableStateOf(java.time.ZoneId.systemDefault().id) }
    val context = LocalContext.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by androidx.compose.runtime.remember { mutableStateOf(Instant.now()) }
    var permissionEpoch by androidx.compose.runtime.remember { mutableIntStateOf(0) }
    var destinationName by rememberSaveable { mutableStateOf(Destination.TODAY.name) }
    val destination = Destination.valueOf(destinationName)
    val screenState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    var backStack by rememberSaveable { mutableStateOf(listOf<String>()) }
    fun navigate(target: Destination) {
        if (target != destination) {
            backStack =
                if (
                    target in
                        listOf(
                            Destination.TODAY,
                            Destination.SCHEDULE,
                            Destination.HISTORY,
                            Destination.PROGRESS,
                        )
                ) {
                    if (target == Destination.TODAY) emptyList() else listOf(Destination.TODAY.name)
                } else backStack + destinationName
            destinationName = target.name
        }
    }
    fun goBack() {
        destinationName = backStack.lastOrNull() ?: Destination.TODAY.name
        backStack = backStack.dropLast(1)
    }
    var moreExpanded by androidx.compose.runtime.remember { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var passwordAction by rememberSaveable { mutableStateOf<String?>(null) }
    var backupPassword by androidx.compose.runtime.remember { mutableStateOf("") }
    var repeatPassword by androidx.compose.runtime.remember { mutableStateOf("") }
    var passwordDocument by rememberSaveable { mutableStateOf<String?>(null) }
    val resolver = context.contentResolver
    val pickerFailure = stringResource(R.string.open_settings_failed)
    fun launchIntent(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure { model.reportError(pickerFailure) }
    }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionEpoch++
            model.reconcile()
        }
    val backupExport =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let { model.export(resolver, it, false) }
        }
    val csvExport =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            uri?.let { model.export(resolver, it, true) }
        }
    val encryptedExporter =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri != null) {
                passwordDocument = uri.toString()
                passwordAction = "export"
            }
        }
    val encryptedImporter =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                passwordDocument = uri.toString()
                passwordAction = "import"
            }
        }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { model.inspectImport(resolver, it) }
        }
    fun openImport() {
        runCatching {
                importer.launch(
                    arrayOf(
                        "application/json",
                        "application/zip",
                        "text/plain",
                        "application/octet-stream",
                    )
                )
            }
            .onFailure { model.reportError(pickerFailure) }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionEpoch++
            model.reconcile()
            while (true) {
                now = Instant.now()
                delay(1_000)
            }
        }
    }
    // Read fresh after permission callbacks and each foreground resume.
    val notificationsAllowed =
        androidx.compose.runtime.remember(permissionEpoch) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    val exactAllowed =
        androidx.compose.runtime.remember(permissionEpoch) {
            Build.VERSION.SDK_INT < 31 ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }
    val breakChannelAllowed =
        androidx.compose.runtime.remember(permissionEpoch) {
            context
                .getSystemService(NotificationManager::class.java)
                .getNotificationChannel(ReminderScheduler.BREAK_CHANNEL)
                ?.importance != NotificationManager.IMPORTANCE_NONE
        }
    val trayChannelAllowed =
        androidx.compose.runtime.remember(permissionEpoch) {
            context
                .getSystemService(NotificationManager::class.java)
                .getNotificationChannel(ReminderScheduler.TRAY_CHANNEL)
                ?.importance != NotificationManager.IMPORTANCE_NONE
        }
    BackHandler(backStack.isNotEmpty() || destination != Destination.TODAY) { goBack() }
    Scaffold(
        topBar = {
            AdaptiveTrackerTopBar(
                title =
                    stringResource(
                        if (snapshot?.plan == null) R.string.app_name
                        else
                            when (destination) {
                                Destination.TODAY -> R.string.wear_heading
                                Destination.SCHEDULE -> R.string.plan_heading
                                Destination.HISTORY -> R.string.history_heading
                                Destination.PROGRESS -> R.string.reports_title
                                Destination.SETTINGS -> R.string.settings
                                else -> destination.title
                            }
                    ),
                showBack =
                    destination in
                        listOf(
                            Destination.SETTINGS,
                            Destination.DETAILS,
                            Destination.JOURNAL,
                            Destination.PHOTOS,
                        ),
                onBack = ::goBack,
                actions = {
                    if (
                        snapshot?.plan != null &&
                            destination in
                                listOf(
                                    Destination.TODAY,
                                    Destination.SCHEDULE,
                                    Destination.HISTORY,
                                    Destination.PROGRESS,
                                )
                    )
                        Box {
                            androidx.compose.material3.IconButton(
                                onClick = { moreExpanded = true },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription =
                                            context.getString(R.string.more_features)
                                    },
                            ) {
                                TrackerMoreIcon()
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false },
                            ) {
                                for (target in
                                    listOf(
                                        Destination.DETAILS,
                                        Destination.JOURNAL,
                                        Destination.PHOTOS,
                                    )) androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(target.title)) },
                                    onClick = {
                                        moreExpanded = false
                                        navigate(target)
                                    },
                                )
                            }
                        }
                    if (snapshot != null && destination != Destination.SETTINGS)
                        androidx.compose.material3.IconButton(
                            onClick = { navigate(Destination.SETTINGS) },
                            modifier =
                                Modifier.semantics {
                                    contentDescription = context.getString(R.string.settings)
                                },
                        ) {
                            TrackerNavIcon(Destination.SETTINGS)
                        }
                },
            )
        },
        bottomBar = {
            if (snapshot?.plan != null && destination != Destination.SETTINGS && !keyboardVisible)
                TrackerNavigation(destination) { navigate(it) }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (notice != null) {
                val text =
                    when (notice!!) {
                        TrackerViewModel.Notice.SAVED -> R.string.notice_saved
                        TrackerViewModel.Notice.EXPORTED -> R.string.notice_exported
                        TrackerViewModel.Notice.RESTORED -> R.string.notice_restored
                        TrackerViewModel.Notice.DELETED -> R.string.notice_deleted
                        TrackerViewModel.Notice.REMINDER_RETRY -> R.string.notice_reminder_retry
                        TrackerViewModel.Notice.CLEANUP_RETRY -> R.string.notice_cleanup_retry
                    }
                OperationNotice(stringResource(text), model::dismissMessage)
            }
            val state = snapshot
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                screenState.SaveableStateProvider(
                    "$destinationName:${state?.plan?.trackingStartedAt ?: 0}"
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalScreenHeadingVisible provides
                            (state?.plan == null && destination != Destination.SETTINGS)
                    ) {
                        if (state == null) {
                            Column(
                                Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(stringResource(R.string.loading))
                            }
                        } else if (destination == Destination.SETTINGS) {
                            SettingsScreen(
                                state.plan,
                                preferences,
                                busy,
                                notificationsAllowed,
                                exactAllowed,
                                breakChannelAllowed,
                                trayChannelAllowed,
                                startupZone = startupZone,
                                onStartupZone = { startupZone = it },
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= 33)
                                        notificationPermission.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    else
                                        launchIntent(
                                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                .putExtra(
                                                    Settings.EXTRA_APP_PACKAGE,
                                                    context.packageName,
                                                )
                                        )
                                },
                                onOpenNotificationSettings = {
                                    launchIntent(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(
                                                Settings.EXTRA_APP_PACKAGE,
                                                context.packageName,
                                            )
                                    )
                                },
                                onRequestExact = {
                                    if (Build.VERSION.SDK_INT >= 31)
                                        launchIntent(
                                            Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:${context.packageName}"),
                                            )
                                        )
                                },
                                appointmentChannelAllowed =
                                    context
                                        .getSystemService(
                                            android.app.NotificationManager::class.java
                                        )
                                        .getNotificationChannel(
                                            org.alignertracker.app.reminders.ReminderScheduler
                                                .APPOINTMENT_CHANNEL
                                        )
                                        ?.importance !=
                                        android.app.NotificationManager.IMPORTANCE_NONE,
                                onGoal = model::updateGoal,
                                onDetails = { navigate(Destination.DETAILS) },
                                onReminders = model::updateReminders,
                                onExport = {
                                    confirmation = if (it) "exportCsv" else "exportBackup"
                                },
                                onImport = ::openImport,
                                onDelete = { confirmation = "delete" },
                                onEncryptedExport = {
                                    runCatching { encryptedExporter.launch("aligner-backup.atbk") }
                                        .onFailure { model.reportError(pickerFailure) }
                                },
                                onEncryptedImport = {
                                    runCatching {
                                            encryptedImporter.launch(
                                                arrayOf(
                                                    "application/octet-stream",
                                                    "application/zip",
                                                    "*/*",
                                                )
                                            )
                                        }
                                        .onFailure { model.reportError(pickerFailure) }
                                },
                            )
                        } else if (state.plan == null) {
                            OnboardingScreen(
                                busy,
                                { wearing, _ -> model.startTracking(wearing, startupZone) },
                                ::openImport,
                            )
                        } else
                            when (destination) {
                                Destination.TODAY ->
                                    TodayScreen(
                                        state,
                                        now,
                                        busy,
                                        onDetails = { navigate(Destination.DETAILS) },
                                        onToggle = model::setWearing,
                                        onCorrect = { navigate(Destination.HISTORY) },
                                    )
                                Destination.SCHEDULE ->
                                    ScheduleScreen(
                                        state,
                                        busy,
                                        { confirmation = "advance" },
                                        { confirmation = "complete" },
                                        onDetails = { navigate(Destination.DETAILS) },
                                        now = now,
                                    )
                                Destination.HISTORY ->
                                    HistoryScreen(state, now, busy) { editId = it }
                                Destination.PROGRESS ->
                                    ReportsScreen(state, model, now, preferences, busy)
                                Destination.PHOTOS -> PhotosScreen(state, model, busy)
                                Destination.DETAILS ->
                                    TreatmentDetailsScreen(state, model, busy) { goBack() }
                                Destination.JOURNAL -> JournalScreen(state, model, busy)
                                Destination.SETTINGS -> Unit
                            }
                    }
                }
            }
        }
    }
    passwordAction?.let { action ->
        val exporting = action == "export"
        TrackerDialog(
            onDismissRequest = {
                passwordAction = null
                backupPassword = ""
                repeatPassword = ""
            },
            title = {
                Text(
                    stringResource(
                        if (exporting) R.string.secure_export_title
                        else R.string.secure_import_title
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.password_recovery_notice))
                    TrackerTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it.take(1024) },
                        label = { Text(stringResource(R.string.backup_password)) },
                        isError =
                            exporting && backupPassword.isNotEmpty() && backupPassword.length < 12,
                        modifier =
                            Modifier.fieldError(
                                exporting &&
                                    backupPassword.isNotEmpty() &&
                                    backupPassword.length < 12,
                                R.string.password_short,
                            ),
                        supportingText = {
                            if (
                                exporting &&
                                    backupPassword.isNotEmpty() &&
                                    backupPassword.length < 12
                            )
                                Text(stringResource(R.string.password_short))
                        },
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    if (exporting)
                        TrackerTextField(
                            value = repeatPassword,
                            onValueChange = { repeatPassword = it.take(1024) },
                            label = { Text(stringResource(R.string.repeat_password)) },
                            isError =
                                repeatPassword.isNotEmpty() && repeatPassword != backupPassword,
                            modifier =
                                Modifier.fieldError(
                                    repeatPassword.isNotEmpty() && repeatPassword != backupPassword,
                                    R.string.password_mismatch,
                                ),
                            supportingText = {
                                if (repeatPassword.isNotEmpty() && repeatPassword != backupPassword)
                                    Text(stringResource(R.string.password_mismatch))
                            },
                            visualTransformation =
                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            singleLine = true,
                        )
                }
            },
            confirmButton = {
                TextButton(
                    enabled =
                        if (exporting)
                            backupPassword.length >= 12 && backupPassword == repeatPassword
                        else backupPassword.isNotEmpty(),
                    onClick = {
                        val document = passwordDocument?.let(Uri::parse)
                        val password = backupPassword.toCharArray()
                        passwordAction = null
                        passwordDocument = null
                        backupPassword = ""
                        repeatPassword = ""
                        if (document != null) {
                            if (exporting) model.export(resolver, document, false, password)
                            else model.inspectImport(resolver, document, password)
                        } else {
                            password.fill('\u0000')
                            model.reportError(pickerFailure)
                        }
                    },
                ) {
                    Text(stringResource(R.string.continue_backup))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        passwordAction = null
                        backupPassword = ""
                        repeatPassword = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    val activeSnapshot = snapshot
    activeSnapshot
        ?.events
        ?.firstOrNull { it.id == editId }
        ?.let { event ->
            EditEventDialog(activeSnapshot, event, busy, { editId = null }) { at ->
                model.updateEvent(event.id, at) { editId = null }
            }
        }
    pendingRestore?.let { restored ->
        TrackerDialog(
            onDismissRequest = { if (!busy) model.cancelImport() },
            title = { Text(stringResource(R.string.restore_heading)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val plan = restored.plan
                    val description =
                        if (plan == null) stringResource(R.string.restore_empty)
                        else trayLabel(plan)
                    Text(
                        stringResource(R.string.restore_summary, restored.events.size, description)
                    )
                    Text(
                        stringResource(
                            R.string.restore_expanded_counts,
                            restored.phases.size,
                            restored.notes.size,
                            restored.appointments.size,
                            restored.photos.size,
                        )
                    )
                    Text(stringResource(R.string.restore_warning))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.confirmImport()
                        destinationName = Destination.TODAY.name
                        backStack = emptyList()
                    },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.confirm_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = model::cancelImport, enabled = !busy) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    confirmation?.let { action ->
        val title =
            when (action) {
                "advance" ->
                    stringResource(R.string.advance_title, (snapshot?.plan?.currentTray ?: 0) + 1)
                "complete" -> stringResource(R.string.complete_confirm)
                "delete" -> stringResource(R.string.delete_title)
                else -> stringResource(R.string.export_confirm)
            }
        val body =
            when (action) {
                "advance" -> R.string.advance_body
                "complete" -> R.string.complete_warning
                "delete" -> R.string.delete_warning
                else -> R.string.export_warning
            }
        val confirmText =
            when (action) {
                "advance" -> R.string.confirm_advance
                "complete" -> R.string.confirm_complete
                "delete" -> R.string.confirm_delete
                else -> R.string.choose_location
            }
        TrackerDialog(
            onDismissRequest = { if (!busy) confirmation = null },
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = { Text(stringResource(body)) },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        confirmation = null
                        when (action) {
                            "advance" -> model.advance()
                            "complete" -> model.complete()
                            "delete" -> {
                                model.clearAll()
                                destinationName = Destination.TODAY.name
                                backStack = emptyList()
                            }
                            else ->
                                runCatching {
                                        if (action == "exportCsv")
                                            csvExport.launch("aligner-report.csv")
                                        else backupExport.launch("aligner-backup.zip")
                                    }
                                    .onFailure { model.reportError(pickerFailure) }
                        }
                    },
                ) {
                    Text(stringResource(confirmText))
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { confirmation = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (error != null)
        TrackerDialog(
            onDismissRequest = model::dismissMessage,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(R.string.error_detail, error!!)) },
            confirmButton = {
                TextButton(onClick = model::dismissMessage) { Text(stringResource(R.string.close)) }
            },
        )
}

/** Gives enlarged labels room without reducing the user's font scale or hiding destinations. */
@Composable
internal fun TrackerNavigation(destination: Destination, onNavigate: (Destination) -> Unit) {
    val tabs =
        listOf(Destination.TODAY, Destination.HISTORY, Destination.SCHEDULE, Destination.PROGRESS)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / fontScale < 300.dp) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tabs.chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { tab ->
                                val selected = destination == tab
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    color =
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                    contentColor =
                                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .selectable(
                                                selected = selected,
                                                role = Role.Tab,
                                                onClick = { onNavigate(tab) },
                                            )
                                            .heightIn(min = 48.dp)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            stringResource(tab.title),
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = destination == tab,
                        onClick = { onNavigate(tab) },
                        icon = { TrackerNavIcon(tab) },
                        label = { Text(stringResource(tab.title)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTrackerTopBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked =
            maxWidth / androidx.compose.ui.platform.LocalDensity.current.fontScale < 300.dp
        if (stacked) {
            Surface {
                Column(
                    Modifier.fillMaxWidth()
                        .then(
                            Modifier.windowInsetsPadding(
                                androidx.compose.material3.TopAppBarDefaults.windowInsets
                            )
                        )
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .semantics { heading() },
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showBack)
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.recordAction(stringResource(R.string.back)),
                            ) {
                                TrackerUtilityIcon(UtilityIcon.BACK)
                            }
                        actions()
                    }
                }
            }
        } else {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    if (showBack)
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.recordAction(stringResource(R.string.back)),
                        ) {
                            TrackerUtilityIcon(UtilityIcon.BACK)
                        }
                },
                actions = actions,
            )
        }
    }
}
