package com.voice.studio;

import android.content.Context;
import android.media.*;
import android.net.Uri;

import java.io.*;
import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class MediaFiles {
    static void decode(
            Context context,
            Uri uri,
            File output,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        decode(context, uri, output, cancel, progress, false);
    }

    static void decode(
            Context context,
            Uri uri,
            File output,
            AtomicBoolean cancel,
            OmniEngine.Progress progress,
            boolean stereo)
            throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("无法读取音频");
            byte[] header = new byte[12];
            int at = 0, n;
            while (at < 12 && (n = input.read(header, at, 12 - at)) > 0) at += n;
            if (at == 12 && new String(header, 0, 4, "US-ASCII").equals("RIFF")) {
                File raw = new File(output.getPath() + ".source");
                try {
                    try (OutputStream out = new FileOutputStream(raw)) {
                        out.write(header);
                        copy(input, out, cancel, 500000000L);
                    }
                    try (PcmAudio.Reader r = new PcmAudio.Reader(raw);
                            PcmAudio.Writer w =
                                    new PcmAudio.Writer(
                                            output, stereo ? 44100 : 24000, stereo ? 2 : 1)) {
                        StreamingResampler left = new StreamingResampler(r.rate, stereo ? 44100 : 24000);
                        StreamingResampler right = stereo ? new StreamingResampler(r.rate, 44100) : null;
                        for (double t = 0; t < r.seconds(); t += 30) {
                            OmniEngine.check(cancel);
                            if (stereo) {
                                float[][] channel = r.stereo(t, Math.min(30, r.seconds() - t));
                                w.write(
                                        interleave(
                                                left.push(channel[0]), right.push(channel[1])));
                            } else
                                w.write(
                                        left.push(r.read(t, Math.min(30, r.seconds() - t))));
                            progress.update("导入音频 · " + (long)Math.min(t + 30, r.seconds()) + " 秒");
                        }
                        w.write(stereo ? interleave(left.finish(), right.finish()) : left.finish());
                    }
                } finally {
                    raw.delete();
                }
                return;
            }
        }
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        PcmAudio.Writer writer = null;
        StreamingResampler leftStream = null, rightStream = null;
        try {
            extractor.setDataSource(context, uri, null);
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                if (f.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    extractor.selectTrack(i);
                    format = f;
                    break;
                }
            }
            if (format == null) throw new IOException("文件没有音轨");
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();
            int rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    encoding = AudioFormat.ENCODING_PCM_16BIT;
            boolean inputEnd = false, outputEnd = false;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long last = System.nanoTime(), progressAt = 0;
            while (!outputEnd) {
                OmniEngine.check(cancel);
                if (System.nanoTime() - last > 30000000000L) throw new IOException("解码超时");
                if (!inputEnd) {
                    int index = codec.dequeueInputBuffer(10000);
                    if (index >= 0) {
                        ByteBuffer buf = codec.getInputBuffer(index);
                        buf.clear();
                        int count = extractor.readSampleData(buf, 0);
                        if (count < 0) {
                            codec.queueInputBuffer(
                                    index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEnd = true;
                        } else {
                            codec.queueInputBuffer(index, 0, count, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                int index = codec.dequeueOutputBuffer(info, 10000);
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat f = codec.getOutputFormat();
                    if (writer != null && (rate != f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            || channels != f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)))
                        throw new IOException("音轨中途改变格式，请转换为固定采样率音频后导入");
                    rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    encoding =
                            f.containsKey(MediaFormat.KEY_PCM_ENCODING)
                                    ? f.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                    : AudioFormat.ENCODING_PCM_16BIT;
                }
                if (index >= 0) {
                    last = System.nanoTime();
                    if (info.size > 0) {
                        if (channels < 1
                                || channels > 8
                                || rate < 8000
                                || rate > 192000
                                || (encoding != AudioFormat.ENCODING_PCM_16BIT
                                        && encoding != AudioFormat.ENCODING_PCM_FLOAT))
                            throw new IOException("不支持解码器音频格式");
                        ByteBuffer b = codec.getOutputBuffer(index);
                        if (b == null || info.offset < 0 || info.size > b.capacity() - info.offset)
                            throw new IOException("解码器返回无效音频缓冲区");
                        b.position(info.offset);
                        b.limit(info.offset + info.size);
                        b.order(ByteOrder.LITTLE_ENDIAN);
                        int width = encoding == AudioFormat.ENCODING_PCM_FLOAT ? 4 : 2,
                                count = info.size / (width * channels);
                        if (info.size % (width * channels) != 0) throw new IOException("解码音频帧不完整");
                        float[] left = new float[count], right = new float[count];
                        for (int at = 0; at < count; at++) {
                            float sum = 0, a = 0, c = 0;
                            for (int ch = 0; ch < channels; ch++) {
                                float value = width == 4 ? b.getFloat() : b.getShort() / 32768f;
                                sum += value;
                                if (ch == 0) a = value;
                                if (ch == 1) c = value;
                            }
                            left[at] = stereo && channels == 2 ? a : sum / channels;
                            right[at] = stereo && channels == 2 ? c : left[at];
                        }
                        if (writer == null) {
                            writer =
                                    new PcmAudio.Writer(
                                            output, stereo ? 44100 : 24000, stereo ? 2 : 1);
                            leftStream = new StreamingResampler(rate, stereo ? 44100 : 24000);
                            if (stereo) rightStream = new StreamingResampler(rate, 44100);
                        }
                        writer.write(
                                stereo
                                        ? interleave(
                                                leftStream.push(left), rightStream.push(right))
                                        : leftStream.push(left));
                        long now = System.nanoTime();
                        if (progressAt == 0 || now - progressAt >= 1000000000L) {
                            progress.update("导入音频 · " + info.presentationTimeUs / 1000000 + " 秒"); progressAt = now;
                        }
                    }
                    outputEnd = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    codec.releaseOutputBuffer(index, false);
                }
            }
            if (writer == null) throw new IOException("文件音轨为空");
            writer.write(stereo ? interleave(leftStream.finish(), rightStream.finish()) : leftStream.finish());
        } finally {
            if (writer != null) writer.close();
            if (codec != null) {
                try {
                    codec.stop();
                } catch (Exception ignored) {
                }
                codec.release();
            }
            extractor.release();
        }
    }

    static float[] interleave(float[] left, float[] right) {
        if (left.length != right.length) throw new IllegalArgumentException("声道长度");
        float[] values = new float[left.length * 2];
        for (int n = 0; n < left.length; n++) {
            values[n * 2] = left[n];
            values[n * 2 + 1] = right[n];
        }
        return values;
    }

    static void dub(
            Context context,
            Uri video,
            File wav,
            File output,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        MediaExtractor source = new MediaExtractor();
        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        boolean started = false;
        try (PcmAudio.Reader audio = new PcmAudio.Reader(wav)) {
            if (audio.rate != 24000) throw new IOException("配音音轨格式不符");
            source.setDataSource(context, video, null);
            MediaFormat format = null;
            int videoTrack = -1;
            for (int i = 0; i < source.getTrackCount(); i++) {
                MediaFormat f = source.getTrackFormat(i);
                if (f.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    format = f;
                    source.selectTrack(i);
                    break;
                }
            }
            if (format == null) throw new IOException("该文件没有视频轨道；可导出配音 WAV");
            muxer =
                    new MediaMuxer(
                            output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrack = muxer.addTrack(format);
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            try {
                metadata.setDataSource(context, video);
                String rotation =
                        metadata.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (rotation != null) muxer.setOrientationHint(Integer.parseInt(rotation));
            } finally {
                metadata.release();
            }
            MediaFormat aac = MediaFormat.createAudioFormat("audio/mp4a-latm", 24000, 1);
            aac.setInteger(
                    MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            aac.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            aac.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32768);
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
            encoder.configure(aac, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            boolean inputEnd = false, outputEnd = false;
            long cursor = 0, last = System.nanoTime();
            int audioTrack = -1;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!outputEnd) {
                OmniEngine.check(cancel);
                if (System.nanoTime() - last > 30000000000L) throw new IOException("音轨编码超时");
                if (!inputEnd) {
                    int index = encoder.dequeueInputBuffer(10000);
                    if (index >= 0) {
                        ByteBuffer b = encoder.getInputBuffer(index);
                        b.clear();
                        b.order(ByteOrder.LITTLE_ENDIAN);
                        int count = (int) Math.min(b.remaining() / 2, audio.frames - cursor);
                        if (count <= 0) {
                            encoder.queueInputBuffer(
                                    index,
                                    0,
                                    0,
                                    cursor * 1000000 / 24000,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEnd = true;
                        } else {
                            float[] values =
                                    audio.read((double) cursor / 24000, (double) count / 24000);
                            for (float value : values)
                                b.putShort(
                                        (short)
                                                Math.round(
                                                        Math.max(-1, Math.min(.999969f, value))
                                                                * 32768));
                            encoder.queueInputBuffer(
                                    index, 0, values.length * 2, cursor * 1000000 / 24000, 0);
                            cursor += values.length;
                        }
                    }
                }
                int index = encoder.dequeueOutputBuffer(info, 10000);
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (started) throw new IOException("编码格式重复改变");
                    audioTrack = muxer.addTrack(encoder.getOutputFormat());
                    muxer.start();
                    started = true;
                    int capacity =
                            format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)
                                    ? format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                                    : 8388608;
                    if (capacity < 1 || capacity > 67108864) throw new IOException("视频帧过大");
                    ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
                    MediaCodec.BufferInfo frame = new MediaCodec.BufferInfo();
                    long progressAt = 0;
                    while (true) {
                        OmniEngine.check(cancel);
                        buffer.clear();
                        int size = source.readSampleData(buffer, 0);
                        if (size < 0) break;
                        long time = source.getSampleTime();
                        int flags = source.getSampleFlags();
                        if ((flags & MediaExtractor.SAMPLE_FLAG_ENCRYPTED) != 0)
                            throw new IOException("不支持加密视频");
                        frame.set(
                                0,
                                size,
                                time,
                                (flags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                                        ? MediaCodec.BUFFER_FLAG_KEY_FRAME
                                        : 0);
                        buffer.position(0);
                        buffer.limit(size);
                        muxer.writeSampleData(videoTrack, buffer, frame);
                        source.advance();
                        long now = System.nanoTime();
                        if (progressAt == 0 || now - progressAt >= 1000000000L) {
                            progress.update("封装视频 · " + time / 1000000 + " 秒"); progressAt = now;
                        }
                    }
                    last = System.nanoTime();
                }
                if (index >= 0) {
                    last = System.nanoTime();
                    ByteBuffer buffer = encoder.getOutputBuffer(index);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0;
                    if (info.size > 0) {
                        if (!started) throw new IOException("音频编码器未返回格式");
                        buffer.position(info.offset);
                        buffer.limit(info.offset + info.size);
                        muxer.writeSampleData(audioTrack, buffer, info);
                    }
                    outputEnd = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    encoder.releaseOutputBuffer(index, false);
                }
            }
            if (!started) throw new IOException("视频封装失败");
            muxer.stop();
            started = false;
        } finally {
            if (encoder != null) {
                try {
                    encoder.stop();
                } catch (Exception ignored) {
                }
                encoder.release();
            }
            source.release();
            if (muxer != null) {
                if (started)
                    try {
                        muxer.stop();
                    } catch (Exception ignored) {
                    }
                muxer.release();
            }
        }
    }

    static void copy(InputStream in, OutputStream out, AtomicBoolean cancel, long limit)
            throws IOException {
        if (in == null || out == null) throw new IOException("无法打开文件");
        byte[] b = new byte[65536];
        long total = 0;
        int n;
        while ((n = in.read(b)) >= 0) {
            OmniEngine.check(cancel);
            total += n;
            if (total > limit) throw new IOException("文件过大");
            out.write(b, 0, n);
        }
    }
}
