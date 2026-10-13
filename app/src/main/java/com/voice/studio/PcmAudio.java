package com.voice.studio;

import java.io.*;

final class PcmAudio {
    static final class Reader implements Closeable {
        final RandomAccessFile file;
        int rate, channels, width, encoding;
        long frames, offset, dataBytes;

        Reader(File path) throws IOException {
            file = new RandomAccessFile(path, "r");
            try {
                if (!ascii(file, 4).equals("RIFF")) {
                    throw new IOException("请选择 PCM WAV 音频");
                }
                readLE(file, 4);
                if (!ascii(file, 4).equals("WAVE")) throw new IOException("WAV 无效");
                boolean fmt = false, data = false;
                while (file.getFilePointer() + 8 <= file.length()) {
                    String name = ascii(file, 4);
                    long size = readLE(file, 4),
                            begin = file.getFilePointer(),
                            next = begin + size + (size & 1);
                    if (size > file.length() - begin) throw new IOException("WAV 不完整");
                    if (name.equals("fmt ")) {
                        if (size < 16) throw new IOException("WAV 格式无效");
                        encoding = (int) readLE(file, 2);
                        channels = (int) readLE(file, 2);
                        rate = (int) readLE(file, 4);
                        readLE(file, 4);
                        readLE(file, 2);
                        int bits = (int) readLE(file, 2);
                        if (bits % 8 != 0) throw new IOException("WAV 位深无效");
                        width = bits / 8;
                        fmt = true;
                    }
                    if (name.equals("data")) {
                        if (!fmt) throw new IOException("WAV 缺少格式");
                        offset = begin;
                        if (channels < 1
                                || channels > 8
                                || width < 2
                                || width > 4
                                || rate < 8000
                                || rate > 192000) throw new IOException("WAV 格式无效");
                        dataBytes = size;
                        if (size % (channels * width) != 0) throw new IOException("WAV 末尾采样不完整");
                        frames = size / (channels * width);
                        data = true;
                        break;
                    }
                    file.seek(next);
                }
                if (!data
                        || channels < 1
                        || channels > 8
                        || rate < 8000
                        || rate > 192000
                        || !((encoding == 1 && (width == 2 || width == 3 || width == 4))
                                || (encoding == 3 && width == 4)))
                    throw new IOException("只支持 PCM16/24/32 或 float32 WAV");
            } catch (IOException | RuntimeException e) {
                file.close();
                throw e;
            }
        }

        double seconds() {
            return (double) frames / rate;
        }

        /** Read every byte without allocating decoded audio. Integers have no invalid bit patterns. */
        void validateSamples(java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
            file.seek(offset);
            byte[] bytes = new byte[32768];
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            long remaining = dataBytes;
            while (remaining > 0) {
                if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("已暂停生成");
                int count = (int) Math.min(bytes.length, remaining);
                file.readFully(bytes, 0, count);
                if (encoding == 3) {
                    buffer.clear(); buffer.limit(count);
                    while (buffer.remaining() >= 4) if (!Float.isFinite(buffer.getFloat())) throw new IOException("音频包含无效数值");
                }
                remaining -= count;
            }
            if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("已暂停生成");
        }

        float[] read(double start, double seconds) throws IOException {
            return readChannels(start, seconds, false)[0];
        }

        float[][] stereo(double start, double seconds) throws IOException {
            return readChannels(start, seconds, true);
        }

        private float[][] readChannels(double start, double seconds, boolean stereo)
                throws IOException {
            if (!Double.isFinite(start)
                    || !Double.isFinite(seconds)
                    || start < 0
                    || seconds < 0
                    || seconds > 60) throw new IOException("音频时间无效");
            long begin = Math.min(frames, Math.round(start * rate));
            int count = (int) Math.min(frames - begin, Math.round(seconds * rate));
            float[][] out = new float[stereo ? 2 : 1][count];
            file.seek(offset + begin * channels * width);
            int align = channels * width, blockFrames = Math.max(1, 32768 / align);
            byte[] bytes = new byte[blockFrames * align];
            java.nio.ByteBuffer buffer =
                    java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int position = 0; position < count; ) {
                int take = Math.min(blockFrames, count - position);
                file.readFully(bytes, 0, take * align);
                buffer.clear();
                buffer.limit(take * align);
                for (int n = 0; n < take; n++) {
                    double sum = 0, first = 0, second = 0;
                    for (int c = 0; c < channels; c++) {
                        double value;
                        if (encoding == 3) value = buffer.getFloat();
                        else if (width == 2) value = buffer.getShort() / 32768.0;
                        else if (width == 3) {
                            int bits =
                                    (buffer.get() & 255)
                                            | ((buffer.get() & 255) << 8)
                                            | ((buffer.get() & 255) << 16);
                            value = ((bits << 8) >> 8) / 8388608.0;
                        } else value = buffer.getInt() / 2147483648.0;
                        if (!Double.isFinite(value)) throw new IOException("音频包含无效数值");
                        sum += value;
                        if (c == 0) first = value;
                        if (c == 1) second = value;
                    }
                    out[0][position + n] =
                            (float) (stereo && channels == 2 ? first : sum / channels);
                    if (stereo)
                        out[1][position + n] = (float) (channels == 2 ? second : sum / channels);
                }
                position += take;
            }
            return out;
        }

        public void close() throws IOException {
            file.close();
        }
    }

    static final class Writer implements Closeable {
        final RandomAccessFile f;
        final int rate, channels;
        long count;

        Writer(File path, int sampleRate) throws IOException {
            this(path, sampleRate, 1);
        }

        Writer(File path, int sampleRate, int channels) throws IOException {
            if (channels < 1 || channels > 2) throw new IOException("输出声道无效");
            this.channels = channels;
            if (sampleRate < 8000 || sampleRate > 192000) throw new IOException("采样率无效");
            rate = sampleRate;
            f = new RandomAccessFile(path, "rw");
            f.setLength(0);
            f.write(new byte[44]);
        }

        void write(float[] samples) throws IOException {
            if (count + samples.length > 1000000000L) throw new IOException("音频过长");
            byte[] bytes = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                float x = samples[i];
                if (!Float.isFinite(x)) throw new IOException("音频数值无效");
                int v = Math.round(Math.max(-1, Math.min(1, x)) * 32767);
                bytes[2 * i] = (byte) v;
                bytes[2 * i + 1] = (byte) (v >> 8);
            }
            f.write(bytes);
            count += samples.length;
        }

        void silence(double seconds) throws IOException {
            if (!Double.isFinite(seconds) || seconds < 0) throw new IOException("音频时长无效");
            long n = Math.round(seconds * rate * channels);
            float[] zeros = new float[rate];
            while (n > 0) {
                int take = (int) Math.min(n, rate);
                write(take == rate ? zeros : new float[take]);
                n -= take;
            }
        }

        public void close() throws IOException {
            f.seek(0);
            f.writeBytes("RIFF");
            writeLE(f, 36 + count * 2, 4);
            f.writeBytes("WAVEfmt ");
            writeLE(f, 16, 4);
            writeLE(f, 1, 2);
            writeLE(f, channels, 2);
            writeLE(f, rate, 4);
            writeLE(f, rate * channels * 2, 4);
            writeLE(f, channels * 2, 2);
            writeLE(f, 16, 2);
            f.writeBytes("data");
            writeLE(f, count * 2, 4);
            f.close();
        }
    }

    static float[] resample(float[] x, int input, int output) {
        if (input == output) return x;
        int n = (int) Math.round((double) x.length * output / input);
        float[] result = new float[n];
        double cutoff = Math.min(1, (double) output / input);
        for (int i = 0; i < n; i++) {
            double p = (double) i * input / output, sum = 0, norm = 0;
            int center = (int) p;
            for (int k = center - 16; k <= center + 16; k++) {
                double d = p - k, z = d * cutoff;
                if (k < 0 || k >= x.length || Math.abs(d) >= 16) continue;
                double w =
                        (Math.abs(z) < 1e-9 ? 1 : Math.sin(Math.PI * z) / (Math.PI * z))
                                * (Math.abs(d) < 1e-9
                                        ? 1
                                        : Math.sin(Math.PI * d / 16) / (Math.PI * d / 16));
                sum += x[k] * w;
                norm += w;
            }
            result[i] = (float) (norm == 0 ? 0 : sum / norm);
        }
        return result;
    }

    static double rms(float[] x) {
        double sum = 0;
        for (float v : x) sum += v * v;
        return Math.sqrt(sum / Math.max(1, x.length));
    }

    static void fade(float[] x) {
        int n = Math.min(240, x.length / 2);
        for (int i = 0; i < n; i++) {
            float gain = (float) i / n;
            x[i] *= gain;
            x[x.length - 1 - i] *= gain;
        }
    }

    static void markSynthetic(File path) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(path, "rw")) {
            f.seek(f.length());
            byte[] value = "VoiceStudio AI-generated speech\u0000".getBytes("UTF-8");
            int size = 4 + 8 + value.length + (value.length & 1);
            f.writeBytes("LIST");
            writeLE(f, size, 4);
            f.writeBytes("INFOISFT");
            writeLE(f, value.length, 4);
            f.write(value);
            if ((value.length & 1) != 0) f.write(0);
            f.seek(4);
            writeLE(f, f.length() - 8, 4);
        }
    }

    static long readLE(RandomAccessFile f, int bytes) throws IOException {
        long v = 0;
        for (int i = 0; i < bytes; i++) v |= ((long) f.readUnsignedByte()) << (i * 8);
        return v;
    }

    static void writeLE(RandomAccessFile f, long value, int bytes) throws IOException {
        for (int i = 0; i < bytes; i++) f.write((int) (value >> (i * 8)) & 255);
    }

    static String ascii(RandomAccessFile f, int n) throws IOException {
        byte[] b = new byte[n];
        f.readFully(b);
        return new String(b, "US-ASCII");
    }
}
