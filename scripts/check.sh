#!/usr/bin/env bash
set -euo pipefail
./gradlew --no-daemon spotlessCheck testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease
