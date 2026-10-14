package com.voice.studio;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Concatenate already-generated WAV clips from third-party browser demos.
 * This does not synthesize speech, load models, contact servers or hide a failed segment.
 * Use a temporary output path; caller publishes it only after full validation.
 */
final class CloudWavMerge {
    interface Progress { void update(String text); }
    static final int MAX_CLIPS = 24;
    static final double MAX_SECONDS = 1800;
    static final int RATE = 24000;
    static final double GAP_SECONDS = .08;

    static long merge(List<File> clips, File output, AtomicBoolean cancelled, Progress progress)
            throws Exception {
        if (clips == null || clips.size() < 2 || clips.size() > MAX_CLIPS)
            throw new IOException("请选择 2—24 个已生成的 WAV 片段；文件名最好以 01、02 编号");
        if (output == null || output.exists() && !output.delete())
            throw new IOException("无法创建临时合并文件");
        double total = 0;
        boolean complete = false;
        try {
            try (PcmAudio.Writer writer = new PcmAudio.Writer(output, RATE)) {
                for (int i = 0; i < clips.size(); i++) {
                    check(cancelled);
                    File clip=clips.get(i);
                    if (clip == null || !clip.isFile() || clip.length() > 64L*1024*1024)
                        throw new IOException("第 "+(i+1)+" 段不存在或超过 64 MiB");
                    progress.update("正在校验并合并第 "+(i+1)+" / "+clips.size()+" 段…");
                    try (PcmAudio.Reader reader=new PcmAudio.Reader(clip)) {
                        double seconds=reader.seconds();
                        if (!Double.isFinite(seconds) || seconds < .25 || seconds > 300)
                            throw new IOException("第 "+(i+1)+" 段时长异常（需 0.25—300 秒）");
                        total += seconds;
                        if (total + GAP_SECONDS * (clips.size()-1) > MAX_SECONDS)
                            throw new IOException("合并总时长最多 30 分钟，请分成几部作品");
                        reader.validateSamples(cancelled);
                        long audible = 0;
                        for (double at = 0; at < seconds; at += 5) {
                            check(cancelled);
                            float[] frames = reader.read(at, Math.min(5, seconds-at));
                            if (frames.length == 0) continue;
                            for (float value : frames)
                                if (Math.abs(value) > .001f) audible++;
                            float[] converted=PcmAudio.resample(frames,reader.rate,RATE);
                            writer.write(converted);
                        }
                        if (audible < Math.max(1, Math.round(reader.rate * .02)))
                            throw new IOException("第 "+(i+1)+" 段几乎静音，请重新生成；未发布合并作品");
                    }
                    if (i+1<clips.size()) writer.silence(GAP_SECONDS);
                }
            }
            check(cancelled);
            long millis=AcceleratedSpeechClient.validate(output,"",cancelled);
            complete=true;
            return millis;
        } finally {
            if (!complete) output.delete();
        }
    }
    static void check(AtomicBoolean cancelled) throws InterruptedIOException {
        if (cancelled != null && cancelled.get() || Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("已取消音频合并");
    }
}
