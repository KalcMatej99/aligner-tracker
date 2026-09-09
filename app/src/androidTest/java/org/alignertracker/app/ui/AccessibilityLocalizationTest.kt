package org.alignertracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.alignertracker.app.domain.TrackerSnapshot
import org.alignertracker.app.domain.TreatmentPlan
import org.alignertracker.app.domain.WearEvent
import org.alignertracker.app.reminders.ReminderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilityLocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun largeTextKeepsPrimaryAndDataActionsReachable() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                AlignerTheme {
                    Box(Modifier.width(320.dp).height(480.dp)) {
                        SettingsScreen(
                            plan = example().plan,
                            preferences = ReminderPreferences(),
                            busy = false,
                            notificationsAllowed = true,
                            exactAllowed = true,
                            breakChannelAllowed = true,
                            trayChannelAllowed = true,
                            onRequestNotifications = {},
                            onOpenNotificationSettings = {},
                            onRequestExact = {},
                            onGoal = {},
                            onReminders = {},
                            onExport = {},
                            onImport = {},
                            onDelete = {},
                        )
                    }
                }
            }
        }

        compose
            .onNodeWithText("Update target")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        compose
            .onNodeWithText("Export encrypted backup")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        compose
            .onNodeWithText("Restore encrypted backup")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun reminderLabelAndSwitchAreOneLargeToggleTarget() {
        compose.setContent {
            AlignerTheme {
                SettingsScreen(
                    plan = example().plan,
                    preferences = ReminderPreferences(),
                    busy = false,
                    notificationsAllowed = true,
                    exactAllowed = true,
                    breakChannelAllowed = true,
                    trayChannelAllowed = true,
                    onRequestNotifications = {},
                    onOpenNotificationSettings = {},
                    onRequestExact = {},
                    onGoal = {},
                    onReminders = {},
                    onExport = {},
                    onImport = {},
                    onDelete = {},
                )
            }
        }

        compose
            .onNodeWithText("Remind me after a break")
            .performScrollTo()
            .assertIsToggleable()
            .assertIsOff()
            .assertHeightIsAtLeast(56.dp)
            .performClick()
            .assertIsOn()
    }

    @Test
    fun shortLandscapeKeepsTodayActionReachableAtLargeText() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                AlignerTheme {
                    Box(Modifier.width(640.dp).height(320.dp)) {
                        TodayScreen(example(), Instant.parse("2026-09-08T12:00:00Z"), false, {})
                    }
                }
            }
        }

        compose
            .onNodeWithText("Put aligners in")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun invalidEntryExposesErrorSemanticsAlongsideVisibleText() {
        compose.setContent {
            AlignerTheme {
                SettingsScreen(
                    plan = example().plan,
                    preferences = ReminderPreferences(),
                    busy = false,
                    notificationsAllowed = true,
                    exactAllowed = true,
                    breakChannelAllowed = true,
                    trayChannelAllowed = true,
                    onRequestNotifications = {},
                    onOpenNotificationSettings = {},
                    onRequestExact = {},
                    onGoal = {},
                    onReminders = {},
                    onExport = {},
                    onImport = {},
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithText("Prescribed daily hours (optional)").performTextReplacement("0")
        compose.onNodeWithText("Update target").performScrollTo().performClick()
        compose
            .onNodeWithText("Prescribed daily hours (optional)")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose
            .onNodeWithText(
                "Enter prescribed hours greater than 0 and no more than 24, in whole minutes."
            )
            .assertIsDisplayed()
    }

    @Test
    fun rtlHistoryPlacesPreviousAtTheReadingStart() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AlignerTheme {
                    Box(Modifier.width(320.dp).height(480.dp)) {
                        HistoryScreen(example(), Instant.parse("2026-09-08T12:00:00Z"), false, {})
                    }
                }
            }
        }

        val previous =
            compose.onNodeWithText("Previous day").performScrollTo().getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText("Next day").getUnclippedBoundsInRoot()
        assertTrue("Previous should begin on the RTL reading side", previous.left > next.left)
    }

    @Test
    fun datesAndUnicodeDigitsFollowTheUsersLocale() {
        val date = LocalDate.of(2026, 9, 8)
        val english = formatDateForLocale(date, Locale.US)
        val german = formatDateForLocale(date, Locale.GERMANY)

        assertNotEquals(english, german)
        assertTrue(english.contains("2026"))
        assertTrue(german.contains("2026"))
        assertEquals(22, parseUserInteger("٢٢"))
        assertEquals(1290, parseUserHours("٢١٫٥"))
    }

    @Test
    fun themeTextPairsMeetNormalTextContrast() {
        listOf(
                LightColors.onPrimary to LightColors.primary,
                LightColors.onPrimaryContainer to LightColors.primaryContainer,
                LightColors.onSurface to LightColors.surface,
                LightColors.onSurfaceVariant to LightColors.surfaceContainerLow,
                LightColors.error to LightColors.surfaceContainerLow,
                DarkColors.onPrimary to DarkColors.primary,
                DarkColors.onPrimaryContainer to DarkColors.primaryContainer,
                DarkColors.onSurface to DarkColors.surface,
                DarkColors.onSurfaceVariant to DarkColors.surfaceContainerLow,
                DarkColors.error to DarkColors.surfaceContainerLow,
            )
            .forEach { (foreground, background) ->
                assertTrue(
                    "Contrast ${contrast(foreground, background)} for $foreground on $background",
                    contrast(foreground, background) >= 4.5,
                )
            }
    }

    private fun contrast(first: Color, second: Color): Float {
        val light = maxOf(first.luminance(), second.luminance())
        val dark = minOf(first.luminance(), second.luminance())
        return (light + 0.05f) / (dark + 0.05f)
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
            listOf(
                WearEvent(1, start, true),
                WearEvent(2, Instant.parse("2026-09-08T10:00:00Z").toEpochMilli(), false),
            ),
        )
    }
}
