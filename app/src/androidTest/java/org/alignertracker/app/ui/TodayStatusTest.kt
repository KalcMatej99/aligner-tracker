package org.alignertracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.alignertracker.app.TrackerApplication
import org.alignertracker.app.data.TrackerDatabase
import org.alignertracker.app.data.TrackerRepository
import org.alignertracker.app.domain.*
import org.alignertracker.app.reminders.ReminderScheduler
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TodayStatusTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-09-10T12:00:00Z")

    private fun fixture() =
        TrackerSnapshot(
            plan =
                TreatmentPlan(
                    zoneId = "UTC",
                    trackingStartedAt = now.minusSeconds(3600).toEpochMilli(),
                ),
            events = listOf(WearEvent(1, now.minusSeconds(3600).toEpochMilli(), true)),
        )

    @Test
    fun savingKeepsRecordedImageAndDisablesBothActions() {
        var busy by mutableStateOf(false)
        var time by mutableStateOf(now)
        compose.setContent {
            AlignerTheme { TodayScreen(fixture(), time, busy, {}, { busy = true }, {}) }
        }
        compose.onNodeWithText("Take aligners out").performClick()
        compose.onNodeWithText("Saving…").assertIsNotEnabled()
        compose.onNodeWithText("Forgot a switch? Correct it").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Aligners in").assertHasNoClickAction()
        compose.onNodeWithContentDescription("Aligners out").assertDoesNotExist()
        compose.runOnIdle { time = now.plusSeconds(120) }
        compose.onAllNodesWithContentDescription("Aligners in").assertCountEquals(1)
        compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertCountEquals(0)
    }

    @Test
    fun completedAndUnavailableNeverInventARecordedState() {
        var snapshot by mutableStateOf(fixture().copy(events = emptyList()))
        compose.setContent { AlignerTheme { TodayScreen(snapshot, now, false, {}) } }
        compose.onNodeWithText("Current status unavailable").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aligners out").assertDoesNotExist()
        compose.onNodeWithContentDescription("Aligners in").assertDoesNotExist()
        compose.runOnIdle {
            snapshot =
                fixture().let {
                    it.copy(
                        plan = it.plan!!.copy(completed = true, completedAt = now.toEpochMilli())
                    )
                }
        }
        compose.onNodeWithText("Treatment recorded as complete").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aligners in").assertDoesNotExist()
        compose.onNodeWithText("Take aligners out").assertDoesNotExist()
        compose.onNodeWithText("Put aligners in").assertDoesNotExist()
        compose.runOnIdle { snapshot = TrackerSnapshot() }
        compose.onNodeWithContentDescription("Aligners out").assertDoesNotExist()
        compose.onNodeWithContentDescription("Aligners in").assertDoesNotExist()
    }

    @Test
    fun repositoryClockRejectionLeavesStatusAndActionUnchanged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        val repository = TrackerRepository(database, Clock.fixed(now, ZoneOffset.UTC))
        val settings = (context.applicationContext as TrackerApplication).container.reminderSettings
        val models = ViewModelStore()
        try {
            runBlocking { repository.startTracking(true, "UTC") }
            val before = runBlocking { repository.snapshot() }
            lateinit var model: TrackerViewModel
            compose.setContent {
                model = remember {
                    TrackerViewModel(
                            repository,
                            settings,
                            ReminderScheduler(context, repository, settings),
                        )
                        .also { models.put("test", it) }
                }
                val snapshot by model.snapshot.collectAsState()
                val busy by model.busy.collectAsState()
                val error by model.error.collectAsState()
                AlignerTheme {
                    Column {
                        error?.let { androidx.compose.material3.Text(it) }
                        snapshot?.let { TodayScreen(it, now, busy, model::setWearing) }
                    }
                }
            }
            compose.waitUntil(5000) { model.snapshot.value != null }
            compose.onNodeWithText("Take aligners out").performClick()
            compose.waitUntil(5000) { model.error.value != null && !model.busy.value }
            compose
                .onNodeWithText("The clock is at or before the previous switch.", substring = true)
                .assertIsDisplayed()
            compose.onNodeWithContentDescription("Aligners in").assertIsDisplayed()
            compose.onNodeWithContentDescription("Aligners out").assertDoesNotExist()
            compose.onNodeWithText("Take aligners out").assertIsEnabled()
            assertEquals(before, runBlocking { repository.snapshot() })
        } finally {
            compose.runOnIdle { models.clear() }
            database.close()
        }
    }
}
