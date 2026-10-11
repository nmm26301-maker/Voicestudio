package com.voice.studio;

import java.io.IOException;

/** Kaldi-compatible 16 kHz, 80-bin Povey-window log filterbanks with mean normalization. */
final class SpeakerFbank {
    final double[] window = new double[400];
    final double[][] filters = new double[80][257];

    SpeakerFbank() {
        for (int n = 0; n < 400; n++)
            window[n] = Math.pow(.5 - .5 * Math.cos(2 * Math.PI * n / 399), .85);
        double low = mel(20), high = mel(8000), step = (high - low) / 81;
        for (int b = 0; b < 80; b++)
            for (int k = 0; k < 256; k++) {
                double value = mel(k * 16000.0 / 512),
                        left = low + b * step,
                        center = left + step,
                        right = center + step;
                filters[b][k] =
                        value < left || value > right
                                ? 0
                                : value < center ? (value - left) / step : (right - value) / step;
            }
    }

    static double mel(double hz) {
        return 1127 * Math.log(1 + hz / 700);
    }

    float[][][] features(float[] audio) throws IOException {
        int frames = 1 + (audio.length - 400) / 160;
        if (frames < 20 || frames > 6000) throw new IOException("说话人参考长度不合适");
        float[][][] result = new float[1][frames][80];
        double[] mean = new double[80], real = new double[512], imag = new double[512];
        for (int f = 0; f < frames; f++) {
            double dc = 0;
            for (int n = 0; n < 400; n++) dc += audio[f * 160 + n] * 32768;
            dc /= 400;
            java.util.Arrays.fill(real, 0);
            java.util.Arrays.fill(imag, 0);
            for (int n = 0; n < 400; n++) {
                double x = audio[f * 160 + n] * 32768 - dc,
                        previous = audio[f * 160 + Math.max(0, n - 1)] * 32768 - dc;
                real[n] = (x - .97 * previous) * window[n];
            }
            WhisperMel.fft(real, imag, false);
            for (int b = 0; b < 80; b++) {
                double energy = 0;
                for (int k = 0; k < 256; k++)
                    energy += (real[k] * real[k] + imag[k] * imag[k]) * filters[b][k];
                float value = (float) Math.log(Math.max(1.1920928955078125e-7, energy));
                result[0][f][b] = value;
                mean[b] += value;
            }
        }
        for (int f = 0; f < frames; f++)
            for (int b = 0; b < 80; b++) result[0][f][b] -= (float) (mean[b] / frames);
        return result;
    }
}
