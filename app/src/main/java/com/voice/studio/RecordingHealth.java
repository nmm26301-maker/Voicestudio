package com.voice.studio;

import java.io.IOException;

/** PCM progress and no-input watchdog; silence remains valid recorded audio. */
final class RecordingHealth {
    final int rate;
    final long began;
    long frames, lastInput, lastSignal;
    boolean clipped;

    RecordingHealth(int rate, long now) {
        if (rate < 1) throw new IllegalArgumentException("Invalid recording rate");
        this.rate = rate; began = now; lastInput = now; lastSignal = now;
    }
    void accept(short[] samples, int count, long now) {
        if (count < 0 || count > samples.length) throw new IllegalArgumentException("Invalid sample count");
        if (count == 0) return;
        frames += count; lastInput = now;
        int peak = 0;
        for (int n = 0; n < count; n++) peak = Math.max(peak, Math.abs((int)samples[n]));
        if (peak >= 64) lastSignal = now;
        if (peak >= 32760) clipped = true;
    }
    void checkInput(long now) throws IOException {
        if (now - lastInput >= 10000) throw new IOException("麦克风连续 10 秒没有返回音频，请检查录音权限或重新连接耳机");
    }
    long seconds() { return frames / rate; }
    boolean enough() { return frames >= rate; }
    boolean limit(long now) { return seconds() >= 3600 || now - began >= 3600000; }
    String hint(long now) {
        if (now - lastSignal >= 3000) return "输入很轻或静音，请检查麦克风";
        return clipped ? "输入偏响，请远离麦克风" : "正在接收声音";
    }
}
