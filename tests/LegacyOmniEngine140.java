// Frozen 1.4.0 engine for authored-graph before/after regression only.
package com.voice.studio;

import ai.onnxruntime.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native CPU inference for OmniVoice. No sockets, HTTP or server API. */
final class LegacyOmniEngine140 {
    interface Progress {
        void update(String message);
    }

    static final OmniTokenizerCache tokenizerCache = new OmniTokenizerCache();
    long lastGenerationMillis;
    VoicePrompt automaticPrompt;
    private VoicePrompt lastPrompt;
    volatile OrtSession.RunOptions running;

    void cancel() {
        OrtSession.RunOptions r = running;
        if (r != null)
            try {
                r.setTerminate(true);
            } catch (Exception ignored) {
            }
    }

    void generate(
            File modelDir,
            String text,
            String language,
            float speed,
            File output,
            AtomicBoolean cancelled,
            Progress progress)
            throws Exception {
        generate(
                modelDir, text, language, speed, output, cancelled, progress, null, "None", 16, 42);
    }

    void generate(
            File modelDir,
            String text,
            String language,
            float speed,
            File output,
            AtomicBoolean cancelled,
            Progress progress,
            VoicePrompt ref,
            String instruct,
            int steps,
            long seed)
            throws Exception {
        if (text == null || text.trim().isEmpty() || text.length() > 12000)
            throw new IOException("文本需为 1—12000 字");
        if (!Float.isFinite(speed) || speed < .5 || speed > 2) throw new IOException("语速无效");
        if (steps != 16 && steps != 32) throw new IOException("请选择 16 或 32 步");
        long generationStart = System.nanoTime();
        lastGenerationMillis = 0;
        List<String> parts = split(text, speed, ref);
        boolean automatic = ref == null;
        VoicePrompt voice = ref;
        automaticPrompt = null;
        check(cancelled);
        progress.update("读取本机分词器…");
        OmniTokenizer tokenizer = tokenizerCache.get(new File(modelDir, "tokenizer.json"));
        check(cancelled);
        try (PcmAudio.Writer writer = new PcmAudio.Writer(output, 24000)) {
            for (int index = 0; index < parts.size(); index++) {
                check(cancelled);
                final int part = index + 1;
                File clip = new File(output.getParentFile(), output.getName() + ".chunk");
                try {
                    single(
                            modelDir,
                            tokenizer,
                            parts.get(index),
                            language,
                            automatic && voice != null ? 1 : speed,
                            clip,
                            cancelled,
                            m -> progress.update("第 " + part + " / " + parts.size() + " 段 · " + m),
                            voice,
                            voice == null ? instruct : "None",
                            steps,
                            seed);
                    try (PcmAudio.Reader reader = new PcmAudio.Reader(clip)) {
                        writer.write(reader.read(0, reader.seconds()));
                    }
                    if (automatic && voice == null && lastPrompt != null) {
                        voice = lastPrompt;
                        automaticPrompt = lastPrompt;
                    }
                    if (part < parts.size()) writer.silence(.12);
                } finally {
                    clip.delete();
                }
            }
        }
        PcmAudio.markSynthetic(output);
        lastGenerationMillis = (System.nanoTime() - generationStart) / 1000000;
    }

    static double duration(String text, float speed, VoicePrompt ref) {
        return (ref == null
                        ? OmniDuration.frames(text)
                        : ref.frames
                                * OmniDuration.total(text)
                                / Math.max(1, OmniDuration.total(ref.text)))
                / speed;
    }

    static List<String> split(String text, float speed, VoicePrompt ref) {
        return OmniSegments.split(text, speed, ref);
    }

    void single(
            File modelDir,
            OmniTokenizer tokenizer,
            String text,
            String language,
            float speed,
            File output,
            AtomicBoolean cancelled,
            Progress progress,
            VoicePrompt ref,
            String instruct,
            int steps,
            long seed)
            throws Exception {
        // Android calls Os.setenv before this class is initialized; desktop test sets env before
        // launch.
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        progress.update("加载本机 OmniVoice 模型…");
        check(cancelled);
        long[] prefix =
                tokenizer.encode(
                        "<|lang_start|>"
                                + language
                                + "<|lang_end|><|instruct_start|>"
                                + instruct
                                + "<|instruct_end|><|text_start|>"
                                + (ref == null ? "" : ref.text + " ")
                                + text
                                + "<|text_end|>");
        int frames = Math.max(1, (int) duration(text, speed, ref)),
                P = prefix.length,
                R = ref == null ? 0 : ref.frames,
                target = P + R,
                S = target + frames;
        if (frames > 250 || S > 850) throw new IOException("参考文字或单段过长，请缩短参考文字");
        int total = 8 * frames;
        long[] codes = new long[total];
        Arrays.fill(codes, 1024);
        long[] input = new long[2 * 8 * S],
                attention = new long[2 * S],
                positions = new long[2 * S];
        Arrays.fill(input, 1024);
        byte[] mask = new byte[2 * S];
        Arrays.fill(mask, (byte) 1);
        for (int c = 0; c < 8; c++) System.arraycopy(prefix, 0, input, c * S, P);
        for (int i = 0; i < P; i++) mask[i] = 0;
        if (ref != null)
            for (int c = 0; c < 8; c++) System.arraycopy(ref.codes, c * R, input, c * S + P, R);
        for (int i = 0; i < S; i++) {
            attention[i] = 1;
            positions[i] = i;
            positions[S + i] = i;
            if (i < frames) attention[S + i] = 1;
        }
        int remain = total;
        OmniStepSampler sampler = new OmniStepSampler(total);
        Random random = new Random(seed);
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                OrtSession.RunOptions run = new OrtSession.RunOptions()) {
            options.setIntraOpNumThreads(
                    Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())));
            options.setInterOpNumThreads(1);
            options.setMemoryPatternOptimization(false);
            running = run;
            try (OrtSession session =
                    env.createSession(
                            new File(modelDir, "omnivoice.qint8_per_channel.onnx")
                                    .getAbsolutePath(),
                            options);
                    OmniTensorRun tensors = new OmniTensorRun(env, S, input, mask, attention, positions)) {
                for (int step = 0; step < steps; step++) {
                    check(cancelled);
                    progress.update("本机合成 · " + (step + 1) + " / " + steps + " 步");
                    double a = time((double) step / steps), b = time((double) (step + 1) / steps);
                    int k =
                            step == steps - 1
                                    ? remain
                                    : Math.min(remain, (int) Math.ceil(total * (b - a)));
                    if (k <= 0) continue;
                    for (int c = 0; c < 8; c++)
                        for (int t = 0; t < frames; t++) {
                            input[c * S + target + t] = codes[c * frames + t];
                            input[(8 + c) * S + t] = codes[c * frames + t];
                        }
                    FloatBuffer logits = tensors.run(session, run, input);
                    remain -= sampler.update(logits, codes, frames, S, target, k, random);
                }
            }
            check(cancelled);
            progress.update("本机解码音频…");
            try (OrtSession decoder =
                            env.createSession(
                                    new File(modelDir, "higgs_decoder.onnx").getAbsolutePath(),
                                    options);
                    OnnxTensor codeTensor =
                            OnnxTensor.createTensor(
                                    env, LongBuffer.wrap(codes), new long[] {8, 1, frames})) {
                try (OrtSession.Result result =
                        decoder.run(Collections.singletonMap("codes", codeTensor), run)) {
                    FloatBuffer samples = ((OnnxTensor) result.get(0)).getFloatBuffer();
                    float[] pcm = new float[samples.remaining()];
                    samples.get(pcm);
                    if (ref != null && ref.rms < .1)
                        for (int i = 0; i < pcm.length; i++) pcm[i] *= ref.rms / .1;
                    lastPrompt =
                            frames >= 75 && text.length() <= 2000
                                    ? new VoicePrompt(
                                            text,
                                            codes.clone(),
                                            frames,
                                            Math.max(.1, PcmAudio.rms(pcm)))
                                    : null;
                    PcmAudio.fade(pcm);
                    try (PcmAudio.Writer writer = new PcmAudio.Writer(output, 24000)) {
                        writer.write(pcm);
                    }
                }
            }
        } finally {
            running = null;
        }
    }

    static double time(double t) {
        return .1 * t / (1 - .9 * t);
    }

    static void check(AtomicBoolean c) throws IOException {
        if (c.get() || Thread.currentThread().isInterrupted()) throw new IOException("已取消生成");
    }

    static void writeWav(File file, FloatBuffer samples, AtomicBoolean cancelled)
            throws IOException {
        int count = samples.remaining();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            out.write("RIFF".getBytes("US-ASCII"));
            le(out, 36 + count * 2, 4);
            out.write("WAVEfmt ".getBytes("US-ASCII"));
            le(out, 16, 4);
            le(out, 1, 2);
            le(out, 1, 2);
            le(out, 24000, 4);
            le(out, 48000, 4);
            le(out, 2, 2);
            le(out, 16, 2);
            out.write("data".getBytes("US-ASCII"));
            le(out, count * 2, 4);
            for (int i = 0; i < count; i++) {
                if ((i & 8191) == 0) check(cancelled);
                float value = samples.get();
                if (!Float.isFinite(value)) throw new IOException("模型返回无效音频");
                le(out, Math.round(Math.max(-1, Math.min(1, value)) * 32767), 2);
            }
        }
    }

    static void le(OutputStream out, int value, int bytes) throws IOException {
        for (int i = 0; i < bytes; i++) out.write((value >> (8 * i)) & 255);
    }
}
