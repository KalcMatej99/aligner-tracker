# History refinement — #46

Owner requested research and refinement, then clarified that the bar should represent
when each state occurred. PR #47 / #46 now includes that chronological day bar.
The native date picker remains unchanged. No release or owner-phone installation
is included.

## Decision

History shows a midnight-to-midnight timeline, with blue IN, striped gray OUT,
dotted elapsed untracked time, and empty future time. Each boundary is positioned
from its actual instant within the selected treatment-zone day. Axis labels use a
24-hour clock; 24:00 means the following midnight. At larger text sizes fewer axis
labels are shown. Time increases left-to-right in both LTR and RTL, with labels
placed at the same physical instants. Day lengths can be23 or25 hours; local noon
is therefore not forced to the geometric midpoint on a DST transition day.

The inline key replaces “About this breakdown.” The vertical event list uses
filled blue IN and outlined neutral OUT markers with explicit state labels. That
list's connector indicates order only; its row spacing is not a duration scale.
The horizontal day bar is proportional to elapsed duration. Overnight states carry
into the next day even if there is no new event at midnight; before first recording,
tracking gaps and elapsed time after completion remain untracked. Future time is
never counted as IN or OUT. Numeric interval descriptions expose the exact recorded
start/end instants through accessibility semantics. Day totals still come from
unchanged WearMath accounting. Today now reuses the same chronological day bar and legend following explicit owner feedback. Progress retains its original totals bars.

Visible timestamps retain seconds and follow locale and device 12/24-hour format;
the selected date supplies day context. Edit accessibility labels retain full date
and time. The original first-event explanation and prohibition, disabled saving,
completed-treatment read-only behavior and exact event callbacks are preserved.
No timestamp, timezone, accounting, persistence, reminders, backup, widget, Wear,
permission or service contracts change.

Original Compose Canvas source includes [HistoryDayTimeline.kt](../app/src/main/java/org/alignertracker/app/ui/HistoryDayTimeline.kt) and [HistoryVisuals.kt](../app/src/main/java/org/alignertracker/app/ui/HistoryVisuals.kt),
authored by OpenAI Codex for this repository under its GPL-3.0-or-later license.
No external artwork or new dependencies. The neutral palette and primary accent
are reused; filled/outlined geometry and labels carry state in monochrome.
Decorative shapes have no separate spoken descriptions or click actions.

## Research applied

- [NN/g recognition and recall](https://www.nngroup.com/articles/recognition-and-recall/):
  visible cues and labels reduce the need to remember hidden meanings. Applied to
  the visible legend and explicit state labels.
- [NN/g progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/):
  reserve disclosure for secondary complexity and name its destination clearly.
  Here, a short inline key replaces an extra explanation tap.
- [Material accessibility](https://m1.material.io/usability/accessibility.html):
  color should have redundant indicators; native controls retain touch targets.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics):
  related event text is grouped while each edit action stays separate and named.

These principles inform the design; they do not prove this timeline is understood
or preferred by users.

## Validation

[Before](screenshots/history-focus/before.png) and
[updated running app](screenshots/history-focus/after.png) use the same synthetic
records; accumulated wear naturally differs with elapsed time.
[Fixed-clock matrix](screenshots/history-focus/matrix) covers normal/320dp, 200%
text, dark, RTL, expanded pseudolocales and combined constrained variants, plus
saving, completed and partial-treatment states. Scrolled captures expose the list.

27 API36 instrumentation tests passed (HistoryFocusTest, HistoryCaptureTest,
TalkBackSemanticsTest, TrackerScreensTest, AccessibilityLocalizationTest), including
the unchanged native calendar flow and RTL date-navigation semantics.

`scripts/check.sh` passed with JDK21: Spotless, 63 phone and 9 Wear JVM tests,
debug/release lint, and all phone/Wear debug, instrumentation and release builds.

The chronological follow-up adds three projection tests for ordered breaks,
overnight carry, tracking-start/completion clipping, unknown coverage, actual DST
day lengths and equality with WearMath totals. The focused History tests and capture
matrix were rerun. Review caught overlapping numeric axis labels in expanded
pseudolocale; the language-neutral numeric pattern is now non-translatable, while
number formatting remains locale-aware. Exact interval accessibility descriptions
remain localized. The final full check has 66 phone +9 Wear JVM tests.

One material visual review found the list and legend readable across these cases.
A focused test caught edit semantic bounds of40dp after the row refactor; restoring
the prior explicit48dp minimum corrected it. Recheck covers that target, exact
callback, first-event protection, disabled saving, completion and empty history.
No human comprehension, TalkBack speech quality or owner aesthetic acceptance is
claimed from emulator captures or automated semantics. Translation delivery remains
separate; English fallback and debug pseudolocales are exercised.

## Shared Today timeline

The owner accepted History and requested the same chronological bar on Today.
Today now passes its authoritative snapshot to the same component, retaining its
centered illustration, session timer and compact wear/target values beneath the
bar. The static pattern key replaces Today’s explanation-only disclosure.
[Before](screenshots/today-chronological/before.png),
[updated running app](screenshots/today-chronological/after.png), and
[Today matrix](screenshots/today-chronological/matrix).

The shared projection and accounting are unchanged. The Today saving/state test
also verifies the chronological chart is present and the old disclosure is absent.

Today follow-up: 23 focused API36 instrumentation tests passed, including the
Today IN/OUT layout matrix, 320dp/200%/dark/RTL/expanded configurations, saving,
completion, unknown state, accessible status/action labels and failed-save behavior.
Visual review retained readable axis labels and reachable tracking/correction
controls; expanded text requires scrolling to lower summary content.

The final shared-Today `scripts/check.sh` run passed: 66 phone +9 Wear JVM tests,
Spotless, debug/release lint and all required APK builds.

The owner approved the result and authorized merge and deployment. These changes
are now included in private update 10; see [release evidence](release.md). Earlier
preparation-only statements above describe the state before that authorization.
