# Schedule: the next planned change — #48

Owner requested a simpler, more visual Schedule after agreeing its primary question
is “When do I change my aligners next?” Built above the pending Today/History PR#47
without merging or publishing either change. Product, architecture, roadmap,
accessibility and existing schedule implementation were inspected first.

## Result

A planned date and calendar-day countdown lead the page. Current tray position and
an original Canvas line diagram show elapsed days in the current interval. This is
calendar time, never wear adherence or medical progress. Start date and elapsed-day
text explain the graphic. Overdue dates say “Planned date has passed”; nothing
automatically advances. The last tray says “Planned finish” and keeps the existing
completion confirmation callback.

Only three changes after the prominently displayed next one are shown initially.
“Show more planned changes” expands the existing forecast, retaining its12-tray
limit. Recorded changes remain available under an explicitly labelled disclosure.
A short visible estimate note replaces the long explanation; the upcoming section
labels estimates once instead of repeating that word on every row. Treatment
editing remains available. Missing details lead to a single explanatory state and
setup action rather than several repeated unknown facts. Completion remains
read-only with recorded history available.

The foreground clock already used by Today now supplies Schedule’s countdown, so
calendar-date changes use the fixed treatment timezone. Forecast dates still come
from WearMath and its schedule revisions/per-tray intervals. No persistence,
accounting, reminder, backup, picker, timezone placement, widget/Wear, permission or
service contract changes. Advancing/completing still uses the existing confirmation
flow, and busy state disables actions. No human comprehension or owner aesthetic
acceptance is inferred from screenshots or tests.

Original vector drawing is [ScheduleNextChange.kt](../app/src/main/java/org/alignertracker/app/ui/ScheduleNextChange.kt),
authored by OpenAI Codex for this project under GPL-3.0-or-later. It reuses the neutral
palette with a restrained blue accent, has no interaction, and is decorative to
screen readers because adjacent text already supplies its meaning. No images or
new libraries were added.

## Evidence

[Before with unknown details](screenshots/schedule-focus/before.png),
[updated full app with a synthetic treatment plan](screenshots/schedule-focus/after.png),
and [fixed-clock matrix](screenshots/schedule-focus/matrix). These fixtures differ;
the populated screenshot demonstrates planning rather than claiming that unknown
owner treatment details were inferred. Only disposable emulator data was seeded.

Matrix: normal,320dp,200% text,dark,RTL,expanded and combined constrained cases,
saving,completed and partial details. Visual review shortened the estimate note
and removed repeated per-row “Estimated” labels. Extended content scrolls at large
text, while primary controls remain reachable. Focused checks cover the next date,
countdown, elapsed days, expanding forecasts, correct advance/complete callbacks,
disabled saving, final tray, completion and unknown details. The diagram’s labels
use resources and plural-aware countdowns; translated-language delivery remains
separate from pseudolocale evidence.

11 API36 instrumentation tests passed (ScheduleFocusTest, ScheduleCaptureTest and
TrackerScreensTest). After the final row-density adjustment, both Schedule tests
and all capture cases passed again. The full-app [confirmation](screenshots/schedule-focus/confirmation.png)
was opened and cancelled, confirming the original actual-change guard remains.

Final `scripts/check.sh` passed with JDK21: 66 phone +9 Wear JVM tests, Spotless,
debug/release lint and all required phone/Wear debug, instrumentation and release
builds. Local build artifacts are validation outputs, not a published release.
