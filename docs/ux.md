# UX plan

Original Compose Material 3 design: quiet teal primary, generous whitespace, rounded cards and clear typography. Do not reproduce TrayMinder layout, assets, wording or screenshots. Navigation: Today, Schedule, History, Progress; Settings reachable from every screen. More opens Treatment details, Journal and Photos without overcrowding the primary navigation. One primary action near the bottom of Today, within thumb reach. Compact widths scroll; tablets constrain content width. System light/dark initially; no decorative graphs that obscure exact totals.

| Screen | Content and action | Empty/error/loading behavior |
|---|---|---|
| Welcome/setup | Local privacy explanation; total/current tray, start/current-tray date, interval, prescribed goal, timezone and starting IN/OUT state | Inline numeric/date validation; preserve draft after rotation; no permission prompt until reminder enabled |
| Today | Current IN/OUT, duration, worn/removed/covered today, goal; large Put in/Take out button | Loading progress; persistence failure shown; no optimistic false success; completed state read-only |
| Schedule | Current/total trays, due date estimate, future progression; Change tray with confirmation | No silent automatic advancement; last tray offers completion confirmation |
| History | Date navigation and per-day summary; timestamped switches; Edit time | No history says tracking has not begun for date; neighbors constrain edits; first event fixed |
| Progress | Daily, 7/30-day and calendar-month summaries, per-tray totals and optional supportive streaks | Exact text accompanies bars; use the target recorded for each date; incomplete days do not earn streak credit |
| Reminders | Opt-in break delay and tray reminder; permission/channel status | Denied permission retains tracking; open system settings; explain inexact/force-stop limitations |
| Completion | Confirm finishing; preserve visible final summary/history/export | Cannot accumulate future wear; new refinement/retention phase requires explicit settings and current IN/OUT state; the intervening pause is unknown coverage |
| Settings/data | Prescribed target, zone explanation, reminders, complete ZIP or password-encrypted backup, previewed CSV, import and delete | SAF cancel harmless; preview counts then destructive replace confirmation; invalid file never replaces data |

## Interaction requirements

Use native date/time controls or clearly labeled ISO inputs with errors for the initial MVP. Give dates a readable localized display. Back navigation dismisses confirmation first; transaction controls disabled while saving. Use Snackbar or persistent error text for failed writes. Repeated IN/OUT taps must not create duplicate events. Confirm advancing/replacing/deleting/completing. Permissions are optional and prompted in context. Treatment setup asks which tray is actually in use, not just planned treatment start.

## Accessible states and review

48dp minimum touch areas; 4.5:1 normal-text contrast; semantic headings and button labels; no color-only success. Verify TalkBack state/action order, 200% font, 320dp width, landscape, dark mode, keyboard/IME avoidance and long dates. Persisted content appears after loading, never briefly shows empty onboarding when an existing plan is loading. MVP screenshots and outstanding checks belong in validation.md. The calendar has a list alternative and full-date semantic labels. Charts retain exact text equivalents. Photo comparison presents dated local copies; there is no diagnostic analysis.

## Expanded journeys

Treatment details accepts explicit inclusive tray ranges with days per tray and a reason; confirmation creates a schedule revision while retaining prior actual changes. Refinement/retention starts only after confirmation, including an explicit current IN/OUT choice. Target changes clearly take effect tomorrow in the treatment timezone. Forgotten intervals use separate start/end dates and times; impossible DST times are rejected and repeated local times require an explicit UTC offset. Existing transitions cannot overlap.

Journal provides a locale-aligned month grid and date-list alternative, notes and appointment add/edit/delete, appointment completion and optional reminder lead time. Validation errors remain visible and drafts clear only after saving succeeds. Photos accepts the Android photo picker or external camera without broad library/camera permissions, validates date/caption, and shows bounded private copies. Select two photos for comparison or 2–100 for a dated, offline HTML time-lapse with playback controls. Export explains exactly what leaves the app and asks for a destination.

Encrypted export/import asks for a password after the system picker returns. Passwords are not saved in saved-instance state. New passwords require confirmation; wrong password or corrupt authentication fails before preview or replacement. Restore preview includes expanded records and photos. Cancellation leaves treatment intact; a provider-created empty/partial destination may need user deletion.

The home widget and reminder controls carry the state revision they display. Duplicate actions are idempotent; stale actions refresh without changing history. The widget has no ticking duration and no periodic update. Notifications show opt-in status, channel/permission/timing limitations and a 10-minute snooze where meaningful. Clock gaps are labeled as unknown, not counted as wear.

Watch behavior and its explicit pending/rejected/disconnected states are documented in [wear.md](wear.md). Acceptance evidence, including remaining manual checks, is in [validation.md](validation.md).

## Phone redesign — issue #33

[Calm tracking design and evidence](redesign.md) supersedes the original visual
specification above. Warm paper/pine and intentional dark palettes, scalable
system typography, original vector navigation and flat shared sections replace
nested pale cards. Today separates session and daily totals with a reachable
tracking action and correction route; constrained height prioritizes state.
History uses bounded native date selection. Reports show newest days first with
human-readable totals, explicit ranges/coverage and unknown-day text. Secondary
Back returns to the origin and retains drafts. Tracking-first setup from #32,
resource-backed copy for #31 and the adaptive navigation acceptance of #24 remain.

## Neutral phone redesign — candidate 5

[Neutral design decisions](neutral-redesign.md) supersede the warm/pine visual
specification above following owner rejection of published version4. One screen
header, white/charcoal surfaces, restrained blue, compact facts/date controls and
neutral supporting records preserve the tracking-first behavior. Validation and
remaining owner/device gates are in [candidate5 evidence](validation-neutral.md).

## Visual and picker refinement — #37

The version 7 phone refinement supersedes earlier typed-ISO and repeated-offset UI guidance. Native calendar/clock dialogs, saved drafts and explicit repeated-time occurrence choices preserve exact stored instants. Timezone information and pre-start configuration live only in Settings. Accounting, serialization and widget/Wear contracts are unchanged. [Design and source rationale](visual-time-refinement.md); [validation](validation-refinement.md).
