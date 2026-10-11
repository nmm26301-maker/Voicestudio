package com.voice.studio;

import java.util.Arrays;

/** Windowed-sinc resampling with one global sample clock across decoder blocks. */
final class StreamingResampler {
    final int inputRate, outputRate;
    final double cutoff;
    long consumed, produced;
    float[] tail = new float[0];
    boolean finished;

    StreamingResampler(int inputRate, int outputRate) {
        if (inputRate < 8000 || inputRate > 192000 || outputRate < 8000 || outputRate > 192000)
            throw new IllegalArgumentException("Unsupported sample rate");
        this.inputRate = inputRate; this.outputRate = outputRate;
        cutoff = Math.min(1, (double)outputRate / inputRate);
    }

    float[] push(float[] samples) { return convert(samples, false); }
    float[] finish() { return convert(new float[0], true); }

    private float[] convert(float[] samples, boolean end) {
        if (finished) throw new IllegalStateException("Resampler already finished");
        if (samples == null) throw new IllegalArgumentException("Missing samples");
        if (inputRate == outputRate) {
            consumed += samples.length; produced += samples.length; finished = end; return samples;
        }
        long start = consumed - tail.length, total = consumed + samples.length;
        float[] buffer = Arrays.copyOf(tail, tail.length + samples.length);
        System.arraycopy(samples, 0, buffer, tail.length, samples.length);
        long target = end ? Math.round((double)total * outputRate / inputRate)
                : Math.max(0, (long)Math.ceil((double)(total - 16) * outputRate / inputRate));
        int count = Math.toIntExact(Math.max(0, target - produced));
        float[] result = new float[count];
        for (int n = 0; n < count; n++) {
            double position = (double)(produced + n) * inputRate / outputRate;
            long center = (long)position;
            double sum = 0, norm = 0;
            for (long k = center - 16; k <= center + 16; k++) {
                double distance = position - k, z = distance * cutoff;
                if (k < 0 || k >= total || Math.abs(distance) >= 16) continue;
                if (k < start) throw new IllegalStateException("Missing resampling context");
                double weight = (Math.abs(z) < 1e-9 ? 1 : Math.sin(Math.PI * z) / (Math.PI * z))
                        * (Math.abs(distance) < 1e-9 ? 1 : Math.sin(Math.PI * distance / 16) / (Math.PI * distance / 16));
                sum += buffer[(int)(k - start)] * weight; norm += weight;
            }
            result[n] = (float)(norm == 0 ? 0 : sum / norm);
        }
        consumed = total; produced += count; finished = end;
        tail = end ? new float[0] : Arrays.copyOfRange(buffer, Math.max(0, buffer.length - 34), buffer.length);
        return result;
    }
}
