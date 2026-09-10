# Redesign evidence and candidate 4

**Publication update, 2026-09-10:** owner authorized private-store deployment.
Version4 is now [available to install](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_4.apk).
Downloaded APK SHA256 matches the final candidate below; signed indexes and
updated encrypted store recovery verified. Earlier unpublished statements below
record the candidate preparation checkpoint. Physical acceptance remains open.

Implementation: [design audit, principles and review](redesign.md), issues #33/#34.
Branch `design/calm-tracking`; final source tag `design-phone-candidate-4-final`.
This is an unpublished private candidate. Current private publication remains version 3.
No owner phone was connected, installed, reset or otherwise changed.

## Screenshots inspected

Normal: API 36 emulator, 1080×2400 at density420 (411dp), font100%, English/light.
Constrained: 840×1600 at density420 (320dp), font200%, dark.
Pseudolocales test layout, not translation quality. No real translations are claimed.

| Journey | Before | After |
| --- | --- | --- |
| Zero-field start | [Before](screenshots/redesign/before/onboarding-light.png) | [After](screenshots/redesign/after/onboarding-light.png) |
| Partial Today | [Before](screenshots/redesign/before/today-light.png) | [After](screenshots/redesign/after/today-light.png) |
| Established Today, exact same records across signed upgrade | [Before](screenshots/redesign/before/established-today.png) | [After](screenshots/redesign/after/established-today.png) |
| History | [Before](screenshots/redesign/before/history-light.png) | [After](screenshots/redesign/after/history-light.png) |
| Reports | [Before](screenshots/redesign/before/reports-light.png) | [After](screenshots/redesign/after/reports-light.png) |
| Schedule | [Before](screenshots/redesign/before/schedule-light.png) | [After](screenshots/redesign/after/schedule-light.png) |
| Small/dark/large font | [Before](screenshots/redesign/before/today-small-dark.png) | [After](screenshots/redesign/after/today-small-dark.png) |
| Small/dark/large font/RTL | [Before](screenshots/redesign/before/today-small-dark-rtl.png) | [After](screenshots/redesign/after/today-small-dark-rtl.png) |

Additional inspected states: [expanded labels](screenshots/redesign/after/today-small-dark-expanded.png),
[RTL keyboard](screenshots/redesign/after/details-keyboard-small-dark-rtl.png),
[RTL inline error](screenshots/redesign/after/details-error-small-dark-rtl.png),
[compact date correction](screenshots/redesign/after/history-date-small-dark-rtl.png),
[established reports](screenshots/redesign/after/established-reports.png),
[established schedule](screenshots/redesign/after/established-schedule.png),
[restore preview](screenshots/redesign/after/restore-preview.png),
[notification recovery](screenshots/redesign/after/reminder-recovered.png).
Older focused input captures precede the final compact-header icon refinement; field/dialog behavior is unchanged.
Timers naturally advance between captures. Partial comparisons use equivalent unknown-detail datasets;
established Today uses the same 13-event database, with captures in the same minute.
At 200% font, details scroll while the primary tracking action and navigation remain accessible.

## Journey evidence

| Journey | Evidence and limit |
| --- | --- |
| Fresh deliberate IN and OUT, no fields | Manual emulator starts, existing atomic start tests, new OUT integration/recreation test. Unknown prescription/trays stay absent. |
| Switching and missed-transition correction | Real Room/ViewModel integration edits a transition while preserving first coverage event and plan; existing daily journey includes switching/restart. |
| Optional details/drafts | New Back/origin/recreation test; rendered localized input/error/IME checks; QuickStart tests cover later metadata and established edits preserving history. |
| Restart/process recreation | Activity recreation tests and manual force-stop/relaunch; invitation dismissal survives a cold start. |
| Empty/partial/complete History, Reports, Schedule | UI tests, inspected partial captures, signed synthetic established fixture; unknown report days remain unknown and first-minute coverage regression passes. |
| Reminders | Denied native permission prompt, saved enabled break choice, continued tracking, recovered through Android notification settings. Individual channel status shown. Physical delivery remains open. |
| Widget/Wear contracts | Existing widget action/data tests, watch emulator tests, nullable state protocol unchanged; no paired physical acceptance. |
| Backup/restore | Signed release export and replacement preview, cancellation, confirmed restore and cold restart; record comparison below. Encrypted/partial compatibility additionally covered by existing archive tests. |

## Automated checks

61 phone and 9 watch JVM tests pass. Repository formatting, phone/watch debug and minified
release builds, debug/release lint pass with zero lint errors (existing warnings retained).
General phone run: 43/44 passed initially; one synthetic timestamp reused a deliberately
persisted dismissal preference. After fixture isolation, the full 9-test screen class passed.
Thus all 44 unique general cases passed across the recorded run and focused recheck, not
one claimed green full-suite run. Native exported accessibility-tree tests: 2/2 with bundled
TalkBack enabled. They do not validate spoken wording or traversal by a person.
Connection regression: 96 probes and 12 cancellation attempts pass after reproducing #34.
Logs: [evidence/redesign](evidence/redesign).

## Remaining acceptance

Issues #12 (full accessibility/physical gates), #18 (owner Pixel acceptance), #31
(actual translations), #32 (owner comprehension) stay open. Owner feedback must assess
comfort, comprehension and daily use. Actual paired Wear delivery, OEM notification timing,
TalkBack speech/traversal and physical-device usability are unverified. Emulator work does
not satisfy these gates. Publication requires owner approval under the existing procedure;
no store/repository metadata was changed and no new distribution service was introduced.

## Signed candidate and upgrade

Source `506e52d` / `design-phone-candidate-4-final`, built from a clean tree.
Bundle: `/home/matejkalc/.local/share/aligner-tracker/releases/design-candidate-4/`.
Installable phone: `aligner-tracker-phone.apk` (version4 / `1.0.0-dev`).
SHA256: `b9dd73216cb615f24a472f9ea3c1f138f7ece9d711f7617ca86b11b51a6447b8`.
The bundle also contains the signed watch APK, exact tagged source, GPL text,
dependency notices, build instructions, `release.json` and `SHA256SUMS`.
The permanent signer fingerprint remains
`9cac6b2722e6e074bd607586df06335c3ae6300803616b3f38e88706d1ed53a9`.
Both APK signatures/alignment verified. Neither APK has been published.

The actual privately served version3 download matched its published checksum.
Its synthetic schema2 import contained 13 transitions, two historical targets,
two schedule revisions, two tray-history records, a note, an appointment and a gap.
After in-place installation of final signed4, every database table and the saved
reminder datastore compared identically: [comparison](evidence/redesign/signed-upgrade.json).
Clock-observation timestamps advance normally on launch; they are not treatment history.
The older visual-only candidate encountered #34 and is explicitly superseded in the
private evidence folder. Only the final checksum above identifies this candidate.

Final signed candidate: restore preview cancellation returned to Settings; confirmed
replacement returned to Today, then survived a force-stop/cold launch. All treatment
rows remained identical. Restore intentionally refreshed tracker generation/reset
revision to invalidate stale controls: [restore comparison](evidence/redesign/signed-restore.json).
The installed final base APK was pulled back from the emulator and matched the
candidate phone SHA256 exactly. Final watch suite passed 4/4 after the shared
connection lifecycle change.
