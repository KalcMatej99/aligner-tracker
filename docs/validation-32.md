# Issue32: tracking-first phone update

Scope: private phone development pilot, versionCode3 / 1.0.0-dev. This is not
stable-v1 or owner/device acceptance. No physical phone was connected or modified.

## Implementation and material review

Quick start takes an explicit IN/OUT choice and no typed fields. The transaction
captures timestamp/accounting zone; absent prescription fields remain null.
Optional setup and explicit corrections preserve original wear events and prior
schedule versions. New targets do not backfill historical adherence. Room3,
portable3, existing schema1/schema2 import and Wear protocol1 nullable tray are
specified in [schema3](schema-v3.md).

One independent material review found two issues: incoherent schema3 archives
could omit schedule/target history, and established details lacked a correction
route. Both were fixed. Focused regression tests reject the malformed archives
and verify explicit corrections retain original events, targets, prior schedule
revisions/intervals, and closed tray attribution. No further speculative review.

## Automated and emulator evidence

- 61 phone JVM tests and 9 watch JVM tests: no failures/errors/skips. New cases cover
  duplicate start, unknown fields, DST coverage, partial reminders, later effective
  target, absent-total due date, schedule attachment/correction and plain/encrypted
  round trips, atomic malformed restore rejection and old-peer nullable tray.
- `scripts/check.sh --offline`: passed formatting, both JVM suites, debug/release
  lint, phone/watch debug/test APK assembly and both minified release builds.
- API36 phone general instrumentation: 37 cases passed; the new migration class
  initially failed JUnit discovery due to inferred Boolean return. Corrected to
  Unit and focused rerun passed both migrations (39 actual test cases in aggregate).
- Migration evidence checks every existing column of the seeded treatment, events,
  phases, revisions, intervals, tray/target history, notes, gaps and state. Both
  1→2→3 and 2→3 paths pass without destructive fallback.
- Real activity journey passes quick start → OUT → recreation → IN → optional
  schedule attachment with exact event preservation → advance → completion → delete.
  Persistent partial session and complete encrypted archive restore survive reopen.
- Four API36 Wear instrumentation cases passed on the updated watch debug APK.
  No paired-device delivery claim; earlier different-signer emulator installs were
  replaced only on disposable emulators. They are not release-upgrade evidence.

Local raw logs, screenshots and signed artifact evidence are retained under
`/home/matejkalc/.local/share/aligner-tracker/releases/quick-start-3/`.

## Remaining owner and device acceptance

Owner must still try the revised onboarding on the Pixel and report comprehension,
assistance and actual elapsed time (proposed ≤30 seconds). Installation and physical
checks must be coordinated; none was performed automatically. Broader reminder,
accessibility, battery and paired/physical Wear gates #11/#12/#17/#20 remain separate.
#32 stays open for owner confirmation. Full translations belong to #31; this update
adds English resource-backed copy and preserves locale-aware inputs and RTL support.
