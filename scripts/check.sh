#!/usr/bin/env bash
set -euo pipefail
python3 scripts/prepare-test-sdks.py
# Keep the same checks within the hosted 6 GiB job limit. Each release shrinker
# gets a fresh daemon instead of retaining both compilers and prior R8 state.
gradle_options=(--no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process)
./gradlew "${gradle_options[@]}" "$@" spotlessCheck testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest
./gradlew "${gradle_options[@]}" "$@" :app:assembleRelease
./gradlew "${gradle_options[@]}" "$@" :wear:assembleRelease
