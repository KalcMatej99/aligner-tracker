package org.alignertracker.app.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.domain.Appointment
import org.alignertracker.app.domain.PhotoMetadata
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentNote
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.reminders.ReminderPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Semantics/action regressions; these do not impersonate a physical TalkBack audit. */
class TalkBackSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val models = ViewModelStore()
    private val now = Instant.parse("2026-09-08T12:00:00Z")

    @After
    fun clearModels() {
        compose.runOnIdle { models.clear() }
    }

    private fun model(): TrackerViewModel {
        val container =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                    as TrackerApplication)
                .container
        return TrackerViewModel(
                container.repository,
                container.reminderSettings,
                container.reminderScheduler,
            )
            .also { models.put("screen", it) }
    }

    @Test
    fun todaySeparatesRecordedStatusFromActionsWithoutTimerAnnouncements() {
        compose.setContent {
            var snapshot by remember { mutableStateOf(example()) }
            AlignerTheme {
                TodayScreen(snapshot, now, false) { wearing ->
                    snapshot =
                        snapshot.copy(
                            events = snapshot.events + WearEvent(3, now.toEpochMilli(), wearing)
                        )
                }
            }
        }
        compose.onAllNodesWithContentDescription("Aligners out").assertCountEquals(1)
        compose.onNodeWithContentDescription("Aligners out").assertHasNoClickAction()
        compose
            .onNodeWithText("Put aligners in")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        compose.onAllNodesWithContentDescription("Aligners in").assertCountEquals(1)
        compose.onNodeWithContentDescription("Aligners out").assertDoesNotExist()
        compose
            .onNodeWithText("Take aligners out")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .performClick()
        compose.onAllNodesWithContentDescription("Aligners out").assertCountEquals(1)
        compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertCountEquals(0)
    }

    @Test
    fun historyEditActionIdentifiesTheTransitionAndKeepsItsCallback() {
        var edited: Long? = null
        compose.setContent { AlignerTheme { HistoryScreen(example(), now, false) { edited = it } } }
        compose
            .onNode(hasContentDescription("Edit time for Aligners out", substring = true))
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        compose.runOnIdle { assertEquals(2L, edited) }
    }

    @Test
    fun savedNoticeHasPoliteAnnouncementAndNamedDismissAction() {
        var dismissed = false
        compose.setContent { AlignerTheme { OperationNotice("Saved.") { dismissed = true } } }
        val notice = compose.onNodeWithText("Saved.")
        notice.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        )
        assertEquals(
            "Dismiss message",
            notice.fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
        notice.performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun dialogExposesWindowHeadingAndSeparateCancellation() {
        var confirmed = false
        compose.setContent {
            var open by remember { mutableStateOf(true) }
            AlignerTheme {
                if (open)
                    TrackerDialog(
                        onDismissRequest = { open = false },
                        title = { Text("Review replacement") },
                        text = { Text("Existing records will be replaced.") },
                        confirmButton = {
                            TextButton(onClick = { confirmed = true }) { Text("Replace records") }
                        },
                        dismissButton = {
                            TextButton(onClick = { open = false }) { Text("Cancel replacement") }
                        },
                    )
            }
        }
        compose.onNode(isDialog()).assertExists()
        compose
            .onNodeWithText("Review replacement")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Cancel replacement").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { assertFalse(confirmed) }
    }

    @Test
    fun onboardingMakesRequiredStateExplicit() {
        compose.setContent { AlignerTheme { OnboardingScreen(false, { _, _ -> }, {}) } }
        compose.onNodeWithText("Choose IN or OUT to start.").assertExists()
        compose.onNodeWithText("Start tracking").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Aligners in").performScrollTo().performClick()
        compose.onNodeWithText("Start tracking").performScrollTo().assertIsEnabled()
    }

    @Test
    fun reportsKeepDateTotalsAndCoverageTogetherWithoutAnUnlabelledPercentage() {
        compose.setContent {
            AlignerTheme {
                ReportsScreen(example(), remember { model() }, now, ReminderPreferences())
            }
        }
        compose.onNodeWithText("Days: 1").performClick()
        compose.onNodeWithText("Daily values and targets").performScrollTo().performClick()
        compose
            .onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("Partial-day coverage", substring = true))
        val daily = compose.onNode(hasText("Partial-day coverage", substring = true))
        daily.performScrollTo().assertTextContains("Prescribed", substring = true)
        assertTrue(daily.fetchSemanticsNode().config.isMergingSemanticsOfDescendants)
        compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(0)
    }

    @Test
    fun expandedPhaseFieldsExposeSpecificErrorsAndNativeEditing() {
        compose.setContent {
            AlignerTheme { TreatmentDetailsScreen(example(), remember { model() }, false) }
        }
        compose
            .onNodeWithText("Number of trays or retainers")
            .performScrollTo()
            .performTextReplacement("0")
        compose
            .onNodeWithText("Number of trays or retainers")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Enter a whole number from 1 to 1000.",
                )
            )
        compose.onNodeWithText("Number of trays or retainers").performTextReplacement("2")
        compose
            .onNodeWithText("Number of trays or retainers")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun photoSelectionNamesTheDatedRecordAndExposesSelectedState() {
        val snapshot =
            example()
                .copy(
                    photos =
                        listOf(
                            PhotoMetadata(
                                id = 1,
                                capturedAt = now.toEpochMilli(),
                                mimeType = "image/jpeg",
                                byteSize = 1,
                                sha256 = "test",
                                caption = "Front view",
                            )
                        )
                )
        compose.setContent { AlignerTheme { PhotosScreen(snapshot, remember { model() }, false) } }
        val select =
            compose.onNode(
                hasContentDescription(
                    "Select for comparison or export: Progress photo",
                    substring = true,
                )
            )
        // The list is lazy, so bring the record into composition through its stable item key.
        compose.onNode(hasScrollToIndexAction()).performScrollToKey(1L)
        select.assertIsNotSelected().performClick().assertIsSelected()
        compose
            .onNode(hasContentDescription("Delete Progress photo", substring = true))
            .assertHasClickAction()
        val description =
            select.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertTrue(description.contains("Front view"))
        assertTrue(description.contains("2026"))
    }

    @Test
    fun journalActionsIdentifyRecordsAndEditingUsesDistinctFields() {
        val captured = Instant.now().toEpochMilli()
        val snapshot =
            example()
                .copy(
                    notes =
                        listOf(
                            TreatmentNote(id = 1, occurredAt = captured, text = "Bring spare case")
                        ),
                    appointments =
                        listOf(
                            Appointment(
                                id = 1,
                                startsAt = captured,
                                durationMinutes = 30,
                                title = "Tray review",
                                completed = true,
                            )
                        ),
                )
        compose.setContent { AlignerTheme { JournalScreen(snapshot, remember { model() }, false) } }
        val list = compose.onNode(hasScrollToIndexAction())
        list.performScrollToKey("note1")
        compose
            .onNode(hasContentDescription("Edit Note", substring = true))
            .assertHasClickAction()
            .performClick()
        list.performScrollToNode(hasText("Journal note"))
        compose.onNodeWithText("Journal note").assertTextContains("Bring spare case")
        list.performScrollToKey("appointment1")
        compose
            .onNode(
                hasContentDescription("Mark upcoming: Appointment, Tray review", substring = true)
            )
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Completed"))
        compose
            .onNode(hasContentDescription("Edit Appointment, Tray review", substring = true))
            .performClick()
        list.performScrollToNode(hasText("Appointment title"))
        compose.onNodeWithText("Appointment title").assertTextContains("Tray review")
        list.performScrollToNode(hasText("Duration in minutes"))
        compose.onNodeWithText("Duration in minutes").performTextReplacement("0")
        compose
            .onNodeWithText("Duration in minutes")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Enter a duration from 1 to 1440 minutes.",
                )
            )
        list.performScrollToNode(hasText("Appointment note (optional)"))
        compose.onNodeWithText("Appointment note (optional)").assertExists()
    }

    private fun example(): TrackerSnapshot {
        val start = Instant.parse("2026-09-08T09:00:00Z").toEpochMilli()
        return TrackerSnapshot(
            TreatmentPlan(
                startDate = "2026-09-08",
                totalTrays = 20,
                currentTray = 1,
                daysPerTray = 7,
                currentTrayStartedOn = "2026-09-08",
                dailyGoalMinutes = 1320,
                zoneId = "UTC",
                trackingStartedAt = start,
            ),
            listOf(WearEvent(1, start, true), WearEvent(2, start + 3600000, false)),
        )
    }
}
