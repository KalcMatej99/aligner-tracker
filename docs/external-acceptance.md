# External acceptance procedures

Status: procedures only; no physical acceptance results are claimed. The owner has chosen to be the sole tester and to install/test all phone features on their Pixel on **2026-09-09**. Follow the ordered [Pixel acceptance session](pixel-acceptance.md). This replaces the former five-person pilot requirement. Account changes, release signing/publication and unrelated device changes are not part of that session. Accelerated emulator results supplement human/device evidence.

Use only synthetic treatment data. Give every run an evidence ID and record the source commit, APK SHA-256, signer fingerprint, device model, Android/Wear OS version, security patch, OEM build, Google Play Services version, locale, time zone, permission/channel state, battery restrictions, exact steps, wall-clock timestamps, screen recording or screenshots, redacted logs, expected result, observed result and pass/fail. Keep private treatment records, personal photos and device/account identifiers out of committed evidence. The owner may use their own records after synthetic acceptance, but destructive cases must use disposable synthetic records only.

## Physical phone and OEM reminder matrix (#11)

The wider release matrix remains open independently of the owner Pixel pilot; it is not an entry requirement for that session. For broad device acceptance, run the entire matrix on physical devices covering API 26, API 33 and API 36. Include a current Pixel/AOSP-like device and at least two non-Google OEM software builds; record their exact background and auto-start controls rather than assigning an unverified “aggressive” label. Test the lowest supported API, the notification-runtime-permission boundary and the current target API. If one device covers two properties, retain at least three distinct physical devices overall.

For each dedicated test device, use a fresh candidate install and a synthetic plan. On the personal Pixel preserve any existing install and records; verify signer compatibility before an in-place update and never uninstall to work around a signature mismatch. Coordinate reboot, time changes, force-stop, Doze and system settings changes with the owner during the scheduled session, restore original settings, and record any case not run:

1. Leave notification permission denied or the app channel blocked. Schedule break, tray and appointment reminders. Confirm the app reports the block truthfully, does not claim delivery, and tracking remains usable.
2. Grant notifications. Test the normal inexact path, then the exact-alarm path where the OS exposes and grants special access. Revoke exact access and confirm reconciliation selects the documented fallback without a crash or stale exact-alarm claim.
3. Schedule each reminder at least ten minutes ahead. Turn the screen off and leave the app backgrounded. Record scheduled and observed post times, notification content, action availability and whether each logical reminder posts once.
4. Enter Doze with the device unplugged using `adb -s SERIAL shell dumpsys battery unplug` followed by `adb -s SERIAL shell dumpsys deviceidle force-idle`; confirm the state with `adb -s SERIAL shell dumpsys deviceidle` and repeat reminder delivery. Exit with `adb -s SERIAL shell dumpsys deviceidle unforce` and `adb -s SERIAL shell dumpsys battery reset`. Record delayed delivery as observed behavior; do not silently move the expected time.
5. Reboot with reminders pending. Unlock once if the OEM requires it, leave the app unopened, and verify boot reconciliation and absence of stale/duplicate notifications.
6. Change wall time forward and backward, then change the time zone with reminders pending. Restore automatic time afterward. Confirm tracking's clock warning/gap behavior, rescheduled future reminders, and suppression of stale alarms.
7. Install the next same-signed version with `adb install -r` while reminders are pending. Verify package-replaced reconciliation and one valid future reminder.
8. Force-stop the app from system settings with a reminder pending. Confirm the platform blocks delivery until the user launches the app again; after launch, confirm the UI explains/reconciles current state without claiming the missed reminder fired.
9. Exercise notification actions and snooze twice, including a rapid repeated tap. Verify one durable state transition or snooze per intended action and no stale action after the phone state changes.
10. Disable every OEM-specific auto-start/background switch available to the app, repeat the pending-reminder cases, then restore each switch one at a time. Record exact labels and results. Do not request a blanket battery-optimization exemption as a workaround.

Useful captures include `adb -s SERIAL shell dumpsys alarm`, `dumpsys jobscheduler`, `dumpsys notification --noredact` only with synthetic content, and a filtered logcat started before scheduling. Redact device/account identifiers before preserving evidence. A device row passes only when every result is recorded, no data is lost, denied states are truthful, and no stale or duplicate action changes history. One passing OEM does not generalize to another.

## Physical 24-hour battery and wakeup study (#17)

These longer measurements are a separate release gate, not a fixed-duration obligation or prerequisite for the one-owner pilot. Measure phone and watch separately on the same candidate when the hardware is available. Use a stable device configuration, connectivity, brightness/always-on setting, notification settings and daily interaction script. Do not compare an idle baseline with an unusually busy candidate day.

Run at least two 24-hour baseline periods and two 24-hour candidate periods per device, alternating their order. The phone baseline keeps the app installed with no active plan, reminders or watch connection; the candidate period uses a synthetic active plan, paired watch, three IN/OUT cycles, one note, one history view, one scheduled reminder and the normal Tile/complication refresh pattern. The watch baseline keeps the companion installed but inactive and the candidate period uses the same paired script. Start each run at a matched charge level, unplugged, and record ambient display/radio conditions and unrelated device use.

At the start and end of each run:

```bash
adb -s SERIAL shell dumpsys batterystats --reset
# Unplug and execute the recorded 24-hour script.
adb -s SERIAL shell dumpsys batterystats --charged > batterystats.txt
adb -s SERIAL bugreport bugreport.zip
```

Extract the application UID's estimated drain, foreground/background CPU time, partial wake locks, wakeups, alarms, jobs and network bytes. Network bytes attributable to the app should remain absent except local Wear transport accounted for by system services. Use Perfetto/System Trace or Android Studio Power Profiler for any suspicious interval. Android's [battery investigation guide](https://developer.android.com/topic/performance/power/setup-battery-historian) notes that Battery Historian is no longer actively maintained, so treat it as an optional view of the retained bugreport rather than the sole result.

Report raw values for every run, median baseline/candidate difference and run-to-run range. Set numerical phone and watch budgets only after this evidence exists; the budget must name metric, unit, scenario and device. A release passes after the agreed budgets are recorded and every measured regression above them is fixed or explicitly accepted. This document intentionally invents no battery threshold before measurement.

For startup and 50,000-event performance in #17, capture cold-start time and frame timing on at least one low/mid-range physical phone plus the primary phone, and time a maximum-size report/export with peak memory. Preserve the benchmark command and raw output, then set numerical budgets from the observed distribution. A single emulator measurement does not close the issue.

## TalkBack and scaled-content audit (#12)

Follow Android's [accessibility testing guidance](https://developer.android.com/guide/topics/ui/accessibility/testing): enable TalkBack, use linear left/right swipe navigation and double-tap activation, then repeat core screens using explore-by-touch. Enable visible speech output for the evidence recording. Perform one pass without looking at the display.

On a physical phone, complete setup, Today IN/OUT, schedule change, tray advance/completion, refinement/retention restart, event correction, missing-interval insertion, history/calendar navigation, note and appointment CRUD, photo import/compare/delete, reports, widget and notification actions, encrypted export, cancelled restore, wrong-password restore, confirmed replacement restore and delete-all confirmation/cancel. On a physical watch, cover app launch, sync, IN/OUT, pending acknowledgement, rejection, Tile and complication.

For every screen verify that all actionable elements are reachable once in a logical order; spoken labels identify purpose and current state; disabled controls explain why; headings and grouped content are understandable; focus survives validation errors and returns sensibly after dialogs; transient success/error messages are announced; destructive actions state their target; no operation depends only on color, position or gesture; and no decorative element receives focus. Repeat at 200% font, maximum display size, 320 dp width, landscape where supported, dark theme and one installed RTL system locale. Restore the device's original display and locale settings afterward. Record clipping, hidden controls, focus traps, duplicate announcements and ambiguous dates as defects. Automated semantics tests supplement this audit but do not replace it.

## Physical Wear OS acceptance (#20)

The owner has a Garmin, not a Wear OS watch. This gate remains untested; the Pixel session neither satisfies it nor adds Garmin support or removes Wear from v1 scope. When suitable hardware is available, use a physical Google Play Services-capable Wear OS device paired to the physical phone. Install phone and watch APKs built from the same commit, version and signing certificate. Record Wear OS and Play Services versions. Keep phone history authoritative throughout.

1. With no phone plan, sync and verify the watch says setup is required. Create the phone plan, sync, and compare tray, completion and acknowledged IN/OUT state in app, Tile and complication.
2. From a fresh acknowledged revision, record the opposite state on the watch. Verify a pending indication appears immediately, the phone records one event at phone receipt time, and all watch surfaces show the accepted phone revision after acknowledgement.
3. Rapidly repeat the same requested state. Verify it is blocked or resolves without a second phone event. Preserve the existing automated command-ledger evidence for exact same-ID replay; the UI creates a fresh opaque ID for each user intent and exposes no physical replay hook.
4. Disconnect both Bluetooth and Wi-Fi between the pair. Queue one watch transition and record the pending ID/revision from synthetic-only debug evidence. Change phone state locally before reconnecting. Reconnect and verify the queued command is durably rejected as stale, the rejection is visible on watch, and phone history contains only the phone action.
5. Repeat offline queue/reconnect without changing the phone. Verify one accepted transition. Interrupt the phone and watch processes during pending and acknowledgement phases, reopen them, and verify the durable outcome is neither lost nor duplicated.
6. Restore a phone backup, which rotates the opaque generation, while an older watch command is pending. Reconnect and verify rejection; the pre-restore command must never replay into restored history.
7. Exercise app, Tile and complication entry points in round and square layouts. Verify current state, action wording, touch target, TalkBack description, pending state and refresh after a phone action. Reboot each device and repeat sync.
8. Remove the phone from range, disable Play Services connectivity, and force-stop each app in turn. Verify the watch reports unavailable/pending state truthfully and never presents an unacknowledged transition as phone history.
9. Complete the matched 24-hour watch battery run above with Tile and complication installed.

No physical duplicate-message injector exists in the release UI. Exact idempotency-ID replay remains an automated repository/protocol test unless a reviewed test-only transport harness is built and excluded from release. This limitation must remain visible in #20 evidence; do not imply the physical test proved wire-level duplicate replay.

## Owner usability pilot (#18)

The owner's 2026-09-08 instruction replaces recruitment and five-person observation with **one tester: the owner**, using their Pixel on 2026-09-09. No participant recruitment, random participant codes, external contact, additional consent form or minimum number of days is required. The owner has authorized this session; the test procedure is [pixel-acceptance.md](pixel-acceptance.md). Broader OEM, battery and Wear coverage remain separate gates and do not prevent the phone pilot.

Use the short core journey first, then the complete phone checklist. Record what the owner actually did and understood, including confusion, wrong turns, help needed and any difference from expected behavior. Automation may prepare synthetic fixtures, verify APKs and calculate expected totals, but it cannot count as the owner's completion or comprehension. If help was needed, record it and repeat the affected task after a concrete fix or explanation; do not silently count an assisted attempt as an independent success.

The owner should explain in their own words when tracking begins, what missing/partial coverage means, when a target change takes effect, why denied notifications prevent reminders, and what plaintext exports and confirmed replacement restore expose or replace. Keep concise synthetic reproduction steps and paraphrased findings locally; commit no personal treatment/photo content. Create issues only for concrete material findings.

The pilot passes when the owner has recorded an outcome for each phone case, completed the core journey and understood these boundaries, with no unresolved data loss, misleading medical interpretation, privacy exposure or blocker in the tested phone workflows. Untested cases stay NOT RUN/BLOCKED; they do not inherit a PASS from emulator results. Lesser findings need a triaged decision and owner. #18 and its #22 dependency remain open until actual human results are reviewed. Passing this owner pilot is evidence for this tester/device, not a general usability or release-readiness claim.

## Gate record

As of 2026-09-08, the physical OEM/API matrix, physical TalkBack audit, physical phone/watch battery measurements, physical watch journey, usability pilot, user-owned signing exercise and distribution approval have no recorded external acceptance evidence in this repository. The owner Pixel session is scheduled for 2026-09-09; it has not occurred. Each gate remains open until its applicable evidence and decision are reviewed. No “release ready” statement is valid while any of these gates is open.

The bundled dependency/license audit is complete in [dependency-licenses.md](dependency-licenses.md); external store/F-Droid acceptance and publication authorization are separate. Paired-emulator Wear transport also remains open: the available phone lacks the companion/pairing setup described in [wear.md](wear.md). Standalone official-GMS binder delivery does not satisfy that journey.
