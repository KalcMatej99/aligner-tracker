# Changelog

## 1.1.2 / code15 — 2026-09-12

- Show OUT intervals as continuous outlined blocks on Today and History, without inline text (#68).
- Add a labelled Now marker and clearer legend; preserve exact interval times and stored records.

## 1.1.1 / code14 — 2026-09-12

- Correct Google Play icon and screenshot PNG formats, preserving artwork and pixels (#66).
- Increment phone and Wear version metadata; tracking behavior and stored data are unchanged.

## Private development update 8

- Today shows original fitted/separated aligner status illustrations with distinct accessible status and labelled opposite actions (#40).
- Preserves recorded-state saving safeguards, session context and the version 7 layout.

## 1.0.0-dev / code3 — private phone update, 2026-09-09

Start tracking with an explicit IN/OUT choice and no typing. Prescribed hours,
tray details and treatment dates are optional and can be added or corrected later.
Unknown information stays unknown in reports, reminders, widgets and watch status.
Non-destructive schema3 upgrade and old backup imports preserve existing records.
Same permanent signer as private code2; [validation](docs/validation-32.md).
Owner Pixel usability and other physical acceptance remain open.

## 1.0.0-dev — original development implementation

Expanded v1.0 development implementation: variable schedules and preserved phase/target/tray history; missing-interval corrections and clock-gap coverage; safe widget/notification controls and reminder snooze; notes/appointments/calendar; historical-goal reports and optional streaks; complete encrypted portable archives and private photo comparison/offline HTML time-lapse; Wear OS companion with deterministic phone ownership; accessibility/localization and migration coverage.

Accessibility follow-up: contextual TalkBack actions, explicit tracking state, polite status messages, specific form errors, grouped report/photo descriptions and platform accessibility regression tests. The usability pilot is now a sole-owner Pixel session with validated disposable history fixtures.

This is an internal test build. Consult docs/v1-checklist.md and Forgejo issues for acceptance state. Physical-device, pilot, signing and publication gates remain separate. No published release is authorized by this changelog.

## 0.1.0-dev — internal MVP

Onboarding, persistent IN/OUT tracking, explicit fixed schedule/completion, reminders, corrections/history, statistics, schema1 JSON backup/restore, CSV and deletion. Historical local acceptance at commit 2d6f2be is recorded in docs/validation.md.
