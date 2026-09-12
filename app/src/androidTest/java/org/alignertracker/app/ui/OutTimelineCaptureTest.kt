package org.alignertracker.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import org.alignertracker.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Actual production screens with synthetic records; screenshots do not establish comprehension. */
class OutTimelineCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun threeBreaksRemainThreeIntervalsAcrossPresentationModes() {
        fun at(time: String) = Instant.parse("2026-09-12T${time}:00Z").toEpochMilli()
        val now = Instant.parse("2026-09-12T21:00:00Z")
        val snapshot =
            TrackerSnapshot(
                plan =
                    TreatmentPlan(
                        zoneId = "UTC",
                        trackingStartedAt = at("01:00"),
                        dailyGoalMinutes = 1200,
                    ),
                events =
                    listOf(
                        WearEvent(1, at("01:00"), true),
                        WearEvent(2, at("07:30"), false),
                        WearEvent(3, at("08:15"), true),
                        WearEvent(4, at("12:00"), false),
                        WearEvent(5, at("14:00"), true),
                        WearEvent(6, at("18:15"), false),
                        WearEvent(7, at("19:15"), true),
                    ),
            )
        data class Case(
            val name: String,
            val today: Boolean = false,
            val dark: Boolean = false,
            val font: Float = 1f,
            val width: Int = 400,
            val rtl: Boolean = false,
            val empty: Boolean = false,
        )
        val cases =
            listOf(
                Case("history-light"),
                Case("today-light", today = true),
                Case("history-dark", dark = true),
                Case("history-large-rtl", dark = true, font = 2f, width = 320, rtl = true),
                Case("today-large", today = true, font = 2f, width = 320),
                Case("history-empty", empty = true),
            )
        var selected by mutableStateOf(cases.first())
        compose.setContent {
            val c = selected
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, c.font),
                LocalLayoutDirection provides
                    if (c.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                MaterialTheme(
                    colorScheme = if (c.dark) DarkColors else LightColors,
                    typography = TrackerTypography,
                    shapes = TrackerShapes,
                ) {
                    Surface(Modifier.safeDrawingPadding().width(c.width.dp).fillMaxHeight()) {
                        key(c.name) {
                            val data =
                                if (c.empty) snapshot.copy(events = emptyList()) else snapshot
                            if (c.today) TodayScreen(data, now, false, {})
                            else HistoryScreen(data, now, false, {})
                        }
                    }
                }
            }
        }
        for (c in cases) {
            compose.runOnIdle { selected = c }
            val timeline =
                compose.onNodeWithContentDescription(
                    "Day timeline · 24-hour clock.",
                    substring = true,
                )
            timeline.performScrollTo().assertIsDisplayed()
            val description =
                timeline
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.ContentDescription]
                    .joinToString()
            assertEquals(if (c.empty) 0 else 3, Regex("Aligners out:").findAll(description).count())
            capture(c.name)
            compose
                .onNodeWithText("Each OUT block is one continuous break.")
                .performScrollTo()
                .assertIsDisplayed()
            compose.onNodeWithText("Not tracked").assertIsDisplayed()
            if (c.font > 1f) capture(c.name + "-legend")
        }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("capture") != "true") return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output =
            File(instrumentation.targetContext.getExternalFilesDir(null), "out-timeline/$name.png")
        output.parentFile!!.mkdirs()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(250)
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
