package com.voice.studio;

import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** A bounded, foreground-only reference recorder embedded in the voice page. */
final class VoiceCapturePanel {
    final WorkspaceActivity owner;
    final LinearLayout body;
    final EditText name, transcript;
    final TextView status, record, listen, save, exportReference;
    final Meter meter;
    volatile boolean capturing, stop;
    volatile long frames;
    File draft;

    VoiceCapturePanel(WorkspaceActivity owner, LinearLayout parent) {
        this.owner = owner;
        body = new LinearLayout(owner); body.setOrientation(1);
        body.setPadding(0, owner.dp(8), 0, owner.dp(8));
        body.setBackgroundColor(Color.TRANSPARENT); parent.addView(body);
        status = owner.label(body, "点击录音，清晰说话 3—10 秒", 14, true);
        meter = new Meter(owner); body.addView(meter, new LinearLayout.LayoutParams(-1, owner.dp(64)));
        name = owner.field(body, "人物 / 音色名称", owner.getSharedPreferences("studio", 0).getString("voice-draft-name", "我的声音"));
        transcript = owner.field(body, "参考录音对应文字（可留空）", owner.getSharedPreferences("studio", 0).getString("voice-draft-text", ""));
        transcript.setMinLines(2);
        owner.label(body, "录音保存在手机；仅在生成时上传到所选服务。", 12, false);
        record = action("● 开始录音", this::request);
        listen = action("▷ 试听录音", () -> { if (draft != null) owner.preview(draft, "人物声音试听"); });
        save = action("保存为人物音色", this::submit);
        exportReference = action("导出 WAV 到手机 · 供免费网页克隆", () -> {
            if (!capturing && draft != null && draft.isFile()) owner.saveOutput(draft);
            else owner.toast("请先完成录音");
        });
        String path = owner.getSharedPreferences("studio", 0).getString("voice-draft-file", "");
        if (path.matches("[a-f0-9-]+\\.wav")) {
            File candidate = new File(StudioFiles.dir(owner, "audio"), path);
            if (candidate.isFile()) draft = candidate;
        }
        refresh();
    }

    TextView action(String title, Runnable task) {
        owner.button(body, title, task); return (TextView) body.getChildAt(body.getChildCount() - 1);
    }
    void request() {
        if (capturing) { stop = true; record.setText("正在结束录音…"); record.setEnabled(false); return; }
        if (!owner.visible || !owner.section.equals("音色")) return;
        if (owner.checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
            owner.requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 89); return;
        }
        start();
    }
    void start() {
        if (capturing || !owner.visible || !owner.section.equals("音色")) return;
        if (StudioService.microphone || !StudioService.recording.isEmpty() || DictationOverlay.visible) {
            owner.toast("请先结束听写并关闭悬浮听写，再录制人物音色"); return;
        }
        if (owner.preview != null) { owner.preview.close(); owner.preview = null; }
        capturing = true; stop = false; frames = 0;
        final File previous = draft;
        final File target = new File(StudioFiles.dir(owner, "audio"), UUID.randomUUID() + ".wav");
        draft = target;
        persist(); status.setText("正在打开麦克风…"); refresh();
        Thread worker = new Thread(() -> capture(target, previous), "voice-reference-microphone");
        worker.start();
    }
    void capture(File target, File previous) {
        AudioRecord mic = null;
        String failure = "";
        try (PcmAudio.Writer writer = new PcmAudio.Writer(target, 16000)) {
            int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) throw new IOException("手机不支持此录音格式");
            mic = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(32000, minimum * 2));
            if (mic.getState() != AudioRecord.STATE_INITIALIZED) throw new IOException("无法打开麦克风，请检查权限");
            if (stop) return;
            mic.startRecording();
            if (mic.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IOException("麦克风被占用，请关闭其他录音应用");
            short[] samples = new short[800];
            RecordingHealth health = new RecordingHealth(16000, SystemClock.elapsedRealtime());
            long lastUi = 0;
            while (!stop && frames < 160000) {
                int count = mic.read(samples, 0, (int)Math.min(samples.length, 160000 - frames), AudioRecord.READ_NON_BLOCKING);
                if (count < 0) throw new IOException("录音中断，请重新录制");
                long now = SystemClock.elapsedRealtime();
                if (count == 0) { health.checkInput(now); Thread.sleep(20); continue; }
                health.accept(samples, count, now);
                float[] audio = new float[count]; double sum = 0;
                for (int n = 0; n < count; n++) { audio[n] = samples[n] / 32768f; sum += audio[n] * audio[n]; }
                writer.write(audio); frames += count;
                if (now - lastUi >= 80) {
                    lastUi = now;
                    final float level = (float)Math.sqrt(sum / count);
                    final double elapsed = frames / 16000.0;
                    final String hint = health.hint(now);
                    owner.ui.post(() -> {
                        if (owner.gone || !capturing) return;
                        meter.push(level); status.setText(String.format(Locale.CHINA, "● REC  %.1f / 10 秒 · %s", elapsed, hint));
                    });
                }
            }
        } catch (Exception e) { failure = e.getMessage() == null ? "录音失败，请重试" : e.getMessage(); }
        finally {
            if (mic != null) { try { mic.stop(); } catch (RuntimeException ignored) { } mic.release(); }
            if (frames == 0) {
                target.delete(); draft = previous;
                owner.getSharedPreferences("studio", 0).edit().putString("voice-draft-file", previous == null ? "" : previous.getName()).apply();
            } else if (previous != null && !previous.equals(target)) previous.delete();
            capturing = false;
            final String message = failure;
            owner.ui.post(() -> { if (!owner.gone && !capturing) { refresh(); if (!message.isEmpty()) status.setText(message + "；可检查已有录音或重录"); } });
        }
    }
    void refresh() {
        boolean usable = false;
        double seconds = 0;
        if (!capturing && draft != null && draft.isFile()) {
            try (PcmAudio.Reader reader = new PcmAudio.Reader(draft)) { seconds = reader.seconds(); usable = seconds >= 3 && seconds <= 10.01; }
            catch (Exception ignored) { }
        }
        record.setEnabled(true); record.setText(capturing ? "■ 结束录音" : "● " + (draft == null ? "开始录音" : "重新录音"));
        listen.setEnabled(!capturing && seconds > 0); listen.setAlpha(listen.isEnabled() ? 1 : .45f);
        save.setEnabled(!capturing && usable); save.setAlpha(save.isEnabled() ? 1 : .45f);
        exportReference.setEnabled(!capturing && usable); exportReference.setAlpha(exportReference.isEnabled() ? 1 : .45f);
        if (!capturing) {
            meter.active = false; meter.invalidate();
            status.setText(seconds > 0 ? String.format(Locale.CHINA, "录音已保留 · %.1f 秒%s", seconds, usable ? " · 可试听与保存" : " · 少于 3 秒，请重录") : "点击录音，清晰说话 3—10 秒");
        }
    }
    void submit() {
        if (capturing || draft == null) return;
        try {
            owner.queueRecordedVoice(draft, name.getText().toString(), transcript.getText().toString());
            persist();
        } catch (Exception e) { owner.toast(e.getMessage()); }
    }
    void persist() {
        owner.getSharedPreferences("studio", 0).edit().putString("voice-draft-name", name.getText().toString())
                .putString("voice-draft-text", transcript.getText().toString())
                .putString("voice-draft-file", draft == null ? "" : draft.getName()).apply();
    }
    void pause() { stop = true; persist(); }

    /** Actual RMS history; no simulated microphone waveform. */
    static final class Meter extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float[] levels = new float[48]; boolean active;
        Meter(android.content.Context context) { super(context); setContentDescription("录音实时音量"); }
        void push(float rms) { System.arraycopy(levels, 1, levels, 0, levels.length - 1); levels[levels.length - 1] = Math.min(1, rms * 7); active = true; invalidate(); }
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), center = h * .5f;
            paint.setColor(CosmicUi.LINE); paint.setStrokeWidth(1); canvas.drawLine(0, center, w, center, paint);
            paint.setStrokeWidth(Math.max(2, w / 95)); paint.setStrokeCap(Paint.Cap.ROUND);
            for (int n = 0; n < levels.length; n++) {
                paint.setColor(active ? CosmicUi.CYAN : CosmicUi.MUTED);
                float x = (n + .5f) * w / levels.length, height = Math.max(2, levels[n] * h * .43f);
                canvas.drawLine(x, center - height, x, center + height, paint);
            }
        }
    }
}
