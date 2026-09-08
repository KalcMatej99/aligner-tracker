package org.alignertracker.app.controls

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.alignertracker.app.MainActivity
import org.alignertracker.app.R
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.WearCommand

class TrackerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8000) { refresh(context) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TrackerWidget::class.java))
            if (ids.isEmpty()) return
            val snapshot =
                (context.applicationContext as TrackerApplication).container.repository.snapshot()
            val active = snapshot.plan != null && !snapshot.plan.completed
            val wearing = snapshot.events.lastOrNull()?.wearing == true
            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.tracker_widget)
                views.setTextViewText(
                    R.id.widget_state,
                    context.getString(
                        when {
                            snapshot.plan == null -> R.string.widget_setup
                            snapshot.plan.completed -> R.string.widget_completed
                            wearing -> R.string.widget_in
                            else -> R.string.widget_out
                        }
                    ),
                )
                views.setTextViewText(
                    R.id.widget_action,
                    context.getString(
                        if (active) {
                            if (wearing) R.string.widget_take_out else R.string.widget_put_in
                        } else R.string.widget_open
                    ),
                )
                val open =
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                views.setOnClickPendingIntent(R.id.widget_state, open)
                views.setOnClickPendingIntent(
                    R.id.widget_action,
                    if (active) TrackerControlReceiver.action(context, snapshot, !wearing) else open,
                )
                manager.updateAppWidget(id, views)
            }
        }
    }
}

/** Explicit immutable commands carry the rendered revision, never an unqualified toggle. */
class TrackerControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8000) {
                    val container = (context.applicationContext as TrackerApplication).container
                    container.clockGuard.observe()
                    container.repository.applyWearCommand(
                        WearCommand(
                            idempotencyId = intent.getStringExtra("command") ?: return@withTimeout,
                            expectedGeneration =
                                intent.getStringExtra("generation") ?: return@withTimeout,
                            expectedRevision = intent.getLongExtra("revision", -1),
                            wearing = intent.getBooleanExtra("wearing", false),
                            requestedAt = intent.getLongExtra("requested", 0),
                            source = org.alignertracker.app.domain.CommandSource.PHONE,
                        )
                    )
                    container.reminderScheduler.reconcile()
                    TrackerWidget.refresh(context)
                }
            } catch (_: Exception) {
                org.alignertracker.app.reminders.ReminderScheduler.enqueueRecovery(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION = "org.alignertracker.app.CONTROL"

        fun action(context: Context, snapshot: TrackerSnapshot, wearing: Boolean): PendingIntent {
            val identity =
                "${snapshot.stateVersion.generation}:${snapshot.stateVersion.revision}:$wearing"
            val command = UUID.nameUUIDFromBytes(identity.toByteArray()).toString()
            return PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, TrackerControlReceiver::class.java)
                    .setAction(ACTION)
                    .setData(Uri.parse("aligner-control:$command"))
                    .putExtra("command", command)
                    .putExtra("generation", snapshot.stateVersion.generation)
                    .putExtra("revision", snapshot.stateVersion.revision)
                    .putExtra("wearing", wearing)
                    .putExtra("requested", System.currentTimeMillis()),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
