# Roadmap and agent execution

Canonical work is in [Forgejo issues](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues), mirrored at creation in backlog.json. Milestones: Foundation, MVP, Beta, v1.0. Repository issue state is authoritative after implementation.

Critical path: #1 evidence/contracts → #2 build → #3 persistence/time → #4 UI → #5/#7/#8 workflows → #9 integrated acceptance. #6 reminders branches after #3 and joins #9. #10 privacy joins #6/#8 and must pass before internal MVP handoff. Beta #11 physical reminder reliability, #12 accessibility, #17 performance and #18 usability precede #21 release. Widgets/photos/watch are not MVP blockers.

Parallel lanes after contracts: (A) domain/Room/backup core #3/#8; (B) Compose UI #4/#5/#7 and SAF wiring #8 once API agreed. Coordinator owns build, integration, docs, CI and issue reconciliation. Reminders #6 can start after lane A finishes so maximum two implementation writers is respected. Agents read issue body from backlog.json or Forgejo plus architecture before edits; inspect current files, own distinct packages, stage only their files and commit clearly. Coordinator runs one integrated material review and focused correction check, then posts exact evidence and closes accepted issues.

Highest risks/prototype early: time/DST/clock rollback and import invariants (#3/#8); Android alarm permission/Doze and stale PendingIntent behavior (#6/#11); process restart and Room transaction coherence (#9); large text/one-hand corrections (#4/#12). No event-count-based false positive streak. No app-generated treatment advice.

MVP test entry requires a debug APK plus setup, tracking, corrections, explicit tray progression/completion, covered-time summaries, opted-in reminders and local backup/restore; build/lint/JVM checks and emulator smoke recorded. Real-user pilot remains gated by explicit physical-device checks and user consent. A compiled APK alone does not close device acceptance issues.

New material audit findings get independent bounded issues with reproducer, impact, acceptance and dependencies. No endless speculative audit loops. Finish the requested MVP and retain clear open Beta/v1 work.
