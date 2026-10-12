#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR to Eclipse ECJ 3.38+}"
: "${JSON_JAR:?Set JSON_JAR to the org.json JVM implementation}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to Android SDK 35}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
for suite in synthesis sampler parallel_sampler models audio motion voices; do
    bash "$project_dir/tests/run_${suite}.sh"
done
python3 "$project_dir/tests/check_cosmic_ui.py"
python3 "$project_dir/tests/check_inline_generation.py"
echo 'Host regression passed. Device UI, microphone, real model inference and Fish account calls require separate acceptance.'
