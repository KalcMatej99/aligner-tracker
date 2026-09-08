#!/usr/bin/env python3
"""Inventory licenses for coordinates in Gradle dependency reports.

The script is offline and read-only. It resolves POMs and archives from Gradle's
local modules cache, reports missing evidence, and never guesses a license.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from xml.etree import ElementTree


COORDINATE = re.compile(
    r"^(?P<prefix>.*?)(?:\+---|\\---)\s+"
    r"(?P<group>[A-Za-z0-9_.-]+):(?P<artifact>[A-Za-z0-9_.-]+)"
    r"(?::(?P<declared>[^\s]+))?(?:\s+->\s+(?P<resolved>[^\s]+))?"
)
NOTICE_NAME = re.compile(r"(^|/)META-INF/(LICENSE|LICENSE\..*|NOTICE|NOTICE\..*)$", re.I)


@dataclass(frozen=True, order=True)
class Coordinate:
    group: str
    artifact: str
    version: str


def parse_reports(paths: list[Path]) -> tuple[set[Coordinate], set[Coordinate]]:
    coordinates: set[Coordinate] = set()
    direct: set[Coordinate] = set()
    for path in paths:
        for line in path.read_text(encoding="utf-8", errors="strict").splitlines():
            match = COORDINATE.match(line)
            if (
                not match
                or "FAILED" in line
                or "{strictly" in line
                or re.search(r"\((?:c|n)\)\s*$", line)
            ):
                continue
            raw_version = match.group("resolved") or match.group("declared")
            if not raw_version:
                continue
            version = raw_version.rstrip("(*),")
            if version in {"(n)", "(c)"} or version.startswith("{"):
                continue
            coordinate = Coordinate(match.group("group"), match.group("artifact"), version)
            coordinates.add(coordinate)
            if match.group("prefix") == "":
                direct.add(coordinate)
    return coordinates, direct


def cached_files(cache: Path, coordinate: Coordinate) -> list[Path]:
    base = cache / coordinate.group / coordinate.artifact / coordinate.version
    return sorted(path for path in base.glob("*/*") if path.is_file())


def pom_licenses(pom: Path, cache: Path, seen: set[Path] | None = None) -> list[str]:
    seen = set() if seen is None else seen
    if pom in seen:
        return []
    seen.add(pom)
    try:
        root = ElementTree.parse(pom).getroot()
    except (ElementTree.ParseError, OSError):
        return ["UNREADABLE_POM"]
    namespace = ""
    if root.tag.startswith("{"):
        namespace = root.tag.partition("}")[0] + "}"
    results: list[str] = []
    for license_node in root.findall(f"{namespace}licenses/{namespace}license"):
        name = (license_node.findtext(f"{namespace}name") or "").strip()
        url = (license_node.findtext(f"{namespace}url") or "").strip()
        value = name or "unnamed"
        if url:
            value += f" ({url})"
        results.append(value)
    if not results:
        parent = root.find(f"{namespace}parent")
        if parent is not None:
            group = (parent.findtext(f"{namespace}groupId") or "").strip()
            artifact = (parent.findtext(f"{namespace}artifactId") or "").strip()
            version = (parent.findtext(f"{namespace}version") or "").strip()
            if group and artifact and version and "${" not in version:
                parent_files = cached_files(cache, Coordinate(group, artifact, version))
                parent_pom = next((path for path in parent_files if path.suffix.lower() == ".pom"), None)
                if parent_pom:
                    results.extend(pom_licenses(parent_pom, cache, seen))
    return sorted(set(results))


def archive_notices(paths: list[Path]) -> list[str]:
    results: set[str] = set()
    for path in paths:
        if path.suffix.lower() not in {".aar", ".jar", ".zip"}:
            continue
        try:
            with zipfile.ZipFile(path) as archive:
                for name in archive.namelist():
                    if NOTICE_NAME.search(name):
                        results.add(f"{path.name}:{name}")
        except (OSError, zipfile.BadZipFile):
            results.add(f"{path.name}:UNREADABLE_ARCHIVE")
    return sorted(results)


def escape(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("reports", nargs="+", type=Path, help="Gradle dependencies report files")
    parser.add_argument(
        "--cache",
        type=Path,
        default=Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
        / "caches/modules-2/files-2.1",
        help="Gradle modules cache root",
    )
    args = parser.parse_args()
    for report in args.reports:
        if not report.is_file():
            parser.error(f"report does not exist: {report}")

    coordinates, direct = parse_reports(args.reports)
    if not coordinates:
        print("No resolved external coordinates found.", file=sys.stderr)
        return 2

    print("| Dependency | Scope | POM license declarations | Local notice evidence |")
    print("|---|---|---|---|")
    unknown = 0
    for coordinate in sorted(coordinates):
        files = cached_files(args.cache, coordinate)
        poms = [path for path in files if path.suffix.lower() == ".pom"]
        declarations = sorted({item for pom in poms for item in pom_licenses(pom, args.cache)})
        if not poms:
            declarations = ["MISSING_POM"]
        elif not declarations:
            declarations = ["NO_LICENSE_IN_POM"]
        if declarations[0] in {"MISSING_POM", "NO_LICENSE_IN_POM", "UNREADABLE_POM"}:
            unknown += 1
        notices = archive_notices(files) or ["none found"]
        name = f"{coordinate.group}:{coordinate.artifact}:{coordinate.version}"
        scope = "direct" if coordinate in direct else "transitive"
        print(
            f"| `{escape(name)}` | {scope} | {escape('; '.join(declarations))} | "
            f"{escape('; '.join(notices))} |"
        )
    print()
    print(f"Coordinates: {len(coordinates)}; unresolved POM license declarations: {unknown}.")
    return 1 if unknown else 0


if __name__ == "__main__":
    raise SystemExit(main())
