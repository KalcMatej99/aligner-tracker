#!/usr/bin/env bash
# Local preparation only: never uploads an AAB, exports a key or changes a store.
set -euo pipefail
: "${JAVA_HOME:?Use the repository JDK 21}"
: "${ALIGNER_BUNDLETOOL:?Path to bundletool 1.18.3 jar}"
: "${ALIGNER_KEYSTORE:?Existing owner app keystore for local compatibility checks}"
: "${ALIGNER_KEY_PASSWORD_FILE:?Restricted password file}"
: "${ALIGNER_KEY_ALIAS:?Existing owner alias}"
: "${ALIGNER_PLAY_OUTPUT:?New local output directory outside the checkout}"
if [[ -n "$(git status --porcelain)" ]]; then
  echo 'Build from a clean committed source tree.' >&2; exit 1
fi
if [[ -e "$ALIGNER_PLAY_OUTPUT" ]]; then
  echo 'Use a new output directory; existing candidates are immutable.' >&2; exit 1
fi
case "$(realpath -m "$ALIGNER_PLAY_OUTPUT")" in
  "$(git rev-parse --show-toplevel)"/*) echo 'Output must be outside the checkout.' >&2; exit 1 ;;
esac
printf '%s  %s\n' a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29 "$ALIGNER_BUNDLETOOL" | sha256sum -c -
umask 077
mkdir -p "$ALIGNER_PLAY_OUTPUT"
python3 scripts/render-privacy-policy.py --check
./gradlew --no-daemon --max-workers=2 spotlessCheck :app:lintRelease :app:bundleRelease
"$JAVA_HOME/bin/java" -jar "$ALIGNER_BUNDLETOOL" validate --bundle=app/build/outputs/bundle/release/app-release.aab >"$ALIGNER_PLAY_OUTPUT/bundle-validation.txt"
"$JAVA_HOME/bin/jarsigner" -keystore "$ALIGNER_KEYSTORE" -storepass:file "$ALIGNER_KEY_PASSWORD_FILE" -signedjar "$ALIGNER_PLAY_OUTPUT/aligner-tracker.aab" app/build/outputs/bundle/release/app-release.aab "$ALIGNER_KEY_ALIAS"
"$JAVA_HOME/bin/jarsigner" -verify -verbose -certs "$ALIGNER_PLAY_OUTPUT/aligner-tracker.aab" >"$ALIGNER_PLAY_OUTPUT/aab-signature.txt"
"$JAVA_HOME/bin/java" -jar "$ALIGNER_BUNDLETOOL" dump config --bundle="$ALIGNER_PLAY_OUTPUT/aligner-tracker.aab" >"$ALIGNER_PLAY_OUTPUT/bundle-config.json"
"$JAVA_HOME/bin/java" -jar "$ALIGNER_BUNDLETOOL" build-apks --bundle="$ALIGNER_PLAY_OUTPUT/aligner-tracker.aab" --output="$ALIGNER_PLAY_OUTPUT/universal.apks" --mode=universal --ks="$ALIGNER_KEYSTORE" --ks-key-alias="$ALIGNER_KEY_ALIAS" --ks-pass="file:$ALIGNER_KEY_PASSWORD_FILE"
"$JAVA_HOME/bin/java" -jar "$ALIGNER_BUNDLETOOL" build-apks --bundle="$ALIGNER_PLAY_OUTPUT/aligner-tracker.aab" --output="$ALIGNER_PLAY_OUTPUT/device-splits.apks" --connected-device --device-id=emulator-5554 --ks="$ALIGNER_KEYSTORE" --ks-key-alias="$ALIGNER_KEY_ALIAS" --ks-pass="file:$ALIGNER_KEY_PASSWORD_FILE"
git rev-parse HEAD >"$ALIGNER_PLAY_OUTPUT/source-commit.txt"
git archive --format=tar.gz --prefix=aligner-tracker/ --output="$ALIGNER_PLAY_OUTPUT/source.tar.gz" HEAD
cp LICENSE docs/dependency-licenses.md docs/development.md "$ALIGNER_PLAY_OUTPUT/"
# This AAB uses the existing app key locally. Choose/approve Play App Signing and
# the eventual upload key in Console before uploading. No key transfer here.
