# Release preparation and artifact verification

Current private phone update: **versionCode 9**, published on 2026-09-10 after the
owner preferred the updated Today screenshot and requested “Merge and deploy”.
[PR #44](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/pulls/44) is merged.
[Install APK](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_9.apk)
while connected to Tailscale, or refresh Kalc Apps in F-Droid.

Today centers a larger aligner illustration and session timer, places compact
recorded wear / actual target below the bar, and removes the separate prescribed-time
sentence and tray index. Source tag `phone-release-9` points to
`0ec4b220e49d846aceec388d46b6cea315a457df`; merged main
`073d9fa33afe3ac297088ff8970fe8b8fd175e41` has the same Git tree.
Hosted [run 39](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/actions/runs/39)
passed on the final candidate. Local release lint, formatting and both release builds
passed; the preceding UI change passed 30 instrumentation tests and all repository
checks (63 phone + 9 Wear JVM tests).

Phone SHA256: `1ae78d68b8fa8f9f4c32af5319c657d6d3c6fb7349678f63d0e799c59b17013b`.
The original app and repository signing identities are unchanged. Signed 8→9 API36
emulator upgrade preserved every database table. No preference datastore was present
before or after; this is not populated-reminder evidence.
[Upgrade evidence](evidence/release9/upgrade-verification.json),
[before](screenshots/release9/signed-before.png),
[after](screenshots/release9/signed-after.png).

Fresh HTTPS downloads matched every snapshot file, including the APK and signed
indexes; non-Tailscale LAN access still returns403. The mobile store page lists9
first with no overflow or page errors. Versions2–8 remain available. Wear9 is a local
companion artifact, not a phone-store entry. No owner phone installation occurred.
The owner's positive screenshot feedback is recorded; human comprehension, TalkBack
speech and physical-device acceptance are not inferred from it.
[Live verification](evidence/release9/store-live-verification.json),
[browser evidence](evidence/release9/store-browser.json),
[store capture](screenshots/release9/store-mobile.png),
[release manifest](evidence/release9/release.json).

Previous private phone update: **versionCode 8**, published with the owner's “Merge and deploy”
authorization on 2026-09-10 after merging [PR #41](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/pulls/41).
[Install APK](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_8.apk) while connected to Tailscale,
or refresh Kalc Apps in F-Droid. Original fitted/separated Today status illustrations
retain the session context and explicit opposite-action labels.

Source tag `phone-release-8`, commit `18c837221773ecc76cf8ecf184b45481b3a3336c`.
Merged main `5a3978c0ed32a908926d272a9cd659ef71c979f9` has exactly the same Git tree as
the signed candidate. Hosted [run 34](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/actions/runs/34)
passed for that candidate; local formatting, release lint and both release builds
passed. Phone SHA256:
`1618b6e0ebafb75589baf73588d269bfb383e0b2ec7634aa730b956b0688b85b`.

The actual signed 7→8 API36 emulator upgrade preserved every database table. The
fixture had no persisted reminder preferences before or after; this is not a
populated-reminder acceptance claim. [Before](screenshots/release8/signed-before.png),
[after](screenshots/release8/signed-after.png), [upgrade evidence](evidence/release8/upgrade-verification.json).
No owner phone was installed onto. Human comprehension, TalkBack speech, aesthetics
and physical acceptance remain open. Wear8 is a local signed companion artifact,
not a phone-store entry.

All HTTPS snapshot files were downloaded and matched, signed indexes and APK
signatures verified, and the mobile page inspected for version8/no overflow/no page
errors. App and repository identities are unchanged; versions2–7 remain available;
non-Tailscale LAN requests still return403. [Live verification](evidence/release8/store-live-verification.json),
[browser](evidence/release8/store-browser.json), [page capture](screenshots/release8/store-mobile.png),
[release manifest](evidence/release8/release.json).
The first promotion attempts found missing Java tools in the default and sudo PATH;
no live pointer changed until the installed JDK21/SDK were supplied explicitly and
snapshot verification passed. No system tool installation or shared configuration
change was needed.


Previous private phone update: **versionCode 7**, published with owner authorization
on 2026-09-10 after merging [PR #38](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/pulls/38).
[Install APK](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_7.apk) while connected to Tailscale.
Merged main `bf59c97`; application code matches tested source `3ec3246`, tag
`visual-time-candidate-7-final`. Phone SHA256
`064d69842930871443aec6a90b273515d20225755aeee72301656e9c1cf7cb4e`.
All HTTPS snapshot files and signed indexes were downloaded and verified; app and
repository signing identities are unchanged. Versions 2–6 remain available.
Non-Tailscale LAN access returns 403. [Deployment evidence](evidence/release7/store-live-verification.json).
[Validation and same-signer upgrade preservation](validation-refinement.md).
No owner phone installation; owner aesthetic and physical acceptance remain open.
Wear remains a local companion artifact, outside the phone catalogue.

Previous private phone update: **versionCode 6**, published with explicit owner authorization
on 2026-09-10. [Install APK](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_6.apk).
Source tag `phone-release-6`, merged main commit `690f321340132694a85a3514bab8d86ba996599a`.
APK SHA256 `b3b0c97937aef80f12b12aee85e7a73b2f52278e94ff66e0f2593466f25b46da`.
PRs #36 and #35 are merged. The main tree exactly matches the checked build tree.
Repository formatting, JVM tests, debug/release lint and phone/watch builds passed.
The signed 5→6 emulator upgrade preserved all database tables and reminder bytes.
Live HTTPS APK and signed indexes match the verified snapshot; versions 2–5 remain
available. App/repository signing identities are unchanged. Encrypted store backup
was read back and the restored identity signed/verified in isolation.
No owner phone was installed onto. Owner aesthetic/device acceptance remains open.
[Deployment evidence](evidence/release6/store-live-verification.json).

Previous private phone update (issue #32): versionCode **3**, `1.0.0-dev`, source
`91536654d68d3afb05e003d7f045f81422f9376a`, tag `pilot-phone-quick-start-3`.
[Download](https://apps.server.matejkalc.com/fdroid/repo/org.alignertracker.app_3.apk).
APK SHA256 `4c013f25d66408c97d1e26522f3c8338a168cd086cb6a7623312a88b355549c8`;
source archive SHA256 `c601f6710a0a14a81fc831939c1e58647ebce87472a91d6eb77e00f122a2fda3`.
The permanent signer below is unchanged. Actual signed version2→3 emulator upgrade
preserved synthetic records and reminder preferences. [Validation](validation-32.md)
records tests, visual checks, exact served artifact verification and open owner gates.
Bundle: `/home/matejkalc/.local/share/aligner-tracker/releases/quick-start-3/`.

## First pilot publication (historical)

Status: the owner authorized private phone pilot publication through Kalc Apps on 2026-09-09 and confirmed creating a permanent local app signing identity with encrypted backup. Version code 2 / `1.0.0-dev` remains a development pilot, not stable v1 acceptance. The preparation procedure below is retained for future stable candidates; physical/paired Wear and final signed upgrade acceptance remain open.

Private pilot source: `82a11d0da1369db89e13d049eedc7cb913e8fea9`, annotated tag `pilot-phone-2026-09-09`. Phone APK SHA-256: `ac6c68c9227677deea713e05ff5953c6cd519b298bef50508a9090471497b4dc`. App signer SHA-256: `9cac6b2722e6e074bd607586df06335c3ae6300803616b3f38e88706d1ed53a9`. Source archive SHA-256: `796926321b35d5ca2c744d4656941f98a545121eec25ccb8a42b09d1ac52d732`.

Operator identity is under `/home/matejkalc/.local/share/aligner-tracker/signing/` (0700; key/password files 0600), alias `aligner-release`. Password input is the restricted `password.txt` file; omit `--key-pass` for this PKCS12 identity so apksigner reuses the keystore password. The repository key is separate. Encrypted backup: `/home/matejkalc/backups/aligner-tracker/`; recovery key: `/home/matejkalc/.config/aligner-tracker/recovery.key`. Local recovery was read back and restored in isolation, and the restored key signed the verified APK. This is local recovery, not off-host redundancy. Preserve these files for update continuity; never regenerate or rotate them during a routine release.

The release bundle, checks, immutable store manifest and custody evidence are under `/home/matejkalc/.local/share/aligner-tracker/releases/pilot-82a11d0da136/`. 55 phone JVM tests, formatting, release lint and release build passed. Store issue [#11](https://forgejo.server.matejkalc.com/matejkalc/kalc-app-store/issues/11) records publication verification. [Add the private repository](https://apps.server.matejkalc.com/add/) while connected to Tailscale. Only the phone APK is included. Existing debug-signed installations cannot update in place; protect their records with a verified encrypted portable backup before any owner-performed reinstall.


## Candidate inputs

A candidate starts from a clean, reviewed commit and an annotated immutable tag. Before building, record the commit, tag, phone and watch `versionCode`/`versionName`, Java version, Android SDK/build-tools versions and the dependency-inventory hashes. Remove the `-dev` version suffix only through a reviewed release change. Both modules use application ID `org.alignertracker.app`; keep their versions aligned.

Use the repository's pinned local toolchain where available:

```bash
export JAVA_HOME=/home/matejkalc/.local/share/aligner-android/jdk21/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/home/matejkalc/.local/share/aligner-android/sdk
git status --short
git rev-parse HEAD
./scripts/check.sh
```

Do not accept a dirty tree or a failed check. `assembleRelease` is expected to create unsigned, minified APKs under `app/build/outputs/apk/release/` and `wear/build/outputs/apk/release/`. Confirm the actual filenames from `output-metadata.json`; an unsigned APK is a build intermediate, not an installable release artifact.

Create the corresponding source archive from the final tag and hash it:

```bash
export ALIGNER_RELEASE_TAG=replace-with-final-tag
git archive --format=tar.gz --prefix="aligner-tracker-$ALIGNER_RELEASE_TAG/" \
  --output=/tmp/aligner-tracker-source.tar.gz "$ALIGNER_RELEASE_TAG"
sha256sum /tmp/aligner-tracker-source.tar.gz
```

Reject `*-debug.apk` and `*-androidTest.apk` as release inputs. They are internal test artifacts even when their tests pass. The candidate gate accepts only the release outputs built from the recorded tag, then aligned, signed and verified below.

## User-owned signing identity

Android requires the same signing certificate for an in-place update, and losing the key prevents future updates under the same application ID. The [Android signing guide](https://developer.android.com/studio/publish/app-signing) recommends a validity period of at least 25 years. The user creates and controls the key outside the checkout, stores its passwords only in a password manager or interactive prompt, and keeps at least two encrypted backups in separately controlled locations.

One example that prompts interactively for all sensitive values is:

```bash
export ALIGNER_KEYSTORE=/absolute/private/path/aligner-release.p12
export ALIGNER_KEY_ALIAS=aligner-release
keytool -genkeypair -v \
  -keystore "$ALIGNER_KEYSTORE" \
  -storetype PKCS12 \
  -alias "$ALIGNER_KEY_ALIAS" \
  -keyalg RSA -keysize 4096 -validity 9125
```

Never put the keystore, passwords or a signing properties file in this repository. Use the same certificate for the phone and watch APKs. Before first use, record the expected certificate SHA-256 fingerprint in the private release record and verify the backup can be opened.

With Android build-tools 35.0.0, align before signing, then sign both APKs. `apksigner` prompts for passwords when they are omitted:

```bash
export ALIGNER_BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"

"$ALIGNER_BUILD_TOOLS/zipalign" -p -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  /tmp/aligner-phone-aligned.apk
"$ALIGNER_BUILD_TOOLS/apksigner" sign \
  --ks "$ALIGNER_KEYSTORE" --ks-key-alias "$ALIGNER_KEY_ALIAS" \
  --out /tmp/aligner-phone.apk /tmp/aligner-phone-aligned.apk

"$ALIGNER_BUILD_TOOLS/zipalign" -p -f 4 \
  wear/build/outputs/apk/release/wear-release-unsigned.apk \
  /tmp/aligner-wear-aligned.apk
"$ALIGNER_BUILD_TOOLS/apksigner" sign \
  --ks "$ALIGNER_KEYSTORE" --ks-key-alias "$ALIGNER_KEY_ALIAS" \
  --out /tmp/aligner-wear.apk /tmp/aligner-wear-aligned.apk
```

Do not modify an APK after signing. Verify alignment, signatures, certificate identity, package/version metadata and checksums independently:

```bash
"$ALIGNER_BUILD_TOOLS/zipalign" -c -v 4 /tmp/aligner-phone.apk
"$ALIGNER_BUILD_TOOLS/zipalign" -c -v 4 /tmp/aligner-wear.apk
"$ALIGNER_BUILD_TOOLS/apksigner" verify --verbose --print-certs /tmp/aligner-phone.apk
"$ALIGNER_BUILD_TOOLS/apksigner" verify --verbose --print-certs /tmp/aligner-wear.apk
"$ALIGNER_BUILD_TOOLS/aapt" dump badging /tmp/aligner-phone.apk | head -n 3
"$ALIGNER_BUILD_TOOLS/aapt" dump badging /tmp/aligner-wear.apk | head -n 3
sha256sum /tmp/aligner-phone.apk /tmp/aligner-wear.apk
```

The private release manifest records every command outcome, the source commit/tag and archive hash, phone/watch APK SHA-256 hashes, signer certificate SHA-256, package/version data, toolchain versions, dependency-inventory hash, automated test counts, physical acceptance evidence links and all open limitations. A second person or a clean environment must recompute the hashes and certificate fingerprint before publication approval.

## Fresh install and phone/watch pairing

Use dedicated test devices with synthetic data. Confirm each serial before installation:

```bash
adb devices -l
adb -s PHONE_SERIAL install /tmp/aligner-phone.apk
adb -s WATCH_SERIAL install /tmp/aligner-wear.apk
adb -s PHONE_SERIAL shell dumpsys package org.alignertracker.app | grep -E 'versionCode|versionName'
adb -s WATCH_SERIAL shell dumpsys package org.alignertracker.app | grep -E 'versionCode|versionName'
```

Complete setup on the phone, open the watch app, and execute the physical-watch procedure in [external acceptance](external-acceptance.md). Uninstalling deletes app-private records; export and verify an encrypted portable backup before any uninstall of data that matters.

## Installed schema-1 upgrade

Commit `2d6f2bedd592df026e5584c5d75cc0ae6bcb9e50` is the accepted MVP baseline with Room schema 1, version code 1 and version name `0.1.0-dev`. Build it from a separate clean checkout, sign it with the same candidate certificate, and install it on the phone test device. Do not use a debug-signed baseline to test a release-signed upgrade.

Run the upgrade twice from a fresh baseline install: once with an open plan and once after explicit completion. In each baseline app, create a synthetic plan with a non-default zone, current tray above 1, a changed goal, at least four alternating IN/OUT events spanning two dates, a corrected event and enabled reminder choices. Export a schema-1 JSON backup and copy it off-device. Record its SHA-256 and the exact plan/events shown before upgrade.

Install the candidate without uninstalling:

```bash
adb -s PHONE_SERIAL install -r /tmp/aligner-phone.apk
```

After first launch, verify that setup is not shown and that the tray/count, interval, goal, zone, completion state and every event timestamp/state match the baseline. Verify the migration's documented schema-1 expansion: one initial aligner phase, one fixed schedule revision, one historical target, and one current-tray history record whose earlier tray boundaries remain unknown. Record one new IN/OUT action, one schedule change and one target change, force-stop/reopen, and verify all old and new history remains. Export a complete candidate backup, inspect its preview, restore it onto a fresh candidate install, and verify the same records. Separately import the saved schema-1 JSON through the candidate restore flow, confirm the preview before replacement, and verify its plan/events.

Any data loss, destructive fallback, signature mismatch, migration exception, invented earlier tray history or changed event time blocks release. A signature mismatch requires uninstall/reinstall and therefore is not an upgrade result; use the encrypted export/restore path only after recording that distinction.

## Distribution gates

The phone and watch now build against the Apache-2.0 microG Wear client, with no proprietary Google Play Services artifacts in the audited runtime graphs; see [dependency licenses](dependency-licenses.md). This removes the former bundled-client license blocker while preserving GPL-3.0-or-later without an added exception. The installed Wear transport service remains a separate runtime dependency: phone treatment features work without it, while paired watch communication needs a compatible service and pairing stack.

F-Droid acceptance is unconfirmed. Its [inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/) requires freely licensed dependencies and an accepted source/build process; its [anti-feature definitions](https://f-droid.org/en/docs/Anti-Features/#Non-Free-Dependencies) distinguish dependencies on separately installed non-free software. A future submission must disclose the optional phone integration and the watch runtime requirement for maintainer assessment, including any applicable anti-feature labels. The earlier definite rejection claim based on embedded proprietary AARs no longer describes this build. No F-Droid build, submission or maintainer approval has occurred.

A distribution bundle must include the signed APKs, SHA-256 manifest, signer fingerprint, exact tagged corresponding source, build instructions, GPL-3.0 text, third-party license inventory and required notices. Publication, store/developer accounts, signing and key custody all require separate explicit authorization. Passing local builds or emulator tests does not make the release ready while the physical and pilot gates in [external acceptance](external-acceptance.md) remain open.

## Historical redesign candidate 4

The redesign is prepared on `design/calm-tracking`, source tag
`design-phone-candidate-4-final` (`506e52d`). Version4 retains the permanent signer;
actual signed3→4 emulator upgrade preserves all tables and reminder datastore.
See [redesign validation](validation-redesign.md) for screenshots, tests, hashes and
candidate location. The owner subsequently authorized version4 publication; it is the served update
as reverified on2026-09-10. Owner/device gates remain open.


## Neutral redesign version 5 (published)

Source tag: `neutral-phone-candidate-5-final`
(`5e5ba6d3c5109513a2eb0a5670ada5b376186aac`). Phone and watch use versionCode 5,
above the served version 4 reverified on 2026-09-10, with the unchanged owner signer.
The tested local bundle is
`/home/matejkalc/.local/share/aligner-tracker/releases/neutral-candidate-5/`.
See [neutral validation](validation-neutral.md) for checksums, exact same-signer
4→5 data comparison, restore evidence and remaining owner/device gates.

For an owner-authorized installation, the phone APK is `aligner-tracker-phone.apk`
in that directory. `adb -s PHONE_SERIAL install -r /path/to/aligner-tracker-phone.apk`
performs an upgrade; select the intended device explicitly. No physical-phone
installation or store publication was performed. Previous publication authorization
is not authorization to publish this candidate. Do not use the superseded initial
`neutral-phone-candidate-5` tag; the final tag above includes the accessibility
regression assertion correction.

The owner subsequently requested “Deploy latest version”; version 5 was published
to the existing Tailscale-only phone store on 2026-09-10. The earlier candidate-only
statements record preparation status before that authorization. Wear remains a local
companion artifact and was not added to the phone catalogue.

## Visual and picker candidate 7 preparation (historical)

Owner-requested refinement #37 is prepared from `3ec3246`, tag `visual-time-candidate-7-final`, with versionCode7 and the existing app identity. [Validation and signed6→7 data-preservation evidence](validation-refinement.md). Local installable bundle: `/home/matejkalc/.local/share/aligner-tracker/releases/visual-time-candidate-7/`. This does not replace the published version6 or authorize installation onto the owner phone. Owner aesthetic and physical acceptance remain open.

The owner subsequently requested “Merge and deploy”; PR #38 was merged and the exact
signed phone candidate above was published on 2026-09-10. Earlier unpublished status
records the preparation checkpoint, not the current store state.
