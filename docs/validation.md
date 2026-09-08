# MVP validation — 2026-09-08

**Internal-evaluation MVP accepted locally.** This is a debug build; physical-device reliability, hosted CI execution and production release signing remain separate gates. No medical effectiveness claim is made.

## Automated evidence

| Check | Result |
|---|---|
| JVM / Robolectric | **34 tests passed**: 7 backup, 6 transactional repository, 7 wear arithmetic, 4 reminder rules, 10 actual scheduler/shadow tests. |
| Android instrumentation | **9 tests passed, 0 failures/errors**, API36 Android16 x86_64 emulator: real database reopen; 6 Compose screen tests; full real-activity journey; large-font adaptive navigation. |
| Formatting | Spotless/ktfmt apply and check passed. |
| Android lint | **0 errors, 19 warnings**. Warnings concern deliberately pinned compatible dependency updates, plural/localization candidates, unused resources and minor typography/KTX suggestions. Localization polish is #12; dependency/release audit is #21. |
| Build | Debug APK assembled and installed successfully. No crash-buffer entries during the manual walkthrough. |
| Repository | `git diff --check` passed; wrapper distribution SHA256 pinned, CI action references pinned to commits. |

Commands used: `./gradlew testDebugUnitTest assembleDebug lintDebug`; after UI corrections, `./gradlew spotlessApply spotlessCheck lintDebug connectedDebugAndroidTest`. The final full instrumentation run completed in30s, tests19.054s. Unchanged domain/reminder tests were not needlessly rerun after navigation-only changes. JDK21, Gradle8.13, compile/target36 and build-tools35.0.0. Robolectric SDK cache is project-local under ignored `.gradle/robolectric-maven` to avoid relying on a writable global Maven cache.

APK: `app/build/outputs/apk/debug/app-debug.apk` (about13MB). SHA256:

```
10e6dcd044a556bdc45dd4c706294886df7bad794ead8af1cfb06afdc1067d08
```

## Manual emulator evidence

Only synthetic records were used. No physical device was connected or altered.

- Launched onboarding; no account/network setup. Instrumentation entered prescribed values and explicitly tested OUT as initial state and invalid setup rejection.
- Imported a synthetic schema1 file through Android DocumentsUI/Downloads → inspected two-event preview → explicitly replaced → Today displayed expected tray2/20, Europe/Rome and current OUT. Repeated with the exported three-event backup later.
- Force-stopped/relaunched the installed app: OUT persisted; Put IN created the next event.
- Exported JSON through CreateDocument/Downloads, pulled the resulting file and checked schema1, three events, latest IN and tray2. Exported CSV and verified timezone and `tracked_millis = worn_millis + removed_millis`, with partial coverage.
- Used History → Edit time to correct the latest IN from16:28:31 to16:27:31. Saved timestamp displayed correctly; neighbors/initial marker remained visible.
- Enabled break reminders through the contextual Android notification permission prompt; chose1minute and saved. While app was backgrounded, notification1001 was actually posted with private visibility. Reopening preserved the delivered marker; recording IN cleared the notification. This proves one awake-emulator path, not Doze/OEM reliability or exact latency.
- Inspected ordinary portrait, dark theme,320dp width with200% system font, and a constrained landscape layout. Found mid-word bottom-navigation wrapping, opened #24, implemented two-row adaptive text tabs, and verified all four labels/touch/selected semantics in an instrumented regression and actual screenshot. At large font the main action remains reachable by scrolling; font scaling is not capped.
- The real-activity automated journey covers setup → OUT → activity recreation → IN → confirmed next tray → confirmed completion → confirmed delete, with Room state assertions. Separate persistence test closes/reopens the on-disk database.

Screenshots: [onboarding](screenshots/onboarding-api36.png), [Today](screenshots/today-api36.png), [history correction](screenshots/history-api36.png), [large-text navigation before](screenshots/today-320dp-font200-dark.png), [after](screenshots/today-320dp-font200-dark-fixed.png), [large-text schedule](screenshots/schedule-320dp-font200-dark.png), [landscape](screenshots/today-landscape.png).

## Privacy review

APK permission inspection confirmed **no INTERNET, camera, broad storage, advertising ID, location or account permission**. App explicitly requests notifications, boot recovery and optional exact alarms. WorkManager merges WAKE_LOCK, ACCESS_NETWORK_STATE and FOREGROUND_SERVICE permissions for its standard runtime; this app never starts a foreground timer/worker or requests a battery exemption. AndroidX adds its signature-level non-exported receiver permission. Automatic backup and device-transfer domains are excluded; JSON/CSV exports require system-picker choice and a plaintext warning. Reminder receipts contain synthetic event identity/timestamp locally and never leave the app. Data deletion resets reminder preferences/receipts and cancels pending work/alarms; external exports remain user-owned files.

One read-only reminder/privacy review found no material source blocker. Integration review corrected import-success/error handling, channel-block status and extreme history-date input. The large-font defect #24 was fixed and rechecked. No competitor implementation/assets were copied.

## CI and remaining gates

Forgejo Actions workflow is configured to run formatting, unit tests, lint and debug build and preserve report/APK artifacts. **Hosted execution is not accepted:** live Forgejo admin API reports shared runner `kalc-server-docker` offline; repository has no dedicated runner. #23 records the exact dependency. A local passing build is not reported as a hosted CI pass; shared runner infrastructure was not reconfigured.

Open Beta gates: API26/33 and OEM physical-device alarms, reboot/Doze/force-stop/revocation matrix (#11); TalkBack, contrast audit, localization/RTL and broader layout validation (#12);24h battery/startup/large-history measurements (#17); consented real-user pilot (#18). Clock-forward jumps while no events are created cannot be distinguished from real elapsed time by this wall-clock MVP; rollback behind an event is rejected. Reports use a fixed treatment zone and the current goal; historical goals/variable phases are #14. Notifications cannot bypass force-stop, channel controls or OEM restrictions. Post-before-persist delivery markers allow a duplicate in a narrow crash window; no exactly-once claim is made.

Encrypted backups (#16), widgets (#13), photos (#19), Wear OS (#20), richer reports/streaks (#22), and signing/distribution (#21) are explicit backlog work. There is no published production/store release or hosted CI result yet.
