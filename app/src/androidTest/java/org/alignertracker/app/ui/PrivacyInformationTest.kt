package org.alignertracker.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test

class PrivacyInformationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun offlinePolicyAndHealthNoticeCanBeReadAndDismissed() {
        val rtl = InstrumentationRegistry.getArguments().getString("rtl") == "true"
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    if (rtl) androidx.compose.ui.unit.LayoutDirection.Rtl
                    else androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                AlignerTheme { Column { PrivacyInformation() } }
            }
        }
        compose.onNodeWithText("Privacy policy").performClick()
        compose.onNodeWithText("Who provides this app").assertIsDisplayed()
        capture("privacy")
        compose
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Questions and policy changes"))
        compose.onNodeWithText("Questions and policy changes").assertIsDisplayed()
        compose.onNodeWithText("Close information").assertIsDisplayed().performClick()
        compose.onNodeWithText("About health tracking").performClick()
        compose
            .onNodeWithText("Aligner Tracker is not a medical device", substring = true)
            .assertIsDisplayed()
        capture("health")
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText("Privacy policy").assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory =
            File(context.getExternalFilesDir(null), "play-information").apply { mkdirs() }
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
