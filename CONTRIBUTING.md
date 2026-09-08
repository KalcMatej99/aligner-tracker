# Contributing

Read AGENTS.md, the assigned Forgejo issue, product, architecture and roadmap. Propose a scoped issue before material new work; claim it with an implementation comment. Use branches or non-overlapping ownership and small commits referencing issue numbers. Never copy competitor code, assets, wording, branding or screenshots. Original contributions are GPL-3.0-or-later.

Run `./gradlew spotlessApply`, then `./scripts/check.sh`; add meaningful domain tests for time/persistence changes and emulator coverage for visible flows. Include test evidence and unresolved device checks in the issue/PR. Keep strings translatable, errors accessible, and imports atomic. Avoid speculative abstraction, dependencies and tests that merely mirror implementation.

No secrets, health-data fixtures from real users, analytics or network access. Use synthetic records in tests. Signing credentials stay outside git. Do not close an issue solely because an artifact compiled; satisfy its acceptance criteria. Release publication and user contact require explicit authorization.
