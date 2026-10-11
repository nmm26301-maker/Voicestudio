package com.voice.studio;

import java.io.IOException;
import java.util.Arrays;

/** Waveform similarity overlap-add for modest duration corrections without pitch shifting. */
final class AudioTiming {
    static float[] readCompleteClip(PcmAudio.Reader audio) throws IOException {
        // A 60-second subtitle can accept up to 2.5x its duration. Never silently
        // discard the tail of a longer generated sentence before fitting it.
        if (audio.seconds() > 150) throw new IOException("生成的配音片段超过 150 秒，请拆分字幕或缩短文字");
        float[] result = new float[(int)Math.round(audio.seconds() * 24000)];
        StreamingResampler resampler = new StreamingResampler(audio.rate, 24000);
        int written = 0;
        for (long frame = 0; frame < audio.frames; ) {
            int count = (int)Math.min(audio.frames - frame, audio.rate * 5L);
            float[] chunk = audio.read((double)frame / audio.rate, (double)count / audio.rate);
            float[] converted = resampler.push(chunk);
            System.arraycopy(converted, 0, result, written, converted.length);
            written += converted.length; frame += count;
        }
        float[] tail = resampler.finish();
        System.arraycopy(tail, 0, result, written, tail.length);
        return result;
    }

    static float[] fit(float[] input, int frames) throws IOException {
        if (input.length < 1024 || frames < 1024) throw new IOException("配音片段太短");
        double ratio = (double) input.length / frames;
        if (ratio < .4 || ratio > 2.5) throw new IOException("配音长度与字幕时长差距过大，请缩短文字或调整字幕时间");
        if (input.length == frames) return Arrays.copyOf(input, input.length);
        final int window = 1024, hop = 256, search = 128;
        float[] out = new float[frames + window], weight = new float[out.length];
        for (int pos = 0; pos < frames; pos += hop) {
            int
                    expected =
                            Math.min(
                                    input.length - window,
                                    Math.max(0, (int) Math.round(pos * ratio))),
                    chosen = expected;
            if (pos > 0) {
                double best = -Double.MAX_VALUE;
                for (int candidate = Math.max(0, expected - search);
                        candidate <= Math.min(input.length - window, expected + search);
                        candidate += 4) {
                    double dot = 0, a = 0, b = 0;
                    for (int j = 0; j < window - hop; j += 4) {
                        float
                                previous =
                                        weight[pos + j] > .0001f
                                                ? out[pos + j] / weight[pos + j]
                                                : 0,
                                current = input[candidate + j];
                        dot += previous * current;
                        a += previous * previous;
                        b += current * current;
                    }
                    double score =
                            dot / Math.sqrt(a * b + 1e-20) - Math.abs(candidate - expected) * 1e-7;
                    if (score > best) {
                        best = score;
                        chosen = candidate;
                    }
                }
            }
            for (int j = 0; j < window && pos + j < frames; j++) {
                float w = (float) (.5 - .5 * Math.cos(2 * Math.PI * (j + .5) / window));
                out[pos + j] += input[chosen + j] * w;
                weight[pos + j] += w;
            }
        }
        float[] result = new float[frames];
        for (int i = 0; i < frames; i++) result[i] = weight[i] > .0001f ? out[i] / weight[i] : 0;
        PcmAudio.fade(result);
        return result;
    }
}
