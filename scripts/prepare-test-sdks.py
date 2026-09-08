#!/usr/bin/env python3
"""Prefetch pinned Robolectric SDKs with system TLS and verified SHA-512.

Robolectric 4.16.1 / SDK 35 and 36. Keep the Maven layout expected by its resolver.
A mismatched or incomplete file is never promoted into the test cache.
"""
import argparse
import hashlib
from pathlib import Path
import urllib.request

BASE = "https://repo.maven.apache.org/maven2/org/robolectric/android-all-instrumented"
PINS = {
    "15-robolectric-13954326-i7": {
        "jar": "5fc64ffccf4788154162686b3966ddd63abea8326542c465e2a7f0129fc92770d0e1e8ddcfabd7f1500e491ec02e3b4104b9e51b61fccd9cf08006c9dc02987c",
        "pom": "e16eb42ba12b823ede906bec317f73688c58d05ebc7f6f4c39ff555c08926515aac59dbddddd888b87c85a022d756d6aa1520cc0276e2adee2caf946ad79e659",
    },
    "16-robolectric-13921718-i7": {
        "jar": "0576318b89abb0a1c9a760a61bb68ccd2d509b33164b8265d6b1fb587a4b7629e1e805b6c7f8fbefab94fc03c8db8100b1d4856a32728803c242d2677aa7ec76",
        "pom": "1638998c677480fb21c074cc66ce286123f581b6c6a9c7b82fdc13f2b1f3ae806bcadb790c8fb73c2606e63bcef71ef9e26c75e874bf94a02caf028a098333d6",
    },
}


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha512").hexdigest()


def prepare(root):
    for version, files in PINS.items():
        directory = root / "org/robolectric/android-all-instrumented" / version
        directory.mkdir(parents=True, exist_ok=True)
        for suffix, expected in files.items():
            name = f"android-all-instrumented-{version}.{suffix}"
            target = directory / name
            if not target.exists() or digest(target) != expected:
                temporary = directory / (name + ".partial")
                try:
                    with urllib.request.urlopen(f"{BASE}/{version}/{name}", timeout=120) as source, temporary.open("wb") as output:
                        while chunk := source.read(1024 * 1024):
                            output.write(chunk)
                            if output.tell() > 256 * 1024 * 1024:
                                raise ValueError("Unexpectedly large test SDK artifact")
                    if digest(temporary) != expected:
                        raise ValueError(f"SHA-512 mismatch for {name}")
                    temporary.replace(target)
                finally:
                    temporary.unlink(missing_ok=True)
            target.with_suffix(target.suffix + ".sha512").write_text(expected)
            print(f"Verified {name}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=Path, default=Path(__file__).resolve().parents[1] / ".gradle/robolectric-maven")
    prepare(parser.parse_args().cache)
