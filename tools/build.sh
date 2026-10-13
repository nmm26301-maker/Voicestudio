#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [ "${VOICE_PREVIEW:-0}" = "1" ] && [ "${VOICE_STANDALONE:-0}" = "1" ]; then
    echo "Choose VOICE_PREVIEW or VOICE_STANDALONE, not both." >&2
    exit 1
fi
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to an Android SDK with platform 35 and build-tools 35.0.0}"
: "${ECJ_JAR:?Set ECJ_JAR to Eclipse ECJ compiler jar (3.38+)}"
android_tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
platform_jar="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
ort_aar="$project_dir/app/libs/onnxruntime-android-1.30.0.aar"
if [ ! -f "$ort_aar" ]; then python3 "$project_dir/tools/fetch_runtime.py"; fi
# Public backups deliberately exclude the original upgrade signing key.
key_path="$project_dir/signing/voicestudio-development.p12"
key_alias="voicestudio"
if [ "${VOICE_PREVIEW:-0}" = "1" ] || [ "${VOICE_STANDALONE:-0}" = "1" ]; then
    # A separate package and key, never a replacement key for the installed app.
    key_path="$project_dir/signing/voicestudio-preview.p12"
    key_alias="voicestudio-preview"
fi
if [ "${VOICE_UNSIGNED:-0}" != "1" ] && [ ! -f "$key_path" ]; then
    echo "Missing signing key: $key_path" >&2
    if [ "${VOICE_STANDALONE:-0}" = "1" ]; then
        echo "Restore the shipped preview signing backup to preserve upgrades and data." >&2
    elif [ "${VOICE_PREVIEW:-0}" = "1" ]; then
        echo "Restore the preview key or follow README.md to create a separate preview signing identity." >&2
    else
        echo "Restore the original key for an upgrade, or set VOICE_UNSIGNED=1 for compile/package validation only." >&2
    fi
    exit 1
fi
if [ "${VOICE_PREVIEW:-0}" != "1" ] && [ "${VOICE_UNSIGNED:-0}" != "1" ]; then
    certificate_sha="$(keytool -exportcert -keystore "$key_path" -storepass android -alias "$key_alias" | sha256sum | cut -d ' ' -f 1)"
    expected_certificate="7e63e9ad6f5910ceb64e62e54009a200e50dcedc9709e396e6a393b38f0f5a33"
    if [ "${VOICE_STANDALONE:-0}" = "1" ]; then
        expected_certificate="8d6cde5c3a0876b56a0191dbefd28160fc79604d1ebf7dff664cae736ff5f860"
    fi
    if [ "$certificate_sha" != "$expected_certificate" ]; then
        echo "Signing certificate differs from the previous APK; refusing an incompatible upgrade." >&2
        exit 1
    fi
fi
artifact_dir="$project_dir/build-manual"
mkdir -p "$artifact_dir"
out_dir="$(mktemp -d "$artifact_dir/compile.XXXXXX")"
aapt_flags=()
apk_name="VoiceStudio-1.5.11-Hybrid.apk"
if [ "${VOICE_DEBUG:-0}" = "1" ]; then
    aapt_flags+=(--debug-mode)
    apk_name="VoiceStudio-1.5.11-Hybrid-debug.apk"
fi
manifest_flags=()
if [ "${VOICE_PREVIEW:-0}" = "1" ]; then
    manifest_flags+=(--preview)
    apk_name="${apk_name%.apk}-Preview.apk"
fi
if [ "${VOICE_STANDALONE:-0}" = "1" ]; then
    manifest_flags+=(--standalone)
    apk_name="${apk_name%.apk}-Standalone.apk"
fi
python3 - "$ort_aar" "$out_dir/ort" <<'PY'
import hashlib, pathlib, sys, zipfile
root = pathlib.Path(sys.argv[2])
expected = {
    'jni/arm64-v8a/libonnxruntime.so': 'df5d25c72a868dca773597c71e2000756d43fe4d70ade516d3693c54e12e0ada',
    'jni/arm64-v8a/libonnxruntime4j_jni.so': '0695257178815f8c58c1719ba07d6d5d91a2b339634328cca2a0a98aa7ff2952',
}
with zipfile.ZipFile(sys.argv[1]) as archive:
    for name in ['classes.jar', *expected]:
        data = archive.read(name)
        if name in expected and hashlib.sha256(data).hexdigest() != expected[name]:
            raise RuntimeError('Pinned ONNX native library mismatch: ' + name)
        path = root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        if path.read_bytes() != data:
            raise RuntimeError('Incomplete runtime extraction: ' + name)
PY
mkdir -p "$out_dir/classes" "$out_dir/dex" "$out_dir/res" "$out_dir/generated"
"$android_tools/aapt2" compile --dir "$project_dir/app/src/main/res" -o "$out_dir/res"
python3 "$project_dir/tools/prepare_manifest.py" "$project_dir/app/src/main/AndroidManifest.xml" "$out_dir/AndroidManifest.xml" "${manifest_flags[@]}"
"$android_tools/aapt2" link -o "$out_dir/base.apk" -I "$platform_jar" --manifest "$out_dir/AndroidManifest.xml" --custom-package com.voice.studio --java "$out_dir/generated" -A "$project_dir/app/src/main/assets" --min-sdk-version 26 --target-sdk-version 34 "${aapt_flags[@]}" "$out_dir"/res/*.flat
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -classpath "$platform_jar:$out_dir/ort/classes.jar" -d "$out_dir/classes" "$project_dir"/app/src/main/java/com/voice/studio/*.java "$out_dir"/generated/com/voice/studio/R.java
python3 - "$out_dir/classes" "$out_dir/classes.jar" <<'PY'
import sys,zipfile,pathlib
root=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(sys.argv[2],'w') as z:
    for f in root.rglob('*.class'):z.write(f,f.relative_to(root))
PY
"$android_tools/d8" --lib "$platform_jar" --min-api 26 --output "$out_dir/dex" "$out_dir/classes.jar" "$out_dir/ort/classes.jar"
python3 - "$out_dir/base.apk" "$out_dir/unsigned.apk" "$out_dir/dex" "$out_dir/ort/jni" <<'PY'
import sys,zipfile,pathlib
with zipfile.ZipFile(sys.argv[1]) as src,zipfile.ZipFile(sys.argv[2],'w') as dst:
    for entry in src.infolist():
        dst.writestr(entry.filename,src.read(entry.filename),compress_type=zipfile.ZIP_STORED if entry.filename=='resources.arsc' else zipfile.ZIP_DEFLATED)
    for f in pathlib.Path(sys.argv[3]).glob('*.dex'): dst.write(f,f.name)
    for f in pathlib.Path(sys.argv[4]).rglob('*.so'): dst.write(f,'lib/'+str(f.relative_to(sys.argv[4])),compress_type=zipfile.ZIP_STORED)
PY
"$android_tools/zipalign" -f -P 16 4 "$out_dir/unsigned.apk" "$out_dir/aligned.apk"
if [ "${VOICE_UNSIGNED:-0}" = "1" ]; then
    unsigned_name="${apk_name%.apk}-unsigned.apk"
    cp "$out_dir/aligned.apk" "$artifact_dir/$unsigned_name"
    "$android_tools/zipalign" -c -P 16 4 "$artifact_dir/$unsigned_name"
    echo "Unsigned validation build: $artifact_dir/$unsigned_name (not an installable upgrade)"
else
    # Sign on the local temporary filesystem, then publish verified bytes atomically.
    # This avoids partial/mapped output on mounted workspaces after Java exits.
    signed_dir="$(mktemp -d)"
    trap 'rm -rf "$signed_dir"' EXIT
    "$android_tools/apksigner" sign --ks "$key_path" --ks-pass pass:android --key-pass pass:android --ks-key-alias "$key_alias" --out "$signed_dir/$apk_name" "$out_dir/aligned.apk"
    "$android_tools/apksigner" verify --verbose "$signed_dir/$apk_name"
    "$android_tools/zipalign" -c -P 16 4 "$signed_dir/$apk_name"
    python3 - "$signed_dir/$apk_name" "$artifact_dir/$apk_name" <<'PYTHON'
import io, os, pathlib, sys, zipfile
payload = pathlib.Path(sys.argv[1]).read_bytes()
with zipfile.ZipFile(io.BytesIO(payload)) as archive:
    if archive.testzip() is not None:
        raise RuntimeError("Signed APK ZIP verification failed")
target = pathlib.Path(sys.argv[2])
pending = target.with_suffix(".pending")
try:
    with pending.open("wb") as stream:
        stream.write(payload)
        stream.flush()
        os.fsync(stream.fileno())
    if pending.read_bytes() != payload:
        raise RuntimeError("Incomplete signed APK publication")
    pending.replace(target)
finally:
    pending.unlink(missing_ok=True)
PYTHON
    "$android_tools/apksigner" verify --verbose "$artifact_dir/$apk_name"
    "$android_tools/zipalign" -c -P 16 4 "$artifact_dir/$apk_name"
fi
