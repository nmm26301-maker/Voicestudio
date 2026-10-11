#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -d "$output_dir" \
    "$project_dir/app/src/main/java/com/voice/studio/OmniStepSampler.java" \
    "$project_dir/tests/LegacySampler153.java" "$project_dir/tests/SamplerSpeedTest.java"
java -Xmx384m -cp "$output_dir" com.voice.studio.SamplerSpeedTest "$@"
