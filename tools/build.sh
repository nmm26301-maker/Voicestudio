#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to an Android SDK with platform 35 and build-tools 35.0.0}"
: "${ECJ_JAR:?Set ECJ_JAR to Eclipse ECJ compiler jar (3.38+)}"
android_tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
platform_jar="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
ort_aar="$project_dir/app/libs/onnxruntime-android-1.30.0.aar"
if [ ! -f "$ort_aar" ]; then python3 "$project_dir/tools/fetch_runtime.py"; fi
# Public backups deliberately exclude the original upgrade signing key.
key_path="$project_dir/signing/voicestudio-development.p12"
if [ "${VOICE_UNSIGNED:-0}" != "1" ] && [ ! -f "$key_path" ]; then
    echo "Missing original signing key: $key_path" >&2
    echo "Restore the original key for an upgrade, or set VOICE_UNSIGNED=1 for compile/package validation only." >&2
    exit 1
fi
artifact_dir="$project_dir/build-manual"
mkdir -p "$artifact_dir"
out_dir="$(mktemp -d "$artifact_dir/compile.XXXXXX")"
aapt_flags=()
apk_name="VoiceStudio-1.5.1-Hybrid.apk"
if [ "${VOICE_DEBUG:-0}" = "1" ]; then
    aapt_flags+=(--debug-mode)
    apk_name="VoiceStudio-1.5.1-Hybrid-debug.apk"
fi
unzip -q "$ort_aar" classes.jar "jni/arm64-v8a/*" -d "$out_dir/ort"
mkdir -p "$out_dir/classes" "$out_dir/dex" "$out_dir/res" "$out_dir/generated"
"$android_tools/aapt2" compile --dir "$project_dir/app/src/main/res" -o "$out_dir/res"
python3 - "$project_dir/app/src/main/AndroidManifest.xml" "$out_dir/AndroidManifest.xml" <<'PYXML'
import sys,xml.etree.ElementTree as ET
ET.register_namespace('android','http://schemas.android.com/apk/res/android')
tree=ET.parse(sys.argv[1]);key='{http://schemas.android.com/tools}node'
for parent in tree.iter():
    for child in list(parent):
        if child.get(key)=='remove':parent.remove(child)
tree.write(sys.argv[2],encoding='utf-8',xml_declaration=True)
PYXML
"$android_tools/aapt2" link -o "$out_dir/base.apk" -I "$platform_jar" --manifest "$out_dir/AndroidManifest.xml" --java "$out_dir/generated" -A "$project_dir/app/src/main/assets" --min-sdk-version 26 --target-sdk-version 34 "${aapt_flags[@]}" "$out_dir"/res/*.flat
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
    "$android_tools/apksigner" sign --ks "$key_path" --ks-pass pass:android --key-pass pass:android --ks-key-alias voicestudio --out "$artifact_dir/$apk_name" "$out_dir/aligned.apk"
    "$android_tools/apksigner" verify --verbose "$artifact_dir/$apk_name"
    "$android_tools/zipalign" -c -P 16 4 "$artifact_dir/$apk_name"
fi
