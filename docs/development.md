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
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
ANDROID_SERIAL=emulator-5556 ./gradlew :wear:connectedDebugAndroidTest
```

API36 phone and Wear images are supported for this development procedure. Paired Data Layer acceptance additionally needs the compatible phone companion app and interactive pairing; see [wear.md](wear.md). Standalone watch launch and durable storage tests do not prove phone/watch delivery. Only synthetic records belong in emulator fixtures, screenshots and performance artifacts. No personal device resets are part of setup.

### Pinned test runtime downloads

Python 3.11+ and curl are required by `scripts/check.sh`. Before Gradle tests, `prepare-test-sdks.py` fetches the exact Robolectric 4.16.1 API35/API36 instrumented SDK artifacts through verified system TLS and checks repository-pinned SHA-512 values before atomically promoting them into `.gradle/robolectric-maven`. Existing files are rehashed; corrupt or interrupted downloads cannot be accepted. This avoids lazy Java-TLS SDK fetching inside tests, which failed with AEAD tag errors on hosted job795 even with the runner's AVX workaround. Neither certificate verification nor tests are bypassed. Change the SDK pins only with a Robolectric/API update and independently verified Maven Central digests.

### CI memory budget

The expanded phone/watch release build exceeded the job's 6 GiB cgroup limit
in hosted run11: the kernel killed Gradle while a separate Kotlin compiler
daemon retained about1.9 GiB. CI now passes `--max-workers=2` and
`-Pkotlin.compiler.execution.strategy=in-process` through `scripts/check.sh`.
The compiler shares Gradle's existing3 GiB heap instead of keeping a second
large heap alive through R8. All formatting, tests, lint and build tasks remain
required; job/VM memory limits are unchanged. Local invocations may omit these
CI-specific resource arguments. References: [Kotlin execution strategy](https://kotlinlang.org/docs/compiler-execution-strategy.html),
[Gradle worker limit](https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_performance).

Run12 also reproduced a TLS record-integrity failure in Python/OpenSSL before
Gradle started. This is broader than a demonstrated Java-only problem; the VM
root cause remains unconfirmed. SDK prefetch now uses strict-HTTPS curl with
three bounded transfer retries, size/time limits and the same SHA-512 pins.
A checksum mismatch or exhausted transfer fails the job. Neither certificate
verification nor any test is retried, disabled or ignored.
