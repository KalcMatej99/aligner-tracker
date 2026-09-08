# Aligner Tracker

A free, open-source Android app for recording clear-aligner wear and following a user-entered treatment schedule. Independently designed and implemented; TrayMinder is functional research inspiration, with no copied proprietary code, branding, UI assets or content. Not affiliated with TrayMinder or an aligner manufacturer.

**Local first:** no account, backend, ads, subscriptions, analytics or Internet permission. Every shipped feature is free. Treatment records stay in app-private storage unless you explicitly export them. Follow your clinician's prescribed target and schedule; this app records and reminds, and does not make treatment decisions.

## MVP

- One-tap IN/OUT tracking, persisted across app closure and restarts.
- Prescribed wear target, accurate midnight/DST accounting and visible partial-day coverage.
- Current tray, date estimates, explicit tray changes and treatment completion.
- Optional break/tray reminders, permission status and Android recovery handling.
- Date history, timestamp corrections and basic progress statistics.
- Portable JSON backup/validated restore, CSV report and complete local deletion.
- Native Kotlin, Compose Material3, Room, DataStore, Coroutines/Flow and WorkManager.

Android8/API26 or later; target/compile36. Development version0.1.0. See [validation evidence and limitations](docs/validation.md) for the exact tested state; physical-device/OEM acceptance and production release signing are separate gates.

## Build

Use JDK21 and Android SDK36/build-tools35.0.0; set `ANDROID_HOME` or ignored local.properties.

```sh
./gradlew spotlessApply
./scripts/check.sh
./gradlew connectedDebugAndroidTest  # running emulator/device required
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. See [development setup](docs/development.md).

## Project

- [Research, competitor comparison and free/paid evidence](docs/research.md)
- [Product requirements](docs/product.md) · [UX plan](docs/ux.md)
- [Architecture/API contract](docs/architecture.md) · [Android research](docs/android-research.md)
- [Roadmap and critical path](docs/roadmap.md) · [Forgejo issues](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues)
- [Privacy](docs/privacy.md) · [Backup format](docs/backup-format.md)
- [Contributing](CONTRIBUTING.md) · [Agent execution instructions](AGENTS.md)

Milestones: **Foundation → MVP → Beta → v1.0**. Widgets, variable schedules/refinements, photos, richer reports and Wear OS are tracked later work; there is no paywall planned for them. Optional sync requires a separate privacy/conflict design and is not an MVP dependency.

## License

Copyright2026 Aligner Tracker contributors. GPL-3.0-or-later; see [LICENSE](LICENSE). Dependency licenses remain their own. No user health data or signing keys belong in this repository.
