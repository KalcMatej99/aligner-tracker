# Development setup

Install Android Studio or official command-line Android SDK; accept SDK licenses. Install JDK21, platform36, build-tools35.0.0 and platform-tools. Set `JAVA_HOME` to JDK21 and `ANDROID_HOME` to SDK, or put your local `sdk.dir` in ignored local.properties. Do not commit machine paths. Gradle wrapper downloads pinned8.13 with SHA256 verification. Linux needs unzip and standard build tools.

```
./gradlew spotlessApply
./scripts/check.sh
./gradlew connectedDebugAndroidTest  # running API26+ emulator/device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open repository root in Android Studio, sync Gradle, run app. App ID `org.alignertracker.app`. Debug build version1.0.0-dev. No backend/keys/account are required. Offline runtime works; first build needs dependency downloads. Seed only synthetic treatment data for screenshots/tests.

Dependencies and rationale: architecture.md and android-research.md. Room schema JSON is checked into app/schemas; migrations must be tested, no destructive migration fallback. Kotlin formatting is ktfmt through Spotless. CI uses the same check script. Instrumented tests require an emulator; hosted runner support and physical acceptance are reported in validation.md.

## Release

Debug APK is for internal evaluation, not a signed production release. Release build uses R8. User-owned signing keys and passwords are supplied outside version control; release pipeline/store publication remains #21. Never generate a long-term signing identity silently. Forgejo artifact retention must avoid real user data. GPL notice and complete corresponding source accompany distribution.

## Hosted Forgejo CI

The `Android` workflow runs `scripts/check.sh` on `ubuntu-latest`. This label is
provided by the repository-scoped `kalc-server-aligner-vm` runner, using a
digest-pinned Ubuntu 24.04 container with Node 24 inside a dedicated KVM guest. The workflow installs
Temurin JDK21, Android platform36 and build-tools35.0.0. Both debug and unsigned release builds are checked. No emulator or release
signing runs in this lane.

A push to `main`, a pull request, or **Actions → Android → Run workflow** starts
validation. Check the run's commit against current `main`. Download and unzip
`android-reports` (JUnit XML, HTML unit-test and lint reports) and
`android-test-apks` (both phone/watch debug and instrumented-test APKs) and `android-unsigned-release-apks` from the run page. Artifacts are retained for 14 days; missing
artifact files fail the upload step. Checkout credentials are removed before
build steps. The workflow declares `contents: read` for portability, but Forgejo
16.0.3 does **not** enforce that declaration: non-fork job tokens retain repository
write access until the job finishes. Main-branch push/merge protection permits
only the owner, and the runner has no deployment credentials. Fork PRs require
approval for untrusted authors and receive read-only tokens; review workflow/code changes before approval.

CI uses immutable pins for checkout v7.0.1, setup-java v6.0.0, setup-android
v4.0.1 and Forgejo's upload-artifact v5. The setup-java step uses
`NODE_OPTIONS=--preserve-symlinks-main` because its ESM entrypoint guard otherwise
skips installation through Forgejo's `/var/run` symlink. The runtime preflight
requires JDK21, Node24, the exact SDK packages and absence of runner credentials
and engine sockets. The runner retains the reproduced TLS workaround
`JAVA_TOOL_OPTIONS=-XX:UseAVX=2`; removing it requires a controlled reproduction.

Runner capacity is one, each job has a 45-minute deadline and a 6 GiB memory / four
CPU limit. Fresh jobs download SDK and dependencies; a first build can take longer.
The runner has no host Docker access and no engine socket or host credentials are
mounted into jobs. Host-private service ports are blocked; DNS and host HTTPS
remain reachable for Forgejo. Infrastructure configuration, evidence and rollback:
[forgejo-runner-ci](https://forgejo.server.matejkalc.com/matejkalc/forgejo-runner-ci).

## Expanded modules and local acceptance

The existing `app` module owns all treatment records; `wear` is the optional companion. Both have the same application ID and must have matching signing certificates. `scripts/check.sh` runs formatting, both JVM suites, debug/release lint, debug/test APK assembly and minified unsigned release builds.

Run emulator instrumentation on the appropriate target only (phone tests must not run on a round watch):

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notClass=org.alignertracker.app.ui.PlatformAccessibilityTest
ANDROID_SERIAL=emulator-5556 ./gradlew :wear:connectedDebugAndroidTest
```

API36 phone and Wear images are supported for this development procedure. Paired Data Layer acceptance additionally needs the compatible phone companion app and interactive pairing; see [wear.md](wear.md). Standalone watch launch and durable storage tests do not prove phone/watch delivery. Only synthetic records belong in emulator fixtures, screenshots and performance artifacts. No personal device resets are part of setup.

### Pinned test runtime downloads

Python 3.11+ and curl are required by `scripts/check.sh`. Before Gradle tests, `prepare-test-sdks.py` fetches the exact Robolectric 4.16.1 API35/API36 instrumented SDK artifacts through verified system TLS and checks repository-pinned SHA-512 values before atomically promoting them into `.gradle/robolectric-maven`. Existing files are rehashed; corrupt or interrupted downloads cannot be accepted. This avoids lazy Java-TLS SDK fetching inside tests, which failed with AEAD tag errors on hosted job795 even with the runner's AVX workaround. Neither certificate verification nor tests are bypassed. Change the SDK pins only with a Robolectric/API update and independently verified Maven Central digests.

### Hosted memory budget

Run11/job799 completed tests, lint, debug APKs and phone release assembly, then the guest kernel recorded a cgroup OOM kill during Wear R8 (6 GiB job limit, Java RSS about 4 GiB plus retained compiler/worker processes). `scripts/check.sh` now caps Gradle workers at two, compiles Kotlin in the build process, and runs each release shrinker in a fresh single-use daemon after the test/lint/debug stage. All checks and R8 remain enabled. No runner memory limit, shared service or sandbox boundary is changed. Final hosted acceptance for the merged resource-budget fixes is recorded in Forgejo #23.

### Notification instrumentation permission

On a disposable API33+ emulator, temporarily grant `android.permission.POST_NOTIFICATIONS` to `org.alignertracker.app` before running `ReminderPlatformTest`. Record the original grant and restore it afterward; the test deliberately does not change user permission itself. The 2026-09-08 API36 run exercised actual AlarmManager delivery, then the original denied permission was restored. Do not change permissions on personal devices without their authorized acceptance scope.

### Cross-runtime TLS compatibility

Run12 also reproduced a TLS record-integrity failure in Python/OpenSSL before
Gradle started. This is broader than a demonstrated Java-only problem; the VM
root cause remains unconfirmed. SDK prefetch now uses strict-HTTPS curl with
three bounded transfer retries, size/time limits and the same SHA-512 pins.
A checksum mismatch or exhausted transfer fails the job. Neither certificate
verification nor any test is retried, disabled or ignored.

The separate infrastructure lane configured the isolated VM to mask AVX-512, VAES and VPCLMULQDQ as a controlled
compatibility mitigation while retaining AES, PCLMULQDQ and AVX2. The
`JAVA_TOOL_OPTIONS` workaround is still present. Infrastructure issue3 records
the cross-runtime probes and uncertainty; successful samples do not establish
a hardware or upstream root cause. CI includes Gradle stack traces so dependency
resolution failures retain their underlying causes.


### Platform accessibility and actual TalkBack

Run the general phone suite separately from `PlatformAccessibilityTest`: the widget test uses
UiAutomation's default service-suppression mode. On the disposable API36 `aligner-api36` Google APIs
image, TalkBack is bundled at `/product/app/talkback/talkback.apk` (observed version
16.0.0.738667889). No account, download or app dependency is needed. Record existing accessibility
settings, enable TalkBack in emulator Settings, and verify its bound service with
`adb -s emulator-5554 shell dumpsys accessibility`. Then run both platform tests using the matching
installed app and instrumentation APKs:

```sh
adb -s emulator-5554 shell am instrument -w -r \
  -e class org.alignertracker.app.ui.PlatformAccessibilityTest \
  org.alignertracker.app.test/androidx.test.runner.AndroidJUnitRunner
```

The test fails explicitly without an enabled spoken service; it never silently skips that
prerequisite. It uses `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`, temporarily requests touch
exploration and restores the original UiAutomation service flags afterward. Compose 1.9.5 excludes
UiAutomation from the enabled-service list used to deliver accessibility events, so a focus action
alone can return true without emitting an event. With real TalkBack bound, both the focus event and
focused-node identity are required, alongside real action dispatch, error and dialog cancellation.
Grouped labels are read within their native actionable/heading parent, not assumed to be flat text.

For actual TalkBack traversal, use real next/previous gestures and activation, with Developer
settings → Display speech output for observable speech content. Directly assigning focus to a node
is platform evidence, not traversal evidence. Restore the emulator's original TalkBack/notification
settings after capture. Physical Pixel ergonomics and owner comprehension remain PX-17 tomorrow;
never apply emulator setup/reset commands to a personal phone.
