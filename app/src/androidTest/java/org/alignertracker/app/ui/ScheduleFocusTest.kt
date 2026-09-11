package org.alignertracker.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.time.Instant
import org.alignertracker.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScheduleFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nextChangeAndSafeguardsRemainTruthful() {
        val now = Instant.parse("2026-09-11T12:00:00Z")
        var snapshot by
            mutableStateOf(
                TrackerSnapshot(
                    plan =
                        TreatmentPlan(
                            zoneId = "UTC",
                            currentTray = 3,
                            totalTrays = 20,
                            daysPerTray = 7,
                            currentTrayStartedOn = "2026-09-08",
                            trackingStartedAt = now.minusSeconds(86400).toEpochMilli(),
                        )
                )
            )
        var busy by mutableStateOf(false)
        var advanced = 0
        var completed = 0
        compose.setContent {
            AlignerTheme {
                ScheduleScreen(snapshot, busy, { advanced++ }, { completed++ }, now = now)
            }
        }
        compose.onNodeWithText("In 4 days").assertIsDisplayed()
        compose.onNodeWithText("3 of 7 planned days elapsed").assertIsDisplayed()
        compose.onNodeWithText("Tray 8").assertDoesNotExist()
        compose.onNodeWithText("Show more planned changes").performScrollTo().performClick()
        compose.onNodeWithText("Tray 8").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Record next tray").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, advanced)
            busy = true
        }
        compose.onNodeWithText("Record next tray").assertIsNotEnabled()
        compose.runOnIdle {
            busy = false
            snapshot = snapshot.copy(plan = snapshot.plan!!.copy(currentTray = 20))
        }
        compose.onNodeWithText("Planned finish").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Record next tray").assertDoesNotExist()
        compose.onNodeWithText("Complete treatment").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, completed)
            snapshot =
                snapshot.copy(
                    plan = snapshot.plan!!.copy(completed = true, completedAt = now.toEpochMilli())
                )
        }
        compose.onNodeWithText("Next planned change").assertDoesNotExist()
        compose.onNodeWithText("Complete treatment").assertDoesNotExist()
        compose.runOnIdle {
            snapshot =
                snapshot.copy(
                    plan = TreatmentPlan(zoneId = "UTC", trackingStartedAt = now.toEpochMilli())
                )
        }
        compose.onNodeWithText("Plan your next change").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("In 4 days").assertDoesNotExist()
    }
}
