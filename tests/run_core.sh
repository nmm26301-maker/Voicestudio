#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR to Eclipse ECJ compiler jar}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -d "$out_dir" "$project_dir/app/src/main/java/com/voice/studio/AudioChecks.java" "$project_dir/tests/AudioChecksTest.java"
java -cp "$out_dir" com.voice.studio.AudioChecksTest
