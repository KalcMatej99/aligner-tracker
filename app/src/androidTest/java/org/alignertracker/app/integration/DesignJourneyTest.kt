package org.alignertracker.app.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.MainActivity
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.reminders.ReminderPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real Room/ViewModel/navigation regression coverage for #33; disposable synthetic data only. */
class DesignJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container
        get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container

    @Before
    fun reset() = runBlocking {
        container.repository.clearAll()
        container.reminderSettings.update(ReminderPreferences())
        container.reminderScheduler.resetAfterRestore()
    }

    private fun ready(text: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun seed(): TrackerSnapshot {
        val now = Instant.now().toEpochMilli()
        val start = now - 3_600_000
        val snapshot =
            TrackerSnapshot(
                plan = TreatmentPlan(zoneId = "UTC", trackingStartedAt = start),
                events = listOf(WearEvent(1, start, true), WearEvent(2, now - 1_800_000, false)),
            )
        runBlocking { container.repository.replaceFromBackup(snapshot) }
        ready("Put aligners in")
        return runBlocking { container.repository.snapshot() }
    }

    @Test
    fun zeroFieldOutStartAndActivityRecreationPreserveUnknowns() {
        ready("Aligners out")
        compose.onNodeWithText("Aligners out").performScrollTo().performClick()
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        ready("Put aligners in")
        val before = runBlocking { container.repository.snapshot() }
        assertFalse(before.events.single().wearing)
        assertNull(before.plan!!.dailyGoalMinutes)
        assertNull(before.plan!!.currentTray)
        compose.activityRule.scenario.recreate()
        ready("Put aligners in")
        assertEquals(before, runBlocking { container.repository.snapshot() })
        compose.onNodeWithText("Put aligners in").assertIsDisplayed().performClick()
        ready("Take aligners out")
        assertEquals(2, runBlocking { container.repository.snapshot().events.size })
    }

    @Test
    fun correctionRouteEditsRealTransitionWithoutChangingCoverageStart() {
        val before = seed()
        compose.onNodeWithText("Forgot a switch? Correct it").assertIsDisplayed().performClick()
        compose.onNodeWithText("Edit time").performScrollTo().performClick()
        val corrected = before.events.last().at - 300_000
        val local = Instant.ofEpochMilli(corrected).atZone(ZoneId.of("UTC"))
        compose
            .onNodeWithText("Date and time (YYYY-MM-DD HH:MM:SS)")
            .performScrollTo()
            .performTextReplacement(
                local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            )
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                container.repository.snapshot().events.last().at != before.events.last().at
            }
        }
        val after = runBlocking { container.repository.snapshot() }
        assertEquals(before.events.first(), after.events.first())
        assertEquals(before.plan, after.plan)
        assertEquals(2, after.events.size)
    }

    @Test
    fun backReturnsToOriginAndPreservesOptionalDraftAfterRecreation() {
        seed()
        compose.onNodeWithText("Schedule").performClick()
        compose.onNodeWithText("Edit treatment details").performScrollTo().performClick()
        compose
            .onNodeWithText("Current tray (optional)")
            .performScrollTo()
            .performTextReplacement("3")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Your schedule").assertExists()
        compose.onNodeWithText("Edit treatment details").performScrollTo().performClick()
        compose.onNodeWithText("Current tray (optional)").performScrollTo().assertTextContains("3")
        assertNull(runBlocking { container.repository.snapshot().plan!!.currentTray })
    }

    @Test
    fun newlyStartedReportIncludesSubMinuteCoverage() {
        val start = Instant.now().toEpochMilli() - 2000
        runBlocking {
            container.repository.replaceFromBackup(
                TrackerSnapshot(
                    plan = TreatmentPlan(zoneId = "UTC", trackingStartedAt = start),
                    events = listOf(WearEvent(1, start, true)),
                )
            )
        }
        ready("Take aligners out")
        compose.onNodeWithText("Progress").performClick()
        compose.onNodeWithText("Days: 1").performClick()
        compose.onNodeWithText("1 of 1 days have recorded time").assertExists()
        compose.onNodeWithText("No recorded time. Wear is unknown.").assertDoesNotExist()
    }

    @Test
    fun reportUnknownDayIsNotDisplayedAsZeroWear() {
        seed()
        compose.onNodeWithText("Progress").performClick()
        ready("Reports")
        compose.onAllNodesWithText("No recorded time. Wear is unknown.")[0].assertExists()
        compose.onNodeWithText("Worn 0 min", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Prescribed target unavailable for this day").assertExists()
    }
}
