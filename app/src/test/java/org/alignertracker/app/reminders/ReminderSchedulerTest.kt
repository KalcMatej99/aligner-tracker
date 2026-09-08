package org.alignertracker.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.data.TrackerDatabase
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.TreatmentPlan
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Exercises the scheduler against Android service shadows and real persistence. Delivery enters via
 * the payload actually scheduled with AlarmManager; these tests do not emulate Doze or OS delivery.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReminderSchedulerTest {
    private lateinit var context: Context
    private lateinit var database: TrackerDatabase
    private lateinit var repository: TrackerRepository
    private lateinit var settings: ReminderSettings
    private lateinit var scheduler: ReminderScheduler
    private lateinit var alarms: AlarmManager
    private lateinit var notifications: NotificationManager
    private lateinit var clock: MutableClock

    @Before
    fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        clock = MutableClock(Instant.ofEpochMilli(System.currentTimeMillis()))
        database = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        repository = TrackerRepository(database, clock)
        settings = ReminderSettings(context)
        settings.update(ReminderPreferences())
        settings.clearDelivered()
        alarms = context.getSystemService(AlarmManager::class.java)
        notifications = context.getSystemService(NotificationManager::class.java)
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(notifications).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        try {
            WorkManager.getInstance(context)
        } catch (_: IllegalStateException) {
            // Some Robolectric manifests omit AndroidX Startup's provider initialization.
            WorkManager.initialize(context, Configuration.Builder().build())
        }
        scheduler = ReminderScheduler(context, repository, settings)
    }

    @After
    fun close() {
        if (::scheduler.isInitialized) scheduler.cancelAll()
        if (::database.isInitialized) database.close()
    }

    @Test
    fun `reconciliation and repeated callback do not repost a delivered break`() = runBlocking {
        startBreak()
        val payload = payload("break")
        deliver(payload)
        val posted = shadowOf(notifications).allNotifications.single()
        assertTrue(scheduled().isEmpty())

        // A fresh service object must use the persisted marker, rather than an in-memory flag.
        scheduler = ReminderScheduler(context, repository, settings)
        scheduler.reconcile()
        deliver(payload)
        assertSame(posted, shadowOf(notifications).allNotifications.single())
        assertTrue(scheduled().isEmpty())
    }

    @Test
    fun `putting aligners in cancels a pending break alarm`() = runBlocking {
        startBreak(ReminderPreferences(enabled = true, breakMinutes = 30))
        assertEquals(1, scheduled().size)
        repository.setWearing(true)
        scheduler.reconcile()
        assertTrue(scheduled().isEmpty())
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    @Test
    fun `putting aligners in dismisses the delivered break notification`() = runBlocking {
        startBreak()
        deliver(payload("break"))
        assertEquals(1, shadowOf(notifications).size())
        repository.setWearing(true)
        scheduler.reconcile()
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
        assertTrue(scheduled().isEmpty())
    }

    @Test
    fun `completion clears a visible break and a pending tray reminder`() = runBlocking {
        startBreak(ReminderPreferences(enabled = true, breakMinutes = 1, trayEnabled = true))
        deliver(payload("break"))
        assertEquals(1, shadowOf(notifications).size())
        assertEquals("tray", payload("tray").getStringExtra("kind"))
        repository.completeTreatment()
        scheduler.reconcile()
        assertTrue(scheduled().isEmpty())
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    @Test
    fun `deleting history clears alarms notifications and persisted delivery identities`() =
        runBlocking {
            startBreak(ReminderPreferences(enabled = true, breakMinutes = 1, trayEnabled = true))
            val oldPayload = payload("break")
            deliver(oldPayload)
            assertFalse(settings.delivered().isEmpty())
            repository.clearAll()
            settings.update(ReminderPreferences())
            scheduler.resetAfterRestore()
            deliver(oldPayload)
            assertTrue(scheduled().isEmpty())
            assertTrue(shadowOf(notifications).allNotifications.isEmpty())
            assertTrue(settings.delivered().isEmpty())
        }

    @Test
    fun `callback from a previous break cannot notify for the current break`() = runBlocking {
        startBreak()
        val stalePayload = payload("break")
        repository.setWearing(true)
        clock.value = clock.value.plusMillis(1)
        repository.setWearing(false)
        scheduler.reconcile()
        val currentPayload = payload("break")
        assertNotEquals(stalePayload.getStringExtra("key"), currentPayload.getStringExtra("key"))
        deliver(stalePayload)
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
        assertTrue(settings.delivered().isEmpty())
        assertEquals(currentPayload.getStringExtra("key"), payload("break").getStringExtra("key"))
    }

    @Test
    fun `notification permission denial does not consume a due reminder`() = runBlocking {
        startBreak()
        val pending = payload("break")
        shadowOf(RuntimeEnvironment.getApplication())
            .denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        deliver(pending)
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
        assertTrue(settings.delivered().isEmpty())
        assertTrue(scheduled().isEmpty())

        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        scheduler.reconcile()
        deliver(payload("break"))
        assertEquals(1, shadowOf(notifications).size())
    }

    @Test
    fun `system notification switch prevents scheduling without consuming reminder`() = runBlocking {
        startBreak()
        val pending = payload("break")
        shadowOf(notifications).setNotificationsEnabled(false)
        deliver(pending)
        assertTrue(scheduled().isEmpty())
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
        assertTrue(settings.delivered().isEmpty())
    }

    @Test
    fun `blocked break channel prevents delivery while tray channel remains scheduled`() =
        runBlocking {
            startBreak(ReminderPreferences(enabled = true, breakMinutes = 1, trayEnabled = true))
            val pending = payload("break")
            notifications.createNotificationChannel(
                NotificationChannel(
                    ReminderScheduler.BREAK_CHANNEL,
                    "Break reminders",
                    NotificationManager.IMPORTANCE_NONE,
                )
            )
            assertEquals(
                NotificationManager.IMPORTANCE_NONE,
                notifications.getNotificationChannel(ReminderScheduler.BREAK_CHANNEL).importance,
            )
            deliver(pending)
            assertTrue(shadowOf(notifications).allNotifications.isEmpty())
            assertTrue(settings.delivered().isEmpty())
            assertEquals(listOf("tray"), scheduled().map { alarmPayload(it).getStringExtra("kind") })
        }

    @Test
    fun `precise alarms require both explicit opt in and platform grant`() = runBlocking {
        val preferences = ReminderPreferences(enabled = true, breakMinutes = 30)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        startBreak(preferences)
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, scheduled().single().windowLengthMs)

        settings.update(preferences.copy(precise = true))
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.reconcile()
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, scheduled().single().windowLengthMs)

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.reconcile()
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, scheduled().single().windowLengthMs)
        assertTrue(scheduled().single().isAllowWhileIdle)
    }

    private suspend fun startBreak(
        preferences: ReminderPreferences = ReminderPreferences(enabled = true, breakMinutes = 1)
    ) {
        val started = clock.instant().minusSeconds(120)
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
            wearing = false,
        )
        settings.update(preferences)
        scheduler.reconcile()
    }

    private fun scheduled(): List<ShadowAlarmManager.ScheduledAlarm> =
        shadowOf(alarms).scheduledAlarms.filter {
            it.operation != null &&
                alarmPayload(it).action?.startsWith("org.alignertracker.app.REMINDER_") == true
        }

    // Copy the payload: PendingIntent FLAG_UPDATE_CURRENT deliberately replaces stored extras.
    private fun alarmPayload(alarm: ShadowAlarmManager.ScheduledAlarm): Intent =
        Intent(shadowOf(requireNotNull(alarm.operation)).savedIntent)

    private fun payload(kind: String): Intent =
        scheduled().map(::alarmPayload).single { it.getStringExtra("kind") == kind }

    private suspend fun deliver(payload: Intent) {
        scheduler.handleAlarm(
            requireNotNull(payload.getStringExtra("kind")),
            requireNotNull(payload.getStringExtra("key")),
        )
    }

    private class MutableClock(var value: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = fixed(value, zone)
        override fun instant(): Instant = value
    }
}
