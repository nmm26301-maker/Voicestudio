#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR" -d "$output_dir" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniTensorRun.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniOutputShape.java" \
 "$project_dir/tests/LegacyTensorRun155.java" "$project_dir/tests/FirstOutputTest.java"
if [[ "${1:-}" == "--benchmark" ]]; then
 fixture="$project_dir/validation/staging-fixture/omnivoice.qint8_per_channel.onnx"
else
 fixture="$project_dir/validation/output-fixtures"
fi
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx768m \
 -cp "$output_dir:$ORT_HOST_JAR" com.voice.studio.FirstOutputTest "$fixture" "$@"
