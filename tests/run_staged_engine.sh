#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
sources=()
for name in OmniEngine OmniChunkPipeline OmniTensorRun OmniStepSampler OmniTokenizer OmniTokenizerCache OmniWordPattern OmniDuration OmniSegments PcmAudio VoicePrompt; do
    sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR:$JSON_JAR" -d "$output_dir" \
    "${sources[@]}" "$project_dir/tests/LegacyOmniEngine140.java" "$project_dir/tests/StagedEngineTest.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx768m -cp "$output_dir:$ORT_HOST_JAR:$JSON_JAR" \
    com.voice.studio.StagedEngineTest "$project_dir/validation/staging-fixture"
