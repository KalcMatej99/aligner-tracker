package org.alignertracker.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.Locale
import org.alignertracker.app.R
import org.alignertracker.app.domain.*
import org.junit.Rule
import org.junit.Test

/** Reproducible synthetic Schedule-only emulator captures, not a human usability test. */
class ScheduleCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun captureMatrix() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val now = Instant.parse("2026-09-11T16:30:00Z")
        val start = now.minusSeconds(9 * 3600).toEpochMilli()
        val base =
            TrackerSnapshot(
                plan =
                    TreatmentPlan(
                        zoneId = "UTC",
                        trackingStartedAt = start,
                        dailyGoalMinutes = 1200,
                        currentTray = 3,
                        totalTrays = 20,
                        daysPerTray = 7,
                        currentTrayStartedOn = "2026-09-08",
                    ),
                events =
                    listOf(
                        WearEvent(1, start, true),
                        WearEvent(2, now.minusSeconds(1800).toEpochMilli(), false),
                    ),
            )
        data class Case(
            val name: String,
            val width: Int = 400,
            val font: Float = 1f,
            val dark: Boolean = false,
            val locale: String = "en-US",
            val busy: Boolean = false,
            val completed: Boolean = false,
            val partial: Boolean = false,
        )
        val cases =
            listOf(
                Case("normal"),
                Case("320dp", width = 320),
                Case("font200", font = 2f),
                Case("dark", dark = true),
                Case("rtl", locale = "ar-XB"),
                Case("expanded", locale = "en-XA"),
                Case("small-dark-expanded", 320, 2f, true, "en-XA"),
                Case("small-dark-rtl", 320, 2f, true, "ar-XB"),
                Case("saving", busy = true),
                Case("completed", completed = true),
                Case("partial", partial = true),
            )
        var selected by mutableStateOf(cases.first())
        var wearing by mutableStateOf(false)
        compose.setContent {
            val c = selected
            val configuration =
                Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(c.locale))
                }
            val localized = context.createConfigurationContext(configuration)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides
                    if (c.locale == "ar-XB") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, c.font),
            ) {
                MaterialTheme(
                    colorScheme = if (c.dark) DarkColors else LightColors,
                    typography = TrackerTypography,
                    shapes = TrackerShapes,
                ) {
                    Surface(Modifier.safeDrawingPadding().width(c.width.dp).fillMaxHeight()) {
                        // Recreate scroll state for a comparable top-of-screen capture.
                        key(c.name, wearing) {
                            val plan =
                                if (c.partial)
                                    TreatmentPlan(zoneId = "UTC", trackingStartedAt = start)
                                else base.plan!!
                            val snapshot =
                                base.copy(
                                    plan =
                                        plan.copy(
                                            completed = c.completed,
                                            completedAt =
                                                if (c.completed) now.toEpochMilli() else null,
                                        ),
                                    events =
                                        if (wearing)
                                            base.events +
                                                WearEvent(
                                                    3,
                                                    now.minusSeconds(600).toEpochMilli(),
                                                    true,
                                                )
                                        else base.events,
                                )
                            ScheduleScreen(snapshot, c.busy, {}, {}, now = now)
                        }
                    }
                }
            }
        }
        for (c in cases) for (state in listOf(true)) {
            compose.runOnIdle {
                selected = c
                wearing = state
            }
            compose.waitForIdle()
            val configuration =
                Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(c.locale))
                }
            val resources = context.createConfigurationContext(configuration).resources
            compose.onNodeWithText(resources.getString(R.string.time_details)).assertDoesNotExist()
            capture("${c.name}-${if (state) "in" else "out"}")
            if (c.font == 2f || c.locale == "en-XA") {
                // Scroll the actual content viewport; bottom actions remain fixed.
                compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
                compose.waitForIdle()
                capture("${c.name}-${if (state) "in" else "out"}-scrolled")
            }
        }
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (InstrumentationRegistry.getArguments().getString("capture") != "true") return
        val output =
            File(
                instrumentation.targetContext.getExternalFilesDir(null),
                "schedule-focus/$name.png",
            )
        output.parentFile!!.mkdirs()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(250) // Allow SurfaceFlinger to present the composed frame.
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
