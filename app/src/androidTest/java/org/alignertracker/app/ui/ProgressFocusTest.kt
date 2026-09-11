package org.alignertracker.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.time.Instant
import org.alignertracker.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal fun progressFixture(): TrackerSnapshot {
    val start = Instant.parse("2026-09-05T00:00:00Z")
    val events =
        (0..6)
            .flatMap { day ->
                val at = start.plusSeconds(day * 86400L)
                listOf(
                    WearEvent(day * 2L + 1, at.toEpochMilli(), true),
                    WearEvent(
                        day * 2L + 2,
                        at.plusSeconds(listOf(21, 22, 20, 23, 21, 22, 18)[day] * 3600L)
                            .toEpochMilli(),
                        false,
                    ),
                )
            }
            .filter { it.at <= Instant.parse("2026-09-11T18:30:00Z").toEpochMilli() }
    return TrackerSnapshot(
        plan =
            TreatmentPlan(
                zoneId = "UTC",
                trackingStartedAt = start.toEpochMilli(),
                dailyGoalMinutes = 1320,
            ),
        events = events,
        trackingGaps =
            listOf(
                TrackingGap(
                    1,
                    start.plusSeconds(2 * 86400L).toEpochMilli(),
                    start.plusSeconds(3 * 86400L).toEpochMilli(),
                )
            ),
    )
}

class ProgressFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun periodChartAndToolsAreDistinct() {
        var selected = 0
        compose.setContent {
            AlignerTheme {
                ProgressOverview(progressFixture(), Instant.parse("2026-09-11T18:30:00Z")) {
                    selected = it
                }
            }
        }
        compose.onNodeWithText("Average daily wear").assertIsDisplayed()
        compose.onNodeWithText("21 h 48 min").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sep 7, 2026", substring = true).assertExists()
        compose.onNodeWithText("30 days").performClick().assertIsSelected()
        compose.onNodeWithText("Details & export").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(30, selected) }
        compose.onNodeWithText("Daily data").assertDoesNotExist()
    }
}
