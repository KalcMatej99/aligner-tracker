package org.alignertracker.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.time.Instant
import org.alignertracker.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun historyKeepsEditingSafeguardsAndVisibleLegend() {
        val now = Instant.parse("2026-09-10T12:00:00Z")
        val start = now.minusSeconds(3600).toEpochMilli()
        var busy by mutableStateOf(false)
        var snapshot by
            mutableStateOf(
                TrackerSnapshot(
                    plan = TreatmentPlan(zoneId = "UTC", trackingStartedAt = start),
                    events = listOf(WearEvent(1, start, true), WearEvent(2, start + 1800000, false)),
                )
            )
        var edited: Long? = null
        compose.setContent { AlignerTheme { HistoryScreen(snapshot, now, busy) { edited = it } } }
        compose.onNodeWithText("About this breakdown").assertDoesNotExist()
        compose.onNodeWithText("Each OUT block is one continuous break.").assertIsDisplayed()
        compose
            .onNodeWithContentDescription("Edit time for Aligners in", substring = true)
            .assertDoesNotExist()
        val edit =
            compose.onNodeWithContentDescription("Edit time for Aligners out", substring = true)
        edit
            .performScrollTo()
            .assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
            .performClick()
        compose.runOnIdle {
            assertEquals(2L, edited)
            busy = true
        }
        edit.assertIsNotEnabled()
        compose.runOnIdle {
            snapshot =
                snapshot.copy(
                    plan = snapshot.plan!!.copy(completed = true, completedAt = now.toEpochMilli())
                )
        }
        edit.assertDoesNotExist()
        compose.runOnIdle { snapshot = snapshot.copy(events = emptyList()) }
        compose
            .onNodeWithText("No state changes on this date.", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
