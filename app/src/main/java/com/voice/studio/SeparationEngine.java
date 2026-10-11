package com.voice.studio;

import ai.onnxruntime.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** UVR MDX-Net Inst HQ 3: stereo STFT -> predicted accompaniment -> residual vocals. */
final class SeparationEngine {
    static final int FFT = 6144,
            HOP = 1024,
            BINS = 3072,
            FRAMES = 256,
            LENGTH = HOP * (FRAMES - 1),
            TRIM = FFT / 2,
            CORE = LENGTH - 2 * TRIM;
    final Spectral transform = new Spectral(FFT);
    final double[] window = new double[FFT];
    volatile OrtSession.RunOptions running;

    SeparationEngine() {
        for (int k = 0; k < FFT; k++) window[k] = .5 - .5 * Math.cos(2 * Math.PI * k / FFT);
    }

    void cancel() {
        try {
            if (running != null) running.setTerminate(true);
        } catch (Exception ignored) {
        }
    }

    float[] spectrum(float[][] audio, AtomicBoolean cancel) throws IOException {
        float[] out = new float[4 * BINS * FRAMES];
        double[] real = new double[FFT], imag = new double[FFT];
        for (int channel = 0; channel < 2; channel++)
            for (int frame = 0; frame < FRAMES; frame++) {
                OmniEngine.check(cancel);
                Arrays.fill(imag, 0);
                for (int n = 0; n < FFT; n++) {
                    int at = frame * HOP + n - TRIM;
                    if (at < 0) at = -at;
                    if (at >= LENGTH) at = 2 * LENGTH - at - 2;
                    real[n] = audio[channel][at] * window[n];
                }
                transform.transform(real, imag, false);
                for (int bin = 3; bin < BINS; bin++) {
                    out[((channel * 2) * BINS + bin) * FRAMES + frame] = (float) real[bin];
                    out[((channel * 2 + 1) * BINS + bin) * FRAMES + frame] = (float) imag[bin];
                }
            }
        return out;
    }

    float[][] waveform(FloatBuffer spectrum, AtomicBoolean cancel) throws IOException {
        float[][] out = new float[2][LENGTH];
        double[] weight = new double[LENGTH], real = new double[FFT], imag = new double[FFT];
        for (int frame = 0; frame < FRAMES; frame++)
            for (int n = 0; n < FFT; n++) {
                int at = frame * HOP + n - TRIM;
                if (at >= 0 && at < LENGTH) weight[at] += window[n] * window[n];
            }
        for (int channel = 0; channel < 2; channel++)
            for (int frame = 0; frame < FRAMES; frame++) {
                OmniEngine.check(cancel);
                Arrays.fill(real, 0);
                Arrays.fill(imag, 0);
                for (int bin = 0; bin < BINS; bin++) {
                    real[bin] = spectrum.get(((channel * 2) * BINS + bin) * FRAMES + frame);
                    imag[bin] = spectrum.get(((channel * 2 + 1) * BINS + bin) * FRAMES + frame);
                    if (bin > 0) {
                        real[FFT - bin] = real[bin];
                        imag[FFT - bin] = -imag[bin];
                    }
                }
                transform.transform(real, imag, true);
                for (int n = 0; n < FFT; n++) {
                    int at = frame * HOP + n - TRIM;
                    if (at >= 0 && at < LENGTH) out[channel][at] += real[n] * window[n];
                }
            }
        for (int channel = 0; channel < 2; channel++)
            for (int n = 0; n < LENGTH; n++)
                out[channel][n] = (float) (weight[n] > 1e-12 ? out[channel][n] / weight[n] : 0);
        return out;
    }

    void separate(
            File models,
            File wav,
            File vocals,
            File instrumental,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = OmniCodec.options();
                OrtSession session =
                        env.createSession(
                                new File(models, "model.onnx").getAbsolutePath(), options);
                OrtSession.RunOptions run = new OrtSession.RunOptions();
                PcmAudio.Reader reader = new PcmAudio.Reader(wav);
                PcmAudio.Writer voice = new PcmAudio.Writer(vocals, 44100, 2);
                PcmAudio.Writer music = new PcmAudio.Writer(instrumental, 44100, 2)) {
            running = run;
            long total = Math.round(reader.seconds() * 44100);
            for (long cursor = 0; cursor < total; cursor += CORE) {
                OmniEngine.check(cancel);
                float[][] input = new float[2][LENGTH];
                long first = cursor - TRIM;
                int pad = (int) Math.max(0, -first);
                double start = Math.max(0, first) / 44100.0;
                float[][] block =
                        reader.stereo(
                                start,
                                Math.min(
                                        (double) (LENGTH - pad) / 44100,
                                        Math.max(0, reader.seconds() - start)));
                for (int channel = 0; channel < 2; channel++) {
                    float[] pcm = PcmAudio.resample(block[channel], reader.rate, 44100);
                    System.arraycopy(
                            pcm, 0, input[channel], pad, Math.min(pcm.length, LENGTH - pad));
                }
                float[] encoded = spectrum(input, cancel);
                try (OnnxTensor tensor =
                                OnnxTensor.createTensor(
                                        env,
                                        FloatBuffer.wrap(encoded),
                                        new long[] {1, 4, BINS, FRAMES});
                        OrtSession.Result result =
                                session.run(Collections.singletonMap("input", tensor), run)) {
                    float[][] separated =
                            waveform(((OnnxTensor) result.get(0)).getFloatBuffer(), cancel);
                    int count = (int) Math.min(CORE, total - cursor);
                    float[] v = new float[count * 2], m = new float[count * 2];
                    for (int n = 0; n < count; n++)
                        for (int channel = 0; channel < 2; channel++) {
                            float accompaniment = separated[channel][n + TRIM] * 1.022f;
                            m[n * 2 + channel] = accompaniment;
                            v[n * 2 + channel] = input[channel][n + TRIM] - accompaniment;
                        }
                    voice.write(v);
                    music.write(m);
                }
                progress.update(
                        "人声分离 · "
                                + Math.min(total, cursor + CORE) / 44100
                                + " / "
                                + (long) reader.seconds()
                                + " 秒");
            }
        } finally {
            running = null;
        }
    }
}
