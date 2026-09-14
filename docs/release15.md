# Version 1.1.2 (15) — clearer day timelines

Owner approved the OUT-block preview, requested removal of inline text, then
explicitly requested merge and deployment. PR #69 implements continuous outlined
OUT periods, the Now marker and explicit legend labels on Today and History.
Phone and Wear version metadata is 1.1.2 (15); app identity and storage schema are
unchanged. [Design, screenshots and focused validation](out-timeline.md).

Initial distribution scope was the existing Tailscale-only Kalc Apps phone repository
at https://apps.server.matejkalc.com. The installed phone app can update with the
existing owner signing identity. Older APK/source/notices downloads are retained.
Wear remains a local companion artifact. No public F-Droid update or physical-phone
installation is included.

Google Play distribution was added afterwards at the owner's request. On 2026-09-14
the approved phone AAB was uploaded to the **internal testing track** with fastlane
`supply`, using the `aligner-play-upload@bridgebill.iam.gserviceaccount.com` service
account. The track holds versionCode 15 with status `completed` and en-US release
notes from `fastlane/metadata/android/en-US/changelogs/15.txt`. Store listing text,
icon, feature graphic and the four phone screenshots were synced and verified live
by SHA-256 against `fastlane/metadata`. Production, beta and alpha tracks are empty.
The `supply` configuration is committed under `fastlane/`.

Committing store-content changes required account-level admin on the service
account; the app-level "Release apps to testing tracks" grant covers the bundle,
track release and changelog but not listing text or images. Narrowing that account
back to the minimum ("Release apps to testing tracks" plus "Manage store presence")
is outstanding.

The Play upload is not a distribution acceptance. No tester list, Play-delivered
split-APK installation or physical-device session is recorded here. Play App
Signing enrollment against the existing owner signing identity is unconfirmed, so
no claim is made that Kalc Apps installations can update from Play; verify this
before sharing the opt-in link with anyone who already has the app. As noted for
code 14, the listing source URL still points at the `1.1.0-13` archive and must be
republished for this code before distributing code 15 through Play.

Release files and operational evidence are stored outside Git in
`/home/matejkalc/.local/share/aligner-tracker/releases/1.1.2-15/`:
signed phone/Wear APKs, signed phone AAB, corresponding source, notices, checksums,
signature checks, signed-emulator upgrade comparison, store publication and live
HTTPS verification. The source commit recorded there and tag `v1.1.2-15` identify
the built release. Issue #68 records the final deployment receipt.
