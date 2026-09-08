# v1.0 implementation and acceptance checklist

Refreshed 2026-09-08 after expanded phone/watch implementation and bounded emulator acceptance. Foundation and MVP remain accepted historical milestones. Checked items below mean the stated software work and evidence are complete; they do not close the separate physical, pairing, final-build or publication gates. [Validation](validation-v1.md) records exact results and limits; Forgejo remains authoritative for issue status.

## Implemented and checked software

- [x] #14: additive DB1→2 migration and schema1 import, variable intervals/explicit adjustments, refinements/retention, actual tray and historical target records, forgotten intervals, transactional validation and clock/DST coverage. Actual API26 MVP APK replacement preserved the original plan/events exactly.
- [x] #13: widget/notification state actions, stale/idempotent protection, reminders/snooze and lifecycle recovery; real widget PendingIntent behavior and focused rules/repository checks accepted.
- [x] #15: notes and appointments/reminders, calendar/list UI, portable data and deletion implementation; JVM/instrumented coverage and synthetic UI checks recorded. Full physical usability remains separate.
- [x] #16: authenticated encrypted complete archive, password KDF, validation before atomic restore, schema1 compatibility. Actual SAF wrong-password, preview-cancel and confirmed full-photo restore passed.
- [x] #19: private dated photos/import/capture lifecycle, selected-photo comparison, bounded normalization/metadata stripping, archive ownership/deletion recovery and HTML time-lapse; actual browser playback/scrub/no-network checks passed. Physical external-camera behavior remains device acceptance.
- [x] #20 implementation: installable watch app, Tile/complication, durable outbox and deterministic phone reconciliation, Apache microG transport adapter/correlated replies; 8 JVM and 4 API36 instrumentation tests plus official-GMS local-node delivery evidence. Paired transport is explicitly unchecked below.
- [x] #22: daily/weekly/monthly/per-tray reports, charts/text, previews/export and neutral optional streaks with historical goals/partial-day rules; automated and synthetic UI evidence.
- [x] #12 implementation and sampled emulator checks: localized formatting/resources, semantics/contrast/touch targets,320dp/200% font, light/dark, landscape and RTL. Corrected expanded-header assertion passed on focused rerun. This does not establish exhaustive pseudolocale/TalkBack/device acceptance.
- [x] #17 emulator measurements: cold launch,50k history, summaries, JSON/CSV/encryption and Reports rendering samples recorded with limitations. Physical battery/wakeup budgets remain unchecked.
- [x] #21 preparation: updated171-coordinate free-software runtime inventory, GPL preserved, privacy/license/distribution documentation and actual debug-signed upgrade evidence. No signing exception, release key or publication.

## Remaining integration and external gates

- [ ] Final local integration: `scripts/check.sh` and final count-label build passed; phone/watch debug/test and unsigned release artifacts produced. Final text-screen/pseudolocale rechecks and hosted artifact checksums remain to reconcile. Phone22 instrumentation accepted across full/focused runs.
- [ ] #23: final expanded commit's hosted workflow success and downloaded phone/watch APKs/reports verified; historical MVP/earlier local results do not satisfy this.
- [ ] #20 paired emulator: compatible phone companion and interactive pairing; actual sync/IN/OUT, offline/reconnect/stale rejection across two nodes. Same-node binder tests do not satisfy this.
- [ ] #11: physical Android/OEM reminder matrix including Doze/reboot/force-stop.
- [ ] #12: full TalkBack, keyboard/switch access, complete viewport/pseudolocale and physical-device audit.
- [ ] #17: matched 24 h physical battery and wakeup observations.
- [ ] #18: consented usability pilot.
- [ ] #20: physical watch pairing/reconnect, Tile, complication, accessibility and battery.
- [ ] #21: user-owned signing, release-signed upgrade matrix, complete notice/source bundle and explicitly authorized distribution. F-Droid acceptance requires maintainer/source-build assessment.

[External acceptance](external-acceptance.md) and [release preparation](release.md) define concrete procedures. Software completion never substitutes for these gates or establishes release readiness. Final integration/issue evidence is coordinator-owned; no additional speculative features are needed. Cloud sync/backend, automatic medical decisions, accounts, billing, analytics and unauthorized publication remain outside scope.
