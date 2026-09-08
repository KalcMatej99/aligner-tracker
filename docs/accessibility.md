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

## Manual and device gates

The following checks need an API 36 emulator pass and then a physical-phone pass before issue #12
can be accepted completely:

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
`docs/validation.md`. A passing compile or semantic-tree assertion is supporting evidence, not a
TalkBack device audit.
