# Calm tracking — phone design, issue #33

Baseline: 9c79442, inspected 2026-09-09 on disposable API36 emulator. Owner request authorizes a coherent redesign; it does not provide new owner usability findings beyond #32. Finish: implemented native screens, exercised journeys, inspected comparable captures, repository checks, material review, focused commits and installable candidate. Physical acceptance remains open.

## Evidence and priorities

| Priority | Evidence type | Finding | Decision |
| --- | --- | --- | --- |
| 1 | Observed Today | Missing tray is above state; correction help below long totals; session and totals share similar type | State hero, explicitly labelled session, accumulated wear summary, reachable persistent action and correction route |
| 1 | Observed History | Always-visible ISO date form and four stacked totals precede events | Native date selection on demand, compact paired totals that stack for enlarged text |
| 1 | Observed Reports | Rules dominate first viewport; unknown days show zero minutes and empty bars | Human duration summaries and explicit range, untracked text without zero-wear claims, explanatory details secondary |
| 2 | Observed theme | Default purple selections coexist with teal; repeated padded card nesting | Complete semantic palette, intentional typography/shapes, flatter shared sections and vector nav icons |
| 2 | Owner feedback #32 | Original onboarding required too much and mandatory fields were unclear | Preserve implemented zero-field start; make mutually exclusive choices generous, retain secondary restore and automatic zone |
| 2 | Design hypothesis | Purpose-based grouping and concise labels improve comprehension | Apply across optional details, Schedule and Settings; owner comprehension unmeasured |

Issues read with comments live: #12 and #18 open for remaining accessibility/owner acceptance; #24 closed with accepted adaptive navigation; #31 open/planning only (English resources and pseudolocales, no translated-language claim); #32 open only for owner acceptance after implemented/published version3. #21 documents scoped earlier private publications, not blanket authorization for every future update. Prepare this candidate for review; do not infer new publication authorization.

KB search returned no relevant project design decision. Repository and actual emulator observations are the evidence for this work. No competitor material used.

## Published principles and application

- [Android mobile design](https://developer.android.com/design/ui/mobile): use platform themes/components. Keep Compose/Material, native controls and existing navigation destinations.
- [Layout basics](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-basics): reachable essential interactions, purposeful grouping, consistent spacing, safe areas. Apply to Today action placement and removing nested padding; adapt content instead of reducing fonts.
- [Android accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility): scalable text, contrast, descriptive semantics, grouped content. State always uses words; palette pairs checked in both themes; timers remain outside live regions.
- [Compose API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): native semantics and actual 48dp minimum bounds avoid overlapping expanded hit regions. Use full-width selection rows and labelled buttons.
- [Material 3](https://m3.material.io/) and its [official Compose implementation guide](https://developer.android.com/develop/ui/compose/designsystems/material3): shared color, typography and shape roles propagate through components. M3 site requires JavaScript in text fetch; implementation guide provides readable primary-source specifics.
- [GOV.UK question pages](https://design-system.service.gov.uk/patterns/question-pages/): ask only necessary information, explain purpose, mark optional and support unknown responses. Native adaptation: one explicit current-state choice, then Start; optional treatment groups remain editable, with no forced web-style wizard.

These publications motivate the principles. Palette, hierarchy, grouping and wording below are design judgments, not proven outcomes for this app.

## Direction and reusable system

Warm paper background with deep pine primary; pale sage IN and muted clay OUT surfaces. OUT is a routine state, never an error. Complete semantic colors in light/dark remove accidental default purple. System sans typography with medium-weight headings and generous line height; large duration values remain scalable. Spacing uses 4/8/12/16/20/24/32dp, page gutter20dp, content max680dp, section gap24dp. Rounded 12dp fields, 20dp containers, 28dp hero; no decorative shadows or progress rings. Original vector navigation pictograms accompany text labels; enlarged text retains #24 two-column navigation. No new animation or continuously ticking service.

Section headings sit on the page, with whitespace/dividers instead of repeated nested cards. Today separates current session from accumulated wear and preserves unknown coverage/goal. Reports keep recorded facts, day coverage and prescribed targets separate. No persistence/schema/protocol changes are needed.

## Validation

Implementation checks passed: 61 phone and 9 watch JVM tests; repository formatting, phone/watch debug and minified release builds, debug/release lint (zero errors). The final general phone run passed 43 of 44 cases; its sole failure was test preference leakage from repeated use of the same synthetic tracking timestamp. After isolating that fixture, all 9 screen tests passed, covering the failed case. Final native accessibility checks passed 2/2 with bundled TalkBack enabled; watch checks passed 4/4. Logs are in [evidence/redesign](evidence/redesign). These are not full TalkBack speech/traversal or physical-watch acceptance.

Before and after captures use disposable API 36 emulator data. Normal captures use 411dp width, 100% font, English/light; constrained captures use 320dp, 200% font, dark, including ar-XB RTL and en-XA expansion. Actual images were inspected. Partial datasets have the same unknown-treatment shape, with different capture times and naturally advancing timers; they are not pixel-diff fixtures. All content remains scrollable at large font. Screenshot index and signed candidate validation follow in the delivery evidence.

## Material review and corrections

One source/interaction review covered the modified theme, navigation, Today, History,
Reports, supporting layout and state boundaries. Corrected findings: constrained
Today initially gave too much space to its fixed secondary action (only the tracking
action stays pinned at constrained height); new report absence wording must refer
to the historical day, not imply the current prescription is missing; passing
minute-rounded time into History/Reports could suppress coverage in the first
minute after starting; native History date selection needed an explicit dark style.
Reports now retain all three aggregate durations and show newest days first.
Navigation retains primary-tab Back-to-Today behavior while secondary screens
return to their origin; restore/delete clear that navigation trail. No domain,
Room schema, backup codec, reminder schedule or Wear protocol implementation changes.

The optional setup invitation uses one device-local presentation preference keyed
to the tracking session, so dismissal survives a cold restart. It is excluded from
portable treatment backups, and never affects prescribed details or their history.
Settings and Schedule always retain the editing route.

Focused rendered recheck: the 320dp/200% native calendar had dense day cells, so
constrained layouts now open a scrollable date-entry dialog with explicit format,
localized preview, bounds checking and separate cancel/confirm actions. Roomier
layouts retain the platform calendar. The RTL keyboard capture also exposed
reserved bottom-navigation space obscuring editing; navigation now yields its
space while the IME is visible. Invalid prescribed-hours submission brings the
field and its supporting error into view. Both were re-inspected on the emulator;
no text/font or touch-target reduction was used.

Expanded-text inspection (`en-XA`, 320dp/200%) required a compact header: its
More/Settings actions use explicit 48dp icon controls with resource-backed
accessibility labels. Normal-size layouts retain the text actions. The product
name is intentionally non-translatable, consistent with #31. The adaptive
navigation still displays all four full destination labels and their selected
state. These changes preserve space for a readable current-state region when
translated labels expand.
