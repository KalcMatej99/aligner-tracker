package org.alignertracker.app.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneId
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
        compose
            .onNodeWithContentDescription("Edit time for", substring = true)
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Choose time").performScrollTo().performClick()
        compose.onNodeWithText("Use keyboard").performScrollTo().performClick()
        val original = Instant.ofEpochMilli(before.events.last().at).atZone(ZoneId.of("UTC"))
        val correctedMinute = if (original.minute > 5) original.minute - 5 else original.minute + 5
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement(correctedMinute.toString())
        compose.onAllNodesWithText("Save").onLast().performScrollTo().performClick()
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
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Your schedule").assertExists()
        compose.onNodeWithText("Edit treatment details").performScrollTo().performClick()
        compose.onNodeWithText("Current tray (optional)").performScrollTo().assertTextContains("3")
        assertNull(runBlocking { container.repository.snapshot().plan!!.currentTray })
    }

    @Test
    fun dateBrowsingPreservesRecordsAndBoundsNextDay() {
        val before = seed()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithContentDescription("Next day").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous day").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Next day").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Next day").assertIsNotEnabled()
        compose.onNodeWithText("Choose date").assertHasClickAction()
        assertEquals(before, runBlocking { container.repository.snapshot() })
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
    fun appointmentTitleEditPreservesExactRepeatedInstant() {
        val base = seed()
        val appointment =
            org.alignertracker.app.domain.Appointment(
                1,
                Instant.parse("2026-10-25T01:30:17.123Z").toEpochMilli(),
                30,
                "Synthetic visit",
            )
        runBlocking {
            container.repository.replaceFromBackup(
                base.copy(
                    plan = base.plan!!.copy(zoneId = "Europe/Rome"),
                    appointments = listOf(appointment),
                )
            )
        }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Calendar and notes").performClick()
        compose
            .onNode(hasScrollToIndexAction())
            .performScrollToNode(hasContentDescription("Edit Appointment", substring = true))
        compose
            .onNodeWithContentDescription("Edit Appointment", substring = true)
            .performScrollTo()
            .performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Appointment title"))
        compose
            .onNodeWithText("Appointment title")
            .performScrollTo()
            .performTextReplacement("Edited visit")
        compose.activityRule.scenario.onActivity { activity ->
            activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        compose.waitForIdle()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Save appointment"))
        compose.onNodeWithText("Save appointment").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                container.repository.snapshot().appointments.single().title == "Edited visit"
            }
        }
        val after = runBlocking { container.repository.snapshot().appointments.single() }
        assertEquals(appointment.startsAt, after.startsAt)
        assertEquals(appointment.durationMinutes, after.durationMinutes)
    }

    @Test
    fun reportUnknownDayIsNotDisplayedAsZeroWear() {
        seed()
        compose.onNodeWithText("Progress").performClick()
        ready("Reports")
        compose.onAllNodesWithText("No recorded time. Wear is unknown.")[0].assertExists()
        compose.onNodeWithText("Worn 0 min", substring = true).assertDoesNotExist()
        compose
            .onAllNodesWithContentDescription(
                "Prescribed target unavailable for this day",
                substring = true,
            )[0]
            .assertExists()
    }
}
