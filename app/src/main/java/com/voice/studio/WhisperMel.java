package com.voice.studio;

import java.io.*;
import java.util.*;

final class WhisperMel {
    final float[][] mel = new float[80][201];
    final double[] hann = new double[400],
            cr = new double[400],
            ci = new double[400],
            br = new double[1024],
            bi = new double[1024];

    WhisperMel(InputStream source) throws IOException {
        try (DataInputStream in = new DataInputStream(source)) {
            for (int m = 0; m < 80; m++) for (int k = 0; k < 201; k++) mel[m][k] = in.readFloat();
        }
        for (int n = 0; n < 400; n++) {
            hann[n] = .5 - .5 * Math.cos(2 * Math.PI * n / 400);
            double angle = Math.PI * n * n / 400.0;
            cr[n] = Math.cos(angle);
            ci[n] = Math.sin(angle);
            br[n] = cr[n];
            bi[n] = ci[n];
            if (n > 0) {
                br[1024 - n] = cr[n];
                bi[1024 - n] = ci[n];
            }
        }
        fft(br, bi, false);
    }

    float[][][] features(float[] audio) {
        float[] pcm = Arrays.copyOf(audio, 480000);
        float[][][] output = new float[1][80][3000];
        double max = -10;
        double[] real = new double[1024], imag = new double[1024], power = new double[201];
        for (int frame = 0; frame < 3000; frame++) {
            Arrays.fill(real, 0);
            Arrays.fill(imag, 0);
            for (int n = 0; n < 400; n++) {
                int at = frame * 160 + n - 200;
                if (at < 0) at = -at;
                if (at >= pcm.length) at = pcm.length * 2 - at - 2;
                double x = pcm[at] * hann[n];
                real[n] = x * cr[n];
                imag[n] = -x * ci[n];
            }
            fft(real, imag, false);
            for (int n = 0; n < 1024; n++) {
                double r = real[n] * br[n] - imag[n] * bi[n], i = real[n] * bi[n] + imag[n] * br[n];
                real[n] = r;
                imag[n] = i;
            }
            fft(real, imag, true);
            for (int k = 0; k < 201; k++) {
                double r = real[k] * cr[k] + imag[k] * ci[k], i = imag[k] * cr[k] - real[k] * ci[k];
                power[k] = r * r + i * i;
            }
            for (int m = 0; m < 80; m++) {
                double sum = 0;
                for (int k = 0; k < 201; k++) sum += mel[m][k] * power[k];
                float value = (float) Math.log10(Math.max(1e-10, sum));
                output[0][m][frame] = value;
                max = Math.max(max, value);
            }
        }
        for (int m = 0; m < 80; m++)
            for (int f = 0; f < 3000; f++)
                output[0][m][f] = (float) ((Math.max(max - 8, output[0][m][f]) + 4) / 4);
        return output;
    }

    static void fft(double[] r, double[] i, boolean inverse) {
        int n = r.length;
        for (int a = 1, b = 0; a < n; a++) {
            int bit = n >> 1;
            for (; (b & bit) != 0; bit >>= 1) b ^= bit;
            b ^= bit;
            if (a < b) {
                double x = r[a];
                r[a] = r[b];
                r[b] = x;
                x = i[a];
                i[a] = i[b];
                i[b] = x;
            }
        }
        for (int size = 2; size <= n; size <<= 1) {
            double angle = (inverse ? 2 : -2) * Math.PI / size,
                    stepR = Math.cos(angle),
                    stepI = Math.sin(angle);
            for (int base = 0; base < n; base += size) {
                double wr = 1, wi = 0;
                for (int k = 0; k < size / 2; k++) {
                    int a = base + k, b = a + size / 2;
                    double vr = r[b] * wr - i[b] * wi, vi = r[b] * wi + i[b] * wr;
                    r[b] = r[a] - vr;
                    i[b] = i[a] - vi;
                    r[a] += vr;
                    i[a] += vi;
                    double next = wr * stepR - wi * stepI;
                    wi = wr * stepI + wi * stepR;
                    wr = next;
                }
            }
        }
        if (inverse)
            for (int a = 0; a < n; a++) {
                r[a] /= n;
                i[a] /= n;
            }
    }
}
