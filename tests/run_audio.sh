#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR to Eclipse ECJ 3.38+ compiler jar}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -d "$out_dir" \
  "$project_dir/app/src/main/java/com/voice/studio/AudioChecks.java" \
  "$project_dir/app/src/main/java/com/voice/studio/PcmAudio.java" \
  "$project_dir/app/src/main/java/com/voice/studio/AudioTiming.java" \
  "$project_dir/app/src/main/java/com/voice/studio/RecordingHealth.java" \
  "$project_dir/tests/RecordingHealthTest.java" \
  "$project_dir/app/src/main/java/com/voice/studio/RecordedInput.java" \
  "$project_dir/tests/RecordedInputTest.java" \
  "$project_dir/app/src/main/java/com/voice/studio/StreamingResampler.java" \
  "$project_dir/tests/StreamingResamplerTest.java" \
  "$project_dir/tests/AudioChecksTest.java" \
  "$project_dir/tests/PcmFormatsTest.java" \
  "$project_dir/tests/AudioPipelineTest.java"
java -cp "$out_dir" com.voice.studio.AudioChecksTest
java -cp "$out_dir" com.voice.studio.PcmFormatsTest
java -cp "$out_dir" com.voice.studio.AudioPipelineTest
java -cp "$out_dir" com.voice.studio.RecordingHealthTest
java -cp "$out_dir" com.voice.studio.RecordedInputTest
java -cp "$out_dir" com.voice.studio.StreamingResamplerTest
