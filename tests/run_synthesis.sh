#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$JSON_JAR" -d "$out_dir" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniDuration.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniSegments.java" \
 "$project_dir/app/src/main/java/com/voice/studio/VoicePrompt.java" \
 "$project_dir/app/src/main/java/com/voice/studio/SynthesisOutput.java" \
 "$project_dir/app/src/main/java/com/voice/studio/SynthesisRequests.java" \
 "$project_dir/app/src/main/java/com/voice/studio/FishClient.java" \
 "$project_dir/app/src/main/java/com/voice/studio/AudioChecks.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniWordPattern.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniTokenizer.java" \
 "$project_dir/app/src/main/java/com/voice/studio/SynthesisProgress.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniTokenizerCache.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniStepSampler.java" \
 "$project_dir/app/src/main/java/com/voice/studio/WorksCatalog.java" \
 "$project_dir/app/src/main/java/com/voice/studio/WaveformEnvelope.java" \
 "$project_dir/app/src/main/java/com/voice/studio/PcmAudio.java" \
 "$project_dir/app/src/main/java/com/voice/studio/OmniChunkPipeline.java" \
 "$project_dir/tests/ChunkPipelineTest.java" \
 "$project_dir/tests/AccelerationTest.java" \
 "$project_dir/tests/TokenizerProgressTest.java" \
 "$project_dir/tests/SynthesisFlowTest.java" "$project_dir/tests/FishClientTest.java"
java -cp "$out_dir:$JSON_JAR" com.voice.studio.SynthesisFlowTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.FishClientTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.AccelerationTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.TokenizerProgressTest "$project_dir/validation/tokenizer-pattern-1.3.9.txt" "$project_dir/validation/tokenizer-fixtures-1.3.9.json"

java -cp "$out_dir:$JSON_JAR" com.voice.studio.ChunkPipelineTest
