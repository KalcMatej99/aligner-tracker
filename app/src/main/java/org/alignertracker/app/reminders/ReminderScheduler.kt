package org.alignertracker.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.alignertracker.app.MainActivity
import org.alignertracker.app.R
import org.alignertracker.app.data.TrackerRepository

class ReminderScheduler(
    context: Context,
    private val repository: TrackerRepository,
    private val settings: ReminderSettings,
) {
    private val context = context.applicationContext
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val mutex = Mutex()
    private val companionScope =
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
    private var companionUpdate: kotlinx.coroutines.Job? = null

    init {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                    BREAK_CHANNEL,
                    context.getString(R.string.channel_break),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply { lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                    APPOINTMENT_CHANNEL,
                    context.getString(R.string.channel_appointments),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply { lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                    TRAY_CHANNEL,
                    context.getString(R.string.channel_tray),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply { lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE }
        )
    }

    suspend fun reconcile() {
        mutex.withLock { reconcileLocked() }
        // Optional watch delivery is a bounded one-off task; it never holds the phone UI busy.
        val application = context.applicationContext as? org.alignertracker.app.TrackerApplication
        if (application != null) {
            companionUpdate?.cancel()
            companionUpdate =
                companionScope.launch {
                    kotlinx.coroutines.withTimeoutOrNull(2000) {
                        try {
                            application.container.wearBridge.publishStatusToNearbyWearNodes()
                        } catch (exception: Exception) {
                            if (exception is kotlinx.coroutines.CancellationException)
                                throw exception
                        }
                    }
                }
        }
    }

    private suspend fun reconcileLocked() {
        (context.applicationContext as? org.alignertracker.app.TrackerApplication)
            ?.container
            ?.clockGuard
            ?.observe()
        val snapshot = repository.snapshot()
        try {
            org.alignertracker.app.controls.TrackerWidget.refresh(context)
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) throw exception
        }
        val prefs = settings.preferences.first()
        if (
            snapshot.plan == null ||
                ((snapshot.plan.completed || (!prefs.enabled && !prefs.trayEnabled)) &&
                    snapshot.appointments.none {
                        !it.completed && it.reminderMinutesBefore != null
                    })
        ) {
            cancelAll()
            return
        }
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                RECOVERY_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderRecoveryWorker>(12, TimeUnit.HOURS).build(),
            )
        val pending = candidates(snapshot, prefs)
        for (kind in listOf("break", "tray", "appointment")) {
            alarmManager.cancel(intent(kind))
            if (kind == "appointment") {
                val validKeys =
                    candidates(snapshot, prefs, true)
                        .filter { it.kind == kind }
                        .map { it.key }
                        .toSet()
                notificationManager.activeNotifications
                    .filter { it.id == code(kind) }
                    .forEach { visible ->
                        if (
                            visible.notification.extras.getString("tracker_reminder_key") !in
                                validKeys
                        )
                            notificationManager.cancel(visible.tag, visible.id)
                    }
            }
            val candidate = pending.find { it.kind == kind }
            if (candidate == null) {
                // IN, completion and changed preferences clear old visible prompts too.
                if (
                    kind == "break" &&
                        (snapshot.events.lastOrNull()?.wearing != false || !prefs.enabled)
                )
                    notificationManager.cancel(code(kind))
                if (kind == "tray" && !prefs.trayEnabled) notificationManager.cancel(code(kind))
                if (
                    kind == "appointment" &&
                        ReminderRules.pending(snapshot, prefs).none { it.kind == "appointment" }
                )
                    cancelVisible(kind)
                continue
            }
            if (kind != "appointment") notificationManager.cancel(code(kind))
            if (!canNotify(kind)) continue
            val whenAt = candidate.dueAt.coerceAtLeast(System.currentTimeMillis() + 1000)
            val pendingIntent = intent(kind, candidate.key)
            val exactAllowed = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
            if (prefs.precise && exactAllowed) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        whenAt,
                        pendingIntent,
                    )
                    continue
                } catch (_: SecurityException) {
                    /* Grant may be revoked between check and call. */
                }
            }
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pendingIntent)
        }
    }

    private suspend fun candidates(
        snapshot: org.alignertracker.app.domain.TrackerSnapshot,
        prefs: ReminderPreferences,
        includeDelivered: Boolean = false,
    ): List<ReminderCandidate> {
        val delivered = if (includeDelivered) emptySet() else settings.delivered()
        return ReminderRules.pending(snapshot, prefs)
            .map { settings.snoozed(it) }
            .filter { it.key !in delivered }
            .sortedBy { it.dueAt }
    }

    suspend fun snooze(kind: String, key: String) =
        mutex.withLock {
            val snapshot = repository.snapshot()
            val prefs = settings.preferences.first()
            val candidate =
                candidates(snapshot, prefs, true).firstOrNull { it.kind == kind && it.key == key }
                    ?: return@withLock
            val base =
                ReminderRules.pending(snapshot, prefs).firstOrNull {
                    it.kind == kind && settings.snoozed(it).key == key
                } ?: return@withLock
            if (candidate.dueAt > System.currentTimeMillis()) return@withLock
            settings.snooze(kind, base.key, System.currentTimeMillis() + 10 * 60_000)
            notificationManager.cancel(if (kind == "appointment") key else null, code(kind))
            reconcileLocked()
        }

    suspend fun handleAlarm(kind: String, key: String) =
        mutex.withLock {
            if (kind !in listOf("break", "tray", "appointment")) return@withLock
            val prefs = settings.preferences.first()
            val candidate =
                candidates(repository.snapshot(), prefs).find { it.kind == kind && it.key == key }
            if (
                candidate == null ||
                    candidate.dueAt > System.currentTimeMillis() ||
                    !canNotify(kind)
            ) {
                reconcileLocked()
                return@withLock
            }
            val open =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val title =
                if (kind == "break") context.getString(R.string.reminder_break_title)
                else
                    context.getString(
                        if (kind == "appointment") R.string.reminder_appointment_title
                        else R.string.reminder_tray_title
                    )
            val body =
                if (kind == "break") context.getString(R.string.reminder_break_body)
                else
                    context.getString(
                        if (kind == "appointment") R.string.reminder_appointment_body
                        else R.string.reminder_tray_body
                    )
            val snapshot = repository.snapshot()
            val notification =
                NotificationCompat.Builder(context, channel(kind))
                    .setSmallIcon(R.drawable.ic_tracker)
                    .addExtras(
                        android.os.Bundle().apply {
                            putString("tracker_reminder_key", candidate.key)
                        }
                    )
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setContentIntent(open)
                    .apply {
                        if (snapshot.plan?.completed == false)
                            addAction(
                                0,
                                context.getString(
                                    if (snapshot.events.lastOrNull()?.wearing == true)
                                        R.string.widget_take_out
                                    else R.string.widget_put_in
                                ),
                                org.alignertracker.app.controls.TrackerControlReceiver.action(
                                    context,
                                    snapshot,
                                    snapshot.events.lastOrNull()?.wearing != true,
                                ),
                            )
                    }
                    .addAction(
                        0,
                        context.getString(R.string.reminder_snooze),
                        PendingIntent.getBroadcast(
                            context,
                            code(kind),
                            Intent(context, ReminderReceiver::class.java)
                                .setAction("org.alignertracker.app.SNOOZE")
                                .setData(
                                    android.net.Uri.parse(
                                        "aligner-snooze:" + android.net.Uri.encode(candidate.key)
                                    )
                                )
                                .putExtra("kind", kind)
                                .putExtra("key", candidate.key),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(
                        NotificationCompat.Builder(context, channel(kind))
                            .setSmallIcon(R.drawable.ic_tracker)
                            .setContentTitle(context.getString(R.string.reminder_private_title))
                            .build()
                    )
                    .build()
            try {
                notificationManager.notify(
                    if (kind == "appointment") candidate.key else null,
                    code(kind),
                    notification,
                )
                // Post first: a crash before this write can repeat, but cannot silently consume a
                // reminder.
                settings.markDelivered(candidate)
            } catch (_: SecurityException) {
                /* Permission revoked: do not consume. */
            }
            reconcileLocked()
        }

    private fun cancelVisible(kind: String) {
        notificationManager.activeNotifications
            .filter { it.id == code(kind) }
            .forEach { notificationManager.cancel(it.tag, it.id) }
        notificationManager.cancel(code(kind))
    }

    fun cancelAll() {
        for (kind in listOf("break", "tray", "appointment")) {
            alarmManager.cancel(intent(kind))
            cancelVisible(kind)
        }
        WorkManager.getInstance(context).cancelUniqueWork(RECOVERY_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK)
    }

    suspend fun resetAfterRestore() =
        mutex.withLock {
            cancelAll()
            settings.clearDelivered()
            reconcileLocked()
        }

    private fun canNotify(kind: String): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            notificationManager.getNotificationChannel(channel(kind))?.importance !=
                NotificationManager.IMPORTANCE_NONE

    private fun intent(kind: String, key: String = ""): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            code(kind),
            Intent(context, ReminderReceiver::class.java)
                .setAction("org.alignertracker.app.REMINDER_$kind")
                .putExtra("kind", kind)
                .putExtra("key", key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val BREAK_CHANNEL = "break_reminders"
        const val TRAY_CHANNEL = "tray_reminders"
        const val APPOINTMENT_CHANNEL = "appointment_reminders"
        const val RECOVERY_WORK = "reminder_periodic_recovery"
        const val IMMEDIATE_WORK = "reminder_event_recovery"

        private fun code(kind: String) =
            when (kind) {
                "break" -> 1001
                "tray" -> 1002
                else -> 1003
            }

        private fun channel(kind: String) =
            when (kind) {
                "break" -> BREAK_CHANNEL
                "tray" -> TRAY_CHANNEL
                else -> APPOINTMENT_CHANNEL
            }

        fun enqueueRecovery(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<ReminderRecoveryWorker>().build(),
                )
        }
    }
}
