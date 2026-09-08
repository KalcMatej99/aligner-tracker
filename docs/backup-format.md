# Portable data, schema 1

UTF-8 JSON object: `schemaVersion` (exactly1), `plan` (one treatment object or null), `events` (ordered array). `plan` has id1, ISO dates `startDate`/`currentTrayStartedOn`, `totalTrays`, `currentTray`, `daysPerTray`, `dailyGoalMinutes`, IANA `zoneId`, epoch-millisecond `trackingStartedAt`, `completed` and nullable `completedAt`. Each event has positive unique `id`, epoch-millisecond `at` and Boolean `wearing` indicating state from that instant.

The first event equals trackingStartedAt; timestamps strictly increase, states alternate, no event is future-dated or after completion. Plan date/count/goal/zone ranges are validated by the same code used for setup. Empty backups contain null plan and no events. Maximum UTF-8 size5MiB and50,000 events. Unknown schema/fields, duplicate keys, excessive nesting, invalid references/times and malformed UTF-8 are rejected. No third-party TrayMinder import is claimed.

Restore is **replacement**, not merge. The system picker supplies a document, the app bounds and validates the full content, displays a preview and asks for confirmation, then writes Room in one transaction. Failure leaves old records. Reminder preferences/Android permissions are device-local and not in this format; existing reminder choices remain, delivery records reset, stale alarms cancel and current plan is reconciled. Export is plaintext; the user chooses the destination/provider. No automatic backup is provided.

CSV is a report, not a restore format: daily date, treatment_timezone, worn_millis, removed_millis, tracked_millis, day_millis, current_goal_minutes and coverage. Durations are exact elapsed milliseconds; day_millis reflects DST. Coverage is full/partial/untracked. All comparisons use the current prescribed goal, because historical goals are deferred to #14. Before first tracking is not implicitly IN or OUT. Treatment completion clips accumulation. CSV quoting/newlines follow common RFC4180 conventions.

Any incompatible schema change must ship a tested importer/migration for previous exported versions and Room migrations preserving installed data.
