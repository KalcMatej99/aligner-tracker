# UX plan

Original Compose Material 3 design: quiet teal primary, generous whitespace, rounded cards and clear typography. Do not reproduce TrayMinder layout, assets, wording or screenshots. Navigation: Today, Schedule, History, Progress; Settings reachable from every screen. One primary action near the bottom of Today, within thumb reach. Compact widths scroll; tablets constrain content width. System light/dark initially; no decorative graphs that obscure exact totals.

| Screen | Content and action | Empty/error/loading behavior |
|---|---|---|
| Welcome/setup | Local privacy explanation; total/current tray, start/current-tray date, interval, prescribed goal, timezone and starting IN/OUT state | Inline numeric/date validation; preserve draft after rotation; no permission prompt until reminder enabled |
| Today | Current IN/OUT, duration, worn/removed/covered today, goal; large Put in/Take out button | Loading progress; persistence failure shown; no optimistic false success; completed state read-only |
| Schedule | Current/total trays, due date estimate, future progression; Change tray with confirmation | No silent automatic advancement; last tray offers completion confirmation |
| History | Date navigation and per-day summary; timestamped switches; Edit time | No history says tracking has not begun for date; neighbors constrain edits; first event fixed |
| Progress | 7/30-day averages, goal-hit days and treatment fraction | Clearly label partial coverage and current-goal comparison; future/unknown days excluded |
| Reminders | Opt-in break delay and tray reminder; permission/channel status | Denied permission retains tracking; open system settings; explain inexact/force-stop limitations |
| Completion | Confirm finishing; preserve visible final summary/history/export | Cannot accumulate future wear; starting a separate phase requires later multi-phase work |
| Settings/data | Prescribed target, zone explanation, reminders, JSON backup, CSV, import and delete | SAF cancel harmless; preview counts then destructive replace confirmation; invalid file never replaces data |

## Interaction requirements

Use native date/time controls or clearly labeled ISO inputs with errors for the initial MVP. Give dates a readable localized display. Back navigation dismisses confirmation first; transaction controls disabled while saving. Use Snackbar or persistent error text for failed writes. Repeated IN/OUT taps must not create duplicate events. Confirm advancing/replacing/deleting/completing. Permissions are optional and prompted in context. Treatment setup asks which tray is actually in use, not just planned treatment start.

## Accessible states and review

48dp minimum touch areas; 4.5:1 normal-text contrast; semantic headings and button labels; no color-only success. Verify TalkBack state/action order, 200% font, 320dp width, landscape, dark mode, keyboard/IME avoidance and long dates. Persisted content appears after loading, never briefly shows empty onboarding when an existing plan is loading. MVP screenshots and outstanding checks belong in validation.md. Calendar grid, custom charts and photos follow later, after the accessible text/list experience works.
