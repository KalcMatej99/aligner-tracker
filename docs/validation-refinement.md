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
