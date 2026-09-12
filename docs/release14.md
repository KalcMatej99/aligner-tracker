# Version 1.1.1 (14) — store asset formats

Owner-requested scope: issue #66. Phone and Wear share versionName `1.1.1`
and versionCode `14`; app identity, runtime source, resources and schema are unchanged.

Both listing icons now use 8-bit RGBA (32-bit PNG), and all four phone screenshots
in both metadata directories use 8-bit RGB (24-bit PNG, no alpha). The existing
feature graphic already uses the required RGB format. Dimensions and decoded RGBA
pixels match the previous committed assets exactly; all six mirrored asset pairs
are byte-identical. [Format and hash evidence](google-play/evidence/image-formats-14.json).

Validation command (JDK 21, installed Android SDK):

```sh
./gradlew --offline --no-daemon --max-workers=2 spotlessCheck \
  :app:lintRelease :wear:lintRelease :app:bundleRelease \
  :app:assembleRelease :wear:assembleRelease
python3 scripts/render-privacy-policy.py --check
```

The local release directory is
`/home/matejkalc/.local/share/aligner-tracker/releases/1.1.1-14/`.
It holds signed phone/watch APKs, a signed phone AAB, corresponding source,
corrected store assets, build log and a checksum/signature manifest. Existing owner
signing identity is retained. The source commit recorded there is authoritative.

This is a packaging update. Previous code-13 runtime evidence is historical;
no new physical-device, Play-delivery or paired-Wear acceptance is claimed.
Play Console submission, private-store deployment and public F-Droid submission
are separate from preparing this release. The previously hosted code-13 source
archive and F-Droid recipe remain historical; publish the new corresponding source
and update the listing source URL before distributing code 14 through Play.
