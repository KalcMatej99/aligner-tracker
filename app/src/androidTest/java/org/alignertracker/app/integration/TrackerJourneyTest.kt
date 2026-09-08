package org.alignertracker.app.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.MainActivity
import org.alignertracker.app.TrackerApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real activity + ViewModel + Room journey using synthetic records only. */
class TrackerJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container
        get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container

    @Before
    fun cleanSyntheticState() = runBlocking {
        container.repository.clearAll()
        container.reminderSettings.update(org.alignertracker.app.reminders.ReminderPreferences())
        container.reminderScheduler.resetAfterRestore()
    }

    @Test
    fun setupTrackRestartAdvanceCompleteAndDelete() {
        compose.waitUntil(10_000) { runBlocking { container.repository.snapshot().plan == null } }
        compose.onNodeWithText("Total trays").performScrollTo().performTextReplacement("2")
        compose
            .onNodeWithText("Prescribed days per tray")
            .performScrollTo()
            .performTextReplacement("7")
        compose
            .onNodeWithText("Prescribed daily hours (for example 22)")
            .performScrollTo()
            .performTextReplacement("21.5")
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { container.repository.snapshot().plan != null } }
        compose.onNodeWithText("Take aligners out").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().events.size == 2 }
        }
        assertFalse(runBlocking { container.repository.snapshot().events.last().wearing })
        compose.activityRule.scenario.recreate()
        compose
            .onNodeWithText("Put aligners in")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().events.size == 3 }
        }
        compose.onNodeWithText("Schedule").performClick()
        compose.onNodeWithText("Record next tray").performScrollTo().performClick()
        compose.onNodeWithText("I changed trays").performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().plan?.currentTray == 2 }
        }
        compose.onNodeWithText("Complete treatment").performScrollTo().performClick()
        compose.onNodeWithText("Finish recording").performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().plan?.completed == true }
        }
        val completed = runBlocking { container.repository.snapshot() }
        assertTrue(completed.plan!!.completedAt != null)
        assertEquals(3, completed.events.size)
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Delete all local data").performScrollTo().performClick()
        compose.onNodeWithText("Delete local records").performClick()
        compose.waitUntil(10_000) { runBlocking { container.repository.snapshot().plan == null } }
        assertTrue(runBlocking { container.repository.snapshot().events.isEmpty() })
    }
}
