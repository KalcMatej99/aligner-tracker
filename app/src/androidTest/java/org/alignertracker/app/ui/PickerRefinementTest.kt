package org.alignertracker.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import java.time.*
import org.alignertracker.app.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PickerRefinementTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun optionalDateCancelAndRecreationDoNotCreateValue() {
        var saved = ""
        val restore = StateRestorationTester(compose)
        restore.setContent {
            AlignerTheme { DateField(saved, { saved = it }, R.string.picker_date) }
        }
        compose.onNodeWithText("Date").performClick()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals("", saved)
        compose.onNodeWithText("Date").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals("", saved)
    }

    @Test
    fun calendarSelectionCommitsOnlyOnSave() {
        var saved = ""
        compose.setContent {
            AlignerTheme {
                CalendarDialog("2026-09-10", LocalDate.of(2100, 12, 31), { saved = it }, {})
            }
        }
        compose.onNodeWithText("Use keyboard").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("09092026")
        assertEquals("", saved)
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals("2026-09-09", saved)
    }

    @Test
    fun clockSelectionPreservesSubminutePrecisionAndCancellation() {
        var saved = LocalTime.of(9, 30, 17, 123_000_000)
        compose.setContent { AlignerTheme { ClockDialog(saved, { saved = it }, {}) } }
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(LocalTime.of(9, 30, 17, 123_000_000), saved)
    }

    @Test
    fun repeatedTimeRequiresOccurrenceWithoutExposingOffsets() {
        var saved by mutableStateOf("02:30:17.123")
        compose.setContent {
            AlignerTheme {
                TimeField(
                    saved,
                    { saved = it },
                    R.string.choose_time,
                    LocalDate.parse("2026-10-25"),
                    ZoneId.of("Europe/Rome"),
                )
            }
        }
        compose.onNodeWithText("Second occurrence").performClick()
        assertEquals("02:30:17.123+01:00", saved)
        compose.onAllNodesWithText("+01:00", substring = true).assertCountEquals(0)
    }

    @Test
    fun correctionDraftSurvivesRecreationAndKeepsExactInstant() {
        val initial = Instant.parse("2026-07-01T07:30:17.123Z").toEpochMilli()
        var result: Long? = null
        val restore = StateRestorationTester(compose)
        restore.setContent {
            AlignerTheme { MomentField(initial, { result = it }, ZoneId.of("Europe/Rome"), true) }
        }
        compose.waitForIdle()
        assertEquals(initial, result)
        compose.onNodeWithText("Choose time").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(initial, result)
    }
}
