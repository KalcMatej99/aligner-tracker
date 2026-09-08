# Expanded treatment schema 2

Schema 2 is the complete plaintext JSON payload embedded in a portable archive. It remains sensitive health-routine data. `BackupCodec.decode` accepts strict schema 1 and schema 2; `encode` writes schema 2. Encryption and archive media framing are separate from this payload.

## Treatment and history

The top-level object contains `schemaVersion`, `plan`, `events`, `phases`, `scheduleRevisions`, `trayIntervals`, `trayHistory`, `targetHistory`, `notes`, `appointments`, `photos`, and `trackingGaps`. Unknown and duplicate fields, invalid primitive types, excessive nesting, more than 50,000 wear events, or more than 5 MiB of plaintext JSON are rejected before restore.

The legacy `plan` fields remain the active-phase projection used by existing screens and reminders. They do not replace history:

- `phases` preserves ordered aligner, refinement, and retention phases. Starting a phase is always an explicit user action.
- Each immutable schedule revision belongs to one phase. Its inclusive tray intervals must cover tray 1 through the phase tray count exactly once, with no gap or overlap. A schedule adjustment creates a complete new revision; older revisions remain referenced by historical trays.
- `trayHistory` records actual explicit changes. `startedOn` and `endedOn` preserve treatment-zone calendar dates. Nullable `startedAt` and `endedAt` preserve exact action timestamps when known. A schema-1 migration records only the current legacy tray and uses `trackingStartedAt` as its known time boundary; it does not invent earlier tray transitions.
- `targetHistory` has one prescribed target per effective ISO date. The normal target update becomes effective tomorrow in the treatment zone, preserving today's target. An explicit same-day date is allowed. Reports select the latest target whose effective date is on or before the reported date.
- `trackingGaps` marks coverage that must not be counted as IN or OUT. Gaps are positive, non-overlapping intervals inside recorded treatment time. Clock discontinuities and pauses between a completed phase and a later phase are preserved this way.

Wear events remain strictly ordered alternating state transitions. A forgotten interval correction can only split one existing opposite-state interval with two new transitions. It cannot cross or overlap an existing transition. Completed history can be corrected only inside the recorded completion boundary.

## Notes, appointments, and photos

Notes may reference a phase or historical tray. Appointments store a start instant, duration, optional reminder lead time, neutral completion state, title, and note. Deleting treatment deletes both record types in the same database transaction; Android alarm cancellation is a caller responsibility after the committed mutation.

Photo entries store metadata and may reference a phase or tray. `ownedFileName` is deliberately absent from schema 2. A decoded portable payload therefore cannot claim, delete, or overwrite an app-private file. The archive service stages verified JPEGs under newly generated lowercase UUID `.jpg` names and passes an exact photo-ID-to-filename map to `replaceFromBackupWithCleanup`; the map must contain every restored photo ID and no other ID. The repository accepts ownership only through that trusted parameter. After a successful transaction it returns superseded owned filenames for caller cleanup. On failure the old database and old file ownership remain intact.

## Phone authority and offline commands

`StateVersion` contains an opaque generation and monotonically increasing revision. User-data writes on the phone advance the revision. Restore and delete/start boundaries rotate the generation, and command outcomes are not portable.

A watch or other control command supplies an idempotency ID, expected generation, expected revision, requested state, source, and informational request time. The phone handles it in one Room transaction. A repeated ID returns its original durable accepted or rejected outcome. Generation or revision mismatch is durably rejected. An accepted transition uses the phone receive time; queued watch timestamps never insert into or reorder phone history. A same-state command is accepted without rewriting history. There is no backend or multi-writer merge.

## Installed database migration

Room migration 1 to 2 leaves the schema-1 treatment and wear-event tables intact and adds normalized history, journal, photo, coverage-gap, state-version, and command-ledger tables. A legacy treatment becomes one initial aligner phase, one full fixed-schedule revision, one known current-tray record, and one historical target. The migration creates a fresh opaque generation. It preserves installed records while clearly leaving unknown earlier tray changes unknown.
