#!/usr/bin/env bash
set -euo pipefail
: "${ECJ_JAR:?Set ECJ_JAR}"
: "${JSON_JAR:?Set JSON_JAR}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR}"
: "${1:?Pass the directory containing actual OmniVoice models}"
: "${2:?Pass a new empty output directory}"
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$(mktemp -d)"
trap 'rm -rf "$out_dir"' EXIT
sources=()
for name in OmniEngine OmniPcmWriter OmniEncodingCache OmniDecodedCache SynthesisPreview AudioChecks OmniPrompt OmniDecoderRun OmniChunkPipeline OmniTensorRun OmniOutputShape OmniStepSampler OmniSamplerWorkers OmniTokenizer OmniTokenizerCache OmniWordPattern OmniDuration OmniSegments PcmAudio VoicePrompt;do
 sources+=("$project_dir/app/src/main/java/com/voice/studio/$name.java")
done
mkdir -p "$2"
if [[ -n "$(ls -A "$2")" ]];then echo 'Use a new empty output directory to preserve earlier results.' >&2;exit 1;fi
java -jar "$ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -cp "$ORT_HOST_JAR:$JSON_JAR" -d "$out_dir" "${sources[@]}" "$project_dir/tests/LegacyOmniEngine157.java" "$project_dir/tests/RealSpeechAcceptance.java"
ORT_DISABLE_TELEMETRY=1 python3 "$project_dir/tests/offline_run.py" java -Xmx1536m -cp "$out_dir:$ORT_HOST_JAR:$JSON_JAR" com.voice.studio.RealSpeechAcceptance "$1" "$2"
