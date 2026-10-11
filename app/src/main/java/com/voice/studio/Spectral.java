package com.voice.studio;

/** Reusable FFT for powers of two and 3*power-of-two sizes used by MDX-Net. */
final class Spectral {
    final int n, m;
    final double[][] r, i;
    final double[] cosine, sine;

    Spectral(int n) {
        this.n = n;
        m = n / 3;
        r = new double[3][m];
        i = new double[3][m];
        cosine = new double[n];
        sine = new double[n];
        for (int k = 0; k < n; k++) {
            cosine[k] = Math.cos(-2 * Math.PI * k / n);
            sine[k] = Math.sin(-2 * Math.PI * k / n);
        }
        if ((n & (n - 1)) != 0 && (n % 3 != 0 || (m & (m - 1)) != 0))
            throw new IllegalArgumentException("FFT size");
    }

    void transform(double[] real, double[] imag, boolean inverse) {
        if (real.length != n || imag.length != n) throw new IllegalArgumentException("FFT shape");
        if ((n & (n - 1)) == 0) {
            WhisperMel.fft(real, imag, inverse);
            return;
        }
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < m; k++) {
                r[j][k] = real[3 * k + j];
                i[j][k] = inverse ? -imag[3 * k + j] : imag[3 * k + j];
            }
            WhisperMel.fft(r[j], i[j], false);
        }
        for (int k = 0; k < n; k++) {
            int at = k % m;
            double rr = r[0][at], ii = i[0][at];
            for (int j = 1; j < 3; j++) {
                int angle = k * j % n;
                rr += r[j][at] * cosine[angle] - i[j][at] * sine[angle];
                ii += r[j][at] * sine[angle] + i[j][at] * cosine[angle];
            }
            real[k] = inverse ? rr / n : rr;
            imag[k] = inverse ? -ii / n : ii;
        }
    }
}
