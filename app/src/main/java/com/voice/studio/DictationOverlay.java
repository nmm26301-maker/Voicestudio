package com.voice.studio;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.json.*;

/** Visible user-controlled overlay. Recording and inference stay in StudioService. */
public class DictationOverlay extends Service {
    WindowManager windows;
    LinearLayout bubble, panel;
    WindowManager.LayoutParams params;
    TextView action;
    String job = "";
    static volatile String handoff = "";
    static volatile boolean visible;
    final Handler handler = new Handler();

    public IBinder onBind(Intent i) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int id) {
        if (bubble != null) return START_NOT_STICKY;
        if (!android.provider.Settings.canDrawOverlays(this)
                || checkSelfPermission("android.permission.RECORD_AUDIO")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(
                new NotificationChannel(
                        "dictation-overlay", "悬浮听写", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open =
                PendingIntent.getActivity(
                        this,
                        0,
                        new Intent(this, WorkspaceActivity.class),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note =
                new Notification.Builder(this, "dictation-overlay")
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .setContentTitle("VoiceStudio 悬浮听写")
                        .setContentText("轻点悬浮按钮开始或结束录音")
                        .setOngoing(true)
                        .setContentIntent(open)
                        .build();
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(48, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(48, note);
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = new LinearLayout(this);
        bubble.setOrientation(0);
        bubble.setPadding(dp(8), dp(4), dp(8), dp(4));
        bubble.setBackground(CosmicUi.panel(this,CosmicUi.PURPLE));
        TextView grip = button(bubble, "⋮", () -> {});
        grip.setOnTouchListener(
                new View.OnTouchListener() {
                    float x, y;
                    int px, py;

                    public boolean onTouch(View v, android.view.MotionEvent e) {
                        if (e.getAction() == MotionEvent.ACTION_DOWN) {
                            x = e.getRawX();
                            y = e.getRawY();
                            px = params.x;
                            py = params.y;
                            return true;
                        }
                        if (e.getAction() == MotionEvent.ACTION_MOVE) {
                            params.x =
                                    Math.max(
                                            0,
                                            Math.min(
                                                    getResources().getDisplayMetrics().widthPixels
                                                            - bubble.getWidth(),
                                                    px + (int) (e.getRawX() - x)));
                            params.y =
                                    Math.max(
                                            0,
                                            Math.min(
                                                    getResources().getDisplayMetrics().heightPixels
                                                            - bubble.getHeight(),
                                                    py + (int) (e.getRawY() - y)));
                            try {
                                windows.updateViewLayout(bubble, params);
                            } catch (Exception ignored) {
                            }
                            return true;
                        }
                        return true;
                    }
                });
        action = button(bubble, "录音", this::click);
        button(bubble, "×", () -> stopSelf());
        params =
                new WindowManager.LayoutParams(
                        -2,
                        -2,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = dp(20);
        params.y = dp(200);
        try {
            windows.addView(bubble, params);
            visible = true;
            handler.post(poll);
        } catch (Exception e) {
            toast("无法显示悬浮按钮");
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    final Runnable poll =
            new Runnable() {
                public void run() {
                    if (bubble == null) return;
                    try {
                        if (!handoff.isEmpty()) job = handoff;
                        if (!job.isEmpty()) {
                            JSONObject state =
                                    StudioFiles.read(StudioFiles.job(DictationOverlay.this, job));
                            String status = state.optString("state");
                            action.setText(
                                    status.equals("done")
                                            ? "结果"
                                            : status.equals("failed") || status.equals("paused")
                                                    ? StudioService.needsMicrophone(DictationOverlay.this, state) ? "重录" : "重试"
                                                    : job.equals(StudioService.recording)
                                                            ? "结束"
                                                            : "处理中");
                        }
                    } catch (Exception ignored) {
                    }
                    handler.postDelayed(this, 700);
                }
            };

    void click() {
        if (!job.isEmpty() && job.equals(StudioService.recording)) {
            startService(new Intent(this, StudioService.class).setAction("stopMic"));
            action.setText("处理中");
            return;
        }
        try {
            if (!job.isEmpty()) {
                JSONObject last = StudioFiles.read(StudioFiles.job(this, job));
                if (last.optString("state").equals("done")) {
                    result(StudioFiles.text(StudioFiles.output(this, job, "txt")));
                    return;
                }
                if (last.optString("state").equals("queued")
                        || last.optString("state").equals("running")) {
                    toast("请等待处理完成");
                    return;
                }
                if (!StudioService.needsMicrophone(DictationOverlay.this, last)) {
                    startActivity(new Intent(this, RecordingActivity.class).putExtra("retryJob", job)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
                    action.setText("准备识别"); return;
                }
            }
            record();
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    void record() throws Exception {
        if (!StudioFiles.ready(this, "whisper")) throw new java.io.IOException("请在工作台导入识别模型");
        if (panel != null) {
            windows.removeView(panel);
            panel = null;
        }
        startActivity(new Intent(this, RecordingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        action.setText("准备录音");
    }

    void result(String text) {
        if (panel != null) return;
        panel = new LinearLayout(this);
        panel.setOrientation(1);
        panel.setPadding(dp(16), dp(16), dp(16), dp(12));
        panel.setBackground(CosmicUi.panel(this,CosmicUi.CYAN));
        ScrollView scroll = new ScrollView(this);
        TextView content = new TextView(this);
        content.setText(text);
        content.setTextColor(-1);
        content.setTextSize(16);
        content.setTextIsSelectable(true);
        scroll.addView(content);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, dp(180)));
        button(
                panel,
                "复制文字",
                () -> {
                    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                            .setPrimaryClip(ClipData.newPlainText("VoiceStudio", text));
                    toast("已复制，可粘贴到输入框");
                });
        button(
                panel,
                "重新录音",
                () -> {
                    try {
                        record();
                    } catch (Exception e) {
                        toast(e.getMessage());
                    }
                });
        button(
                panel,
                "收起",
                () -> {
                    if (panel != null) {
                        windows.removeView(panel);
                        panel = null;
                    }
                });
        WindowManager.LayoutParams layout =
                new WindowManager.LayoutParams(
                        dp(300),
                        -2,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
        layout.gravity = Gravity.CENTER;
        try {
            windows.addView(panel, layout);
        } catch (Exception e) {
            panel = null;
            toast("无法显示听写结果");
        }
    }

    TextView button(LinearLayout parent, String text, Runnable task) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(-1);
        view.setTypeface(null, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setBackground(CosmicUi.button(this,false));CosmicUi.press(view);parent.addView(view);
        view.setOnClickListener(v -> task.run());
        return view;
    }

    GradientDrawable background(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(20));
        return drawable;
    }

    int dp(int x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }

    void toast(String text) {
        Toast.makeText(this, text == null ? "操作失败" : text, Toast.LENGTH_LONG).show();
    }

    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (!job.isEmpty() && job.equals(StudioService.recording))
            startService(new Intent(this, StudioService.class).setAction("stopMic"));
        if (windows != null) {
            if (panel != null)
                try {
                    windows.removeView(panel);
                } catch (Exception ignored) {
                }
            if (bubble != null)
                try {
                    windows.removeView(bubble);
                } catch (Exception ignored) {
                }
        }
        bubble = null;
        panel = null;
        handoff = "";
        visible = false;
        stopForeground(true);
        super.onDestroy();
    }
}
