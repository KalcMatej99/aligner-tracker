# v1.0 implementation and acceptance checklist

Refreshed 2026-09-08 from clean main `2d6f2bedd592df026e5584c5d75cc0ae6bcb9e50`, current issues/milestones/comments and workflow API576 (waiting). Foundation and MVP remain accepted historical milestones. This checklist defines finite expanded software scope; unchecked items are not shipped acceptance.

- [ ] #14: DB1 migration; versioned complete schema with schema1 import; variable tray intervals, explicit adjustments, refinements/retention, actual change and historical target records; forgotten interval corrections; transactional validation and clock/DST coverage.
- [ ] #13: home widget and notification IN/OUT with idempotent stale-state protection, useful reminders/snooze and lifecycle recovery.
- [ ] #15: notes and appointment CRUD/reminders, accessible calendar and list, backup/deletion coverage.
- [ ] #16: authenticated encrypted portable backup, password KDF, complete validation before atomic restore, schema1 compatibility, cancellation/error/recovery journeys.
- [ ] #19: private dated photos, import/capture, comparison/time-lapse export, bounded processing/rotation/metadata handling, complete backup/deletion lifecycle.
- [ ] #20: installable functional watch companion, Tile, complication, durable offline actions and deterministic phone reconciliation, emulator integration and separate physical acceptance.
- [ ] #22: daily/weekly/monthly/per-tray reports, charts and text, preview/export, optional neutral streaks with historical goals and partial-day rules.
- [ ] #12: resources/localized formatting, semantics/contrast/touch/focus, 320dp/200% font/landscape/light/dark/pseudolocale/RTL checks.
- [ ] #17: measured startup/rendering/50k history/export/image work and emulator wakeups; fix material regressions.
- [ ] #23: final code hosted workflow success, downloadable phone/watch artifacts and reports verified.
- [ ] #21: phone/watch debug and unsigned release artifacts, checksums, licenses/privacy/changelog/upgrade/distribution assessment.
- [ ] Integration: focused tests then formatting/lint/build and real emulator journeys, one material review and focused correction recheck, scoped commits/pushes and issue evidence.

External gates remain open until evidenced: #11 physical Android/OEM reminder matrix; #12 manual TalkBack where unavailable; #17 physical 24h battery; #18 consented usability pilot; #20 physical watch; #21 user-owned signing and explicitly authorized publication. Prepare exact procedures and all independent work first. Software completion never substitutes for these gates or establishes release readiness.

Critical path: additive shared model/migrations/revision contract → full phone journeys and watch reconciliation → complete portable archive including photos → integrated emulator/hosted checks. Independent existing UI accessibility proceeds alongside core. At most two writer helpers; coordinator owns integration and tracking. No speculative features beyond this checklist; cloud sync/backend, automatic medical decisions, accounts, billing, analytics and release publication are excluded.
