# Continuous OUT intervals — #68

The owner and a tester read the repeated white stripes in gray OUT spans as separate
removal/insertion events. The timeline now gives each recorded OUT interval one
continuous light neutral fill with a contrasting outline. Its exact start, end and
width are unchanged. An `Out` label is drawn only when its measured text plus padding
fits; narrow intervals are never widened to fit a label. Solid IN and outlined OUT
remain distinguishable without recognizing the blue hue.

Elapsed untracked intervals retain their dots. Future time uses an unfilled thin
baseline, distinct from an outlined OUT block. A labelled `Now` marker appears only
on the current treatment-zone date, positioned from its actual instant. The marker
label stays inside the viewport, including near midnight. Axis labels retain their
24-hour chronological left-to-right placement in RTL and on DST days.

The legend uses `Aligners in`, `Aligners out`, `Not tracked` and `Future`, and wraps
at narrow widths or large font sizes. Timeline height accommodates scaled labels.
Existing exact state/start/end screen-reader descriptions are preserved. The shared
totals-only breakdown also drops OUT stripes and updates its explanatory text;
its totals semantics and the Progress screen's unrelated partial-day pattern remain
unchanged. No accounting, database, event, reminder or Wear transport changes.

Design basis:
- [W3C Use of Color](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html):
  shape and text provide visible alternatives to color; stripes are not required.
- [Datawrapper direct labels](https://www.datawrapper.de/blog/text-in-data-visualizations):
  place explanations close to their chart elements without forcing labels to fit.

OUT outline/fill contrast: light 5.62:1, dark 7.31:1. OUT text/fill contrast:
light 13.78:1, dark 11.08:1. These checks support legibility, not comprehension.

`OutTimelineCaptureTest` renders the actual Today and History screen composables
with synthetic September 12 records: OUT 07:30–08:15, 12:00–14:00 and 18:15–19:15;
untracked before 01:00, and future after 21:00. Its matrix includes light/dark,
320dp/200% text, RTL and no records. Captures are emulator evidence, not photographs
of the owner's device or a completed human comprehension test.

Before release, ask the owner/tester to identify the number of breaks, longest break,
untracked time and future time without coaching. Keep comprehension acceptance open
until that feedback arrives. This task does not publish a new release.

## Validation

- Three `HistoryTimeSpansTest` JVM tests passed: chronological OUT periods,
  gap/completion accounting and 23/25-hour DST days.
- Five emulator tests passed: the capture matrix, History editing safeguards and
  three Today state/action regression checks.
- Debug APK and instrumentation APK builds, Spotless and phone debug/release lint
  passed. [Build/test evidence](evidence/out-timeline/).
- Visual review covered light/dark, the empty state, 320dp/200% text and RTL:
  no timeline or legend clipping; short OUT blocks keep their actual widths;
  inline labels are omitted when they would not fit. The narrow-width fixture
  occupies part of the emulator window; its outside margin is test scaffolding.
- After shortening the explanation to “Each OUT block is one continuous break,”
  the capture matrix and History regression are rechecked and screenshots refreshed.

Screenshots: [History](screenshots/out-timeline/history-light.png),
[Today](screenshots/out-timeline/today-light.png),
[dark](screenshots/out-timeline/history-dark.png),
[large RTL](screenshots/out-timeline/history-large-rtl-legend.png),
[large Today](screenshots/out-timeline/today-large-legend.png),
[empty](screenshots/out-timeline/history-empty.png).
