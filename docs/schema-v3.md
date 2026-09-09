# Tracking-first contract (Room / portable schema 3)

Issue #32 supersedes the complete-plan-first contract. The existing `treatment`
row and `TreatmentPlan` name remain compatibility containers for a tracking
session: identity, fixed accounting zone, first event timestamp and completion
state. Prescription metadata is independent and nullable: treatment start,
current tray, total trays, prescribed interval, current tray start and daily goal.
Null means unknown. No zero, tray 1, today's treatment date or default prescription
is substituted. The quick-start transaction captures its clock and selected device
zone and inserts exactly one deliberate IN/OUT event. Duplicate starts fail atomically.

Each supplied count/date is validated independently. Current tray cannot exceed a
known total; tray start cannot precede a known treatment start. Historical treatment
start may be unknown or supplied later; it never controls wear coverage. A due date
needs current tray, interval and tray start; total is unnecessary. Whole-plan
projections, variable schedules, advancement and completion require all four tray
fields. Before that, there are no phases, revisions or tray-history rows. Attaching
a full schedule records only the known current tray, with attachment time as the
start of attributable tracking, and a nullable phase start. It does not infer
previous trays. Existing schedule changes/advance actions retain historical rows. Explicit corrections of current tray/date close the prior attribution at the correction time and append a new row. Total/interval corrections append a revision; prior intervals remain exact, including variable future intervals, and totals cannot exclude already recorded trays. Once a schedule is recorded its four required values can be corrected but not erased.

The first target added later records `effectiveAt` as well as its accounting-zone
`effectiveFrom` date. The partial effective day has no whole-day target comparison;
raw totals remain visible. Subsequent target updates retain the established default
of the next calendar day. No lookup falls back to the newest target for earlier
unknown days. Legacy targets have null effectiveAt and retain their original
whole-day semantics, exact dates, values and IDs. CSV uses an empty goal cell for
unknown/non-whole-day targets. Unknown targets never qualify for an adherence streak.

Migration 2→3 copies every existing treatment and phase column value while making
optional columns nullable, and adds nullable target effectiveAt. All other tables
and reminder preferences are untouched. Migration 1→2→3 remains supported. No
destructive fallback. Exported Room schemas are committed and tested with Room's
MigrationTestHelper, following [Android migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions).

Portable schema3 retains the existing strict top-level contract and photo/archive
envelope, introduces null metadata and effectiveAt, and is emitted for every new
export. Strict schema1/schema2 imports remain supported. Old readers reject version3
before interpreting fields. Plain JSON and encrypted archives share the same codec.
Device-local reminder settings and sync receipts retain their existing lifecycle.

Wear protocol1 already defines nullable tray and a separate hasPlan flag; no wire
shape or version change is needed. hasPlan means a tracking session exists. Existing
peers accept null tray, hide its label and still permit IN/OUT. Version checks remain
mandatory; unsupported versions do not mutate acknowledged status. Widget controls,
break alarms, notes, appointments and photos require only a tracking session. Tray
alarms require a computable due date. No new permission or background service.
