package org.alignertracker.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.time.Instant
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrackerScreensTest {
    @get:Rule val compose = createComposeRule()

    @org.junit.Before
    fun resetSyntheticPresentationPreferences() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getSharedPreferences("presentation", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun quickStartRequiresExplicitStateAndNoTypedFields() {
        var savedZone: String? = null
        var wearing: Boolean? = null
        compose.setContent {
            AlignerTheme {
                OnboardingScreen(
                    false,
                    { state, zone ->
                        wearing = state
                        savedZone = zone
                    },
                    {},
                )
            }
        }
        compose.onNodeWithText("Start tracking").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Total trays").assertDoesNotExist()
        compose.onNodeWithText("Aligners out").performScrollTo().performClick()
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(java.time.ZoneId.systemDefault().id, savedZone)
            assertEquals(false, wearing)
        }
    }

    @Test
    fun quickStartBusyPreventsDuplicateSubmission() {
        compose.setContent {
            AlignerTheme { OnboardingScreen(true, { _, _ -> error("Busy") }, {}) }
        }
        compose.onNodeWithText("Aligners in").assertIsNotEnabled()
        compose.onNodeWithText("Saving…").performScrollTo().assertIsNotEnabled()
        val instrumentation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        if (
            androidx.test.platform.app.InstrumentationRegistry.getArguments()
                .getString("capture") == "true"
        ) {
            val file =
                java.io.File(
                    instrumentation.targetContext.getExternalFilesDir(null),
                    "quick-start-saving.png",
                )
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                file.outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }

    @Test
    fun explicitChoiceSurvivesActivityStateRestoration() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        var wearing: Boolean? = null
        restoration.setContent {
            AlignerTheme { OnboardingScreen(false, { state, _ -> wearing = state }, {}) }
        }
        compose.onNodeWithText("Aligners in").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, wearing) }
    }

    @Test
    fun partialTrackingHasNoInventedTrayOrGoal() {
        val existing = example()
        val partial =
            existing.copy(
                plan =
                    TreatmentPlan(
                        zoneId = "UTC",
                        trackingStartedAt = existing.plan!!.trackingStartedAt,
                    )
            )
        compose.setContent {
            AlignerTheme { TodayScreen(partial, Instant.parse("2026-09-08T12:00:00Z"), false, {}) }
        }
        compose.onNodeWithText("Tray 1", substring = true).assertDoesNotExist()
        compose
            .onNodeWithContentDescription(
                "Target: Prescribed target unavailable for this day",
                substring = true,
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Add your treatment details").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not now").performScrollTo().performClick()
        compose.onNodeWithText("Add your treatment details").assertDoesNotExist()
    }

    @Test
    fun todayShowsUntrackedCoverageAndSingleAction() {
        val snapshot = example()
        var nextState: Boolean? = null
        compose.setContent {
            AlignerTheme {
                TodayScreen(snapshot, Instant.parse("2026-09-08T12:00:00Z"), false) {
                    nextState = it
                }
            }
        }
        compose.onNodeWithText("Put aligners in").performClick()
        compose.runOnIdle { assertEquals(true, nextState) }
        compose
            .onNodeWithText("Untracked elapsed time", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Untracked elapsed time: 9 h 0 min").assertIsDisplayed()
    }

    @Test
    fun completionKeepsHistoryButRemovesTimerAction() {
        val active = example()
        val complete =
            active.copy(
                plan =
                    active.plan!!.copy(
                        completed = true,
                        completedAt = Instant.parse("2026-09-08T11:00:00Z").toEpochMilli(),
                    )
            )
        compose.setContent {
            AlignerTheme { TodayScreen(complete, Instant.parse("2026-09-09T12:00:00Z"), false, {}) }
        }
        compose.onNodeWithText("Treatment recorded as complete").assertIsDisplayed()
        compose.onNodeWithText("Put aligners in").assertDoesNotExist()
        compose.onNodeWithText("Take aligners out").assertDoesNotExist()
    }

    @Test
    fun finalTrayOffersCompletionInsteadOfFurtherAdvancement() {
        val active = example()
        val last = active.copy(plan = active.plan!!.copy(currentTray = 20))
        var requested = false
        compose.setContent {
            AlignerTheme { ScheduleScreen(last, false, {}, { requested = true }) }
        }
        compose.onNodeWithText("Record next tray").assertDoesNotExist()
        compose.onNodeWithText("Complete treatment").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, requested) }
    }

    @Test
    fun progressDoesNotScorePartialDaysAsMissedGoals() {
        compose.setContent {
            AlignerTheme { ProgressScreen(example(), Instant.parse("2026-09-08T12:00:00Z")) }
        }
        compose
            .onNodeWithText("Days with tracked time: 1 of 7")
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onAllNodesWithText(
                "Complete days meeting their recorded target: 0; fully tracked days: 0"
            )[0]
            .assertExists()
    }

    @Test
    fun historyDatePickerCancelKeepsSelectedDay() {
        compose.setContent {
            AlignerTheme {
                HistoryScreen(example(), Instant.parse("2026-09-08T12:00:00Z"), false, {})
            }
        }
        compose.onNodeWithText("Date (YYYY-MM-DD)").assertDoesNotExist()
        compose.onNodeWithContentDescription("Previous day").performScrollTo().performClick()
        compose.onNodeWithText("Sep 7, 2026").assertExists()
        compose.onNodeWithText("Choose date").performClick()
        val automation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("input keyevent 4").close()
        compose.waitForIdle()
        compose.onNodeWithText("Sep 7, 2026").assertExists()
    }

    private fun example(): TrackerSnapshot {
        val start = Instant.parse("2026-09-08T09:00:00Z").toEpochMilli()
        val plan =
            TreatmentPlan(
                startDate = "2026-09-08",
                totalTrays = 20,
                currentTray = 1,
                daysPerTray = 7,
                currentTrayStartedOn = "2026-09-08",
                dailyGoalMinutes = 1320,
                zoneId = "UTC",
                trackingStartedAt = start,
            )
        return TrackerSnapshot(
            plan,
            listOf(
                WearEvent(1, start, true),
                WearEvent(2, Instant.parse("2026-09-08T10:00:00Z").toEpochMilli(), false),
            ),
        )
    }
}
