package com.voice.studio;

import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native CPU inference; staged chunks reuse sessions without holding both large graphs. */
final class OmniEngine {
    interface Progress { void update(String message); }
    static final OmniTokenizerCache tokenizerCache = new OmniTokenizerCache();
    long lastGenerationMillis, tokenizerNanos, loadNanos, inferenceNanos, decodeNanos, modelRunNanos, samplingNanos;
    int backboneLoads, decoderLoads, chunkCount, backboneRuns, samplingWorkers, parallelSamplingRounds;
    int prePinnedChunks, outputShapeFallbacks;
    long outputCopyBytes;
    VoicePrompt automaticPrompt;
    volatile OrtSession.RunOptions running;

    void cancel() {
        OrtSession.RunOptions r = running;
        if (r != null) try { r.setTerminate(true); } catch (Exception ignored) {}
    }

    void generate(File modelDir, String text, String language, float speed, File output,
            AtomicBoolean cancelled, Progress progress) throws Exception {
        generate(modelDir, text, language, speed, output, cancelled, progress, null, "None", 16, 42);
    }

    void generate(File modelDir, String text, String language, float speed, File output,
            AtomicBoolean cancelled, Progress progress, VoicePrompt ref, String instruct,
            int steps, long seed) throws Exception {
        if (text == null || text.trim().isEmpty() || text.length() > 12000)
            throw new IOException("文本需为 1—12000 字");
        if (!Float.isFinite(speed) || speed < .5 || speed > 2) throw new IOException("语速无效");
        if (steps != 8 && steps != 16 && steps != 32) throw new IOException("请选择 8、16 或 32 步");
        long generationStart = System.nanoTime();
        lastGenerationMillis = tokenizerNanos = loadNanos = inferenceNanos = decodeNanos = 0;
        modelRunNanos = samplingNanos = 0;
        backboneLoads = decoderLoads = 0;
        backboneRuns = 0;
        samplingWorkers = parallelSamplingRounds = 0;
        prePinnedChunks = outputShapeFallbacks = 0;outputCopyBytes = 0;
        automaticPrompt = null;
        decodedPinnedChunks = 0;decoderOutputCopyBytes = 0;
        List<String> parts = split(text, speed, ref);
        chunkCount = parts.size();
        boolean automatic = ref == null;
        check(cancelled);
        progress.update("读取本机分词器…");
        long tokenStart = System.nanoTime();
        OmniTokenizer tokenizer = tokenizerCache.get(new File(modelDir, "tokenizer.json"));
        tokenizerNanos += System.nanoTime() - tokenStart;
        check(cancelled);
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                OmniSamplerWorkers workers = new OmniSamplerWorkers(Runtime.getRuntime().availableProcessors(), cancelled);
                OrtSession.RunOptions run = new OrtSession.RunOptions();
                PcmAudio.Writer writer = new PcmAudio.Writer(output, 24000)) {
            options.setIntraOpNumThreads(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())));
            options.setInterOpNumThreads(1);
            options.setMemoryPatternOptimization(false);
            running = run;
            VoicePrompt anchor = OmniChunkPipeline.run(parts.size(), ref, cancelled,
                    new OmniChunkPipeline.Factory<EncodedChunk, VoicePrompt>() {
                public OmniChunkPipeline.Encoder<EncodedChunk, VoicePrompt> encoder() throws Exception {
                    check(cancelled);
                    progress.update("加载本机合成模型…");
                    long start = System.nanoTime();
                    OrtSession session = env.createSession(new File(modelDir,
                            "omnivoice.qint8_per_channel.onnx").getAbsolutePath(), options);
                    loadNanos += System.nanoTime() - start;
                    backboneLoads++;
                    return new OmniChunkPipeline.Encoder<EncodedChunk, VoicePrompt>() {
                        public EncodedChunk encode(int index, VoicePrompt voice) throws Exception {
                            long start = System.nanoTime();
                            try {
                                return encodeChunk(env, session, run, tokenizer, parts.get(index), language,
                                        automatic && voice != null ? 1 : speed, cancelled,
                                        m -> progress.update("第 " + (index + 1) + " / " + parts.size() + " 段 · " + m),
                                        voice, voice == null ? instruct : "None", steps, seed, workers);
                            } finally { inferenceNanos += System.nanoTime() - start; }
                        }
                        public void close() throws Exception { session.close(); }
                    };
                }
                public OmniChunkPipeline.Decoder<EncodedChunk, VoicePrompt> decoder() throws Exception {
                    check(cancelled);
                    progress.update("加载本机音频解码器…");
                    long start = System.nanoTime();
                    OrtSession session = env.createSession(new File(modelDir,
                            "higgs_decoder.onnx").getAbsolutePath(), options);
                    loadNanos += System.nanoTime() - start;
                    decoderLoads++;
                    OmniDecoderRun buffers = new OmniDecoderRun(env);
                    return new OmniChunkPipeline.Decoder<EncodedChunk, VoicePrompt>() {
                        public VoicePrompt decode(int index, EncodedChunk chunk, VoicePrompt voice) throws Exception {
                            check(cancelled);
                            progress.update("第 " + (index + 1) + " / " + parts.size() + " 段 · 本机解码音频…");
                            long start = System.nanoTime();
                            try {
                                VoicePrompt result = decodeChunk(buffers, session, run, chunk, voice, writer, cancelled);
                                if (index + 1 < parts.size()) writer.silence(.12);
                                return result;
                            } finally { decodeNanos += System.nanoTime() - start; }
                        }
                        public void close() throws Exception {
                            try { buffers.close(); } finally { session.close(); }
                        }
                    };
                }
            });
            if (automatic) automaticPrompt = anchor;
            samplingWorkers = workers.largestWorkerCount;
            parallelSamplingRounds = workers.parallelRounds;
        } finally { running = null; }
        check(cancelled);
        PcmAudio.markSynthetic(output);
        lastGenerationMillis = (System.nanoTime() - generationStart) / 1000000;
    }

    static double duration(String text, float speed, VoicePrompt ref) {
        return (ref == null ? OmniDuration.frames(text) : ref.frames * OmniDuration.total(text)
                / Math.max(1, OmniDuration.total(ref.text))) / speed;
    }

    static List<String> split(String text, float speed, VoicePrompt ref) {
        return OmniSegments.split(text, speed, ref);
    }

    private static final class EncodedChunk {
        final String text;
        final long[] codes;
        final int frames;
        EncodedChunk(String text, long[] codes, int frames) {
            this.text = text; this.codes = codes; this.frames = frames;
        }
    }

    private EncodedChunk encodeChunk(OrtEnvironment env, OrtSession session, OrtSession.RunOptions run,
            OmniTokenizer tokenizer, String text, String language, float speed,
            AtomicBoolean cancelled, Progress progress, VoicePrompt ref, String instruct,
            int steps, long seed, OmniSamplerWorkers workers) throws Exception {
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
        OmniStepSampler sampler = new OmniStepSampler(total, workers);
        Random random = new Random(seed);
        progress.update("本机合成 · 0 / " + steps + " 步");
        try (OmniTensorRun tensors = new OmniTensorRun(env, S, input, mask, attention, positions)) {
                for (int step = 0; step < steps; step++) {
                    check(cancelled);
                    double a = time((double) step / steps), b = time((double) (step + 1) / steps);
                    int k =
                            step == steps - 1
                                    ? remain
                                    : Math.min(remain, (int) Math.ceil(total * (b - a)));
                    if (k <= 0) { progress.update("本机合成 · " + (step + 1) + " / " + steps + " 步"); continue; }
                    for (int c = 0; c < 8; c++)
                        for (int t = 0; t < frames; t++) {
                            input[c * S + target + t] = codes[c * frames + t];
                            input[(8 + c) * S + t] = codes[c * frames + t];
                        }
                    long runStart = System.nanoTime();
                    FloatBuffer logits = tensors.run(session, run, input);
                    backboneRuns++;
                    modelRunNanos += System.nanoTime() - runStart;
                    long samplingStart = System.nanoTime();
                    remain -= sampler.update(logits, codes, frames, S, target, k, random);
                    samplingNanos += System.nanoTime() - samplingStart;
                    check(cancelled);
                    progress.update("本机合成 · " + (step + 1) + " / " + steps + " 步");
                }
                if (tensors.metadataPinned) prePinnedChunks++;
                outputShapeFallbacks += tensors.metadataShapeFallbacks;
                outputCopyBytes += tensors.copiedOutputBytes;
        }
        return new EncodedChunk(text, codes, frames);
    }

    int decodedPinnedChunks;
    long decoderOutputCopyBytes;

    private VoicePrompt decodeChunk(OmniDecoderRun buffers, OrtSession decoder, OrtSession.RunOptions run,
            EncodedChunk chunk, VoicePrompt ref, PcmAudio.Writer writer, AtomicBoolean cancelled) throws Exception {
        long copied = buffers.copiedOutputBytes;
        int pinned = buffers.pinnedRuns;
        FloatBuffer samples = buffers.run(decoder, run, chunk.codes, chunk.frames, cancelled);
        decoderOutputCopyBytes += buffers.copiedOutputBytes - copied;
        decodedPinnedChunks += buffers.pinnedRuns - pinned;
        check(cancelled);
        if (samples == null || !samples.hasRemaining()) throw new IOException("模型返回空音频");
        float[] pcm = new float[samples.remaining()];
        samples.get(pcm);
        if (ref != null && ref.rms < .1)
            for (int i = 0; i < pcm.length; i++) pcm[i] *= ref.rms / .1;
        VoicePrompt anchor = ref == null && chunk.frames >= 75 && chunk.text.length() <= 2000
                ? new VoicePrompt(chunk.text, chunk.codes.clone(), chunk.frames,
                        Math.max(.1, PcmAudio.rms(pcm))) : null;
        PcmAudio.fade(pcm);
        check(cancelled);
        writer.write(pcm);
        check(cancelled);
        return anchor;
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
