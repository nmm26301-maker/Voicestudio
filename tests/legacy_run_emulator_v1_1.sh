# Historical 1.1 UI test only; not a 1.3 device validation.
#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
: "${ANDROID_SDK_ROOT:?}"
export ANDROID_AVD_HOME="$project_dir/../tooling/avd"
export VOICE_ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
export VOICE_APK="$project_dir/build-manual/VoiceStudio-1.1.0-Offline.apk"
export VOICE_TEST_OUTPUT="$project_dir/../tooling/device-tests"
"$ANDROID_SDK_ROOT/emulator/emulator" -avd voicestudio -data "$ANDROID_AVD_HOME/voicestudio.avd/userdata-test2.img" -show-kernel -no-window -no-audio -no-snapshot -no-boot-anim -no-metrics -accel off -gpu swiftshader_indirect -memory 1536 -port 5580 > "$project_dir/../tooling/emulator.log" 2>&1 &
emulator_pid=$!
trap 'kill "$emulator_pid" 2>/dev/null || true' EXIT
"$VOICE_ADB" start-server
for attempt in $(seq 1 80); do
    if [ "$(timeout 4 "$VOICE_ADB" -s emulator-5580 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then
        echo 'Emulator booted; running device checks.'
        python3 "$project_dir/tests/legacy_device_smoke_v1_1.py"
        exit
    fi
    if ! kill -0 "$emulator_pid" 2>/dev/null; then tail -10 "$project_dir/../tooling/emulator.log";exit 1;fi
    if [ $((attempt % 15)) = 0 ]; then echo 'Emulator is still booting.'; fi
    sleep 2
done
echo 'Emulator boot timed out.'
exit 1
