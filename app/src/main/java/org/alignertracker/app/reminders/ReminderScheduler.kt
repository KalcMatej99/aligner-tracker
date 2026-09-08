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
                    TRAY_CHANNEL,
                    context.getString(R.string.channel_tray),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply { lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE }
        )
    }

    suspend fun reconcile() = mutex.withLock { reconcileLocked() }

    private suspend fun reconcileLocked() {
        val snapshot = repository.snapshot()
        val prefs = settings.preferences.first()
        if (
            snapshot.plan == null ||
                snapshot.plan.completed ||
                (!prefs.enabled && !prefs.trayEnabled)
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
        val pending = ReminderRules.pending(snapshot, prefs, settings.delivered())
        for (kind in listOf("break", "tray")) {
            alarmManager.cancel(intent(kind))
            val candidate = pending.find { it.kind == kind }
            if (candidate == null) {
                // IN, completion and changed preferences clear old visible prompts too.
                if (
                    kind == "break" &&
                        (snapshot.events.lastOrNull()?.wearing != false || !prefs.enabled)
                )
                    notificationManager.cancel(code(kind))
                if (kind == "tray" && !prefs.trayEnabled) notificationManager.cancel(code(kind))
                continue
            }
            notificationManager.cancel(code(kind))
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

    suspend fun handleAlarm(kind: String, key: String) =
        mutex.withLock {
            if (kind !in listOf("break", "tray")) return@withLock
            val prefs = settings.preferences.first()
            val candidate =
                ReminderRules.pending(repository.snapshot(), prefs, settings.delivered()).find {
                    it.kind == kind && it.key == key
                }
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
                else context.getString(R.string.reminder_tray_title)
            val body =
                if (kind == "break") context.getString(R.string.reminder_break_body)
                else context.getString(R.string.reminder_tray_body)
            val notification =
                NotificationCompat.Builder(context, channel(kind))
                    .setSmallIcon(R.drawable.ic_tracker)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setContentIntent(open)
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
                notificationManager.notify(code(kind), notification)
                // Post first: a crash before this write can repeat, but cannot silently consume a
                // reminder.
                settings.markDelivered(candidate)
            } catch (_: SecurityException) {
                /* Permission revoked: do not consume. */
            }
            reconcileLocked()
        }

    fun cancelAll() {
        for (kind in listOf("break", "tray")) {
            alarmManager.cancel(intent(kind))
            notificationManager.cancel(code(kind))
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
        const val RECOVERY_WORK = "reminder_periodic_recovery"
        const val IMMEDIATE_WORK = "reminder_event_recovery"

        private fun code(kind: String) = if (kind == "break") 1001 else 1002

        private fun channel(kind: String) = if (kind == "break") BREAK_CHANNEL else TRAY_CHANNEL

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
