#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR to org.json JVM implementation}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
output_dir="$(mktemp -d)"
trap 'rm -rf "$output_dir"' EXIT
mkdir -p "$output_dir/src/com/voice/studio" "$output_dir/classes"
# Only the engine callback type is substituted. No inference is performed in these file tests.
cat > "$output_dir/src/com/voice/studio/OmniEngine.java" <<'JAVA'
package com.voice.studio;
final class OmniEngine { interface Progress { void update(String message); } }
JAVA
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$JSON_JAR" -d "$output_dir/classes" \
  "$output_dir/src/com/voice/studio/OmniEngine.java" \
  "$project_dir/app/src/main/java/com/voice/studio/OmniModels.java" \
  "$project_dir/app/src/main/java/com/voice/studio/ModelParts.java" \
  "$project_dir/app/src/main/java/com/voice/studio/ModelPackageCheck.java" \
  "$project_dir/tests/ModelPackageCheckTest.java" "$project_dir/tests/ModelImportCancellationTest.java" \
  "$project_dir/tests/OmniModelsTest.java" "$project_dir/tests/ModelPartsTest.java" \
  "$project_dir/app/src/main/java/com/voice/studio/StudioFiles.java" \
  "$project_dir/app/src/main/java/com/voice/studio/SubtitleDocument.java" \
  "$project_dir/tests/SubtitleWorkflowTest.java" \
  "$project_dir/tests/host/android/content/Context.java" "$project_dir/tests/host/android/content/res/AssetManager.java" \
  "$project_dir/tests/host/android/util/AtomicFile.java" "$project_dir/tests/host/com/voice/studio/StudioService.java" \
  "$project_dir/tests/host/com/voice/studio/VoicePrompt.java" "$project_dir/tests/StudioRecoveryTest.java" "$project_dir/tests/HistoryConcurrencyTest.java" "$project_dir/tests/QueueWorkflowTest.java"
java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.ModelPartsTest
java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.OmniModelsTest
java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.ModelPackageCheckTest "$project_dir/app/src/main/assets"
java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.ModelImportCancellationTest

java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.StudioRecoveryTest

java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.HistoryConcurrencyTest
java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.SubtitleWorkflowTest

java -cp "$output_dir/classes:$JSON_JAR" com.voice.studio.QueueWorkflowTest
