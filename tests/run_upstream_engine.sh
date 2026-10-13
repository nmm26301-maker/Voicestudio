#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
for name in OmniEngine OmniPcmWriter OmniPrompt OmniDecoderRun OmniChunkPipeline OmniTensorRun OmniOutputShape OmniStepSampler OmniSamplerWorkers OmniTokenizer OmniTokenizerCache OmniWordPattern OmniDuration OmniSegments PcmAudio VoicePrompt;do
 sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR:$JSON_JAR" -d "$out_dir" "${sources[@]}" "$project_dir/tests/UpstreamEngineTest.java" "$project_dir/tests/LegacyEngine1511.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx768m -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.UpstreamEngineTest "$project_dir/validation/staging-fixture"
