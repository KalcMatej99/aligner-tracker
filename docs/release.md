# Release preparation and artifact verification

Status: preparation only. The current configuration is version code 2 and version name `1.0.0-dev`; it is not a public release candidate. No user-owned release key has been created or used, and no release has been published. Debug APKs use the Android test certificate; hosted CI uploads test APKs, unsigned release intermediates and reports. Exact final-run verification is recorded in Forgejo #23.

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
