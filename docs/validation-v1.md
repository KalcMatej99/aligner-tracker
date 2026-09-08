# Expanded v1 validation — 2026-09-08

Development integration is in progress; this document separates measured software results from external gates. Only synthetic emulator records are used. Historical MVP and baseline hosted results do not establish acceptance for the expanded final code.

## Verified integration evidence

- Room database 1→2 migration passes on API36 using the checked schema1 installed database structure; legacy records become normalized history without fabricated earlier tray changes. Schema1 JSON compatibility and malformed/future/overlap rejection are covered by JVM tests.
- API36 phone: 20 instrumentation tests passed after focused corrections: 19 tests in the full run and the widget test on focused rerun. The full run's widget failure was the Android `appwidget --user current` shell command using unresolved user -2; the test now resolves the actual numeric user before binding. No product assertion was weakened.
- Real bound home widget: current state renders, actual PendingIntent changes state, repeating it adds no transition, stale revision and post-delete actions cannot change records.
- Photo instrumentation: 3200×1600 image with EXIF rotation/GPS becomes an 800×1600 normalized private JPEG with GPS removed; complete ZIP round-trip validates image digest/dimensions, stages new ownership, rotates generation and deletes superseded copies. Additional cleanup/capture rechecks are pending.
- API36 Wear OS emulator: debug APK installed; cold launch 1.154 s; 2 instrumented tests pass (durable disk outbox reopen/stale outcome and real Activity launch). The unpaired screen states that phone status is unknown. This is not paired transport acceptance.

## Performance evidence and limits

Measured on accelerated local x86_64 API36 emulator, two virtual cores and 3 GiB phone RAM, debug build. One synthetic 50,000-event run measured: transactional restore 675 ms; 30-day summaries 24 ms; JSON round-trip 1,693 ms; CSV 18 ms; encrypt+decrypt 1 MiB with both 600,000-round PBKDF2 derivations 4,443 ms. KDF and image/archive processing run off the UI thread. These are emulator observations, not physical phone budgets. Portable archive limits are 32 MiB compressed/expanded and time-lapse output 16 MiB to bound heap pressure.

The initial software-emulated device run suffered system startup ANRs; it was discarded as acceptance. Accelerated test emulators were then started without wiping their data. No personal device or shared runner service was altered by this app lane.

## Open acceptance

Final formatting/lint/debug/release builds, final hosted SHA/artifacts, new-screen visual acceptance and cleanup rechecks are still being integrated. Paired phone/watch transport needs a compatible companion app and interactive pairing; standalone and protocol tests cannot substitute. Physical Android/OEM/Doze/reboot/force-stop (#11), full manual accessibility (#12), matched 24-hour physical battery (#17), consented usability pilot (#18), physical watch (#20), and user-owned signing/authorized publication (#21) remain open under [external-acceptance.md](external-acceptance.md). GPL/proprietary Wear client packaging is under concrete investigation before public distribution. No v1 release-ready claim is made.
