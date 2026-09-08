#!/usr/bin/env bash
# Robolectric 4.16.1 / @Config(sdk = [35]), verified against Maven Central.
# Use strict HTTPS through curl for this 200 MB runtime; JSSE intermittently
# fails its TLS integrity check inside the CI VM. Do not retry failing tests.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
version=15-robolectric-13954326-i7
name=android-all-instrumented
relative="org/robolectric/$name/$version"
destination=".gradle/robolectric-maven/$relative"
mkdir -p "$destination"
temporary=
trap 'if [[ -n "$temporary" ]]; then rm -f -- "$temporary"; fi' EXIT
for extension in pom jar; do
  case "$extension" in
    jar) checksum=5fc64ffccf4788154162686b3966ddd63abea8326542c465e2a7f0129fc92770d0e1e8ddcfabd7f1500e491ec02e3b4104b9e51b61fccd9cf08006c9dc02987c ;;
    pom) checksum=e16eb42ba12b823ede906bec317f73688c58d05ebc7f6f4c39ff555c08926515aac59dbddddd888b87c85a022d756d6aa1520cc0276e2adee2caf946ad79e659 ;;
  esac
  filename="$name-$version.$extension"
  target="$destination/$filename"
  if [[ -f "$target" ]]; then
    # Fail on a contaminated existing cache rather than silently trusting it.
    printf '%s  %s\n' "$checksum" "$target" | sha512sum --check --status
    continue
  fi
  temporary=$(mktemp "$destination/.download.XXXXXX")
  curl --fail --show-error --silent --location --proto '=https' --proto-redir '=https' \
    --retry 3 --retry-all-errors --connect-timeout 15 --max-time 300 \
    "https://repo.maven.apache.org/maven2/$relative/$filename" --output "$temporary"
  printf '%s  %s\n' "$checksum" "$temporary" | sha512sum --check --status
  mv -- "$temporary" "$target"
  temporary=
  printf 'Verified Robolectric dependency: %s\n' "$filename"
done
