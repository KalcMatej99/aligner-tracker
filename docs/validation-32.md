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

## Signed pilot and visual acceptance

- Tagged source `91536654d68d3afb05e003d7f045f81422f9376a`, tag
  `pilot-phone-quick-start-3`; release rebuilt from the clean tagged checkout.
- Existing permanent app signer retained. Clean-environment certificate and SHA-256
  recomputation, zip alignment and package/minSDK checks passed.
- Actual currently published signed version2 installed on disposable API36; its
  schema2 fixture was restored through SAF preview/confirmation. Version3 installed
  with `adb install -r` and all original table columns matched exactly: 13 events,
  2 targets, 2 schedule revisions, 3 intervals, 2 tray rows, note, appointment and gap.
- A second same-signer upgrade preserved explicitly enabled tray-reminder DataStore
  preferences byte-for-byte (SHA256 `ae70c1e9f011a7c63b5cc05ea3a577512a22239f7f7785c279c3d712b91f4687`).
- Signed fresh quick start, SAF schema3 complete export, data reset on the emulator,
  preview/confirmed restore and force-stop/reopen preserved all six null fields,
  exact session timestamp/zone and the sole original event. R8 serialization works.
- 320dp / 200% font / dark: quick-start controls, partial Today, optional fields,
  keyboard entry and inline invalid-goal feedback inspected as scrollable layouts.
  RTL layouts inspected after emulator locale/config restart; focused quick-start
  and saving-state tests passed at 320dp/200%. Normal-size signed light-theme quick
  start and restore previews inspected. Captures include loading/saving state.
- Both Android platform accessibility tests passed with the bundled real TalkBack
  service enabled. This establishes native tree/focus/action evidence, not owner
  traversal, speech comprehension or physical-device accessibility acceptance.

Published privately at [Kalc Apps](https://apps.server.matejkalc.com/apps/org.alignertracker.app/).
[Phone APK](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_3.apk),
versionCode3 / `1.0.0-dev`, SHA256
`4c013f25d66408c97d1e26522f3c8338a168cd086cb6a7623312a88b355549c8`.
All 31 served snapshot files matched fresh HTTPS downloads, including the exact
tested APK, source and notices. Downloaded signed repository indexes verified with
unchanged fingerprint `7F72BE14125ABE9A79D5B291956D5BAA30E79E0BA8ED34441485D07C5B21BD32`.
Version2 remains available for old cached indexes. No public store or Wear APK
publication. Operator checks use the established loopback destination because the
host's LAN DNS reaches the deliberate private-route 403; global DNS/ACLs unchanged.

## Remaining owner and device acceptance

Owner must still try the revised onboarding on the Pixel and report comprehension,
assistance and actual elapsed time (proposed ≤30 seconds). Installation and physical
checks must be coordinated; none was performed automatically. Broader reminder,
accessibility, battery and paired/physical Wear gates #11/#12/#17/#20 remain separate.
#32 stays open for owner confirmation. Full translations belong to #31; this update
adds English resource-backed copy and preserves locale-aware inputs and RTL support.
