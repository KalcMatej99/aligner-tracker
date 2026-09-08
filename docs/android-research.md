# Android implementation research

Researched 2026-09-08 against official Android, Kotlin, Google Maven and Google KSP sources. These are implementation decisions, not claims that the app has passed device testing.

## Stable baseline

Use one Android application module, Kotlin, Compose Material 3, manual dependency injection, Room, and Preferences DataStore. The deliberately conservative toolchain below supports API 36; it is not the latest available Android stack. AGP 9.3 supports API 37 but adds the AGP 9 built-in Kotlin integration. Upgrade together in a separate tested change when API 37 becomes an app requirement. [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes).

| Component | Pin | Evidence / reason |
| --- | --- | --- |
| AGP | 8.13.2 | Supports Kotlin 2.3 and SDK up to 36.1 |
| Gradle wrapper | 8.13 | AGP 8.13 documented requirement |
| Java | JDK 21 runtime; JVM 17 bytecode | AGP requires JDK 17 minimum; Gradle 8.13 can run on JDK 21 |
| Kotlin Android and Compose compiler plugins | 2.3.21 | Kotlin compatibility table covers Gradle 8.13 and AGP 8.13.2 |
| KSP2 | 2.3.8 | Stable Google release, Kotlin 2.3 language generation |
| Android SDK | min 26; compile 36; target 36 | Native java.time, notification channels, and supported AGP baseline |
| Compose BOM | 2025.11.01 | Stable, includes Compose UI 1.9.5; align all Compose dependencies with BOM |
| Activity Compose | 1.11.0 | Compatible API 36 release |
| Lifecycle runtime/viewmodel Compose | 2.9.4 | Stable compatible baseline |
| Room runtime/compiler/testing | 2.8.4 | Stable, min SDK 23; KSP code generation |
| DataStore Preferences | 1.2.1 | Stable, preferences only |
| WorkManager, if needed | 2.11.2 | Stable, optional for deferred work |

Sources: [Kotlin compatibility matrix](https://kotlinlang.org/docs/gradle-configure-project.html), [Gradle Java compatibility](https://docs.gradle.org/8.13/userguide/compatibility.html), [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [KSP 2.3.8](https://github.com/google/ksp/releases/tag/2.3.8), [BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.11.01/compose-bom-2025.11.01.pom), [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle), [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore), [WorkManager](https://developer.android.com/jetpack/androidx/releases/work).

Downloaded Google Maven AAR metadata independently confirms these minimum compile SDK / AGP pairs: Compose UI 1.9.5: 35 / 8.6.0; Activity 1.11.0: 36 / 8.9.1; Lifecycle runtime 2.9.4: 34 / 8.1.1; WorkManager 2.11.2: 35 / 8.6.0; DataStore Preferences 1.2.1: 34 / 8.1.1. This validates declared metadata, not a successful project build. Pin versions rather than using dynamic dependencies. Keep Compose compiler plugin version equal to Kotlin; BOM does not manage compiler, Activity, Room, or Lifecycle versions.

## State and data integrity

Application-scoped container creates one Room database, repository, settings store and scheduler. ViewModels expose immutable StateFlow; Compose collects with lifecycle awareness and sends explicit actions. Keep wear arithmetic as pure Kotlin functions accepting clocks and zones. Room is the durable source of truth, with transaction-protected session changes. The UI's visible ticker derives elapsed time from stored timestamps and never creates database writes every second. This follows the recommended repository, ViewModel and unidirectional data-flow architecture. [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations).

Store aligner plans, wear sessions, edits and clinical target settings in Room when they must change atomically. Use DataStore for independent display preferences. A Room transaction cannot atomically commit DataStore. Enforce one open session and no overlapping intervals within the database transaction; reject conflicting edits and invalid imports before writing. Export Room schemas and preserve data with tested migrations; do not use destructive migration fallback. [Room release and schema guidance](https://developer.android.com/jetpack/androidx/releases/room).

## Reminders and Android limits

Default to one-shot `AlarmManager.setAndAllowWhileIdle` reminders, with copy explaining Android may delay delivery. No foreground service or persistent wake lock is needed for a running timer. WorkManager is for deferred reconciliation or maintenance, not minute-precise reminders. If precise alarms become an explicit product requirement, offer `SCHEDULE_EXACT_ALARM`, check `canScheduleExactAlarms`, reschedule after access grants, and fall back on denial or revocation. Do not claim unrestricted `USE_EXACT_ALARM` eligibility. [Alarm scheduling](https://developer.android.com/develop/background-work/services/alarms).

Ask for `POST_NOTIFICATIONS` only after a user enables reminders on API 33+. Tracking must keep working when permission is denied or a channel is disabled. Distinguish the app toggle, system notification permission and notification channel state in settings. [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

Doze and battery restrictions can delay reminders. Test idle behavior and explain it accurately instead of routinely requesting battery-optimization exemptions. A short broadcast receiver should verify current persisted state before posting; a stale reminder must not notify after an aligner is reinserted. [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby).

Force stop prevents reliable reminders until the user reopens/interacts with the app; Android 15 also cancels pending intents. Rebuild schedules on launch and after boot, package replacement, time and time-zone changes. Listen for `BOOT_COMPLETED` with `RECEIVE_BOOT_COMPLETED`; ordinary credential-protected history remains unavailable before first unlock. Do not promise reminder delivery while stopped or powered off. [Stopped-state behavior](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state), [alarm reboot guidance](https://developer.android.com/develop/background-work/services/alarms).

Scheduler design decision: stable pending-intent identities per reminder type; cancel before replacement, re-read current session before notification, and ensure scheduling failures never roll back a successful wear-state commit. Include session identity in the reminder so an old callback cannot announce a new session's elapsed time.

## Clock, midnight and travel

Persist UTC instants plus the relevant zone identifier. Use `elapsedRealtime` for an active duration within a known boot because it includes sleep and is monotonic; never reuse its origin after reboot. Compare wall-clock and monotonic deltas to detect significant clock changes and offer correction when elapsed history is uncertain. Wall-clock-only implementations must document that manual clock changes affect duration. [SystemClock](https://developer.android.com/reference/android/os/SystemClock).

Product policy: choose and display a reporting zone. Calculate day boundaries with `LocalDate.atStartOfDay(zone)` and the next local date, not fixed 24-hour arithmetic. A DST day can be 23 or 25 hours. Split sessions by intersecting their instant interval with each day interval; never count unknown time before tracking began as worn. Reminder local times should be recalculated after zone changes. Java time resolves gaps and overlaps; select and test the intended behavior explicitly. [ZonedDateTime](https://developer.android.com/reference/java/time/ZonedDateTime).

## Backup and privacy

No account, analytics, advertisements or INTERNET permission is needed. Export versioned JSON through `ACTION_CREATE_DOCUMENT`; import with `ACTION_OPEN_DOCUMENT`, without broad storage permission. Explain that the chosen document provider may be a cloud service and exported health-related history is readable unless encryption is explicitly implemented. Handle picker cancellation and I/O errors without modifying existing history. [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files).

Disable automatic backup and explicitly exclude database/files/preferences from both legacy backup rules and Android 12+ cloud/device-transfer rules. `allowBackup=false` alone may not block every OEM device transfer. Do not claim that the application sandbox alone is application-level encryption. [Auto Backup configuration](https://developer.android.com/identity/data/autobackup).

Import decision: bound input size and record counts, validate format version, numbers, timestamps, identifiers, referential integrity, overlap and open-session invariants; preview a replacement and obtain user confirmation before a single Room transaction. Export a consistent logical database snapshot rather than copying a live SQLite file without its WAL. Round-trip test history and settings, including an active session, malformed input and unsupported schema versions.

## Acceptance and CI

Run pure domain tests for overlap, duplicate taps, midnight, DST, unknown time, target changes and clock rollback. Instrument Room transaction/migration tests and a Compose onboarding-to-toggle-to-history flow using AndroidJUnitRunner. A build or unit test pass is not reminder acceptance. [AndroidJUnitRunner](https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/runner).

CI should run Gradle unit tests, lint and debug assembly on pinned Java/Gradle, then instrumented tests on a pinned emulator with KVM. Test API 26 and API 36 before release. Physical-device acceptance covers process kill, force stop/reopen, reboot/unlock, screen off/Doze, notification denial, disabled channels, timezone changes and export/import cancellation. Record unavailable hardware or emulator cases as untested, not passed.

For UI acceptance, check dark/light mode, a compact phone and wider layout, large font scale, scrolling with the keyboard, empty/loading/error states, 48 dp touch targets, readable contrast and TalkBack labels/order. Timer updates must not continuously announce via accessibility live regions. Automated Compose accessibility checks complement manual TalkBack and visual review. [Compose accessibility testing](https://developer.android.com/develop/ui/compose/accessibility/testing).
