# Roadmap and agent execution

Canonical work is in [Forgejo issues](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues), mirrored at creation in backlog.json. Milestones: Foundation, MVP, Beta, v1.0. Repository issue state is authoritative after implementation.

Critical path: #1 evidence/contracts → #2 build → #3 persistence/time → #4 UI → #5/#7/#8 workflows → #9 integrated acceptance. #6 reminders branches after #3 and joins #9. #10 privacy joins #6/#8 and must pass before internal MVP handoff. Beta #11 physical reminder reliability, #12 accessibility, #17 performance and #18 usability precede #21 release. Widgets/photos/watch are not MVP blockers.

Parallel lanes after contracts: (A) domain/Room/backup core #3/#8; (B) Compose UI #4/#5/#7 and SAF wiring #8 once API agreed. Coordinator owns build, integration, docs, CI and issue reconciliation. Reminders #6 can start after lane A finishes so maximum two implementation writers is respected. Agents read issue body from backlog.json or Forgejo plus architecture before edits; inspect current files, own distinct packages, stage only their files and commit clearly. Coordinator runs one integrated material review and focused correction check, then posts exact evidence and closes accepted issues.

Highest risks/prototype early: time/DST/clock rollback and import invariants (#3/#8); Android alarm permission/Doze and stale PendingIntent behavior (#6/#11); process restart and Room transaction coherence (#9); large text/one-hand corrections (#4/#12). No event-count-based false positive streak. No app-generated treatment advice.

MVP test entry requires a debug APK plus setup, tracking, corrections, explicit tray progression/completion, covered-time summaries, opted-in reminders and local backup/restore; build/lint/JVM checks and emulator smoke recorded. Real-user pilot remains gated by explicit physical-device checks and user consent. A compiled APK alone does not close device acceptance issues.

New material audit findings get independent bounded issues with reproducer, impact, acceptance and dependencies. No endless speculative audit loops. Finish the expanded v1.0 checklist and retain evidence-based external acceptance gates.

## Audit additions

#23 tracks hosted CI execution while the existing shared runner is offline. #24 records the discovered320dp/200% font navigation defect and its accepted adaptive-layout fix. See validation.md for exact local acceptance and remaining Beta gates.

## Expanded v1.0 execution

The 2026-09-08 continuation expands all remaining feature issues into production implementation scope, including #16 encrypted backup and #20 working watch companion rather than prototypes. [v1-checklist.md](v1-checklist.md) is the finite completion contract. Preserve closed Foundation/MVP work. Shared schema/migrations and action revision/ownership lead the critical path, followed by complete phone/watch journeys, portable data/media lifecycle and integrated validation. Device/pilot/signing/publication gates remain distinct and open without evidence. #23 currently has waiting run API576; runner repair ownership must be checked before any shared infrastructure mutation.
