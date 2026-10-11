#!/usr/bin/env python3
"""Inspect built APKs, rather than inferring preview isolation from source."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("preview")
parser.add_argument("original", help="Unmodified user-installed 1.5.0 APK")
args = parser.parse_args()
tools = Path(os.environ["ANDROID_SDK_ROOT"]) / "build-tools/35.0.0"
root = Path(__file__).resolve().parents[1]
provenance = json.loads((root / "PROVENANCE.json").read_text())
checks = 0


def check(condition, message):
    global checks
    if not condition:
        raise RuntimeError(message)
    checks += 1


def run(tool, *arguments):
    return subprocess.check_output([str(tools / tool), *map(str, arguments)], text=True)


def manifest(apk):
    nodes = []
    current = None
    for line in run("aapt", "dump", "xmltree", apk, "AndroidManifest.xml").splitlines():
        element = re.match(r"\s*E: (\S+)", line)
        if element:
            current = {"tag": element[1]}
            nodes.append(current)
        attribute = re.match(r'\s*A: ([^=(]+)(?:\([^)]*\))?="([^"]*)"', line)
        if attribute and current is not None:
            current[attribute[1]] = attribute[2]
    return nodes


original = Path(args.original)
preview = Path(args.preview)
original_sha = hashlib.sha256(original.read_bytes()).hexdigest()
check(original_sha == provenance["sha256"], "Original APK is not the recorded 1.5.0")
original_signature = run("apksigner", "verify", "--verbose", "--print-certs", original)
preview_signature = run("apksigner", "verify", "--verbose", "--print-certs", preview)
digest_pattern = r"Signer #1 certificate SHA-256 digest: ([a-f0-9]+)"
original_cert = re.search(digest_pattern, original_signature)[1]
preview_cert = re.search(digest_pattern, preview_signature)[1]
check(original_cert == provenance["signingCertificateSha256"], "Original certificate mismatch")
check(original_cert != preview_cert, "Preview unexpectedly uses the original signing identity")
for scheme in ("v2", "v3"):
    check(re.search(r"Verified using " + scheme + r" scheme .*: true", preview_signature), "Missing " + scheme)
run("zipalign", "-c", "-P", "16", "4", preview)
checks += 1
old_nodes = manifest(original)
nodes = manifest(preview)
check(old_nodes[0]["package"] == "com.voice.studio", "Original package mismatch")
check(nodes[0]["package"] == "com.voice.studio.preview", "Preview would replace the original package")
check(nodes[0]["android:versionName"] == "1.5.1-hybrid-preview", "Unexpected preview version")
badging = run("aapt", "dump", "badging", preview)
check("versionCode='17'" in badging, "Unexpected version code")
check("application-label:'VoiceStudio 测试版'" in badging, "Preview lacks distinct launcher label")
check("launchable-activity: name='com.voice.studio.MainActivity'" in badging, "Launcher class rewritten")
old_authorities = {n["android:authorities"] for n in old_nodes if "android:authorities" in n}
authorities = {n["android:authorities"] for n in nodes if "android:authorities" in n}
check(authorities == {"com.voice.studio.preview.audio"}, "Incorrect preview provider authority")
check(authorities.isdisjoint(old_authorities), "Provider authority collides with the old app")
with zipfile.ZipFile(preview) as built, zipfile.ZipFile(original) as old:
    check(built.testzip() is None, "APK ZIP member corruption")
    dex = b"".join(built.read(name) for name in built.namelist() if re.fullmatch(r"classes\d*\.dex", name))
    for node in nodes:
        if node["tag"] in ("application", "activity", "service", "provider", "receiver"):
            name = node.get("android:name", "")
            check(name.startswith("com.voice.studio."), "Unresolved component: " + name)
            check(("L" + name.replace(".", "/") + ";").encode() in dex, "Component absent from DEX: " + name)
    libraries = {name for name in built.namelist() if name.startswith("lib/")}
    check(libraries == {name for name in old.namelist() if name.startswith("lib/")}, "Native library set changed")
    for name in libraries:
        check(built.read(name) == old.read(name), "Native runtime changed: " + name)
    check("resources.arsc" in built.namelist(), "Resource table missing")
    check(built.getinfo("resources.arsc").compress_type == zipfile.ZIP_STORED, "Resource table compressed")
    check(b"content://com.voice.studio.audio/" not in dex, "Sharing still targets the original provider")
    check(not any(name.endswith((".p12", ".jks", ".keystore")) for name in built.namelist()), "Signing key bundled in APK")
print(json.dumps({
    "status": "passed", "checks": checks,
    "originalApkSha256": original_sha, "originalCertificateSha256": original_cert,
    "previewPackage": nodes[0]["package"],
    "previewApkSha256": hashlib.sha256(preview.read_bytes()).hexdigest(),
    "previewCertificateSha256": preview_cert,
    "scope": "Signed APK, package/provider isolation, class resolution, unchanged native runtime, ZIP alignment. No device installation or inference performed."
}, indent=2))
