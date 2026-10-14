package com.voice.studio;

import ai.onnxruntime.*;

import org.json.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class SpeakerEngine implements AutoCloseable {
    final OrtEnvironment env = OrtEnvironment.getEnvironment();
    final OrtSession.SessionOptions options;
    final OrtSession session;
    final SpeakerFbank fbank = new SpeakerFbank();
    volatile OrtSession.RunOptions running;

    SpeakerEngine(File models) throws Exception {
        env.setTelemetry(false);
        options = OmniCodec.options();
        OrtSession created = null;
        try {
            created = env.createSession(new File(models, "model.onnx").getAbsolutePath(), options);
        } catch (Exception e) {
            options.close();
            throw e;
        }
        session = created;
    }

    void cancel() {
        try {
            if (running != null) running.setTerminate(true);
        } catch (Exception ignored) {
        }
    }

    float[] embedding(float[] audio, AtomicBoolean cancel) throws Exception {
        OmniEngine.check(cancel);
        try (OrtSession.RunOptions run = new OrtSession.RunOptions();
                OnnxTensor features = OnnxTensor.createTensor(env, fbank.features(audio))) {
            running = run;
            try (OrtSession.Result result =
                    session.run(
                            Collections.singletonMap(
                                    session.getInputNames().iterator().next(), features),
                            run)) {
                FloatBuffer values = ((OnnxTensor) result.get(0)).getFloatBuffer();
                float[] out = new float[values.remaining()];
                values.get(out);
                double norm = 0;
                for (float x : out) norm += x * x;
                if (!Double.isFinite(norm) || norm < 1e-12) throw new IOException("说话人特征无效");
                norm = Math.sqrt(norm);
                for (int i = 0; i < out.length; i++) out[i] /= norm;
                return out;
            }
        } finally {
            running = null;
        }
    }

    static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }

    JSONArray group(
            File wav,
            JSONArray segments,
            double threshold,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        return group(wav, segments, threshold, 0, cancel, progress);
    }

    JSONArray group(File wav, JSONArray segments, double threshold, int speakerCount,
            AtomicBoolean cancel, OmniEngine.Progress progress) throws Exception {
        if (!Double.isFinite(threshold) || threshold < .2 || threshold > .95)
            throw new IOException("分组阈值无效");
        if (speakerCount < 0 || speakerCount > 32) throw new IOException("人物数量需为 0—32，0 为自动");
        List<float[]> features = new ArrayList<>();List<Double> weights = new ArrayList<>();List<Integer> indices = new ArrayList<>();
        JSONArray result = new JSONArray();
        try (PcmAudio.Reader reader = new PcmAudio.Reader(wav)) {
            for (int n = 0; n < segments.length(); n++) {
                OmniEngine.check(cancel);
                JSONObject segment = new JSONObject(segments.getJSONObject(n).toString());
                double start = segment.getDouble("start"), duration = Math.min(10, segment.getDouble("end") - start);
                if (!Double.isFinite(start) || !Double.isFinite(duration) || start < 0 || duration < .5 || start + duration > reader.seconds() + .05)
                    throw new IOException("请将字幕校对为至少 0.5 秒的有效片段，再进行说话人分组");
                float[] samples = PcmAudio.resample(reader.read(start, duration), reader.rate, 16000);
                segment.put("speaker", 0);result.put(segment);
                if (PcmAudio.rms(samples) < .002) continue;
                features.add(embedding(samples, cancel));weights.add(Math.min(8, duration));indices.add(n);
                progress.update("人物声音特征 · " + (n + 1) + " / " + segments.length() + " 条");
            }
        }
        progress.update(speakerCount > 0 ? "正在按 " + speakerCount + " 人统一分组…" : "正在合并同一人物的声音片段…");
        int[] labels = SpeakerClusters.group(features, weights, speakerCount, threshold, cancel);
        for (int n = 0; n < labels.length; n++) result.getJSONObject(indices.get(n)).put("speaker", labels[n]);
        return result;
    }

    public void close() throws Exception {
        session.close();
        options.close();
    }
}
