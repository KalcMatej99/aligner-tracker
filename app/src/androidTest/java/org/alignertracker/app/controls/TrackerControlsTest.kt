package org.alignertracker.app.controls

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.domain.TreatmentPlan
import org.junit.Assert.*
import org.junit.Test

class TrackerControlsTest {
    @Test
    fun boundWidgetRendersAndActualPendingIntentRejectsDuplicateAndStaleCommands() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TrackerApplication>()
        val repository = app.container.repository
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val user =
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand("am get-current-user")
                )
                .use { it.readBytes().toString(Charsets.UTF_8).trim().toInt() }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "appwidget grantbind --package org.alignertracker.app --user $user"
                )
            )
            .use { it.readBytes() }
        val host = AppWidgetHost(app, 101)
        val id = host.allocateAppWidgetId()
        try {
            repository.clearAll()
            val now = Instant.now().minusSeconds(60)
            val date = now.atZone(ZoneOffset.UTC).toLocalDate().toString()
            repository.start(
                TreatmentPlan(
                    startDate = date,
                    totalTrays = 2,
                    currentTray = 1,
                    daysPerTray = 7,
                    currentTrayStartedOn = date,
                    dailyGoalMinutes = 1200,
                    zoneId = "UTC",
                    trackingStartedAt = now.toEpochMilli(),
                ),
                true,
            )
            assertTrue(
                AppWidgetManager.getInstance(app)
                    .bindAppWidgetIdIfAllowed(id, ComponentName(app, TrackerWidget::class.java))
            )
            TrackerWidget.refresh(app)
            assertNotNull(AppWidgetManager.getInstance(app).getAppWidgetInfo(id))
            val oldState = repository.snapshot()
            val out = TrackerControlReceiver.action(app, oldState, false)
            out.send()
            withTimeout(10_000) { while (repository.snapshot().events.last().wearing) delay(25) }
            assertEquals(2, repository.snapshot().events.size)
            out.send()
            delay(250)
            assertEquals(2, repository.snapshot().events.size)
            val stale = TrackerControlReceiver.action(app, repository.snapshot(), true)
            repository.updateGoal(1100)
            stale.send()
            delay(250)
            assertFalse(repository.snapshot().events.last().wearing)
            assertEquals(2, repository.snapshot().events.size)
            repository.clearAll()
            out.send()
            delay(250)
            assertNull(repository.snapshot().plan)
        } finally {
            host.deleteAppWidgetId(id)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand(
                        "appwidget revokebind --package org.alignertracker.app --user $user"
                    )
                )
                .use { it.readBytes() }
            repository.clearAll()
        }
    }
}
