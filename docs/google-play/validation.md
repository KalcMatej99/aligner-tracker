# Closed-test candidate validation — 2026-09-11

Candidate application source: `3e4c196f3cdb58dd645b8b1291c77accc929f25c`.
Later commits update public hosting, source distribution and validation documents;
they do not alter the application packaged in this candidate.
Local bundle directory:
`/home/matejkalc/.local/share/aligner-tracker/releases/play-1.1.0-13/`.

## Code and package checks

- Local formatting, phone debug/release lint, debug APK/instrumented APK, release
  AAB and Wear release builds passed. JVM results: **67 phone + 9 Wear**, no
  failures/errors/skips ([counts](evidence/jvm-tests.json)).
- Bundletool 1.18.3 was downloaded from Google's GitHub release; its SHA256 matched
  the release API's digest. `validate` passed, and bundle configuration declares
  `PAGE_ALIGNMENT_16K` ([configuration](evidence/bundle-config.json)).
- The AAB was signed locally with the existing owner key, checked with jarsigner,
  and converted to a universal APK and a device-specific split set. The universal
  APK's signer matches the existing release identity; versionName is 1.1.0 and
  versionCode is 13; the merged manifest has no INTERNET permission.
- [Artifact checksums and source identity](evidence/manifest.json). Corresponding
  source includes app/Wear/shared sources, resources, tests, Gradle wrapper/build
  scripts, build instructions, license and notices. Unrelated historical
  screenshots are omitted from the smaller public archive.
- `scripts/render-privacy-policy.py --check` confirms both public Markdown/HTML
  are generated from the exact English strings displayed offline in the app.

## Emulator evidence

- Privacy/health test passed in normal size, 320dp/200% text/dark mode, and a
  320dp/200% text/dark Compose RTL configuration. It reads to the final policy
  heading, checks the fixed close control, opens the health notice and dismisses
  with system Back. Native settings route to the policy was also opened manually
  in the signed bundle-derived installation.
- Six tests passed on an API35 **16384-byte-page x86_64 emulator**: explicit native
  library loading, information-dialog access, two persistence/restore tests,
  rotated-photo/archive lifecycle and real platform reminder/snooze/stale-action
  behavior ([test output](evidence/runtime16kb.txt)).
- The native `.so` bytes in the instrumented debug app match those in the signed
  universal release for x86_64. Fresh signed-release startup, IN → OUT, process
  stop/restart retaining OUT, and OUT → IN were exercised on the 16 KB emulator
  ([runtime record](evidence/runtime16kb.json), [signed capture](evidence/screenshots/signed-16kb.png)).
- Static ELF LOAD checks passed for both 64-bit ABIs, and `zipalign -c -P 16 4`
  passed. [Raw native results](evidence/native-alignment.json) retain the RELRO
  end remainders: graphics-path's remainder is nonzero, so the static checker does
  **not** assert that every newer documented RELRO check passed. Direct library
  loading passed on the x86_64 16 KB runtime; arm64 hardware validation remains
  open. No unrelated dependency upgrade or custom native rebuild was introduced.
- Actual signed **code 12 → bundle-generated code 13 splits** preserved every
  database table, including one treatment, one appointment, one note and 13 wear
  events. Preference datastores were absent on both sides; this is not populated
  reminder-preference upgrade evidence ([comparison](evidence/upgrade.json)).
- Google Play has not delivered these APKs. The local split-install result must
  not be reported as Play delivery or Play App Signing enrollment acceptance.

## Visual review and corrections

The information viewer retains native button/Back semantics, headings, a single
scrollable reading region and a fixed close control. Normal, dark and large-text
captures were inspected. The first web-policy review found long headings could
cause horizontal overflow at 320px/200% browser text; wrapping was fixed and the
focused browser recheck passed. The initial SVG export tool omitted the app mark;
the PNGs were regenerated with CairoSVG and visually checked against the original
SVG sources. These are operator rendering tools, not app dependencies.

The first instrumentation compilation used an unavailable Espresso helper;
system Back now uses the existing Android instrumentation API. A device-property
RTL attempt did not visibly mirror the screen; final evidence uses an explicit
Compose RTL configuration. The initial screenshot navigation helper matched the
wrong Back text; final listing captures use exact tab labels. Incomplete captures
and failed attempts are not reported as passing checks.

Listing screenshots are **1080 × 2160 actual emulator captures** with synthetic
records. The initial shorter viewport obscured the day bar, so the captures use
a taller 2:1 viewport rather than editing the app UI or compositing screen images.
[Today](assets/phone/01-today.png), [History](assets/phone/02-history.png),
[Schedule](assets/phone/03-schedule.png), [Progress](assets/phone/04-progress.png).

## Public hosting

The Forgejo hostname resolves publicly to a Tailscale IP; the initial repository
URL was therefore unsuitable as a public policy. The standalone policy and source
archive were published on the owner's existing public website via
[portfolio PR #45](https://github.com/KalcMatej99/portfolio/pull/45), after CI and
Vercel preview passed. Only the static policy/source files were added. The policy
has no JavaScript, analytics, form or login. Existing private-store restrictions
were not changed.

[Public HTTPS/hash evidence](evidence/public-hosting.json) and
[Chromium mobile/large-dark checks](evidence/public-browser.json). Downloaded policy
HTML equals the generated app-policy text representation; downloaded source archive
matches the local candidate. The independent web-search fetcher rejected the URL
as unsafe without fetching it; live HTTPS and Chromium verification are the actual
access evidence, not a claim that Google's Play reviewer has fetched it.

## Explicit remaining gates

No Play account exists. Account registration/identity/payment/device verification,
owner choice of age groups and countries, final Console declarations, existing-key
transfer/Play App Signing enrollment, upload and Play-generated delivery checks,
12 real testers/14 consecutive days and the production-access application remain
unperformed. Physical reminder reliability, human TalkBack speech, clinical or
human comprehension, arm64 16 KB hardware and paired Wear acceptance are not
established by this work. Existing physical acceptance issues remain open.

No Play upload, private-store update or owner-phone installation was performed.
