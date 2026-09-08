# Accessibility and localization

This document records the implementation rules and the current evidence for the phone UI. It does
not claim a physical-device or human usability audit.

## Implementation contract

- Every user-facing sentence and control label lives in an Android string or plurals resource.
  Technical wire values such as ISO input patterns and zone IDs remain stable, while their labels
  explain the expected format.
- Display dates and timestamps use the active Android locale. Editable dates and timestamps remain
  ISO-shaped so input is unambiguous; nearby read-only values use the localized display. Timestamp
  displays retain the UTC offset so daylight-saving overlaps remain distinguishable.
- Durations use localized plurals rather than abbreviations. Resource formatting handles displayed
  numbers, and numeric plan fields accept Unicode decimal digits plus comma, period, or the Arabic
  decimal separator for the hours decimal separator.
- Zone IDs, UTC offsets, and localized date strings are wrapped with Android bidi isolation before
  insertion into a sentence. Layout uses start/end-aware Compose behavior; no left/right placement
  encodes meaning.
- Each screen and card section has heading semantics. Native buttons, chips, fields, and dialogs keep
  their platform roles and actions. A labelled switch is one 56dp-high toggle target across the full
  row, so TalkBack does not encounter a detached label and duplicate switch node.
- Validation remains visible in text, is exposed through the field's error semantics, and the shared
  form error is an assertive live region. State and completion are always written in text; color does
  not carry status by itself.
- Interactive controls are at least 48dp high. Screens and dialog bodies scroll, font scaling is not
  capped, and cards grow vertically when text wraps. New controls must remain reachable at 320dp,
  200% font, and short landscape heights.

## Contrast audit

The fixed light and dark palettes specify foreground colors explicitly. Relative-luminance checks
for every normal-text pair used by these screens meet the 4.5:1 WCAG threshold:

| Pair | Light | Dark |
|---|---:|---:|
| Primary button text / primary | 6.49:1 | 7.75:1 |
| Primary-container text / primary container | 13.37:1 | 7.30:1 |
| Main text / surface | 15.73:1 | 13.52:1 |
| Secondary text / low surface container | 8.65:1 | 9.72:1 |
| Error text / low surface container | 5.99:1 | 9.74:1 |

The calculation uses the WCAG sRGB relative-luminance formula. It covers theme colors in code; a
device screenshot audit is still needed to detect display, OEM, or system rendering differences.

## Automated checks

Compose instrumentation checks cover the following behavior:

- primary and export/restore actions remain reachable and at least 48dp high in a 320dp-wide,
  200%-font viewport;
- each reminder label and switch form one 56dp toggle node with checked-state semantics;
- invalid fields expose both visible supporting text and accessibility error semantics;
- an RTL History row puts Previous day at the reading start;
- localized date output differs between US English and German, and Arabic-Indic numeric input is
  accepted;
- all explicit light/dark normal-text color pairs meet 4.5:1.

Debug builds enable Android pseudo-locales so `en-XA` exposes expansion/clipping problems and
`ar-XB` exposes bidi assumptions without claiming a translation.

## Recorded expanded UI checks — 2026-09-08

Synthetic API36 screenshots confirmed that the primary state action remains reachable by scrolling at 320dp/200% font, dark mode remains readable, and short-landscape Reports scrolls inside its content viewport. Arabic locale plus forced RTL mirrored header/navigation and localized dates; strings fall back to English, so this is not Arabic translation acceptance. Selected-photo comparison renders two dated cards vertically.

The expanded More/Settings header initially squeezed the title into broken lines. The adaptive header now places the title above separately reachable actions at narrow effective width. Visual review accepted the correction; the new instrumented overflow assertion then found that the title also needed full width. After that focused correction, both navigation/header tests passed in 6.062s with single-line/no-overflow assertions retained. [Focused test result](evidence/api36-header-focused.txt), [before](screenshots/v1/header-before-320dp-font200.png) and [after](screenshots/v1/header-after-320dp-font200.png).

Further samples: [RTL/dark320dp](screenshots/v1/rtl-dark-320dp.png), [calendar at 200% font](screenshots/v1/calendar-font200.png), [photo comparison](screenshots/v1/photo-comparison.png), and [encrypted restore preview](screenshots/v1/encrypted-restore-preview.png). These are sampled views, not an exhaustive matrix. Final full phone instrumentation passed25 tests, including expanded dialog/field/calendar regressions; [validation](validation-v1.md) separates this from device gates.

## Manual and device gates

The following remaining checks need complete recorded coverage and a physical-phone pass before
issue #12 can be accepted completely; the samples above do not satisfy the full matrix:

- traverse setup, Today, Schedule, History, Progress, Settings, validation, and confirmation dialogs
  with TalkBack; verify focus order, spoken labels/state/actions, announcements after validation, and
  keyboard dismissal;
- inspect every screen at 200% font on 320dp portrait and short landscape, in both light and dark
  themes, including long `en-XA` text and `ar-XB` RTL;
- verify focus indicators and contrast in rendered screenshots, and exercise a hardware keyboard or
  switch-access path;
- verify localized dates, numerals, ISO editing, zone IDs, and UTC offsets on-device around an actual
  daylight-saving overlap.

Record screenshots, Android version, density, font scale, locale, theme, and any failures in
`docs/validation-v1.md`. A passing compile or semantic-tree assertion is supporting evidence, not a
TalkBack device audit.

The final en-XA/ar-XB320dp/200% pass found and corrected shared dialog action overlap, floating-label overlap and crowded calendar/report rows (#28/#29). Dialogs now scroll as one region with vertically separated actions; fields reserve measured floating-label clearance; month controls and report chips adapt to available width. Three new regression tests pass within the25-test phone suite, and the exact visual/cancellation recheck is linked from [validation-v1.md](validation-v1.md).
