package com.voice.studio;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
import org.json.JSONObject;

/** A visible handoff keeps microphone service startup eligible on Android 14+. */
public class RecordingActivity extends Activity {
    final Handler handler = new Handler();
    TextView message;
    String job = "";
    boolean starting, resumed, permissionDenied;
    long began;

    protected void onCreate(Bundle saved) {
        super.onCreate(saved); CosmicUi.install(this);
        if (saved != null) { job = saved.getString("job", ""); starting = !job.isEmpty(); began = saved.getLong("began"); permissionDenied = saved.getBoolean("permissionDenied"); }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);box.setBackground(CosmicUi.panel(this,CosmicUi.CYAN));
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this); title.setText("VoiceStudio · 悬浮听写"); title.setTextSize(20); box.addView(title);
        message = new TextView(this); message.setText("正在打开麦克风…"); message.setTextSize(15); message.setPadding(0, pad, 0, pad); box.addView(message);
        Button close = new Button(this); close.setText("取消");close.setTextColor(CosmicUi.INK);close.setBackground(CosmicUi.button(this,false));CosmicUi.press(close); close.setOnClickListener(v -> cancelAndFinish()); box.addView(close);
        setContentView(box);
        setFinishOnTouchOutside(false);
    }

    protected void onResume() {
        super.onResume();
        resumed = true;
        if (!starting && !permissionDenied) handler.post(this::begin);
        else if (starting) handler.post(watch);
    }

    void begin() {
        if (isFinishing() || starting || !resumed || permissionDenied) return;
        try {
            if (!DictationOverlay.visible) throw new java.io.IOException("悬浮听写已关闭，请重新打开");
            if (!StudioFiles.ready(this, "whisper")) throw new java.io.IOException("请先在工作台导入识别模型");
            String retry = getIntent().getStringExtra("retryJob");
            JSONObject previous = retry == null ? null : StudioFiles.read(StudioFiles.job(this, retry));
            if (previous != null && (!previous.optString("type").equals("dictation") || StudioService.needsMicrophone(RecordingActivity.this, previous)))
                throw new java.io.IOException("原录音不可用，请重新录音");
            if (previous == null && checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 1); return;
            }
            if (previous == null) job = StudioService.enqueue(this, new JSONObject().put("type", "dictation").put("title", "悬浮听写").put("recognitionLanguage", "auto"));
            else { StudioService.resume(this, retry); job = retry; }
            DictationOverlay.handoff = job;
            starting = true;
            began = SystemClock.elapsedRealtime();
            handler.post(watch);
        } catch (Exception e) { message.setText(e.getMessage() == null ? "无法开始录音，请回到工作台重试" : e.getMessage()); }
    }

    final Runnable watch = new Runnable() {
        public void run() {
            if (isFinishing() || job.isEmpty()) return;
            if (!DictationOverlay.visible) { cancelAndFinish(); return; }
            if (job.equals(StudioService.recording)) { finish(); return; }
            try {
                JSONObject task = StudioFiles.read(StudioFiles.job(RecordingActivity.this, job));
                String state = task.optString("state");
                if (state.equals("failed") || state.equals("paused")) { message.setText(task.optString("progress")); return; }
                if (state.equals("done")) { finish(); return; }
                if (!StudioService.needsMicrophone(RecordingActivity.this, task)) { finish(); return; }
                message.setText(task.optString("progress", "正在打开麦克风…"));
            } catch (Exception e) { message.setText("无法读取录音任务，请回到工作台检查"); return; }
            if (SystemClock.elapsedRealtime() - began > 20000) {
                cancelTask(); message.setText("录音启动超时，请回到工作台检查麦克风权限并重试"); return;
            }
            handler.postDelayed(this, 150);
        }
    };

    void cancelTask() {
        if (!job.isEmpty()) try { startService(new Intent(this, StudioService.class).setAction("cancelTask").putExtra("id", job)); } catch (RuntimeException ignored) { }
    }
    void cancelAndFinish() { cancelTask(); finish(); }
    public void onBackPressed() { cancelAndFinish(); }
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) begin();
        else { permissionDenied = true; message.setText("未授权麦克风，请授权后重新开始"); }
    }
    protected void onPause() { resumed = false; handler.removeCallbacksAndMessages(null); super.onPause(); }
    protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    protected void onSaveInstanceState(Bundle state) {
        state.putString("job", job); state.putLong("began", began); state.putBoolean("permissionDenied", permissionDenied); super.onSaveInstanceState(state);
    }
}
