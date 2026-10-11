#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"; : "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"; trap 'rm -rf "$out_dir"' EXIT
platform_jar="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
java -jar "$ECJ_JAR" -source 8 -target 8 -cp "$platform_jar" -d "$out_dir" "$project_dir"/app/src/main/java/com/voice/studio/{FastOutSlowInInterpolator,LookupTableInterpolator}.java "$project_dir/tests/CosmicMotionTest.java"
java -cp "$out_dir:$platform_jar" com.voice.studio.CosmicMotionTest
