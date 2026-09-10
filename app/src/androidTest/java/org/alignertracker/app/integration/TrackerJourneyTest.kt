package org.alignertracker.app.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
        compose.onNodeWithText("Aligners in").performScrollTo().performClick()
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { container.repository.snapshot().plan != null } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Take aligners out").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Take aligners out").performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().events.size == 2 }
        }
        assertFalse(runBlocking { container.repository.snapshot().events.last().wearing })
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Put aligners in").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().events.size == 3 }
        }
        val original = runBlocking { container.repository.snapshot().events }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Treatment details").performClick()
        compose
            .onNodeWithText("Current tray (optional)")
            .performScrollTo()
            .performTextReplacement("1")
        compose
            .onNodeWithText("Total trays (optional)")
            .performScrollTo()
            .performTextReplacement("2")
        compose
            .onNodeWithText("Prescribed days per tray (optional)")
            .performScrollTo()
            .performTextReplacement("7")
        compose
            .onNodeWithContentDescription("Use today for Current tray start (optional)")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Save treatment details").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.repository.snapshot().plan?.hasSchedule == true }
        }
        assertEquals(original, runBlocking { container.repository.snapshot().events })
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
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Delete all local data").performScrollTo().performClick()
        compose.onNodeWithText("Delete local records").performClick()
        compose.waitUntil(10_000) { runBlocking { container.repository.snapshot().plan == null } }
        assertTrue(runBlocking { container.repository.snapshot().events.isEmpty() })
    }
}
