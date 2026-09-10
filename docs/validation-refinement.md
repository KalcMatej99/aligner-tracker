# Refinement validation — candidate 7

Issue [#37](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues/37). [Decisions and sources](visual-time-refinement.md).

## Comparable visual evidence

Signed version 6 baseline and version 7 use the same synthetic established records; live session time advances naturally between captures. Fresh and partial starts are equivalent zero-field journeys, with no invented prescription. Normal configuration: API36, 1080×2400, density420, English, font100%. Constrained configuration: 840×1600 (320dp), font200%. Screenshots are actual emulator renders, not mockups.

| Journey | Version 6 | Candidate 7 |
|---|---|---|
| Fresh start | [Before](screenshots/refinement/before/fresh.png) | [After](screenshots/refinement/after/fresh.png) |
| Partial/unknown prescription | [Before](screenshots/refinement/before/partial.png) | [After](screenshots/refinement/after/partial.png) |
| Established Today | [Before](screenshots/refinement/before/today.png) | [After](screenshots/refinement/after/today.png) |
| History | [Before](screenshots/refinement/before/history.png) | [After](screenshots/refinement/after/history.png) |
| Schedule | [Before](screenshots/refinement/before/schedule.png) | [After](screenshots/refinement/after/schedule.png) |
| Reports: target changes and gap | [Before](screenshots/refinement/before/reports.png) | [After](screenshots/refinement/after/reports.png) |

## Review

One material implementation/visual review identified and corrected an unbounded native calendar width; exposed the native input mode directly for constrained text; preserved the report range total; removed raw offsets from interval confirmation; included dates in spoken chart summaries; and gave target markers a contrasting outline. Focused rechecks follow these changes.

Visual judgment: state/action hierarchy and neutral version6 identity remain coherent. Today has fewer competing totals. Seven report days fit where the baseline showed roughly two detailed rows. Missing time remains visibly distinct. The tray sequence adds recorded context without suggesting proportional duration. Native dialogs necessarily use more space than compact screen controls. This assessment is not owner aesthetic acceptance.

## Evidence status

Repository checks pass: **63 phone JVM tests + 9 watch JVM tests**, formatting, debug/release lint (zero errors), phone/watch debug, test and minified release builds. [Command log](evidence/refinement/repository-checks.txt).

The initial full phone instrumentation run passed 48/52. Remaining checks required updated native-picker/disclosure selectors, stable lazy-list/keyboard navigation in the appointment test and the documented emulator notification grant. Focused rechecks cover these cases; native Android accessibility checks additionally require bundled TalkBack enabled. These logs describe exported nodes/actions, not human speech/traversal acceptance. Physical Pixel installation, human TalkBack traversal, owner aesthetic acceptance, OEM reminders and paired Wear acceptance are outside this emulator result. No publication is authorized by this candidate preparation.

All **52 unique general phone cases** passed across the initial full run and focused rechecks, rather than one claimed green full-suite run. The five picker cases passed in the [picker recheck](evidence/refinement/picker-appointment-recheck.txt); its appointment scroll failure was corrected and [the exact repeated-time appointment edit passed](evidence/refinement/appointment-preservation.txt). [Native Android accessibility: 2/2 passed](evidence/refinement/platform-accessibility.txt) with bundled TalkBack enabled. Report semantics and notification recovery passed in the [harness recheck](evidence/refinement/harness-recheck.txt). Only harness/selectors changed after the final production-code checks; final formatting passes.

## Initial signed upgrade checkpoint

Built from clean tagged source `681ae2f` / `visual-time-candidate-7`, versionCode **7**, versionName `1.0.0-dev`. Both phone and watch signatures/alignment verify with permanent signer `9cac6b2722e6e074bd607586df06335c3ae6300803616b3f38e88706d1ed53a9`. Both manifests retain no Internet permission.

The actual downloaded signed6 APK was installed on the disposable emulator, populated with the preserved synthetic fixture and launched. Candidate7 was installed with `adb install -r`, without uninstall between signed versions. Every database table and reminder-preference byte hash matched afterward: [upgrade comparison](evidence/refinement/signed-upgrade.json). [Before upgrade](screenshots/refinement/before/upgrade-today.png) / [after upgrade](screenshots/refinement/after/upgrade-today.png). No schema changes or export/accounting implementation changes; existing serialization, time-math, migration and archive tests pass.

Bundle: `/home/matejkalc/.local/share/aligner-tracker/releases/visual-time-candidate-7/`. Includes signed phone/watch APKs, exact tagged source, GPL license, dependency inventory, build instructions, signer verification, release manifest and checksums. Phone SHA256: `6fc1bb8fde25020b3903037a16a2064508460bca0ec765bdc043c65a9f2a6684`. Watch SHA256: `b6dabc7f7cf797c7b36f2770c132854c99eafe8c0560910cd73f28d7343aed3f`. The store remains version6; this candidate is not published.

## Constrained rendering and limits

Inspected [320dp/200% dark](screenshots/refinement/after/small-dark.png), [forced RTL](screenshots/refinement/after/small-dark-rtl.png), [expanded en-XA](screenshots/refinement/after/expanded-small-dark.png) and [ar-XB](screenshots/refinement/after/pseudo-rtl-small-dark.png). Compare [small baseline](screenshots/refinement/before/small-dark.png) and [RTL baseline](screenshots/refinement/before/small-dark-rtl.png). The saved partial fixture is reused; elapsed durations naturally advance. Scrolling is required at these sizes; text is not capped. Native [calendar](screenshots/refinement/after/calendar.png) and [constrained localized input mode](screenshots/refinement/after/calendar-small-dark.png) remain available, with calendar/keyboard controls explicitly labelled. Native input mode avoids forcing a seven-column calendar into a narrow, enlarged-text viewport.

These are sampled configurations and journeys, not exhaustive coverage of every screen/state/configuration combination. Real-language translation, human TalkBack speech/traversal, physical font/keyboard behavior and owner aesthetics remain unverified. The standalone Wear emulator attempt exited before tests; watch JVM tests and both builds passed, but this run adds no paired-watch acceptance. Two emulator process exits during concurrent work were recovered by using the phone sequentially; they were not attributed to app crashes. No accounts, external service, physical installation or publication was added.

The first file-only comparison was superseded: copied fixture directories retained the previous debug UID SELinux category and prevented release reads. The emulator fixture labels were corrected to the installed package context. The repeated signed6→7 check verified rendered records on both versions and exact table/preference equality. This was a fixture setup error, not a persistence migration.

Native [12-hour clock](screenshots/refinement/after/clock.png), [320dp/200% clock](screenshots/refinement/after/clock-small-dark.png) and [scrolled confirmation controls](screenshots/refinement/after/clock-small-dark-actions.png) were inspected. Clock values remain legible and Save/Cancel are reachable by scrolling.

## Final candidate — authoritative artifact

The final range label explicitly says **Recorded wear in this range**, with aggregate OUT/tracked durations retained under About this breakdown. This presentation-only follow-up passed focused release compilation/lint and final clean formatting/phone/watch release builds. It changes no picker or data behavior.

**Use final source `3ec3246`, tag `visual-time-candidate-7-final`.** This supersedes the initial artifact hashes above. Final phone SHA256: `064d69842930871443aec6a90b273515d20225755aeee72301656e9c1cf7cb4e`; final watch SHA256: `2e76aa818c08a9e43d1603649173d41f11033e06f92313d8720b7449700b93b9`. [Manifest](evidence/refinement/release-candidate.json). The bundle path is unchanged; it now contains these final artifacts and corresponding source.

The final APK was again installed directly over signed6, with rendered records verified on both versions and exact database/reminder equality. The [upgrade evidence](evidence/refinement/signed-upgrade.json) identifies this final checksum. [Visual comparison viewer](visual-comparison.html) provides before/after pairs. [Settings accounting zone](screenshots/refinement/after/settings-zone.png) is the sole phone timezone surface.

Additional final captures: [24-hour clock](screenshots/refinement/after/clock-24h.png), [empty History](screenshots/refinement/after/empty.png). The native clock switched presentation with the emulator 12/24-hour setting; cancellation left the event unchanged. The settings screenshot and signed Reports capture use the final artifact.
