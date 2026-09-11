# Progress: recorded wear over time — #51

Progress answers “How consistent has my recorded wear been over time?” Today is
for the current session, History for individual transitions, and Schedule for the
next planned change. This page describes recorded behaviour, not tooth movement,
clinical success or permission to change trays.

The owner requested a complete visual redesign because the previous page and its
buttons were confusing. Main was verified at `4cadaff`; #22's report semantics and
open owner/device acceptance dependency were inspected. No relevant saved Progress
design guidance was found in the KB search. This is an implementation candidate,
not a merged or published update.

## Design and buttons

- One 7/30-day selector replaces four competing range chips. Today and calendar
  History remain available for single-day work; the near-duplicate month preset
  is removed from Progress.
- A large average uses fully recorded closed calendar days only. Today's partial
  duration and days with gaps do not lower it. With no qualifying day it shows an
  unavailable value and a short explanation, not a fabricated average.
- A single chronological column chart shows daily recorded wear on a zero-based
  hours axis. Historical target marks are drawn for each day independently.
  Stripes identify partial days; a baseline dash means no recorded data. The
  chart compares daily totals, unlike Today/History's within-day chronological bars.
- A coverage strip and counts distinguish complete, partial and unknown days.
  Target comparisons include only complete days with a recorded target. No red/
  green judgement, streak pressure, pie chart, illustration or animation is added.
- “Details & export” is the one secondary entry. Its sheet contains exact daily
  values, chart explanation, existing tray reports when available, and the optional
  streak setting as a labelled Switch. The chart itself is noninteractive.
- “Export all history (CSV)” makes the existing export scope explicit; the chosen
  chart period does not filter the CSV. Existing preview, warning, native picker,
  cancellation and ViewModel export remain. Busy state disables export and streak
  changes. Duplicate Daily data / breakdown / report-help buttons are removed.
- The header now says Progress, matching its navigation label.

## Research applied

[Government Analysis Function chart guidance](https://analysisfunction.civilservice.gov.uk/policy-store/data-visualisation-charts/)
supports choosing charts for the comparison task, clear axes and labels, and
accessible alternatives. We use bars with a common baseline, restrained colour,
patterns, full date/value semantics per bar and exact values in the details sheet.

[NN/g progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/)
supports prioritizing common information and clearly labelling access to secondary
options. We consolidate supporting tools into one named sheet, keeping the trend
and coverage visible initially.

[Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
explains why custom drawing needs explicit semantics. Each date has a localized
spoken description of wear, out time, coverage and target. Unknown values are
spoken as unknown. Material chips, sheet, buttons and switch retain native roles.
Timer ticks do not create live-region announcements. Chart time direction remains
left-to-right in RTL; labels use the configured locale. Numeric axes avoid
pseudolocale expansion, including fractional midpoints on 25-hour days.

These sources support design principles; they do not establish comprehension of
this specific page. New copy uses Android resources with a plural-aware average
basis. Real translation delivery remains under #31.

## Source and data boundary

Original Compose Canvas chart, coverage strip and legend authored by Codex for
this project in `ProgressOverview.kt`. No external graphic assets, SVG runtime,
chart dependency or image library. ReportMath/WearMath remain authoritative and
unchanged. No schema, persistence, permissions, reminder formula, backup, timezone,
widget or Wear contract changes. The legacy unused ProgressScreen is untouched.

## Evidence and limits

[Before](screenshots/progress-focus/before.png) and
[after](screenshots/progress-focus/after.png) show the same sparse synthetic
emulator records, with naturally elapsed foreground time between captures.
[Populated example](screenshots/progress-focus/demo.png) uses a separately seeded
synthetic week; it is not the owner's history.
[30-day view](screenshots/progress-focus/month.png),
[details](screenshots/progress-focus/details.png), and
[native CSV picker](screenshots/progress-focus/export-picker.png) are actual app
captures. The picker was opened and cancelled; no external report was sent.

Two focused API36 instrumentation tests passed, including period selection,
expected full-day average, date semantics and secondary callback. The capture
matrix covers normal, 320dp, 200% font, dark, RTL, expanded pseudolocale,
combined small/dark/large text, completion, absent target and no events. Its
"saving" capture is only the read-only overview, not a simulated saving transaction.
Matrix captures are isolated composables; the separate full-app captures include
actual app navigation. Large text needs scrolling; it is not shrunk to fit.

One material visual review identified compressed range chips and crowded axis
labels in the combined small/expanded case. Wrapping chips, numeric hours and fewer
date labels corrected these; the focused tests and matrix passed again and the
corrected small/expanded images were inspected. An irrelevant empty tray-history
message was removed from the secondary sheet. Screen and automated checks do not
claim human comprehension, TalkBack speech quality or owner aesthetic acceptance.

`scripts/check.sh` passed: 67 phone + 9 Wear JVM tests, formatting, debug/release
lint, debug/instrumentation builds and both release builds. The added JVM test
checks 23/25-hour complete days, today, gaps and completion. The final focused
formatting, debug lint and debug build recheck also passed after the small
presentation corrections.
