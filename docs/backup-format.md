# Portable backup and report formats

New exports are a ZIP portable archive containing `records.json` (strict schema 3) and every private normalized photo as `photos/<positive-id>.jpg`. An empty or photo-free treatment uses the same format. [Schema 3](schema-v3.md) adds honest partial setup to the [schema 2](schema-v2.md) records and defines treatment phases, immutable schedules, actual tray history, historical targets, wear events, unknown coverage gaps, notes, appointments and photo metadata. Device preferences, notification permissions, pending watch commands and command outcomes are not portable treatment records.

Imports accept complete ZIP archives, encrypted archives described in [encrypted-backup.md](encrypted-backup.md), and existing schema-1 JSON backups. Schema-1 imports expand the fixed schedule and known current tray without inventing earlier changes. Plain schema-2/schema-3 JSON is accepted only when it references no photos; photo metadata without image bytes is not a complete restore.

To bound memory on ordinary phone heaps, archives are limited to 32 MiB compressed and expanded, records to 5 MiB and 50,000 events, each photo to 10 MiB, and the archive to 1,001 entries. Only the exact records and referenced photo names are accepted. Duplicate names, unknown entries, malformed UTF-8, unknown/duplicate fields, invalid dates/references, future or overlapping events, missing photos, digest/size mismatches and invalid image dimensions are rejected before preview. Photos must decode as JPEG with dimensions matching metadata and no dimension above 1,600 pixels. ZIP paths are never filesystem paths.

Restore is replacement after a count preview and explicit confirmation. All images are validated and staged under newly generated private names before one Room transaction replaces records. A failure before commit preserves existing data. Superseded files are deleted only after commit; incomplete cleanup is reported and retried from the private directory inventory on the next app start or Delete data. Restore rotates the command generation so stale phone/watch controls cannot act on restored records.

The file picker chooses the destination/provider. Cancelling selection or the password/restore dialog leaves treatment unchanged. An already-created destination may remain empty after cancellation or incomplete after a write error; retry to a new file and retain the last known-good backup. Exported files belong to the chosen provider and are never deleted by app Delete data. A provider can sync an explicitly exported file using its own settings.

Reminder and optional-streak preferences remain device-local through restore; delivery records reset and reminders reconcile against restored records. Delete data resets these preferences, cancels reminders and removes private photos and temporary captures. See [privacy](privacy.md) for lifecycle limits.

## CSV report

CSV is a factual report, not a restore format: daily date, treatment_timezone, worn_millis, removed_millis, tracked_millis, day_millis, current_goal_minutes and coverage. The historical column name `current_goal_minutes` is retained for CSV compatibility; its value is now the prescribed target recorded for that row's date, rather than today's configured target. Durations are exact elapsed milliseconds; day_millis reflects DST in the saved treatment zone. Coverage is full/partial/untracked; clock gaps and periods outside tracking are excluded. Treatment completion clips accumulation. Before first tracking is not implicitly IN or OUT. Quoting/newlines follow RFC4180 conventions. A preview precedes choosing a destination; nothing is sent automatically.

Any future incompatible schema change must include a tested importer and installed Room migration. Destructive migration fallback is prohibited.

Version3 exports are explicitly rejected by older readers. Legacy schema1/schema2 imports remain supported. Null prescription fields and exact first-target effectiveAt are preserved in both plain and encrypted round trips; CSV leaves unavailable goal cells empty.
