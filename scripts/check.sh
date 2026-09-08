#!/usr/bin/env bash
set -euo pipefail
python3 scripts/prepare-test-sdks.py
./gradlew --no-daemon spotlessCheck testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease
