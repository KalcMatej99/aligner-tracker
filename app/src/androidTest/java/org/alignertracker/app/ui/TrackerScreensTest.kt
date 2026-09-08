package org.alignertracker.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import java.time.Instant
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class TrackerScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun onboardingRejectsEmptyPlanAndAcceptsExplicitOutState() {
        var saved: TreatmentPlan? = null
        var wearing = true
        compose.setContent {
            AlignerTheme {
                OnboardingScreen(
                    false,
                    { plan, state ->
                        saved = plan
                        wearing = state
                    },
                    {},
                )
            }
        }
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(null, saved) }
        compose.onNodeWithText("Total trays").performScrollTo().performTextReplacement("20")
        compose
            .onNodeWithText("Prescribed days per tray")
            .performScrollTo()
            .performTextReplacement("7")
        compose
            .onNodeWithText("Prescribed daily hours (for example 22)")
            .performScrollTo()
            .performTextReplacement("21.5")
        compose.onNodeWithText("Aligners out").performScrollTo().performClick()
        compose.onNodeWithText("Start tracking").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(20, saved?.totalTrays)
            assertEquals(1290, saved?.dailyGoalMinutes)
            assertFalse(wearing)
        }
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
        compose.onNodeWithText("Put aligners in").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, nextState) }
        compose.onNodeWithText("Untracked elapsed time").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("9 hours, 0 minutes").assertIsDisplayed()
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
    fun historyRejectsAnExtremeDateWithoutCalculatingIt() {
        compose.setContent {
            AlignerTheme {
                HistoryScreen(example(), Instant.parse("2026-09-08T12:00:00Z"), false, {})
            }
        }
        compose
            .onNodeWithText("Date (YYYY-MM-DD)")
            .performScrollTo()
            .performTextReplacement("-999999999-01-01")
        compose.onNodeWithText("Go to date").performScrollTo().performClick()
        compose
            .onNodeWithText("Enter a valid date from 1970-01-01 through today.")
            .performScrollTo()
            .assertIsDisplayed()
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
