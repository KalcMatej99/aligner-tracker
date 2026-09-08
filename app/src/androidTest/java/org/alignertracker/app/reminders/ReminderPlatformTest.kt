package org.alignertracker.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.alignertracker.app.R
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.domain.TreatmentPlan
import org.junit.Assert.*
import org.junit.Test

/** Real Android notification and immutable actions, with synthetic local treatment records. */
@SdkSuppress(minSdkVersion = 33)
class ReminderPlatformTest {
    @Test
    fun dueReminderIsPrivateAndActualSnoozeInAndOldActionsRespectPersistedState() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TrackerApplication>()
        val container = app.container
        val repository = container.repository
        val settings = container.reminderSettings
        val scheduler = container.reminderScheduler
        val notifications = app.getSystemService(NotificationManager::class.java)
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val originalPreferences = settings.preferences.first()
        // Revoking a runtime permission stops the app. The external emulator harness grants and
        // restores this permission around instrumentation; this test never changes grant state.
        assertEquals(
            "Emulator harness must grant POST_NOTIFICATIONS before instrumentation",
            PackageManager.PERMISSION_GRANTED,
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS),
        )
        try {
            scheduler.cancelAll()
            repository.clearAll()
            settings.clearDelivered()
            val started = Instant.now().minusSeconds(120)
            val date = started.atZone(ZoneOffset.UTC).toLocalDate().toString()
            repository.start(
                TreatmentPlan(
                    startDate = date,
                    totalTrays = 3,
                    currentTray = 1,
                    daysPerTray = 7,
                    currentTrayStartedOn = date,
                    dailyGoalMinutes = 1200,
                    zoneId = "UTC",
                    trackingStartedAt = started.toEpochMilli(),
                ),
                false,
            )
            val outBackup = repository.snapshot()
            val preferences =
                ReminderPreferences(
                    enabled = true,
                    breakMinutes = 1,
                    precise = alarmManager.canScheduleExactAlarms(),
                )
            settings.update(preferences)
            scheduler.reconcile()
            val fromAlarm =
                withTimeoutOrNull(30_000) {
                    while (
                        notifications.activeNotifications.none {
                            it.notification.channelId == ReminderScheduler.BREAK_CHANNEL
                        }
                    ) delay(100)
                    true
                } == true
            if (!fromAlarm) {
                // Inexact batching is a platform policy, not a transport success. Exercise the real
                // receiver using its public scheduled payload and label this fallback in results.
                val due =
                    ReminderRules.pending(repository.snapshot(), preferences).single {
                        it.kind == "break"
                    }
                app.sendBroadcast(
                    Intent(app, ReminderReceiver::class.java)
                        .setAction("org.alignertracker.app.REMINDER_break")
                        .putExtra("kind", due.kind)
                        .putExtra("key", due.key)
                )
            }
            InstrumentationRegistry.getInstrumentation()
                .sendStatus(
                    0,
                    Bundle().apply {
                        putString(
                            "reminder_delivery",
                            if (fromAlarm) "AlarmManager" else "explicit receiver fallback",
                        )
                    },
                )
            withTimeout(10_000) {
                while (
                    notifications.activeNotifications.none {
                        it.notification.channelId == ReminderScheduler.BREAK_CHANNEL
                    }
                ) delay(50)
            }
            val notification =
                notifications.activeNotifications
                    .single { it.notification.channelId == ReminderScheduler.BREAK_CHANNEL }
                    .notification
            assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
            assertNotNull(notification.publicVersion)
            val publicNotification = notification.publicVersion
            assertEquals(
                app.getString(R.string.reminder_private_title),
                publicNotification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            )
            assertNull(publicNotification.extras.getCharSequence(Notification.EXTRA_TEXT))
            assertNull(publicNotification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
            assertTrue(publicNotification.actions.isNullOrEmpty())
            val putIn =
                notification.actions
                    .single { it.title.toString() == app.getString(R.string.widget_put_in) }
                    .actionIntent
            val snooze =
                notification.actions
                    .single { it.title.toString() == app.getString(R.string.reminder_snooze) }
                    .actionIntent
            snooze.send()
            withTimeout(10_000) {
                while (
                    notifications.activeNotifications.any {
                        it.notification.channelId == ReminderScheduler.BREAK_CHANNEL
                    }
                ) delay(50)
            }
            val persistedSettings = ReminderSettings(app)
            val base =
                ReminderRules.pending(repository.snapshot(), preferences).single {
                    it.kind == "break"
                }
            val snoozed = persistedSettings.snoozed(base)
            assertTrue(
                "Snooze must persist a future due time",
                snoozed.dueAt > System.currentTimeMillis() + 9 * 60_000,
            )
            scheduler.reconcile()
            delay(1500)
            assertTrue(
                "Snoozing must not immediately repost",
                notifications.activeNotifications.none {
                    it.notification.channelId == ReminderScheduler.BREAK_CHANNEL
                },
            )
            assertEquals(1, repository.snapshot().events.size)
            putIn.send()
            withTimeout(10_000) { while (!repository.snapshot().events.last().wearing) delay(50) }
            assertEquals(2, repository.snapshot().events.size)
            putIn.send()
            delay(500)
            assertEquals(
                "Repeated IN action must be idempotent",
                2,
                repository.snapshot().events.size,
            )
            assertTrue(
                notifications.activeNotifications.none {
                    it.notification.channelId == ReminderScheduler.BREAK_CHANNEL
                }
            )

            settings.update(ReminderPreferences())
            repository.replaceFromBackup(outBackup)
            scheduler.resetAfterRestore()
            val restored = repository.snapshot()
            assertNotEquals(outBackup.stateVersion.generation, restored.stateVersion.generation)
            putIn.send()
            delay(500)
            assertEquals(restored.events, repository.snapshot().events)
            assertFalse(repository.snapshot().events.last().wearing)
            repository.clearAll()
            scheduler.resetAfterRestore()
            putIn.send()
            snooze.send()
            delay(500)
            assertNull(repository.snapshot().plan)
            assertTrue(repository.snapshot().events.isEmpty())
            assertTrue(notifications.activeNotifications.isEmpty())
        } finally {
            scheduler.cancelAll()
            repository.clearAll()
            settings.clearDelivered()
            settings.update(originalPreferences)
        }
    }
}
