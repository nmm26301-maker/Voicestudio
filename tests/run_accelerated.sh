#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
for name in JobProgressGate SynthesisPreview AcceleratedSpeechClient GradioSpeechClient CloudReferenceClient CloudHealthClient CloudEngineCatalog CloudConnectionPolicy OfficialSpeechClient FishInlineMessage FishClient RemoteSpeechSession RemoteSpeechPipeline KaggleTask SynthesisRequests AudioChecks PcmAudio OmniDuration OmniSegments OmniPrompt VoicePrompt;do
 sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$JSON_JAR" -d "$out_dir" "${sources[@]}" "$project_dir/tests/OfficialSpeechTest.java" "$project_dir/tests/CloudHealthTest.java" "$project_dir/tests/AcceleratedSpeechTest.java" "$project_dir/tests/OmniPromptTest.java" "$project_dir/tests/GradioSpeechTest.java" "$project_dir/tests/LiveGradioSpeech.java" "$project_dir/tests/RemoteSpeechPipelineTest.java" "$project_dir/tests/LiveGradioPipeline.java" "$project_dir/tests/AudioValidationTest.java" "$project_dir/tests/DownloadRecoveryTest.java" "$project_dir/tests/ProgressPersistenceTest.java"
java -cp "$out_dir:$JSON_JAR" com.voice.studio.OfficialSpeechTest "$out_dir/fish-reference.msgpack"
python3 - "$out_dir/fish-reference.msgpack" <<'MSG'
import sys, struct
# Independent minimal reader, no third-party MessagePack dependency in host CI.
data=open(sys.argv[1],'rb').read();at=0
def read():
 global at
 t=data[at];at+=1
 if t>=0xe0:return t-256
 if t<=0x7f:return t
 if 0xa0<=t<=0xbf:n=t&31
 elif t in (0xd9,0xda,0xdb):
  w={0xd9:1,0xda:2,0xdb:4}[t];n=int.from_bytes(data[at:at+w],'big');at+=w
 elif 0x80<=t<=0x8f:return {read():read() for _ in range(t&15)}
 elif 0x90<=t<=0x9f:return [read() for _ in range(t&15)]
 elif t==0xca:
  v=struct.unpack('>f',data[at:at+4])[0];at+=4;return v
 elif t==0xc6:
  n=int.from_bytes(data[at:at+4],'big');at+=4;v=data[at:at+n];at+=n;return v
 else:raise AssertionError(hex(t))
 v=data[at:at+n].decode('utf-8');at+=n;return v
body=read();assert at==len(data)
assert body['text'].startswith('新文字🙂。')
assert body['references'][0]['text']=='录音原文。'
assert body['references'][0]['audio'][:4]==b'RIFF'
assert len(body['references'][0]['audio'])==240044
assert body['prosody']['speed']==1 and body['format']=='mp3'
print('Fish MessagePack independently decoded: full UTF-8 text + exact WAV reference.')
MSG
java -cp "$out_dir:$JSON_JAR" com.voice.studio.CloudHealthTest
java -cp "$out_dir:$JSON_JAR" com.voice.studio.ProgressPersistenceTest
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
