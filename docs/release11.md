# Private update 11

Prepared following the owner's approval and explicit “Merge and deploy” request.
PR #54 is merged; version 11 includes the prior Progress redesign, the Settings,
Treatment details, Calendar and Notes redesign, and Photo progress access from Progress.

Source tag: `phone-release-11`; source commit: `9caa8022c59aeda951e1c59bb05d8d78a21f2a9a`.
Phone SHA256: `4440389693da76e18f4f7aee9dba71b1271bdf2c65dd2253285aed3799a6d1bb`.
Original app signer and private repository identity are unchanged. Wear is a local
companion artifact, not a phone-store entry. No owner-phone installation occurred.

## Validation

PR #54 hosted CI run 54 passed. Local formatting, release lint and phone/Wear release
builds passed for version 11. Supporting-page implementation evidence includes 67
phone and 9 Wear JVM tests, eight distinct focused emulator tests and 40 visual
captures: [design and validation](supporting-pages.md).

The signed 10 → 11 API36 emulator upgrade preserved every database table, including
one treatment, one note, one appointment and 13 wear events. Preference datastores
were absent on both sides, so this does not establish populated reminder preference
preservation. Progress → Photos and Back work in the signed build.
[Upgrade evidence](evidence/release11/upgrade-verification.json).

The store update retains older APKs and corresponding sources. Human comprehension,
TalkBack speech, physical-device and paired Wear acceptance remain separate.

Published 2026-09-11 after [CI run 56](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/actions/runs/56) passed. PR #55 merged as `6092dcfdb32fcabcbe46d2ac19f4f728efdfd426` with the exact tagged source tree. Every live snapshot file was downloaded over HTTPS and matched; signed indexes and APK identity verified. LAN access remains 403. [Live verification](evidence/release11/store-live-verification.json). Encrypted store backup was read back and its restored identity signed successfully.
