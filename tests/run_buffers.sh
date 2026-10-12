#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR" -d "$output_dir" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniTensorRun.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniOutputShape.java" "$project_dir/tests/OmniTensorRunTest.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -cp "$output_dir:$ORT_HOST_JAR" com.voice.studio.OmniTensorRunTest "$project_dir/validation/buffer-diagnostic.onnx"
