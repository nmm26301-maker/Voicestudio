#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR" -d "$out_dir" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniDecoderRun.java" "$project_dir/tests/DecoderBuffersTest.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx512m \
 -cp "$out_dir:$ORT_HOST_JAR" com.voice.studio.DecoderBuffersTest "$project_dir/validation/decoder-fixtures" "$@"
