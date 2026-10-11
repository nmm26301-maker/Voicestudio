#!/usr/bin/env python3
"""Verify the signed standalone release can upgrade the shipped preview identity.

This validates package metadata and artifacts, not a device installation.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("release")
parser.add_argument("previous", help="Previously shipped 1.5.1 preview APK")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
tools = Path(os.environ["ANDROID_SDK_ROOT"]) / "build-tools/35.0.0"
previous_checks = json.loads((root / "validation/preview-checks-1.5.1.json").read_text())
checks = 0


def check(condition, message):
    global checks
    if not condition:
        raise RuntimeError(message)
    checks += 1


def run(tool, *arguments):
    return subprocess.check_output([str(tools / tool), *map(str, arguments)], text=True)


def inspect(apk):
    badging = run("aapt", "dump", "badging", apk)
    signature = run("apksigner", "verify", "--verbose", "--print-certs", apk)
    manifest = run("aapt", "dump", "xmltree", apk, "AndroidManifest.xml")
    return {
        "package": re.search(r"package: name='([^']+)'", badging)[1],
        "versionCode": int(re.search(r"versionCode='(\d+)'", badging)[1]),
        "version": re.search(r"versionName='([^']+)'", badging)[1],
        "label": re.search(r"application-label:'([^']+)'", badging)[1],
        "certificate": re.search(r"Signer #1 certificate SHA-256 digest: ([a-f0-9]+)", signature)[1],
        "signature": signature,
        "manifest": manifest,
        "authorities": set(re.findall(r'android:authorities[^=]*="([^"]+)"', manifest)),
        "classes": set(re.findall(r'android:name[^=]*="(com\.voice\.studio\.[^"]+)"', manifest)),
        "sha256": hashlib.sha256(Path(apk).read_bytes()).hexdigest(),
    }


old = inspect(args.previous)
new = inspect(args.release)
check(old["sha256"] == previous_checks["previewApkSha256"], "Previous APK differs from the shipped preview")
check(old["certificate"] == previous_checks["previewCertificateSha256"], "Previous certificate mismatch")
check(new["package"] == old["package"] == "com.voice.studio.preview", "Package identity changed")
check(new["certificate"] == old["certificate"], "Signing certificate changed; upgrade would fail")
check(new["versionCode"] > old["versionCode"], "Version code must increase")
check(new["versionCode"] == 18 and new["version"] == "1.5.2-hybrid", "Incorrect release version")
check(new["label"] == "VoiceStudio", "Release still has the preview launcher name")
check(new["authorities"] == old["authorities"] == {"com.voice.studio.preview.audio"}, "Sharing identity changed")
check(new["classes"] == old["classes"], "Application/component identity changed")
check(not re.search(r"android:debuggable[^=]*=.*0xffffffff", new["manifest"]), "Release is debuggable")
check("Number of signers: 1" in new["signature"], "Unexpected signer count")
for scheme in ("v2", "v3"):
    check(re.search(r"Verified using " + scheme + r" scheme .*: true", new["signature"]), "Missing " + scheme)
run("zipalign", "-c", "-P", "16", "4", args.release)
checks += 1
with zipfile.ZipFile(args.release) as built, zipfile.ZipFile(args.previous) as previous:
    check(built.testzip() is None, "APK ZIP corruption")
    dex = b"".join(built.read(n) for n in built.namelist() if re.fullmatch(r"classes\d*\.dex", n))
    for name in new["classes"]:
        check(("L" + name.replace(".", "/") + ";").encode() in dex, "Component absent: " + name)
    check(b"VoiceStudio 1.5.2" in dex, "About page version is stale")
    check(b"content://com.voice.studio.audio/" not in dex, "Sharing points to the legacy app")
    for prefix in ("lib/", "assets/"):
        names = {n for n in built.namelist() if n.startswith(prefix)}
        check(names == {n for n in previous.namelist() if n.startswith(prefix)}, "Asset/runtime set changed")
        for name in names:
            check(built.read(name) == previous.read(name), "Unexpected asset/runtime change: " + name)
    check(built.getinfo("resources.arsc").compress_type == zipfile.ZIP_STORED, "Compressed resources")
    check(not any(n.endswith((".p12", ".jks", ".keystore")) for n in built.namelist()), "Signing key bundled in APK")
print(json.dumps({
    "status": "passed", "checks": checks,
    "package": new["package"], "label": new["label"],
    "version": new["version"], "versionCode": new["versionCode"],
    "certificateSha256": new["certificate"], "apkSha256": new["sha256"],
    "previousApkSha256": old["sha256"], "previousVersionCode": old["versionCode"],
    "scope": "Signed APK identity/version/provider continuity and unchanged assets/runtime verified. No device installation, data-retention or functional acceptance performed."
}, indent=2))
