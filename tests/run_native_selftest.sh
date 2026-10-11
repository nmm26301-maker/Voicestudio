#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR to ONNX Runtime 1.30.0 host Java jar}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
platform_jar="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$platform_jar:$ORT_HOST_JAR" -d "$output_dir" \
    "$project_dir"/app/src/main/java/com/voice/studio/*.java "$project_dir/tests/NativeSelftestTest.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java \
    -cp "$output_dir:$ORT_HOST_JAR:$platform_jar" com.voice.studio.NativeSelftestTest \
    "$project_dir/app/src/main/assets/selftest-identity.onnx"
