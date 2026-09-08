package org.alignertracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TextFieldAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun wrappedFloatingLabelClearsPreviousHeadingAndPreservesEditing() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                AlignerTheme {
                    var value by remember { mutableStateOf("2026-09-08") }
                    Column(Modifier.width(320.dp)) {
                        Text(
                            "Calendar and notes",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.testTag("preceding-heading"),
                        )
                        TrackerTextField(
                            value,
                            { value = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "[History or appointment date (YYYY-MM-DD) one two three four five]",
                                    modifier = Modifier.testTag("floating-label"),
                                )
                            },
                        )
                    }
                }
            }
        }
        val heading = compose.onNodeWithTag("preceding-heading").getUnclippedBoundsInRoot()
        val label = compose.onNodeWithTag("floating-label", useUnmergedTree = true)
        label.assertIsDisplayed()
        val layout = mutableListOf<TextLayoutResult>()
        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        assertTrue("Exercise a genuinely wrapped floating label", layout.single().lineCount > 1)
        assertTrue(
            "Floating label must not cover the preceding heading",
            label.getUnclippedBoundsInRoot().top >= heading.bottom,
        )
        val field = compose.onNode(hasSetTextAction())
        field.performTextReplacement("2026-09-09")
        field.assertTextContains("2026-09-09")
        assertTrue(
            "Focused label must still clear the heading",
            label.getUnclippedBoundsInRoot().top >= heading.bottom,
        )
    }

    @Test
    fun expandedMonthButtonsRemainVisibleAndActionableAtLargeFont() {
        var previous = 0
        var next = 0
        val previousLabel = "[Previous month one two three]"
        val nextLabel = "[Next month one two three]"
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                AlignerTheme {
                    Column(Modifier.width(320.dp)) {
                        JournalMonthNavigation(previousLabel, nextLabel, { previous++ }, { next++ })
                    }
                }
            }
        }
        for (label in listOf(previousLabel, nextLabel)) {
            val node = compose.onNodeWithText(label)
            node.assertIsDisplayed().performClick()
            assertTrue(
                "Month button needs a 48dp target",
                node.getUnclippedBoundsInRoot().height >= 48.dp,
            )
            val layout = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
            assertTrue("Full month label must fit", !layout.single().hasVisualOverflow)
        }
        compose.runOnIdle {
            assertEquals(1, previous)
            assertEquals(1, next)
        }
    }
}
