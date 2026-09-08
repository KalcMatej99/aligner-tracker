package org.alignertracker.app.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DialogAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun expandedReplacementAndCancelStaySeparateAndReachable() {
        val confirm = "Replace with this backup one two three four five"
        val cancel = "Cancel one two three"
        var replaced = false
        var cancelled = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 2f)
            ) {
                AlignerTheme {
                    TrackerDialog(
                        onDismissRequest = { cancelled = true },
                        title = { Text("Review this backup one two three four five") },
                        text = {
                            Text(
                                "All existing records and private photos will be replaced. "
                                    .repeat(12)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { replaced = true }) { Text(confirm) }
                        },
                        dismissButton = {
                            TextButton(onClick = { cancelled = true }) { Text(cancel) }
                        },
                    )
                }
            }
        }
        compose.onNodeWithText(confirm).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(cancel).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(confirm).assertIsDisplayed()
        val confirmBounds = compose.onNodeWithText(confirm).fetchSemanticsNode().boundsInRoot
        val cancelBounds = compose.onNodeWithText(cancel).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Confirmation and cancellation must not overlap",
            confirmBounds.bottom <= cancelBounds.top,
        )
        compose.onNodeWithText(cancel).performClick()
        assertTrue(cancelled)
        assertFalse(replaced)
    }
}
