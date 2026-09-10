package org.alignertracker.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.alignertracker.app.domain.DaySummary
import org.junit.Rule
import org.junit.Test

class TodayWearGoalTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun compactValuesRetainFullAccessibleUnitsAndUnknownValues() {
        var day by mutableStateOf(DaySummary("2026-09-10", 536 * 60_000L, 0, 536 * 60_000L, 1320))
        compose.setContent { AlignerTheme { TodayWearGoal(day) } }
        compose.onNodeWithText("8h56′ / 22h", useUnmergedTree = true).assertExists()
        compose
            .onNodeWithContentDescription(
                "Recorded wear: 8 hours, 56 minutes. Target: 22 hours, 0 minutes"
            )
            .assertExists()
        compose.runOnIdle { day = day.copy(goalMinutes = null) }
        compose.onNodeWithText("8h56′ / —", useUnmergedTree = true).assertExists()
        compose
            .onNodeWithContentDescription(
                "Target: Prescribed target unavailable for this day",
                substring = true,
            )
            .assertExists()
        compose.runOnIdle { day = day.copy(wornMillis = 0, trackedMillis = 0) }
        compose.onNodeWithText("— / —", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Wear is unknown", substring = true).assertExists()
    }
}
