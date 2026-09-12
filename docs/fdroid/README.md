# F-Droid submission

Refs Forgejo #63. Public upstream: https://github.com/KalcMatej99/aligner-tracker.
The `github` remote supplements the existing Forgejo `origin`; publishing there does
not expose the private Kalc Apps repository or change its signing identity.

## Phone build

Package `org.alignertracker.app`, version 1.1.0 (13), GPL-3.0-or-later.
Use JDK 21, Android platform 36, build-tools 35.0.0 and Gradle 8.13. The wrapper
includes the distribution checksum. Build from the repository root:

```
./gradlew --no-daemon --max-workers=2 :app:assembleRelease
```

No signing secret is required to build the unsigned APK. The `:wear` module is
retained in source but is not submitted as a second APK. Upstream descriptions,
original graphics, synthetic-record emulator screenshots and changelog are in
`fastlane/metadata/android/en-US/`.

## Dependency and scanner disclosure

The phone uses Apache-2.0 `org.microg.gms:play-services-wearable:0.3.14.250932`,
not Google's proprietary Maven artifact. Its API-compatible classes retain the
`com.google.android.gms` namespace. The phone works without installed Google Play
services; optional watch communication requires a compatible installed Wear
service and paired stack. See `docs/dependency-licenses.md`, `docs/wear.md` and
`docs/privacy.md` for the reviewed boundary. Do not advertise verified physical
watch support from this submission.

On 2026-09-12, fdroidserver 2.4.5's source scanner passed for public commit
`a800d54eb73841ded09654c47e8ff50e8b4e4944`. Its APK namespace scanner reported
125 matches in `com/google/android/gms/...`. These appear consistent with the
microG compatibility classes, but require packager review; the binary scanner
was **not** clean. No scanner suppression is proposed. The resolved runtime
license inventory documents the microG coordinates and their source licensing.

## Publication checks

The public-history Gitleaks scan covered 90 commits and found no secrets. The
new packaging metadata triggered one generic API-key heuristic on
`AllowedAPKSigningKeys`; this is the intentionally public SHA-256 certificate
fingerprint, not a private key. No signing material, APKs, databases or vault
sessions are tracked in Git.

## Signing and publication

Prefer F-Droid reproducible builds with the existing upstream signing identity.
Do not silently switch to a different F-Droid signing key: Android cannot update
an existing installation across different signing identities. No private key is
submitted to F-Droid. The proposed metadata uses a public upstream APK only after
local reproduction/signature checks; F-Droid must independently reproduce it.

The private Kalc Apps store and Google Play test remain separate. A submitted
request is not F-Droid acceptance or public availability. Device/TalkBack/reminder
and paired-Wear acceptance gates remain open.

## References

- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/Inclusion_Policy/
- https://f-droid.org/docs/Reproducible_Builds/

## Submission receipt — 2026-09-12

- Public repository: https://github.com/KalcMatej99/aligner-tracker
- Source tag: `v1.1.0-13`, commit `e83eb9121b3229fd6a5bc605a85e1378026e91d0`.
- Signed reference: https://github.com/KalcMatej99/aligner-tracker/releases/tag/v1.1.0-13
- Submitted packaging request: https://gitlab.com/fdroid/rfp/-/work_items/4385
- Submission account: `KalcMatej99`, authenticated using owner-approved GitHub OAuth.
- Forgejo preparation PR #64 merged; `github` and `origin` remain separate remotes.

Phone JVM tests, Spotless, release lint and release build passed. A clean public
clone also built successfully. Incremental-versus-clean comparison initially
changed DEX/profile output; both clean build lineages subsequently produced the
same tagged unsigned APK. The final source scanner and metadata lint passed.
The 125 APK namespace findings above remain disclosed, not waived.

| Artifact | SHA-256 |
| --- | --- |
| Unsigned APK from both tagged checkouts | `fb4edc85b2768af969940eea60affb953d75db3ea029bf376174174fb920beed` |
| Signed APK and signature-copied rebuild | `8739cdb53418600c42657d0b27df5d2864d4e2792d65f5c1b51060043c6e9abf` |

The reference APK was signed with the existing owner key using apksigner 34.0.0.
`apksigcopier` 1.1.1 copied that signature to the second build, and apksigner
verified it; the resulting whole-file digest matched. An unauthenticated download
from the GitHub release matched the same signed digest. These are local
reproducibility results, not an F-Droid build-server result.

The RFP requests initial maintainer review of the microG namespace findings and
recipe. No fdroiddata merge request, F-Droid approval, catalog listing, or owner
phone installation is claimed. Follow the RFP for feedback before packaging the
final maintainer-approved recipe. Google Play remains on hold at the owner's request.
