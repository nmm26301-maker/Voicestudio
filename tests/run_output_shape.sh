#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -d "$output_dir" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniOutputShape.java" "$project_dir/tests/OutputShapeTest.java"
java -cp "$output_dir" com.voice.studio.OutputShapeTest
