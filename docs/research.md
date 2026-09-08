# Aligner tracker product research

Research date: **2026-09-08**. Prices are storefront snapshots, not purchase quotes. This research informs an independent Android product; it does not establish clinical effectiveness or replace a clinician's prescribed wear target or change schedule.

## Evidence and limitations

Primary evidence is the developer's website, current store descriptions and changelogs, privacy declarations, and public repositories/releases. Store reviews and Reddit posts establish individual experiences and requests, not defect prevalence. No competitor app was installed, purchased, or tested on a physical phone/watch during this research. “Advertised” therefore does not mean verified functioning. Public pages disagree in several places; conflicts are retained below.

The local memory registry search for `aligner` / `TrayMinder` found no relevant project history. Implementation decisions below are proposals from this research, not recovered prior decisions.

## TrayMinder: current evidence

### Core job and onboarding

TrayMinder serves multiple aligner brands on iOS and Android. Its main advertised jobs are wear tracking, change-date visibility, treatment adjustment, and smile photographs. The official homepage explicitly says registration is unnecessary. Its orthodontist origin is a publisher claim, not independent evidence of outcomes. [Official homepage](https://trayminder.com/), accessed 2026-09-08.

### Timer, reminders, history and treatment behavior

The following compact inventory summarizes the official FAQ, accessed 2026-09-08; it is undated and partly stale on platform support.

| Area | Documented behavior |
|---|---|
| Timer | Timestamp-based; starts paused; manual corrections through Timer Edit or Calendar time entries. |
| Return reminders | Custom duration on pause; repeats after 5, 15, 30, 60, 90, 120 minutes. |
| Schedule | Configurable switch time; appointment reminders; daily notes; configurable week start. |
| Statistics | 7/30/90-day averages; total per aligner; current-treatment scope. |
| History | Calendar records switches; manual switch editing; adjustable goals; default green 20+ hours, yellow 18–20, red below 18. |
| Travel | Automatic timezone/DST handling is claimed; precise aggregation semantics unspecified. |
| Treatment | Adjustable durations/skips; new treatments retain prior records/photos. |
| Photos | Import, brightness/alignment/mirroring, comparison, timelapse, sharing. |
| Recovery | Cloud backup and restore through the same account. |
| Watch | Apple Watch automatic/forced sync; reconnection may delay updates. |

[TrayMinder FAQ](https://trayminder.com/post/frequently-asked-questions).

These published color thresholds are product defaults, **not this project's prescribed wear targets**. A user-selected goal must drive this app's feedback.

### Free versus paid and platform differences

| Evidence | Findings |
|---|---|
| Official FAQ | TrayMinder+ bundles widgets, Siri/shortcuts, iOS Live Activities, ad removal, Apple Watch and unlimited photo storage. Free photos are described as device-storage limited. |
| Android listing | Advertises free download with ads/IAP; base timer, sharing wear data by email/text, calendar, notes and photos. Plus lists widgets, voice commands, Live Updates and **Wear OS app, complications, tile**. Updated September 7, 2026. |
| US iOS listing | IAP: $4.99 and $39.99 TrayMinder+, $69.99 lifetime; legacy watch/ad-removal entries $1.99 each. First two billing periods are not labelled in the IAP table. |

Sources, accessed 2026-09-08: [FAQ](https://trayminder.com/post/frequently-asked-questions), [Google Play, English](https://play.google.com/store/apps/details?id=com.invisatime&hl=en), [US App Store](https://apps.apple.com/us/app/trayminder-aligner-timer/id1320684802).

**Conflict:** FAQ says Android Wear unsupported; newer Android listing advertises it. Treat Wear OS as advertised, untested. Do not position this project as the first watch-capable Android tracker. No purchase was made to establish Android price, grandfathered entitlements, trial routing, or every feature's paywall.

### Recent iOS changes that invalidate older comparisons

The iOS changelog records refinement/retainer support (August 16), phone-number backup login (August 18), weekly calendar (July 30), device-calendar sync (August 27), Plus device sync (August 30), and a notification fix (September 1). These dates are 2026. Accessibility capabilities are undeclared. The privacy label includes tracking identifiers and linked content/usage/diagnostics. [US App Store](https://apps.apple.com/us/app/trayminder-aligner-timer/id1320684802), accessed 2026-09-08.

An undeclared accessibility field does not prove inaccessibility. Calendar sync, retention, and multi-device sync cannot honestly be described as universally missing from TrayMinder. Android parity for these particular recent iOS additions remains unverified.

### Data ownership, export, import, accounts and privacy

The privacy policy is effective **2021-10-23**. It permits optional-cloud identity information, automated device/advertising/usage collection, third-party services and advertising, and consented precise location. It gives an email-based deletion procedure for cloud data. Its broad language and age make current implementation uncertain. [TrayMinder privacy policy](https://trayminder.com/privacy), accessed 2026-09-08.

The Android listing declares no collection/sharing, unlike the policy and iOS declaration. This is a declaration conflict, not a measured network audit. [Google Play](https://play.google.com/store/apps/details?id=com.invisatime&hl=en).

Research did **not** verify a documented interoperable CSV/JSON schema, local full-fidelity backup, third-party log import, conflict resolution, encryption-at-rest model, automatic backup cadence, retention period, or cross-platform restore compatibility. Sharing a report is different from exporting editable underlying events; cloud restore is different from importing another tracker's history. Do not promise a TrayMinder importer without a real, user-provided export sample and a successful round-trip fixture.

## User experience evidence

### Recent and historical complaints

| Evidence | Observation | Product response proposed here |
|---|---|---|
| Android review, June 24, 2026 | A reviewer reports freezing on IN/OUT and battery drain; developer requests diagnostics. | Persist a toggle before showing success; make the most frequent action immediate; measure restart and battery behavior. |
| Android review, August 8, 2021 | A night worker reports OUT becoming IN at midnight. Historical; current defect unconfirmed. | Midnight splits accounting, never changes physical state. |
| Android review, April 3, 2019 | User values retrospective correction. | Make correction visible and quick, with undo. |

[Reviews on Google Play](https://play.google.com/store/apps/details?id=com.invisatime&hl=en), accessed 2026-09-08.

A Reddit discussion displayed as six months old describes anger about formerly familiar widgets/ongoing status becoming paid, intrusive ads, renewal timing, and inaccessible historical trends. Other commenters say core tracking remains free and useful; some still value the paid convenience. Platform, trial and legacy status are not consistently identifiable. Several alternative-app comments are self-promotion. [Subscription discussion](https://www.reddit.com/r/Invisalign/comments/1rgj6ai/trayminder_going_to_a_subscription_service/), accessed 2026-09-08.

A 2022 review reproduced by an aggregator asks for offline file backup and alleges excessive support-email attachments. This is historical secondary evidence, not confirmation of today's attachment contents. [Archived user review](https://justuseapp.com/de/app/1320684802/trayminder-aligner-tracker/contact), accessed 2026-09-08.

**Implication:** trust is damaged when a daily habit suddenly changes price, available history, or interaction cost. The product opportunity is to provide stable access to the user's records, predictable actions, and understandable recovery. Claims of “all users hate subscriptions” or “the timer is now paywalled” are unsupported.

## Relevant competitors

### Commercial applications

| Product | Verified advertised offer | Limits and implications |
|---|---|---|
| **My Invisalign** | Free iPhone app: timer/history, weekly/monthly/yearly progress, schedule, reminders, photos/video, Apple Watch. ClinCheck and Virtual Care require doctor invitation. Android listing also advertises Wear OS. | Brand ecosystem and clinical connection distinguish it. Do not assume every basic feature needs clinic activation simply because connected care does. Public descriptions do not settle offline-login behavior or full export. [iOS](https://apps.apple.com/us/app/my-invisalign/id1325633853), [Android](https://play.google.com/store/apps/details?id=com.aligntech.myinvisalign&hl=en). |
| **OutTime** | iPhone, account-free out-time countdown. Free: timer, tags/break statistics, 7-day averages/streaks, calendar, tray adjustments, reminders, Siri, first photo. Advertised $9.99 one-time Pro: extended stats/photos/timelapse, reports/CSV, Apple Watch, widgets, Live Activities, iCloud sync, cleaning/appointment reminders. | An existing privacy-and-no-subscription alternative. Android release was not established. Developer privacy/uniqueness claims are not audited; do not repeat its “only” claims. [Official site](https://outtime.app/). |
| **TrayTime, Pocketglow** | Established iOS timer product from Pocketglow. A 2023 store review praises editing but objects to separate upper/lower tray entry and requests stronger motivation and tray-duration visibility. | Distinguish this from the identically named Weblak app. Old reviews establish needs, not current missing features. Current price and complete feature gating were not established. [Developer](https://www.pocketglow.com/), [store/reviews](https://apps.apple.com/us/app/traytime/id1250940516?platform=watch). |
| **TrayTime, Weblak** | Separate iPhone product; website says full access needs weekly/yearly Pro subscription. Privacy page describes local logs/preferences. | Different publisher: Souhayb Kamal Dine/Weblak. Do not mix its subscription or claims with Pocketglow's app. [Product](https://weblak.net/apps/traytime/), [privacy](https://weblak.net/apps/traytime/privacy/). |
| **Smilo** | iPhone/iPad: custom goal, timer, editable history, tray scheduling, photos, reminders, streaks, widgets, cleaning log and themes. Advertises no account/sync and local data; full access needs subscription. UK Pro entries £4.99 weekly, £7.99 monthly, £49.99 yearly; additional Premium entries exist. | Privacy label nevertheless declares tracking purchases/identifiers/usage. Dark Interface is declared; TalkBack/VoiceOver testing absent. Avoid copying its suggestion to switch early based on fit. [UK App Store](https://apps.apple.com/gb/app/aligner-tracker-smilo/id6761348584). |

All links accessed 2026-09-08. Region-dependent pricing and listings may change. No score ranking is offered: store ratings come from different populations and time periods.

A clinician-connected product's ability to exchange treatment feedback is outside this project's basic tracker scope. A shareable factual log can support an appointment without pretending to provide treatment assessment.

### Open-source and public-source landscape

Repository search used GitHub's public repository API plus web queries for `invisalign tracker`, `aligner tracker`, `trayminder`, and F-Droid aligner terms. This is a bounded search, not proof that no other projects exist.

| Project | Evidence on 2026-09-08 | Relevance |
|---|---|---|
| **gardncol/invisalign-tracker** | MIT repository. React Native/Expo, local SQLite, IN/OUT, streaks, editable timeline, daily/tray reporting, put-back and tray reminders, overnight prompts. Release API reports v1.4.0 on 2026-08-13; release describes notification/event-entry improvements. | Closest explicitly licensed direct alternative found. Runtime, APK signing, background delivery, backup integrity and accessibility were not tested. Existing open source means “first open aligner tracker” is unjustified. [Repository](https://github.com/gardncol/invisalign-tracker), [release](https://github.com/gardncol/invisalign-tracker/releases/tag/v1.4.0). |
| **RiLights/aligntime** | Public iOS project with source/tests/widget directory; README mixes vision with prospective functionality. GitHub API last push 2021-02-05; no detected license or releases. | Useful needs reference, not established reusable open-source dependency or maintained Android option. [Repository](https://github.com/RiLights/aligntime), [metadata](https://api.github.com/repos/RiLights/aligntime). |
| **bartaxyz/Invisapp** | Public React Native experiment; README checklist leaves design/build/release incomplete. API last push 2017-09-05, no detected license/releases. | Historical evidence of the same use case; not a ready alternative. [Repository](https://github.com/bartaxyz/Invisapp), [metadata](https://api.github.com/repos/bartaxyz/Invisapp). |
| **danielworking06-cpu/trayminder-rn** | MIT repository; API last push 2026-06-03, no releases/readme returned. | Existence and license verified; maturity and feature completeness unestablished. No code/assets copied. [Repository](https://github.com/danielworking06-cpu/trayminder-rn). |
| **TrayQuest** | Website advertises offline/account-free timing, gamification, CSV and JSON backup; stores show “Coming Soon.” Its GitHub link returned 404. EULA grants personal, non-commercial use despite “Open Source Project” footer. | Treat as an unverified product/source lead, not an available FOSS app. A marketing label does not establish a reusable license. [Website](https://www.trayquest.com/), [linked repository](https://github.com/GloriousRedLeader/trayquest). |

No relevant published F-Droid package was established. Generic time/habit trackers could cover portions of the job, but do not by themselves provide tray plans, change history and correction semantics. This research copied no competitor implementation, illustrations, screenshots, text blocks or branding into the product.

## A typical day: proposed independent experience

This scenario is a design exercise, not a claim about any competitor's exact screens. Example target and schedule are user-entered values.

1. **Start mid-treatment.** Enter current tray, total trays, start/change date, planned days per tray, and clinician-given daily goal. Show the resulting next change date before saving. Ask whether aligners are in *now*; do not invent wear earlier today. Let past history be added later.
2. **Breakfast.** One large button records removal immediately. Show “Aligners out,” elapsed break, today's recorded wear, and remaining out-time allowance. Offer familiar reminder durations and an optional reason after recording; neither should block the state change.
3. **Return.** “Put aligners in” closes the break and cancels its pending reminders. Offer an undo that reverses only this action. Do not allow an old reminder action to reopen or close a newer session.
4. **Forgot a tap.** Open today's timeline, adjust actual removal/return times, or add a missed interval. Preview how today's total changes. Validate impossible ranges and overlapping entries instead of silently clipping or double counting.
5. **Lunch runs long.** A local notification states that aligners are still marked out and offers a short snooze or a clearly labelled return action. If notification permission is denied, the home screen explains the limitation without interrupting logging.
6. **Evening.** Show progress in neutral words and exact hours. Distinguish a partially logged day from a confirmed missed target. Do not imply the user should skip meals or ignore clinical guidance to preserve a streak.
7. **Change day.** A reminder proposes the planned switch; the user explicitly records the actual switch. A delay/extension updates future dates through a preview. A calendar date alone never asserts that new trays are physically being worn.
8. **After midnight.** Continue the same IN/OUT state, partition elapsed time into dates, and retain the original timestamps. A 23:50–00:20 break belongs to two dates but remains one editable event.
9. **At the appointment.** Export the selected date range with logged wear, target, tray, unknown/partial-day flags and notes. The user chooses what to share; there is no automatic doctor transmission.
10. **Phone replacement.** Export a versioned backup, import on a fresh installation, review contents and verify timer/history/plan/settings restoration before relying on it.

## Differentiation and priorities

**Recommended positioning:** “An open Android aligner tracker that keeps your records on your phone, makes missed taps easy to fix, and lets you keep and export your history.” This is a proposed promise to validate, not a finished feature claim. It is narrower and more defensible than “the only private tracker,” “more accurate than TrayMinder,” or “first Wear OS tracker.”

| Priority | Product outcome | Acceptance evidence |
|---|---|---|
| P0 | Daily logging is dependable. | One-tap state survives navigation, process death and reboot; duplicate taps cannot create conflicting sessions. |
| P0 | Numbers are understandable and repairable. | Midnight, DST, travel, partial first day and overlapping-edit fixtures; visible timestamps and unknown periods; edit preview/undo. |
| P0 | No data lock-in. | Versioned JSON backup/restore round trip plus human-readable CSV; invalid imports leave existing data untouched; history remains accessible. |
| P0 | Reminders reflect actual state. | Permission-denied, reboot, stale-action, snooze/cancel and return-before-alarm checks on Android; clear status when scheduling cannot be guaranteed. |
| P0 | Plans reflect user decisions. | Planned versus actual switches; configurable per-tray duration and explicit extension; no automatic medical recommendation from under-wear. |
| P1 | Android feels native and accessible. | TalkBack labels/state/actions, large fonts, sufficient contrast, non-color status, touch targets and dark mode verified on device/emulator. |
| P1 | Helpful reporting without guilt. | Daily history, selectable ranges, 7/30/90-day and per-tray summaries; disclosed denominators; optional streaks count only eligible days. |
| P1 | Quick access reduces effort. | Notification controls/widget after timer logic is stable; show last-updated state and reject stale actions. |
| P2 | Photos and extended treatment support. | Local photo ownership/export, accessible comparisons; refinements and retention retain history and use distinct plan settings. |
| P2 | Watch and optional sync. | Only after phone reliability; explicit conflict handling and watch reconnection tests. Prefer a documented protocol over silent last-writer-wins behavior. |

An out-time budget is useful but already exists elsewhere. A sensible home screen can show both recorded wear and remaining allowance; choose the primary emphasis through actual use rather than declaring the model unique. Progress photos, dashboards and watch support are attractive parity work, but they should not delay a reliable timer and recoverable history.

## Specific design questions to resolve in implementation

- **Unknown versus OUT:** Before first use, after data gaps, or following clock anomalies, should the app ask for confirmation? Recommended: retain unknown explicitly, never synthesize a successful full-wear day.
- **Timezones:** Store absolute timestamps and record the timezone needed for interpretation. Document whether reporting follows event-local days or current display timezone. A timezone change must not silently rewrite original events.
- **Daily allowance:** A displayed remainder must account for target, elapsed recorded data, unknown periods and the actual civil-day duration. Avoid assuming all days contain exactly 24 elapsed hours.
- **Streak denominator:** Exclude the unfinished current day and pre-onboarding history; show how edited/partial days affect streaks. Permit hiding gamification.
- **Schedule editing:** Separate edits to future plans from corrections to actual switches. Show affected future dates; preserve prior recorded facts.
- **Reports:** State that durations are self-reported. Include exact date range, timezone/units, goal changes and partial-day status. Do not label a mathematical average as clinical compliance certification.
- **Backups:** Include active session, plans, event IDs, settings, timestamps and schema version. Validate all references before committing import; offer replace/merge only if semantics are fully specified.
- **Privacy:** Keep diagnosis, provider identity and photographs optional. Diagnostic sharing needs a visible preview with no automatic attachment of full treatment data.
- **Accessibility:** A ring or red/green calendar must have a text equivalent. Screen readers should announce meaningful state changes rather than a timer tick every second.

## Remaining competitive verification

Before making public side-by-side superiority claims, install current Android competitors and verify: notification behavior under idle/reboot, exact free/Plus boundaries, Wear OS availability and offline actions, cross-device restoration, export formats, font scaling/TalkBack, and first-day/timezone treatment. Record app version, Android version, region, account/trial state, procedure and outcome. This project's MVP can proceed without purchasing competitors or contacting their users; these are evidence gaps, not blockers to independent implementation.
