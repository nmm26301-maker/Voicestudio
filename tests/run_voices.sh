#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$JSON_JAR" -d "$out_dir" \
  "$project_dir/app/src/main/java/com/voice/studio/PcmAudio.java" \
  "$project_dir/app/src/main/java/com/voice/studio/SpeakerClips.java" \
  "$project_dir/tests/SpeakerClipsTest.java"
java -cp "$out_dir:$JSON_JAR" com.voice.studio.SpeakerClipsTest
