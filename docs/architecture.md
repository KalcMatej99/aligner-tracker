# Architecture and implementation contract

Status: accepted bootstrap decision, 2026-09-08. The MVP contract below is retained for history; the expanded contract at the end supersedes MVP-only module/data/target/backup limitations. See [official-source research](android-research.md).

## Scope and structure

One native Android `:app` module; Kotlin, Compose Material 3, Room, Preferences DataStore, Coroutines/Flow, ViewModel, WorkManager. Namespace/application ID: **`org.alignertracker.app`**. Minimum API26 (Android8); compile/target36. AGP8.13.2, Gradle8.13, Kotlin2.3.21; JDK21 development, JVM17 bytecode. Conservative compatible versions, not a claim of latest release. Manual constructor injection in `TrackerApplication`/`AppContainer`: three repositories/services do not justify a DI code generator. No network permission, analytics, ads, billing, account, backend, or continuously running timer service.

```
app/src/main/java/org/alignertracker/app/
  data/       Room entities/DAO/database, TrackerRepository, BackupCodec
  domain/     plain Kotlin models, interval arithmetic and schedule rules
  reminders/  alarm scheduler, receivers and recovery worker
  ui/         ViewModel, screens, theme
  AppContainer.kt, TrackerApplication.kt, MainActivity.kt
```

Room is authoritative; UI renders snapshots. Store timestamps as epoch milliseconds (`Long`) and treatment dates as ISO local dates (`String`). A fixed treatment zone ID defines daily accounting, independent of phone travel; user sees the zone. Use `java.time` day boundaries, never assume 24-hour days. No speculative feature modules or domain interfaces for every class.

## Shared API contract (all agents must preserve)

Domain data classes in `domain/Models.kt`:

- `TreatmentPlan(id: Long = 1, startDate: String, totalTrays: Int, currentTray: Int, daysPerTray: Int, currentTrayStartedOn: String, dailyGoalMinutes: Int, zoneId: String, trackingStartedAt: Long, completed: Boolean = false)`
- `WearEvent(id: Long = 0, at: Long, wearing: Boolean)` means the new state beginning at `at`; events must strictly increase in time and alternate state.
- `TrackerSnapshot(plan: TreatmentPlan? = null, events: List<WearEvent> = emptyList())`
- `DaySummary(date: String, wornMillis: Long, removedMillis: Long, trackedMillis: Long, goalMinutes: Int)`; partial day and untracked time must be visible.
- `WearMath.summarize(snapshot: TrackerSnapshot, date: LocalDate, now: Instant): DaySummary`; clip to [dayStart, min(dayEnd, now)], split all state intervals across boundaries; first event is tracking coverage start, never invent earlier wear.
- `WearMath.isWearing(snapshot): Boolean` last state or false; `WearMath.nextChangeDate(plan): LocalDate` currentTrayStartedOn + daysPerTray.

`data/TrackerRepository` constructed with Room DB (agent may inject `Clock` default systemUTC). Public API:

- `val snapshots: Flow<TrackerSnapshot>`
- `suspend fun snapshot(): TrackerSnapshot`
- `suspend fun start(plan: TreatmentPlan, wearing: Boolean)` validates ranges, zone/date; refuses replacing existing treatment.
- `suspend fun setWearing(wearing: Boolean)` idempotent, timestamp now; no changes when completed.
- `suspend fun updateEvent(id: Long, at: Long)` validated timestamp strictly between neighbors, not future; initial event may not be moved (explicit error).
- `suspend fun advanceTray()` increments only with explicit action, sets currentTrayStartedOn to today in treatment zone; refuses last tray/completed.
- `suspend fun completeTreatment()` closes tracking by completed flag plus a persisted completion timestamp (add `completedAt: Long? = null` to plan); never auto-complete.
- `suspend fun updateGoal(minutes: Int)` target range 1..1440; historical target policy: MVP uses current goal consistently and labels it, target history deferred.
- `suspend fun replaceFromBackup(snapshot: TrackerSnapshot)` validate all before one Room transaction, preserve old data if invalid.
- `suspend fun clearAll()` transaction.

`data/BackupCodec` object: `fun encode(snapshot): String`, `fun decode(json: String): TrackerSnapshot`, `fun csv(snapshot, now: Instant): String`. Versioned schema1 JSON, strict ranges/size/event validation and no merge. UTF8 max5MiB import, 50,000 events limit. CSV daily totals with tracked duration and explicit treatment timezone. Import preview then explicit replace confirmation; SAF CreateDocument/OpenDocument. Backups are plaintext and contain sensitive treatment history; tell user before export, never log contents.

`reminders/ReminderPreferences(enabled: Boolean = false, breakMinutes: Int = 30, trayEnabled: Boolean = false, precise: Boolean = false)` and `ReminderSettings(context)` exposes `val preferences: Flow<ReminderPreferences>`, `suspend fun update(preferences)`. DataStore single instance. `ReminderScheduler(context, repository, settings)` exposes `suspend fun reconcile()` and `fun cancelAll()`. Main activity + ViewModel reconciliation on resume and after mutations/settings/import/delete. Agent may add settings fields compatibly with defaults. Receivers access `(context.applicationContext as TrackerApplication).container` whose `repository`, `reminderSettings`, `reminderScheduler` are public vals.

## Invariants and failure behavior

All reads forming a snapshot and all writes use Room transactions to avoid mixing old plan/new events. Monotonic event ordering, valid plan and at most one plan. Clock rollback rejects a transition visibly rather than inventing duration; next manual correction is offered. A timer is derived from persisted state, with a foreground lifecycle-bound UI tick only. Completion clips history to `completedAt`; no future wear accrues. Day totals never negative or beyond actual covered duration. No automatic schedule changes based on adherence; projected dates are estimates and physician instructions remain primary.

## Notifications/background

Break reminders follow the last OUT event; IN cancels. Tray reminders use the stored current tray date in the treatment zone and must not change treatment. AlarmManager for user-facing timing, optional SCHEDULE_EXACT_ALARM access only if user chooses, with inexact fallback and truthful status. POST_NOTIFICATIONS requested contextually on API33+. Immutable explicit PendingIntents. Receiver re-reads database/state before posting; stale alarms do nothing. Private lock-screen content. One break reminder per break (no nagging repeats in MVP). Channel controls and force-stop/delayed delivery limitations explained in settings. WorkManager performs infrequent recovery, never minute-precision timing. Reconcile after boot, package update, time/timezone change, permission change, foreground resume, and import. No wake locks held by app code, no blanket battery exemption request.

## Privacy and backups

Room in app-private storage; Android device encryption, not a claim of app-level encryption. Disable Android automatic/cloud backup and device-transfer inclusion rules. No Internet permission or third-party telemetry. SAF grants are transient; shared export remains under user control. Export/restore only explicit user action. Delete-all requires confirmation and cancels alarms/work. No progress photos in MVP; later photo work uses Photo Picker and local private copies with retention/export design.

## Verification and CI

Pure JVM tests: midnight/DST, partial days, completion, irregular events, goal changes, schedule boundaries. Room instrumented tests: transactions, restart persistence, corrections, atomic import. Compose tests: onboarding, in/out, state persistence/navigation, permissions denied; emulator smoke plus screenshots at small width/large font/dark theme. Reminder integration matrix includes API26,33,36; Doze, boot, time changes and force-stop documented as separate physical acceptance. Gradle `testDebugUnitTest lintDebug assembleDebug` baseline; Spotless Kotlin formatting. Forgejo Actions compiles/tests/lints and preserves APK/report artifacts; emulator lane with KVM where runner supports it. Never report a green hosted CI based only on local builds. Signing key stays outside repository; release signing/store upload is separate release milestone.

## Expanded v1.0 contract (supersedes MVP-only scope above)

The phone remains `:app`; user-requested functional Wear support adds a separate installable `:wear` application with the same application ID and compatible signing identity. This is an explicit scope expansion from the initial one-module MVP. A backend is not introduced. Exact additive Room2/schema2 model, migration and ownership semantics are documented in [schema-v2.md](schema-v2.md). The original MVP APIs are compatibility projections; date summaries use historical targets, schedule revisions and actual tray boundaries persist, and detected unknown clock/phase intervals do not count as tracked time.

Native RemoteViews implements the small two-control home widget without an additional UI dependency. A rendered generation/revision and deterministic idempotency ID accompany every immutable action PendingIntent. The receiver applies the authoritative repository transaction and refreshes the widget; stale actions cannot toggle newer state. The widget has no periodic ticking. Break/tray/appointment reminders use independent channels and persisted delivery/snooze state, with stale callback validation and lifecycle recovery.

Portable archives combine strict versioned JSON and normalized private JPEGs. Only validated UUID private filenames from the trusted archive staging service enter photo ownership. ZIP member names never become filesystem paths. [Encrypted envelope](encrypted-backup.md) uses platform JCA primitives, fixed bounded parameters, random salt/nonce and authenticated header. UI operations perform file/crypto work off the main thread and restore only after preview/confirmation. Device-local preferences and sync receipts have explicit nonportable lifecycle rules in privacy.md.

Wear transport exchanges compact status/command DTOs, never complete treatment records. A durable watch queue and phone command ledger provide deterministic duplicate/stale outcomes. Local watch screens, Tile and complication render acknowledged state with pending/rejection labels. Both APKs compile shared `wear-transport` source against the Apache-2.0 microG Wear client. Its Binder adapter queries capabilities/nearby nodes and sends messages to the installed service; modern event binding actions map to the compatible listener binder. Correlation uses UUID request/reply paths under `/aligner/v1`, an expected-node pending map and timeout; durable command IDs survive retries independently of transport UUIDs. The 171-coordinate [runtime inventory](dependency-licenses.md) contains no proprietary Google artifacts.

Phone features remain usable without Google Play services, while watch communication requires a compatible Wear service and paired-device stack. Actual official-GMS local-node binder delivery passed; paired-emulator and physical transport remain unverified. No custom cloud synchronization/backend or INTERNET permission is introduced. Nearby-node checks do not guarantee that Google-controlled Data Layer traffic never uses Google infrastructure. [Wear protocol/evidence](wear.md) and [privacy boundary](privacy.md) define these limits.
