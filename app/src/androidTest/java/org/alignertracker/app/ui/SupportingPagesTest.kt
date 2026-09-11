package org.alignertracker.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.Locale
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.domain.*
import org.alignertracker.app.reminders.ReminderPreferences
import org.junit.*

class SupportingPagesTest {
    @get:Rule val compose = createComposeRule()
    private val models = ViewModelStore()

    @After
    fun clear() {
        compose.runOnIdle { models.clear() }
    }

    private fun model(): TrackerViewModel {
        val c =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                    as TrackerApplication)
                .container
        return TrackerViewModel(c.repository, c.reminderSettings, c.reminderScheduler).also {
            models.put("support", it)
        }
    }

    @Test
    fun collapsibleFormsRetainDraftsAndReportExpandedState() {
        compose.setContent {
            AlignerTheme {
                ExpandableBlock(
                    org.alignertracker.app.R.string.support_plan_edit,
                    "Summary",
                    Destination.DETAILS,
                ) {
                    var draft by
                        androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
                    TextField(draft, { draft = it }, label = { Text("Draft") })
                }
            }
        }
        compose.onNodeWithText("Draft").assertDoesNotExist()
        compose.onNodeWithText("Edit target & tray details").performClick()
        compose.onNodeWithText("Draft").performTextInput("Keep this draft")
        compose.onNodeWithText("Edit target & tray details").performClick().performClick()
        compose.onNodeWithText("Draft").assertTextContains("Keep this draft")
    }

    @Composable
    private fun settings(snapshot: TrackerSnapshot, busy: Boolean = false) {
        SettingsScreen(
            snapshot.plan,
            ReminderPreferences(),
            busy,
            false,
            false,
            false,
            false,
            {},
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }

    @Test
    fun captureSupportingMatrix() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val now = Instant.now().toEpochMilli()
        val base =
            progressFixture()
                .copy(
                    plan =
                        progressFixture()
                            .plan!!
                            .copy(
                                currentTray = 3,
                                totalTrays = 20,
                                daysPerTray = 7,
                                currentTrayStartedOn = "2026-09-08",
                            ),
                    notes = listOf(TreatmentNote(1, now, "Ask about the next tray at my visit.")),
                    appointments =
                        listOf(
                            Appointment(
                                1,
                                now,
                                30,
                                "Tray review",
                                note = "Bring the current aligners",
                            )
                        ),
                )
        data class Case(
            val name: String,
            val width: Int = 400,
            val font: Float = 1f,
            val dark: Boolean = false,
            val locale: String = "en-US",
            val partial: Boolean = false,
            val busy: Boolean = false,
        )
        val cases =
            listOf(
                Case("normal"),
                Case("dark", dark = true),
                Case("small-large", 320, 2f),
                Case("rtl", locale = "ar-XB"),
                Case("expanded", 320, 2f, true, "en-XA"),
                Case("unknown", partial = true),
                Case("busy", busy = true),
                Case("completed"),
            )
        var selected by mutableStateOf(cases.first())
        var page by mutableStateOf("settings")
        compose.setContent {
            val c = selected
            val config =
                Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(c.locale))
                    fontScale = c.font
                }
            CompositionLocalProvider(
                LocalContext provides context.createConfigurationContext(config),
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, c.font),
                LocalLayoutDirection provides
                    if (c.locale == "ar-XB") LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                MaterialTheme(
                    colorScheme = if (c.dark) DarkColors else LightColors,
                    typography = TrackerTypography,
                    shapes = TrackerShapes,
                ) {
                    Surface(Modifier.safeDrawingPadding().width(c.width.dp).fillMaxHeight()) {
                        key(c.name, page) {
                            val snapshot =
                                if (c.partial)
                                    base.copy(
                                        plan =
                                            TreatmentPlan(zoneId = "UTC", trackingStartedAt = now),
                                        notes = emptyList(),
                                        appointments = emptyList(),
                                    )
                                else if (c.name == "completed")
                                    base.copy(
                                        plan = base.plan!!.copy(completed = true, completedAt = now)
                                    )
                                else base
                            val vm = remember { model() }
                            when (page) {
                                "settings" -> settings(snapshot, c.busy)
                                "treatment" -> TreatmentDetailsScreen(snapshot, vm, c.busy)
                                else -> JournalScreen(snapshot, vm, c.busy)
                            }
                        }
                    }
                }
            }
        }
        for (c in cases) for (p in listOf("settings", "treatment", "calendar", "notes")) {
            compose.runOnIdle {
                selected = c
                page = p
            }
            compose.waitForIdle()
            if (p == "notes") {
                val config =
                    Configuration(context.resources.configuration).apply {
                        setLocale(Locale.forLanguageTag(c.locale))
                    }
                compose
                    .onNodeWithText(
                        context
                            .createConfigurationContext(config)
                            .getString(org.alignertracker.app.R.string.support_notes)
                    )
                    .performClick()
            }
            capture("$p-${c.name}")
            if (c.font == 2f) {
                compose
                    .onNode(
                        SemanticsMatcher.keyIsDefined(
                            androidx.compose.ui.semantics.SemanticsProperties
                                .VerticalScrollAxisRange
                        )
                    )
                    .performTouchInput { swipeUp() }
                capture("$p-${c.name}-scrolled")
            }
        }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("capture") != "true") return
        compose.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        i.waitForIdleSync()
        android.os.SystemClock.sleep(500)
        val f = File(i.targetContext.getExternalFilesDir(null), "supporting-pages/$name.png")
        f.parentFile!!.mkdirs()
        compose.onRoot().captureToImage().asAndroidBitmap().let { b ->
            f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            b.recycle()
        }
    }
}
