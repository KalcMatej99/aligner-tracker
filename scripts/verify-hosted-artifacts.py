#!/usr/bin/env python3
"""Verify a successful current-main Forgejo run and its phone/watch artifacts.

Uses existing FORGEJO_API_TOKEN environment; never prints or persists credentials.
Requires Android build-tools on ANDROID_HOME and a Java runtime for apksigner.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

API = "https://forgejo.server.matejkalc.com/api/v1/repos/matejkalc/aligner-tracker"


class SafeRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, *args):
        result = super().redirect_request(request, *args)
        if result and urllib.parse.urlsplit(result.full_url).netloc != urllib.parse.urlsplit(request.full_url).netloc:
            result.remove_header("Authorization")
        return result


def fetch(path):
    request = urllib.request.Request(API + path, headers={"Authorization": "token " + os.environ["FORGEJO_API_TOKEN"]})
    with urllib.request.build_opener(SafeRedirect).open(request, timeout=120) as response:
        return response.read()


def main(run_id, output):
    run = json.loads(fetch(f"/actions/runs/{run_id}"))
    branch = json.loads(fetch("/branches/main"))
    sha = run.get("head_sha") or run.get("commit_sha") or run.get("head_commit", {}).get("id")
    if run["status"] != "success" or sha != branch["commit"]["id"]:
        raise ValueError("Acceptance requires successful current-main run")
    artifacts = json.loads(fetch(f"/actions/runs/{run_id}/artifacts"))
    by_name = {item["name"]: item for item in (artifacts["artifacts"] if isinstance(artifacts, dict) else artifacts)}
    output.mkdir(parents=True, exist_ok=True)
    summary = {"run_id": run_id, "run_url": run["html_url"], "commit": sha, "artifacts": [], "tests": {}, "lint": {}, "apks": []}
    for name in ("android-reports", "android-test-apks", "android-unsigned-release-apks"):
        item = by_name[name]
        data = fetch(f'/actions/artifacts/{item["id"]}/zip')
        (output / (name + ".zip")).write_bytes(data)
        summary["artifacts"].append({"name": name, "id": item["id"], "url": API + f'/actions/artifacts/{item["id"]}/zip', "sha256": hashlib.sha256(data).hexdigest()})
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            if archive.testzip() is not None:
                raise ValueError("Artifact CRC mismatch")
            for member in archive.infolist():
                path = PurePosixPath(member.filename)
                if path.is_absolute() or ".." in path.parts or (member.external_attr >> 16) & 0o170000 == 0o120000:
                    raise ValueError("Unsafe artifact entry")
            archive.extractall(output / name)
    reports = output / "android-reports"
    for module in ("app", "wear"):
        root = reports / module / "build"
        suites = [ET.parse(p).getroot() for p in (root / "test-results/testDebugUnitTest").glob("TEST-*.xml")]
        totals = {key: sum(int(s.get(key, "0")) for s in suites) for key in ("tests", "failures", "errors", "skipped")}
        if not totals["tests"] or any(totals[key] for key in ("failures", "errors", "skipped")):
            raise ValueError(f"Invalid {module} JUnit acceptance: {totals}")
        summary["tests"][module] = totals
        for variant in ("debug", "release"):
            lint = ET.parse(root / f"reports/lint-results-{variant}.xml").getroot()
            counts = {key: sum(issue.get("severity") == key for issue in lint) for key in ("Error", "Warning")}
            if counts["Error"]:
                raise ValueError(f"{module} {variant} lint errors")
            summary["lint"][f"{module}-{variant}"] = counts
        for relative in ("reports/tests/testDebugUnitTest/index.html", "reports/lint-results-debug.html", "reports/lint-results-release.html"):
            if "<html" not in (root / relative).read_text().lower():
                raise ValueError("Missing readable HTML report")
    build_tools = Path(os.environ["ANDROID_HOME"]) / "build-tools/35.0.0"
    apks = list((output / "android-test-apks").rglob("*.apk")) + list((output / "android-unsigned-release-apks").rglob("*.apk"))
    if len(apks) != 6:
        raise ValueError(f"Expected six phone/watch test and release APKs, got {len(apks)}")
    for apk in apks:
        with zipfile.ZipFile(apk) as archive:
            if archive.testzip() is not None or "AndroidManifest.xml" not in archive.namelist() or "classes.dex" not in archive.namelist():
                raise ValueError("Invalid APK structure")
        permissions = subprocess.check_output([build_tools / "aapt", "dump", "permissions", apk], text=True)
        if "androidTest" not in apk.parts and "android.permission.INTERNET" in permissions:
            raise ValueError("Unexpected Internet permission")
        package = subprocess.check_output([build_tools / "aapt", "dump", "badging", apk], text=True).splitlines()[0]
        signed = "unsigned" not in apk.name
        if signed:
            subprocess.run([build_tools / "apksigner", "verify", apk], check=True, capture_output=True)
        summary["apks"].append({"path": str(apk.relative_to(output)), "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(), "bytes": apk.stat().st_size, "signed_test_apk": signed, "package": package})
    (output / "verification.json").write_text(json.dumps(summary, indent=2) + "\n")
    (output / "SHA256SUMS").write_text("".join(f'{item["sha256"]}  {item["path"]}\n' for item in summary["apks"]))
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("run_id", type=int)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    main(args.run_id, args.output)
