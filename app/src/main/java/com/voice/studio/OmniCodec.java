package com.voice.studio;

import ai.onnxruntime.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class OmniCodec {
    VoicePrompt encode(
            File models,
            File wav,
            String text,
            double start,
            double seconds,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        if (seconds < 3 || seconds > 20) throw new IOException("参考声音需为 3—20 秒");
        float[] samples;
        try (PcmAudio.Reader r = new PcmAudio.Reader(wav)) {
            if (start + seconds > r.seconds() + .01) throw new IOException("参考范围超过文件时长");
            samples = PcmAudio.resample(r.read(start, seconds), r.rate, 24000);
        }
        double rms = PcmAudio.rms(samples);
        if (rms < .001) throw new IOException("参考音频没有足够的人声");
        if (rms < .1) for (int i = 0; i < samples.length; i++) samples[i] *= .1 / rms;
        int frames = samples.length / 960;
        OmniDuration.checkReference(text, frames);
        samples = Arrays.copyOf(samples, frames * 960);
        float[][][] acoustic = features(models, "acoustic_encoder.onnx", samples, cancel, progress);
        float[][][] semantic =
                features(
                        models,
                        "semantic_encoder.onnx",
                        PcmAudio.resample(samples, 24000, 16000),
                        cancel,
                        progress);
        int count = Math.min(acoustic[0][0].length, semantic[0][0].length);
        if (Math.abs(count - frames) > 1
                || Math.abs(acoustic[0][0].length - semantic[0][0].length) > 1)
            throw new IOException("参考声音编码时长不一致，已停止创建音色，请检查克隆模型");
        acoustic = trim(acoustic, count);
        semantic = trim(semantic, count);
        OmniEngine.check(cancel);
        progress.update("编码参考音色…");
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = options();
                OrtSession session =
                        env.createSession(
                                new File(models, "quantizer_encoder.onnx").getAbsolutePath(),
                                options);
                OnnxTensor a = OnnxTensor.createTensor(env, acoustic);
                OnnxTensor s = OnnxTensor.createTensor(env, semantic)) {
            Map<String, OnnxTensor> input = new HashMap<>();
            for (String name : session.getInputNames())
                input.put(name, name.contains("semantic") ? s : a);
            try (OrtSession.Result result = session.run(input)) {
                OmniEngine.check(cancel);
                OnnxTensor output = (OnnxTensor) result.get(0);
                long[] shape = output.getInfo().getShape();
                int n = (int) shape[shape.length - 1];
                if (shape.length != 3 || shape[0] != 8 || shape[1] != 1 || n != count)
                    throw new IOException("参考声音编码形状无效，已停止创建音色");
                LongBuffer b = output.getLongBuffer();
                long[] codes = new long[8 * n];
                b.get(codes);
                return new VoicePrompt(text, codes, n, rms);
            }
        }
    }

    float[][][] features(
            File models,
            String file,
            float[] samples,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        OmniEngine.check(cancel);
        progress.update("参考声音 · " + file);
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = options();
                OrtSession session =
                        env.createSession(new File(models, file).getAbsolutePath(), options);
                OnnxTensor waveform =
                        OnnxTensor.createTensor(
                                env,
                                FloatBuffer.wrap(samples),
                                file.startsWith("semantic")
                                        ? new long[] {1, samples.length}
                                        : new long[] {1, 1, samples.length});
                OrtSession.Result result =
                        session.run(
                                Collections.singletonMap(
                                        session.getInputNames().iterator().next(), waveform))) {
            OmniEngine.check(cancel);
            return (float[][][]) result.get(0).getValue();
        }
    }

    static OrtSession.SessionOptions options() throws OrtException {
        OrtSession.SessionOptions o = new OrtSession.SessionOptions();
        o.setIntraOpNumThreads(2);
        o.setInterOpNumThreads(1);
        o.setMemoryPatternOptimization(false);
        return o;
    }

    static float[][][] trim(float[][][] x, int n) {
        for (int b = 0; b < x.length; b++)
            for (int c = 0; c < x[b].length; c++) x[b][c] = Arrays.copyOf(x[b][c], n);
        return x;
    }
}
