package org.alignertracker.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.alignertracker.app.TrackerApplication

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8000) {
                    val scheduler =
                        (context.applicationContext as TrackerApplication)
                            .container
                            .reminderScheduler
                    val kind = intent.getStringExtra("kind") ?: ""
                    val key = intent.getStringExtra("key") ?: ""
                    if (intent.action == "org.alignertracker.app.SNOOZE")
                        scheduler.snooze(kind, key)
                    else scheduler.handleAlarm(kind, key)
                }
            } catch (_: Exception) {
                ReminderScheduler.enqueueRecovery(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class ReminderRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action in
                setOf(
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
                )
        )
            ReminderScheduler.enqueueRecovery(context)
    }
}

class ReminderRecoveryWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        try {
            (applicationContext as TrackerApplication).container.reminderScheduler.reconcile()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
}
