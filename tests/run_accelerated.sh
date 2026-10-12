#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
for name in AcceleratedSpeechClient SynthesisRequests AudioChecks PcmAudio OmniDuration OmniSegments OmniPrompt VoicePrompt;do
 sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$JSON_JAR" -d "$out_dir" "${sources[@]}" "$project_dir/tests/AcceleratedSpeechTest.java" "$project_dir/tests/OmniPromptTest.java"
java -cp "$out_dir:$JSON_JAR" com.voice.studio.AcceleratedSpeechTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.OmniPromptTest
