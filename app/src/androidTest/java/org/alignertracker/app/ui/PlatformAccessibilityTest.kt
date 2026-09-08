package org.alignertracker.app.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.alignertracker.app.R
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Exercises Android's exported accessibility tree and actions, not TalkBack's speech or traversal.
 */
@SdkSuppress(minSdkVersion = 30)
@Suppress("DEPRECATION")
class PlatformAccessibilityTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var automation: UiAutomation
    private var originalServiceFlags = 0
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun connectWithoutSuppressingScreenReaders() {
        automation =
            InstrumentationRegistry.getInstrumentation()
                .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        originalServiceFlags = automation.serviceInfo.flags
        val manager = context.getSystemService(AccessibilityManager::class.java)
        assertTrue(
            "Enable the emulator's bundled TalkBack before running PlatformAccessibilityTest. " +
                "Compose 1.9.5 suppresses virtual-node events when only UiAutomation is connected.",
            manager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
                .isNotEmpty(),
        )
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags =
                    flags or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            }
        compose.waitUntil(5_000) { manager.isTouchExplorationEnabled }
    }

    @After
    fun restoreAutomationServiceFlags() {
        if (::automation.isInitialized) {
            automation.serviceInfo = automation.serviceInfo.apply { flags = originalServiceFlags }
        }
    }

    @Test
    fun todayExportsStateAndRespondsToPlatformFocusAndClick() {
        compose.setContent {
            var snapshot by remember { mutableStateOf(example()) }
            AlignerTheme {
                TodayScreen(snapshot, Instant.parse("2026-09-08T12:00:00Z"), false) { wearing ->
                    snapshot = snapshot.copy(events = snapshot.events + WearEvent(3, NOW, wearing))
                }
            }
        }
        val heading = awaitNode("Today heading") { it.isHeading && it.names(R.string.wear_heading) }
        assertTrue(heading.isVisibleToUser)
        heading.recycle()
        val action = awaitNode("Put in action") { it.isClickable && it.names(R.string.put_in) }
        assertEquals(context.getString(R.string.state_out), action.stateDescription?.toString())
        val event =
            automation.executeAndWaitForEvent(
                {
                    assertTrue(
                        action.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                    )
                },
                {
                    it.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED &&
                        it.packageName?.toString() == context.packageName
                },
                5_000,
            )
        event.recycle()
        val focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        assertEquals("The exported primary action receives accessibility focus", action, focused)
        focused?.recycle()
        assertTrue(action.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        action.recycle()
        val changed =
            awaitNode("Take out action after platform click") {
                it.isClickable && it.names(R.string.take_out)
            }
        assertEquals(context.getString(R.string.state_in), changed.stateDescription?.toString())
        changed.recycle()
    }

    @Test
    fun invalidCorrectionExportsFieldErrorAndCancelClosesItsWindowWithoutSaving() {
        var saved = false
        compose.setContent {
            var open by remember { mutableStateOf(true) }
            AlignerTheme {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Underlying screen")
                }
                if (open) {
                    val snapshot = example()
                    EditEventDialog(
                        snapshot,
                        snapshot.events.last(),
                        false,
                        onDismiss = { open = false },
                        onSave = { saved = true },
                    )
                }
            }
        }
        val heading =
            awaitNode("Correction dialog heading") {
                it.isHeading && it.names(R.string.edit_event_title)
            }
        val dialogWindow = heading.windowId
        heading.recycle()
        val timestamp =
            awaitNode("Timestamp editor") {
                it.isEditable && it.text?.contains("2026-09-08 10:00:00") == true
            }
        assertEquals(dialogWindow, timestamp.windowId)
        assertTrue(
            timestamp.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        "invalid",
                    )
                },
            )
        )
        timestamp.recycle()
        click(R.string.save)
        val invalid =
            awaitNode("Invalid timestamp with a specific exported error") {
                it.isEditable &&
                    it.isContentInvalid &&
                    it.error?.toString() == context.getString(R.string.timestamp_invalid)
            }
        assertEquals("invalid", invalid.text?.toString())
        assertEquals(dialogWindow, invalid.windowId)
        invalid.recycle()
        compose.runOnIdle { assertFalse("Invalid input must not invoke save", saved) }
        click(R.string.cancel)
        val underlying =
            awaitNode("Underlying window after cancellation") {
                it.text?.toString() == "Underlying screen" && it.windowId != dialogWindow
            }
        underlying.recycle()
        compose.runOnIdle { assertFalse("Cancellation must not invoke save", saved) }
    }

    private fun click(label: Int) {
        val node = awaitNode(context.getString(label)) { it.isClickable && it.names(label) }
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        node.recycle()
    }

    private fun AccessibilityNodeInfo.names(label: Int): Boolean {
        val name = context.getString(label)
        var remaining = 64
        // Android exports Compose's control/heading wrapper separately from its text children.
        // Match within that group; never borrow a label from an independently actionable child.
        fun containsName(node: AccessibilityNodeInfo, depth: Int): Boolean {
            if (remaining-- <= 0 || depth > 8) return false
            if (
                node.text?.contains(name) == true || node.contentDescription?.contains(name) == true
            )
                return true
            repeat(node.childCount) { index ->
                val child = node.getChild(index) ?: return@repeat
                val matches =
                    try {
                        child.isVisibleToUser &&
                            !child.isClickable &&
                            containsName(child, depth + 1)
                    } finally {
                        child.recycle()
                    }
                if (matches) return true
            }
            return false
        }
        return containsName(this, 0)
    }

    private fun awaitNode(
        description: String,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        var found: AccessibilityNodeInfo? = null
        var diagnostic = "No accessibility tree retrieved"
        // Unlike a Compose node interaction, querying UiAutomation does not synchronize layout.
        compose.waitForIdle()
        try {
            compose.waitUntil(5_000) {
                val nodes = mutableListOf<AccessibilityNodeInfo>()
                fun collect(node: AccessibilityNodeInfo) {
                    nodes += node
                    repeat(node.childCount) { index -> node.getChild(index)?.let(::collect) }
                }
                automation.rootInActiveWindow?.let(::collect)
                found = nodes.firstOrNull { it.isVisibleToUser && predicate(it) }
                diagnostic =
                    nodes
                        .joinToString("\n") {
                            "window=${it.windowId} package=${it.packageName} class=${it.className} " +
                                "visible=${it.isVisibleToUser} heading=${it.isHeading} " +
                                "clickable=${it.isClickable} editable=${it.isEditable} " +
                                "text=${it.text} description=${it.contentDescription} error=${it.error}"
                        }
                        .ifEmpty { "rootInActiveWindow=null; windows=${automation.windows}" }
                nodes.filter { it !== found }.forEach { it.recycle() }
                found != null
            }
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError(
                "Missing Android accessibility node: $description\n$diagnostic",
                timeout,
            )
        }
        return checkNotNull(found) { "Missing Android accessibility node: $description" }
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
            listOf(WearEvent(1, start, true), WearEvent(2, start + 3_600_000, false)),
        )
    }

    private companion object {
        val NOW: Long = Instant.parse("2026-09-08T12:00:00Z").toEpochMilli()
    }
}
