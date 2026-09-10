# History refinement — #46

Owner requested online research, implementation and a screenshot, preserving the
progress/breakdown bar and date picker. Started from clean main `8a058f7` after
fetching origin; no open PRs or existing changes. Inspected the running version9
History screen and product, architecture, roadmap, design, accessibility and
localization guidance. No release or owner-phone installation is included.

## Decision

The existing “About this breakdown” button only reveals explanation and derived
tracked/future totals. History now shows the four pattern keys inline plus “Daily
totals, not the order of changes.” Exact totals remain in the unchanged bar's
accessible description. The bar, date navigation and native calendar are unchanged.
Today and Progress retain their existing disclosures.

Considered color-only rows, repeated aligner pictures, and a proportional interval
chart. Chose a restrained vertical event timeline: filled blue IN markers and
outlined neutral OUT markers, always accompanied by explicit state labels.
This reduces repeated date text and horizontal separators without implying
unrecorded session durations or adding another competing chart. The thin connector
indicates order only; spacing follows content height, not duration. No inferred
activity or IN/OUT state is inserted on days without events.

Visible timestamps retain seconds and follow locale and device 12/24-hour format;
the selected date supplies day context. Edit accessibility labels retain full date
and time. The original first-event explanation and prohibition, disabled saving,
completed-treatment read-only behavior and exact event callbacks are preserved.
No timestamp, timezone, accounting, persistence, reminders, backup, widget, Wear,
permission or service contracts change.

Original Compose Canvas source is [HistoryVisuals.kt](../app/src/main/java/org/alignertracker/app/ui/HistoryVisuals.kt),
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

One material visual review found the list and legend readable across these cases.
A focused test caught edit semantic bounds of40dp after the row refactor; restoring
the prior explicit48dp minimum corrected it. Recheck covers that target, exact
callback, first-event protection, disabled saving, completion and empty history.
No human comprehension, TalkBack speech quality or owner aesthetic acceptance is
claimed from emulator captures or automated semantics. Translation delivery remains
separate; English fallback and debug pseudolocales are exercised.
