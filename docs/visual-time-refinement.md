# Version 7 refinement — issue #37

Baseline: clean workspace was on an older design branch; fetched main `91a8ea0` and created `design/visual-time-refinement`. Live version 6 APK downloaded through the existing Tailscale address matches SHA256 `b3b0c97937aef80f12b12aee85e7a73b2f52278e94ff66e0f2593466f25b46da`. No physical device connected. KB search returned unrelated results; current repository and emulator supplied project evidence.

## Evidence and decisions

Owner feedback: version 6 is substantially better; preserve neutral surfaces, restrained blue, compact controls and overall direction. Remaining concerns are text density, missing useful visuals, timezone placement and typed dates/times.

Observed in the actual signed version 6 API36 app: Today repeats coverage as four rows and a paragraph; Reports puts a large range total above verbose day rows; Schedule uses text-only future rows; timestamps repeatedly include UTC offsets. History switches to manual ISO input at large font sizes. Corrections and Journal use typed clock formats. Fresh, partial, empty and established journeys were exercised using synthetic data. The established fixture contains 13 transitions, two targets, one gap, two schedule revisions, two actual tray records, a note and an appointment.

Design hypotheses: compact factual graphics will improve scanning; a shared visual vocabulary reduces explanation; labelled disclosures keep supporting rules accessible. These are design judgments, not evidence of owner comprehension or aesthetic acceptance.

| Screen | First question / next action | Refinement |
|---|---|---|
| Setup | Current IN or OUT / Start tracking | Keep zero fields and deliberate selection. Settings owns pre-start zone selection. |
| Today | Current state, session and accumulated wear / record switch | Keep state/action hierarchy; replace repeated totals with a daily breakdown and concise labelled values. |
| History | What was recorded on this date / correct switch | Calendar dialog plus breakdown; preserve adjacent dated events and contextual edit buttons. |
| Schedule | Current tray and next prescribed estimate / explicitly record change | Recorded and estimated tray sequence uses filled/hollow nodes and direct labels. Spacing is not duration. No invented boundaries. |
| Reports | How recorded wear and coverage vary / inspect daily values | Aligned day bars, shared zero scale, missing/future segments and per-date prescribed target markers. Full data disclosure remains. |
| Treatment details | Optional prescription and actual details / save deliberately | Native optional dates; unknown stays absent. Existing numeric inputs and correction confirmation retained. |
| Journal | Find or correct notes/appointments/intervals | Native date and clock fields with occurrence choices; keep numeric durations and reminder intervals. |
| Photos | Compare dated records / add, inspect or remove | Existing images already supply useful visual content; retain image layout and contextual controls. |
| Settings | Configure reminders and manage data | Sole timezone surface; pre-start selector, stored fixed-zone explanation afterward. Existing exports/restore controls retained. |

## Sources consulted 2026-09-10

- [Android content structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure), [navigation](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns) and [color roles](https://developer.android.com/design/ui/mobile/guides/styles/color): retain established destinations, grouping, neutral surfaces and consistent semantic accents. Do not add decorative dashboards.
- [Compose date pickers](https://developer.android.com/develop/ui/compose/components/datepickers) and [time pickers](https://developer.android.com/develop/ui/compose/components/time-pickers): use Material calendar/clock state and explicit confirmation. DatePicker UTC day values are converted to LocalDate, not interpreted as accounting instants. Native alternate keyboard mode is available. At constrained effective widths it opens first; a labelled Show calendar control retains the calendar option.
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles), [adaptive layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps), [pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales): native labelled controls, 48dp targets, scalable scrollable dialogs, chart summaries and expanded/RTL checks. No font-scale cap.
- [UK Government Analysis Function chart guidance](https://analysisfunction.civilservice.gov.uk/policy-store/data-visualisation-charts/): lengths start at zero, use a common scale, label hours, distinguish missing records and do not bridge gaps. Bars answer quantity/coverage questions; the tray sequence is explicitly not a duration scale.

These sources inform implementation choices; they do not prove that this aesthetic is correct.

## Integrity and interactions

No database schema, backup serialization, accounting math, reminders, widget or Wear contract changes. Canvas breakdowns consume existing DaySummary values. Actual day length handles DST; future time is separate from elapsed unknown time. No recorded coverage displays unknown rather than zero wear. Historical target markers come from each summary, never the current plan applied backward. Full chart descriptions include exact durations and historical targets. OUT stripes and unknown dots supplement color.

Date/time display follows locale and device 12/24-hour preference, with no offset labels outside Settings. Internal ISO strings and offsets remain technical representations. Picker date changes preserve local seconds/fractions; clock selection changes hours/minutes while retaining subminute precision. Sole valid offset follows the selected date; repeated times require first/second occurrence, and gaps are rejected. Cancelling never publishes picker selection. Form drafts and picker visibility use saveable state. Saving a correction still validates neighboring events and current time.

Pre-start Settings retains the existing choice of accounting zone. After startup it is fixed, as before: no new setter silently reinterprets historical days. Imported zone validation remains unchanged. A technical offset stays in editing state when needed for exact repeated-time preservation.

## Visual assessment and validation

See [validation](validation-refinement.md) for actual checks, screenshots and open limits. Owner aesthetic, human TalkBack traversal and physical-device acceptance remain unverified. No publication or physical phone installation is included.
