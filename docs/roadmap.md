# Roadmap and agent execution

Canonical work is in [Forgejo issues](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues), mirrored at creation in backlog.json. Milestones: Foundation, MVP, Beta, v1.0. Repository issue state is authoritative after implementation.

Critical path: #1 evidence/contracts → #2 build → #3 persistence/time → #4 UI → #5/#7/#8 workflows → #9 integrated acceptance. #6 reminders branches after #3 and joins #9. #10 privacy joins #6/#8 and must pass before internal MVP handoff. Beta #11 physical reminder reliability, #12 accessibility, #17 performance and #18 usability precede #21 release. Widgets/photos/watch are not MVP blockers.

Parallel lanes after contracts: (A) domain/Room/backup core #3/#8; (B) Compose UI #4/#5/#7 and SAF wiring #8 once API agreed. Coordinator owns build, integration, docs, CI and issue reconciliation. Reminders #6 can start after lane A finishes so maximum two implementation writers is respected. Agents read issue body from backlog.json or Forgejo plus architecture before edits; inspect current files, own distinct packages, stage only their files and commit clearly. Coordinator runs one integrated material review and focused correction check, then posts exact evidence and closes accepted issues.

Highest risks/prototype early: time/DST/clock rollback and import invariants (#3/#8); Android alarm permission/Doze and stale PendingIntent behavior (#6/#11); process restart and Room transaction coherence (#9); large text/one-hand corrections (#4/#12). No event-count-based false positive streak. No app-generated treatment advice.

MVP test entry requires a debug APK plus setup, tracking, corrections, explicit tray progression/completion, covered-time summaries, opted-in reminders and local backup/restore; build/lint/JVM checks and emulator smoke recorded. Real-user pilot remains gated by explicit physical-device checks and user consent. A compiled APK alone does not close device acceptance issues.

New material audit findings get independent bounded issues with reproducer, impact, acceptance and dependencies. No endless speculative audit loops. Finish the expanded v1.0 checklist and retain evidence-based external acceptance gates.

## Audit additions

#23 tracks the final expanded commit's hosted CI and downloadable artifact acceptance; historical runner/MVP results are not final evidence. #24 records the discovered320dp/200% font navigation defect and its accepted adaptive-layout fix. See validation.md for exact local acceptance and remaining Beta gates.

## Expanded v1.0 execution

The 2026-09-08 continuation expands all remaining feature issues into production implementation scope, including #16 encrypted backup and #20 working watch companion rather than prototypes. [v1-checklist.md](v1-checklist.md) is the finite completion contract. Preserve closed Foundation/MVP work. Shared schema/migrations and action revision/ownership lead the critical path, followed by complete phone/watch journeys, portable data/media lifecycle and integrated validation. Device/pilot/signing/publication gates remain distinct and open without evidence. The former waiting API576 snapshot is historical. Final hosted SHA/artifacts remain to be reconciled under [#23](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues/23); do not change shared infrastructure without confirming ownership.

## Expanded software acceptance checkpoint

Phone software has 53 passing JVM tests and 22 accepted API36 instrumentation tests across a full run and focused header correction. Actual API26 MVP APK replacement preserved all original plan/events; 2 focused API26 photo/crypto tests passed. Complete encrypted SAF wrong-password/cancel/confirmed-photo restore and browser time-lapse playback were exercised. Watch has 8 passing JVM and 4 API36 instrumentation tests; the Apache microG client has actual official-GMS same-node binder evidence. The 171-coordinate runtime inventory no longer contains proprietary Google client artifacts. [Validation](validation-v1.md) records evidence and limits.

The finite remaining integration is the final local full check, hosted final SHA/artifacts and issue reconciliation. Paired-emulator Wear exchange is still open separately from #20 physical watch: no companion/account/pairing was configured, so local-node delivery is not paired acceptance. Physical #11/#12/#17/#18 and signing/publication #21 remain open. Do not restart completed feature work or infer release readiness from software checks.
