package org.alignertracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdaptiveNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun narrowLargeTextTabsStayReadableAndSelectable() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                AlignerTheme {
                    var selected by remember { mutableStateOf(Destination.TODAY) }
                    Box(Modifier.width(320.dp)) { TrackerNavigation(selected) { selected = it } }
                }
            }
        }
        listOf("Today", "Schedule", "History", "Progress").forEach { label ->
            val tab = compose.onNodeWithText(label)
            tab.assertIsDisplayed().performClick().assertIsSelected()
            assertTrue(
                "Tab target must remain at least 48dp",
                tab.getUnclippedBoundsInRoot().height >= 48.dp,
            )
            val layouts = mutableListOf<TextLayoutResult>()
            tab.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("Expected one full text label", 1, layouts.size)
            assertEquals("Destination must not break mid-word", 1, layouts.single().lineCount)
            assertTrue(
                "$label clipped: size=${layouts.single().size}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}, paragraph=${layouts.single().multiParagraph.height}",
                !layouts.single().hasVisualOverflow,
            )
        }
    }

    @Test
    fun topBarDoesNotBreakBrandIntoFragmentsAtLargeFont() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                AlignerTheme {
                    Box(Modifier.width(320.dp)) {
                        AdaptiveTrackerTopBar("Aligner Tracker", false, {}) {
                            androidx.compose.material3.TextButton(onClick = {}) {
                                androidx.compose.material3.Text("More")
                            }
                            androidx.compose.material3.TextButton(onClick = {}) {
                                androidx.compose.material3.Text("Settings")
                            }
                        }
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Aligner Tracker").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) {
            it(layouts)
        }
        assertEquals(1, layouts.single().lineCount)
        assertTrue(
            "Header layout size=${layouts.single().size}, paragraphHeight=${layouts.single().multiParagraph.height}, overflowWidth=${layouts.single().didOverflowWidth}, overflowHeight=${layouts.single().didOverflowHeight}",
            !layouts.single().hasVisualOverflow,
        )
        listOf("More", "Settings").forEach {
            compose.onNodeWithText(it).assertIsDisplayed().performClick()
        }
    }
}
