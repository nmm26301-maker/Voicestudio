#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
for name in AcceleratedSpeechClient GradioSpeechClient RemoteSpeechSession RemoteSpeechPipeline KaggleTask SynthesisRequests AudioChecks PcmAudio OmniDuration OmniSegments OmniPrompt VoicePrompt;do
 sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$JSON_JAR" -d "$out_dir" "${sources[@]}" "$project_dir/tests/AcceleratedSpeechTest.java" "$project_dir/tests/OmniPromptTest.java" "$project_dir/tests/GradioSpeechTest.java" "$project_dir/tests/LiveGradioSpeech.java" "$project_dir/tests/RemoteSpeechPipelineTest.java" "$project_dir/tests/LiveGradioPipeline.java" "$project_dir/tests/AudioValidationTest.java" "$project_dir/tests/DownloadRecoveryTest.java"
java -cp "$out_dir:$JSON_JAR" com.voice.studio.AudioValidationTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.DownloadRecoveryTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.AcceleratedSpeechTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.OmniPromptTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.RemoteSpeechPipelineTest

java -cp "$out_dir:$JSON_JAR" com.voice.studio.GradioSpeechTest "$out_dir/task.ipynb"
python3 - "$out_dir/task.ipynb" <<'PYTHON'
import json,ast,sys
j=json.load(open(sys.argv[1]));source="".join(j["cells"][2]["source"]);tree=ast.parse(source)
assignment=next(n for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=="task" for t in n.targets))
text=json.loads(ast.literal_eval(assignment.value.args[0]))["input"]
assert text.endswith("# 尾句") and "__import__" in text
print("Kaggle generated Python parses; user input remains literal data.")
PYTHON
if [ "${VOICE_HF_LIVE_OUTPUT:-}" != "" ];then
 java -cp "$out_dir:$JSON_JAR" com.voice.studio.LiveGradioSpeech "$VOICE_HF_LIVE_OUTPUT" ${VOICE_HF_LIVE_REFERENCE:+"$VOICE_HF_LIVE_REFERENCE"}
fi

if [ "${VOICE_HF_PIPELINE_OUTPUT:-}" != "" ];then
 java -cp "$out_dir:$JSON_JAR" com.voice.studio.LiveGradioPipeline "$VOICE_HF_PIPELINE_OUTPUT"
fi
