# Disposable Pixel acceptance fixtures

These are invented test records, never treatment advice or the owner's history. Import only into a
synthetic test session after the safeguards in [Pixel acceptance](../../../../../docs/pixel-acceptance.md).
The real `BackupCodec` importer and report calculations validate these exact files in
`PixelAcceptanceFixtureTest`; expected-day values were independently calculated by clipping elapsed
intervals at Europe/Rome day boundaries and excluding the two-hour unknown gap.

- `history-schema2.json`: complete photo-free schema-2 import, 13 transitions, two actual trays,
  two schedule revisions, two historical targets, one note, one appointment (reminders off), one gap.
- `legacy-schema1.json`: old-format import with three known events and one current tray. No richer
  history should be invented.
- `invalid-backup.json`: intentionally truncated JSON; preview must reject it without replacing data.
- `expected-days.json`: exact milliseconds and historical targets for 1–8 September 2026.
- `sample-landscape.jpg` / `sample-rotated.jpg`: non-personal image fixtures for picker/rotation checks.

The main fixture ends IN at 12:30 on 8 September 2026. That state continues until a new action;
therefore **today's total depends on when it is imported/viewed**. This is synthetic recorded state,
not proof of a person's wear. All times below are Europe/Rome, UTC+02:00 on these dates. No clock
changes are needed. Start fresh from the fixture when comparing independent correction cases.

| Date (September 2026) | IN | OUT | Known coverage | Recorded goal |
| --- | --- | --- | --- | --- |
| 1 | 11h 30m | 30m | 12h, partial; starts at noon | 22h |
| 2 | 23h | 1h | 24h | 22h |
| 3 | 23h 50m | 10m | 24h | 22h |
| 4 | 23h 50m | 10m | 24h | 22h |
| 5 | 23h 30m | 30m | 24h | 20h |
| 6 | 22h | 0m | 22h, partial; 12:00–14:00 unknown | 20h |
| 7 | 22h 30m | 1h 30m | 24h | 20h |
| 8 | 23h 30m | 30m | 24h | 20h |

On **9 September**, the optional streak is **2** (7 and 8 September); the partial 6 September
breaks it and today earns nothing. Later dates change this value. Tray 1 actually ends at noon
4 September: its interval contains **70h 10m IN, 1h 50m OUT, 72h covered**. Its prescribed interval
remains 7 days despite the later revision; tray 2's recorded prescription is 10 days.

For PX-06, edit event 6 (OUT at `2026-09-03T23:50+02:00`) to
`2026-09-04T00:05+02:00`. It stays before the IN event at 00:10. Expected result: 3 September
**24h IN / 0m OUT**; 4 September **23h 55m IN / 5m OUT**. The total OUT interval shortens
from 20 to 5 minutes. Editing to 00:11 must reject because it crosses the next event.

For the missing-interval case, restore the original fixture, then insert OUT from
`2026-09-02T18:00+02:00` through `2026-09-02T18:20+02:00` inside the existing IN interval.
2 September becomes **22h 40m IN / 1h 20m OUT**, still fully tracked. An interval from 12:30
through 13:30 crosses the existing 13:00 transition and must reject unchanged. Reversed/future
inputs also reject. Do not attempt to fill the explicit 6 September unknown gap using an action
that only inserts a known opposite-state interval.

The appointment is synthetic at 15:00 on 9 September with no reminder. Edit it to an appropriate
near-future time during the actual session to exercise appointment delivery. Image fixtures are
imported separately; photo-inclusive ZIP/encrypted exports are created during PX-13–PX-15.
