package com.voice.studio;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.widget.*;
import java.io.File;
import java.util.Locale;

/** Foreground-only local preview, owned and closed by the calling activity. */
final class AudioPreview implements AutoCloseable {
    final Activity activity;
    final Handler handler = new Handler(Looper.getMainLooper());
    final AudioManager audio;
    final TextView time;
    final SeekBar seek;
    final Button toggle, speed;
    final AlertDialog dialog;
    MediaPlayer player;
    AudioFocusRequest focus;
    boolean ready, dragging, completed, closed;
    boolean receiverRegistered;
    int duration, rateIndex = 1;
    final float[] rates = {.75f, 1f, 1.25f, 1.5f, 2f};
    final BroadcastReceiver noisy = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!ready || closed) return;
            try { player.pause(); } catch (RuntimeException ignored) { }
            abandon(); update();
        }
    };

    AudioPreview(Activity activity, File file, String title) {
        this.activity = activity;
        audio = (AudioManager)activity.getSystemService(Activity.AUDIO_SERVICE);
        LinearLayout body = new LinearLayout(activity); body.setOrientation(1);
        int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        time = new TextView(activity); time.setText("正在加载音频…"); body.addView(time);
        seek = new SeekBar(activity); seek.setEnabled(false); body.addView(seek);
        LinearLayout controls = new LinearLayout(activity); body.addView(controls);
        Button back = button(controls, "−10秒"); toggle = button(controls, "播放");
        Button next = button(controls, "+10秒");
        speed = new Button(activity); speed.setText("试听速度 · 1×"); body.addView(speed);
        toggle.setEnabled(false); back.setEnabled(false); next.setEnabled(false); speed.setEnabled(false);
        dialog = new CosmicDialogBuilder(activity).setTitle(title).setView(body)
                .setNegativeButton("关闭", null).create();
        dialog.setOnDismissListener(d -> close());
        toggle.setOnClickListener(v -> {
            if (!ready) return;
            try {
                if (player.isPlaying()) { player.pause(); abandon(); }
                else { if (completed) { player.seekTo(0); completed = false; } start(); }
                update();
            } catch (RuntimeException e) { fail("播放失败，请重新打开音频"); }
        });
        back.setOnClickListener(v -> jump(-10000)); next.setOnClickListener(v -> jump(10000));
        speed.setOnClickListener(v -> {
            if (!ready) return;
            int requested = (rateIndex + 1) % rates.length;
            try {
                if (player.isPlaying()) player.setPlaybackParams(new PlaybackParams().setSpeed(rates[requested]).setPitch(1f));
                rateIndex = requested; speed.setText("试听速度 · " + rates[requested] + "×");
            } catch (RuntimeException e) {
                Toast.makeText(activity, "该设备无法使用此试听速度", Toast.LENGTH_SHORT).show();
            }
        });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (user) time.setText(clock(value) + " / " + clock(duration));
            }
            public void onStartTrackingTouch(SeekBar bar) { dragging = true; }
            public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                if (ready) try { player.seekTo(bar.getProgress()); completed = false; }
                catch (RuntimeException e) { fail("跳转失败，请重新打开音频"); }
            }
        });
        dialog.show();
        try {
            IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(noisy, filter, Context.RECEIVER_NOT_EXPORTED);
            else activity.registerReceiver(noisy, filter);
            receiverRegistered = true;
            if (!file.isFile() || file.length() < 32) throw new java.io.IOException();
            MediaPlayer pending = new MediaPlayer(); player = pending;
            pending.setAudioAttributes(attributes()); pending.setDataSource(file.getAbsolutePath());
            pending.setOnPreparedListener(mp -> {
                if (closed || player != mp) return;
                handler.removeCallbacks(timeout);
                try {
                    duration = mp.getDuration(); seek.setMax(Math.max(1, duration)); ready = true;
                    seek.setEnabled(true); toggle.setEnabled(true); back.setEnabled(true); next.setEnabled(true); speed.setEnabled(true);
                    start(); handler.post(tick);
                } catch (RuntimeException e) { fail("音频无法播放，请重新生成或导出后检查"); }
            });
            pending.setOnCompletionListener(mp -> {
                if (closed || player != mp) return;
                completed = true; abandon(); update();
            });
            pending.setOnErrorListener((mp, what, extra) -> {
                if (!closed && player == mp) fail("音频无法播放，请重新生成或导出后检查");
                return true;
            });
            pending.prepareAsync(); handler.postDelayed(timeout, 15000);
        } catch (Exception e) { fail("音频不存在或无法加载"); }
    }

    Button button(LinearLayout parent, String text) {
        Button button = new Button(activity); button.setText(text);
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1)); return button;
    }
    AudioAttributes attributes() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
    }
    void start() {
        abandon();
        final AudioFocusRequest[] requested = new AudioFocusRequest[1];
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes())
                .setOnAudioFocusChangeListener(change -> {
                    if (change < 0 && focus == requested[0] && ready && !closed) {
                        try { player.pause(); } catch (RuntimeException ignored) { }
                        abandon(); update();
                    }
                }, handler).build();
        requested[0] = focus;
        if (audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            try { player.setPlaybackParams(new PlaybackParams().setSpeed(rates[rateIndex]).setPitch(1f)); }
            catch (RuntimeException e) {
                rateIndex = 1; speed.setText("试听速度 · 1×");
                Toast.makeText(activity, "该设备无法使用此试听速度，使用正常速度", Toast.LENGTH_SHORT).show();
                player.setPlaybackParams(new PlaybackParams().setSpeed(1f).setPitch(1f));
            }
            player.start();
        }
        else { abandon(); Toast.makeText(activity, "其他应用正在使用声音，请稍后点击播放", Toast.LENGTH_SHORT).show(); }
    }
    void abandon() {
        if (focus != null) { audio.abandonAudioFocusRequest(focus); focus = null; }
    }
    void jump(int delta) {
        if (!ready) return;
        try { player.seekTo(Math.max(0, Math.min(duration, player.getCurrentPosition() + delta))); completed = false; }
        catch (RuntimeException e) { fail("跳转失败，请重新打开音频"); }
    }
    void update() {
        if (!ready || closed) return;
        try {
            int position = completed ? duration : player.getCurrentPosition();
            if (!dragging) { seek.setProgress(position); time.setText(clock(position) + " / " + clock(duration)); }
            toggle.setText(player.isPlaying() ? "暂停" : completed ? "重新播放" : "播放");
        } catch (RuntimeException e) { fail("播放已中断，请重新打开音频"); }
    }
    static String clock(int millis) {
        int seconds = Math.max(0, millis / 1000);
        return seconds >= 3600 ? String.format(Locale.US, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                : String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }
    final Runnable tick = new Runnable() {
        public void run() { update(); if (!closed && ready) handler.postDelayed(this, 250); }
    };
    final Runnable timeout = () -> fail("音频加载超时，请重新打开或导出后检查");
    void fail(String message) {
        handler.removeCallbacksAndMessages(null); ready = false; abandon();
        if (player != null) { player.release(); player = null; }
        toggle.setEnabled(false); seek.setEnabled(false); speed.setEnabled(false); time.setText(message);
    }
    public void close() {
        if (closed) return;
        closed = true; ready = false; handler.removeCallbacksAndMessages(null); abandon();
        if (receiverRegistered) {
            try { activity.unregisterReceiver(noisy); } catch (RuntimeException ignored) { }
            receiverRegistered = false;
        }
        if (player != null) { player.release(); player = null; }
        if (dialog.isShowing()) dialog.dismiss();
    }
}
