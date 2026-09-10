# Today illustration validation — #40

## Scope and environment

2026-09-10, disposable `emulator-5554`, API36 Google APIs x86_64,
1080 × 2400 pixels, 420dpi (411dp physical width), system font scale 1.0.
No physical phone was targeted. The installed version7 was inspected in both
recorded states before replacing the emulator installation with a debug build.
Its synthetic private data was backed up outside Git before the signer change.
The final application remains version7; this task does not create a release.

[Design, authorship and sources](today-status.md).
[Interactive comparison](status-comparison.html).

The matrix uses the real Today composable in an emulator activity with fixed
`2026-09-10T16:30:00Z` data and explicit composition configuration. Normal viewport
is 400dp, narrow viewport 320dp; font scale is 1 or 2. Dark uses the production
DarkColors. `en-XA` and `ar-XB` test resource expansion and direction, not translated
language acceptance. These component fixtures omit app navigation/header; system
bar styling belongs to the test activity and is not app contrast evidence.
Separate full-app captures show the integrated hierarchy: [IN](screenshots/status/after/in.png),
[OUT](screenshots/status/after/out.png), [320dp / 200% / dark](screenshots/status/after/app-small-dark-out.png),
and [correction plus daily wear after scrolling](screenshots/status/after/app-small-dark-out-scrolled.png).
Full-app [before IN](screenshots/status/before/in.png) / [before OUT](screenshots/status/before/out.png)
use the same synthetic record lineage but a live timer, so their totals differ from
the later app captures. Use the fixed-time matrix for exact data comparisons.
The integrated small sample also has a shorter 686dp physical height and exercises
compact mode: correction is initially partly below the scroll edge, then fully
visible alongside recorded wear after a swipe. The primary action stays fixed.
This is inherited compact behavior, not a claim that all facts fit simultaneously.
A captured [loading state](screenshots/status/after/loading.png) has no IN/OUT picture.

Both states have corresponding before/after captures for normal, 320dp, 200% font,
dark, RTL, expanded text, combined 320dp/200%/dark/expanded, combined
320dp/200%/dark/RTL, saving, completed and partial prescription states. Scrolled
captures accompany enlarged/expanded cases. Completion intentionally has no picture
or tracking action; the two completed fixtures have different historical totals.

| Comparison | Before | After |
|---|---|---|
| Recorded IN, fixed fixture | [IN](screenshots/status/before/matrix/normal-in.png) | [IN](screenshots/status/after/matrix/normal-in.png) |
| Recorded OUT, fixed fixture | [OUT](screenshots/status/before/matrix/normal-out.png) | [OUT](screenshots/status/after/matrix/normal-out.png) |
| 320dp / 200% / dark / expanded | [IN](screenshots/status/before/matrix/small-dark-expanded-in.png) | [IN](screenshots/status/after/matrix/small-dark-expanded-in.png) |
| Scrolled supporting detail | [IN](screenshots/status/before/matrix/small-dark-expanded-in-scrolled.png) | [IN](screenshots/status/after/matrix/small-dark-expanded-in-scrolled.png) |
| 320dp / 200% / dark / RTL | [OUT](screenshots/status/before/matrix/small-dark-rtl-out.png) | [OUT](screenshots/status/after/matrix/small-dark-rtl-out.png) |
| Saving | [OUT](screenshots/status/before/matrix/saving-out.png) | [OUT](screenshots/status/after/matrix/saving-out.png) |
| Completed | [Complete](screenshots/status/before/matrix/completed-in.png) | [Complete](screenshots/status/after/matrix/completed-in.png) |
| Partial prescription | [IN](screenshots/status/before/matrix/partial-in.png) | [IN](screenshots/status/after/matrix/partial-in.png) |

## Material review

One material review with focused correction/recheck:

- IN encloses the tooth arch with the aligner outline; OUT has a visibly separated
  aligner above the same arch. One ink color is used in both themes. Initial tooth
  divider endpoints slightly overshot the curved arch; endpoints were corrected to
  sampled points on its Bézier curves and the final render rechecked.
- Neither picture has a filled control background, container, action symbol, click
  semantics or focus request. The opposite action remains in a blue native Button
  with its explicit verb. This reduces the visual suggestion of a button; only a
  human test could establish whether someone would mistake the picture for one.
- Removing the large state words makes the block less text-heavy. This wear session /
  This break and the duration still provide a visible factual cue. Whether people
  understand the pair unaided remains untested; the research does not prove it.
- Session duration retains its size. Recorded wear retains its aligned number and
  breakdown. Normal and 320dp captures show daily wear; combined 200% expanded text
  needs scrolling. The tracking action stays fixed. Correction stays fixed in normal
  height; the existing compact-height mode places it inside the scroll area. Scrolled
  views verify daily values, correction and explanation remain reachable.
- Light/dark illustration ink against the actual surface calculates to **6.65:1 /
  10.10:1**, respectively ([values](evidence/status/contrast.json)). Geometry, not
  opacity or hue, differentiates the pictures. They remain unmirrored in RTL.
- Saving retains the recorded picture and shows the existing disabled Saving…
  action. Completed state retains text and read-only history. A partial prescription
  retains missing-goal/details context without inventing treatment information.

The capture fixture initially took some frames before the compositor presented them
and swiped from a fixed control instead of the scroll viewport. The final fixture
waits for presentation and targets the actual scroll node; those captures supersede
the early samples. The old platform test's status-heading selector was updated to
check the retained daily-summary heading and separate status ImageView.

## Reproduction and checks

Use JDK21, the configured Android SDK and a disposable phone emulator. Build with
`./gradlew spotlessApply :app:assembleDebug :app:assembleDebugAndroidTest`, then install
the debug phone and test APKs. Do not run these reset/fixture procedures on a personal
phone. Capture:

```sh
adb -s emulator-5554 shell am instrument -w -r \
  -e class org.alignertracker.app.ui.TodayStatusCaptureTest -e capture true \
  org.alignertracker.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 pull \
  /sdcard/Android/data/org.alignertracker.app/files/today-status /tmp/status-captures
```

For a baseline capture, use the same test fixture with main `79a0c50`'s `Screens.kt`
and `-e baseline true`; this skips illustration-specific assertions only. All fixture
values/configuration remain the same. Before/after images are unretouched screenshots.

`TodayStatusTest` covers stable recorded imagery while saving/ticking, disabled
tracking and correction, completed/no-plan/no-event presentation and an actual
in-memory Room + TrackerViewModel clock-rejected transition. It asserts the visible
error, unchanged snapshot, retained IN image and enabled Take aligners out action.
`TalkBackSemanticsTest` covers both direction callbacks and exactly one noninteractive
status description, distinct Button semantics, and no Today live regions.
The capture matrix also asserts localized status descriptions and action labels.

Final checks:

| Check | Result / evidence |
|---|---|
| Repository `scripts/check.sh` | PASS: Spotless, 63 phone + 9 Wear JVM tests (zero failures/skips), debug/release lint, both debug/test APKs, both minified unsigned release builds; [log](evidence/status/repository-check.txt) |
| Focused API36 instrumentation | **29/29 passed**, 79.121s: TodayStatus, TalkBackSemantics, TrackerScreens, AccessibilityLocalization and capture matrix; [log](evidence/status/instrumentation.txt) |
| Corrected capture/localized status recheck | **1/1 passed**, 22.100s, 30 final captures; [log](evidence/status/capture-recheck.txt) |
| Exported Android accessibility nodes | **2/2 passed**, 7.566s, actual ImageView description and separate Button action/focus, plus existing correction-dialog checks; [log](evidence/status/platform-accessibility.txt) |
| Baseline fixed-time capture | **1/1 passed**, 22.348s; [log](evidence/status/baseline-capture.txt) |
| Integrated app | Recorded IN → OUT by labelled button, correct OUT description and Put aligners in action; [nodes](evidence/status/app-out-nodes.txt). 320dp/200%/dark scroll exposes correction and daily wear; [nodes](evidence/status/app-small-dark-nodes.txt). |

JDK21 was used. The system JDK25 startup attempt failed before compilation; no check
was waived. A stale platform heading assertion failed before its selector was
corrected and both platform tests passed. The final required check was run after
restoring the final implementation from baseline capture work. Existing compiler
warnings remain; no newly reported lint errors. Android size/font/theme/accessibility
settings were restored after the emulator checks. No Wear emulator or paired-device
acceptance is claimed from JVM tests and builds.

Human recognition,
TalkBack speech/traversal quality, owner aesthetic approval, physical accessibility
and release acceptance remain unverified (#12/#18). No merge/publication is implied.
