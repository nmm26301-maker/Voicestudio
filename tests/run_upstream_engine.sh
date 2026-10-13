#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
for name in OmniEngine OmniPcmWriter OmniEncodingCache OmniDecodedCache SynthesisPreview AudioChecks OmniPrompt OmniDecoderRun OmniChunkPipeline OmniTensorRun OmniOutputShape OmniStepSampler OmniSamplerWorkers OmniTokenizer OmniTokenizerCache OmniWordPattern OmniDuration OmniSegments PcmAudio VoicePrompt;do
 sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR:$JSON_JAR" -d "$out_dir" "${sources[@]}" "$project_dir/tests/UpstreamEngineTest.java" "$project_dir/tests/LegacyEngine1511.java" "$project_dir/tests/EncodingCacheTest.java" "$project_dir/tests/LocalResumeTest.java" "$project_dir/tests/DecodedCacheTest.java" "$project_dir/tests/SynthesisPreviewTest.java" "$project_dir/tests/PreviewFlowTest.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx768m -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.UpstreamEngineTest "$project_dir/validation/staging-fixture"

java -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.EncodingCacheTest
java -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.DecodedCacheTest
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx768m -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.LocalResumeTest "$project_dir/validation/staging-fixture"

java -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.SynthesisPreviewTest
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx768m -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.PreviewFlowTest "$project_dir/validation/staging-fixture"
