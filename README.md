# Aligner Tracker

A free, open-source Android app for recording clear-aligner wear and following a user-entered treatment schedule. Independently designed and implemented; TrayMinder is functional research inspiration, with no copied proprietary code, branding, UI assets or content. Not affiliated with TrayMinder or an aligner manufacturer.

**Local first:** no account, backend, ads, subscriptions, analytics or Internet permission. Every shipped feature is free. Full records stay in app-private storage unless you explicitly export them. The optional watch feature transfers minimal state and commands through Google Play Services; see [transport/privacy limits](docs/wear.md). Follow your clinician's prescribed target and schedule; this app records and reminds, and does not make treatment decisions.

## Public source and F-Droid

Public source and issue reports: [GitHub](https://github.com/KalcMatej99/aligner-tracker).
F-Droid [packaging request #4385](https://gitlab.com/fdroid/rfp/-/work_items/4385)
has been submitted; the app is not yet listed in its official repository.
See [submission details and build instructions](docs/fdroid/README.md).

## Expanded v1 implementation

- One-tap IN/OUT tracking, persisted across app closure and restarts.
- Prescribed wear target, accurate midnight/DST accounting and visible partial-day coverage.
- Variable tray intervals, explicit schedule revisions, refinements/retention and actual tray history.
- Optional break/tray/appointment reminders, snooze, notification controls and a home widget.
- Calendar/list history, forgotten interval corrections, notes, appointments and historical-target reports.
- Complete portable ZIP and authenticated encrypted backup/restore, legacy JSON import, previewed CSV and local deletion.
- Private progress photos, comparison and offline HTML time-lapse export.
- Wear OS companion, Tile and complication with a durable command queue and phone-authoritative reconciliation.
- Native Kotlin, Compose Material3, Room, DataStore, Coroutines/Flow and WorkManager.

Android8/API26 or later; target/compile36. Development version1.0.0-dev. See [validation evidence and limitations](docs/validation.md) for the exact tested state; physical-device/OEM acceptance and production release signing are separate gates.

## Build

Use JDK21, Python3.11+, curl and Android SDK36/build-tools35.0.0; set `ANDROID_HOME` or ignored local.properties.

```sh
./gradlew spotlessApply
./scripts/check.sh
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
ANDROID_SERIAL=emulator-5556 ./gradlew :wear:connectedDebugAndroidTest
```

Phone debug APK: `app/build/outputs/apk/debug/app-debug.apk`; watch: `wear/build/outputs/apk/debug/wear-debug.apk`. Unsigned release APKs are under each module’s `build/outputs/apk/release/`. See [development setup](docs/development.md).

## Project

- [Research, competitor comparison and free/paid evidence](docs/research.md)
- [Product requirements](docs/product.md) · [UX plan](docs/ux.md)
- [Architecture/API contract](docs/architecture.md) · [Android research](docs/android-research.md)
- [Roadmap and critical path](docs/roadmap.md) · [Forgejo issues](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues)
- [Privacy](docs/privacy.md) · [Backup format](docs/backup-format.md)
- [Contributing](CONTRIBUTING.md) · [Agent execution instructions](AGENTS.md)

Milestones: **Foundation → MVP → Beta → v1.0**. Expanded features are implemented in the existing codebase; [the finite checklist](docs/v1-checklist.md) distinguishes verified software from open acceptance gates. This development build is not declared release-ready. Optional cloud sync is outside scope.

## License

Copyright2026 Aligner Tracker contributors. GPL-3.0-or-later; see [LICENSE](LICENSE). Dependency licenses remain their own. No user health data or signing keys belong in this repository.
