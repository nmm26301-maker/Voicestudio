package com.voice.studio;

import android.content.Context;
import android.media.*;
import android.net.Uri;
import android.os.Build;
import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** Explicit on-device checks with synthetic data, never microphone capture. */
final class DeviceChecks {
    interface Check { String run() throws Exception; }
    static void check(JSONArray checks, String name, Check test, OmniEngine.Progress progress) throws Exception {
        progress.update("基础自检 · " + name);
        JSONObject result = new JSONObject().put("name", name);
        long began = System.nanoTime();
        try { result.put("status", "passed").put("detail", test.run()); }
        catch (Throwable failure) {
            result.put("status", "failed").put("detail", failure.getClass().getSimpleName() + ": "
                    + (failure.getMessage() == null ? "请查看设备与模型诊断" : failure.getMessage()));
        }
        result.put("elapsedMs", (System.nanoTime() - began) / 1000000); checks.put(result);
    }
    static String nativeOnnx(byte[] model) throws Exception {
        OrtEnvironment environment = OrtEnvironment.getEnvironment(); environment.setTelemetry(false);
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(1); options.setInterOpNumThreads(1);
            try (OrtSession session = environment.createSession(model, options);
                    OnnxTensor tensor = OnnxTensor.createTensor(environment, new float[][]{{1.25f,-2.5f}});
                    OrtSession.Result result = session.run(Collections.singletonMap("input",tensor))) {
                float[][] actual = (float[][])result.get(0).getValue();
                if (actual.length != 1 || actual[0].length != 2 || actual[0][0] != 1.25f || actual[0][1] != -2.5f)
                    throw new IOException("ONNX 自检输出不匹配");
                return "JNI 初始化与小型 Identity 模型推理通过；不代表语音大模型质量验收";
            }
        }
    }
    static JSONObject run(Context context, OmniEngine.Progress progress) throws Exception {
        JSONObject report = new JSONObject().put("version", context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName)
                .put("created", System.currentTimeMillis()).put("device", Build.MANUFACTURER + " " + Build.MODEL)
                .put("androidSdk", Build.VERSION.SDK_INT);
        JSONArray checks = new JSONArray();
        File folder = new File(context.getCacheDir(), "selftest-" + UUID.randomUUID());
        if (!folder.mkdirs()) throw new IOException("无法创建自检文件");
        try {
            check(checks,"ONNX 本机推理",() -> {
                android.system.Os.setenv("ORT_DISABLE_TELEMETRY", "1", true);
                byte[] graph;
                try (InputStream input = context.getAssets().open("selftest-identity.onnx");
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024]; int count;
                    while ((count = input.read(buffer)) >= 0) bytes.write(buffer,0,count);
                    graph = bytes.toByteArray();
                }
                return nativeOnnx(graph);
            },progress);
            File tone = new File(folder,"tone.wav"), pcm = new File(folder,"pcm.wav"),
                    aac = new File(folder,"tone.m4a"), decoded = new File(folder,"aac.wav");
            float[] samples = new float[24000];
            for (int n = 0; n < samples.length; n++) samples[n] = (float)(.2 * Math.sin(2 * Math.PI * 440 * n / 24000));
            try (PcmAudio.Writer writer = new PcmAudio.Writer(tone,24000)) { writer.write(samples); }
            check(checks,"PCM 音频导入",() -> {
                MediaFiles.decode(context,Uri.fromFile(tone),pcm,new AtomicBoolean(),m -> {});
                try (PcmAudio.Reader reader = new PcmAudio.Reader(pcm)) {
                    if (reader.rate != 24000 || reader.frames != 24000 || PcmAudio.rms(reader.read(0,1)) < .1)
                        throw new IOException("PCM 导入结果不匹配");
                }
                return "1 秒合成测试音导入与 WAV 校验通过";
            },progress);
            check(checks,"AAC 编码与封装",() -> {
                encode(samples,aac); return "AAC-LC 编码并保存 M4A 成功";
            },progress);
            if (aac.isFile() && aac.length() > 32) check(checks,"AAC 解码与连续重采样",() -> {
                MediaFiles.decode(context,Uri.fromFile(aac),decoded,new AtomicBoolean(),m -> {});
                try (PcmAudio.Reader reader = new PcmAudio.Reader(decoded)) {
                    if (reader.rate != 24000 || reader.seconds() < .8 || reader.seconds() > 1.5
                            || PcmAudio.rms(reader.read(0,reader.seconds())) < .04)
                        throw new IOException("AAC 解码时长或信号异常");
                    return "解码通过，输出 " + String.format(Locale.US,"%.3f",reader.seconds()) + " 秒 WAV";
                }
            },progress);
            else checks.put(new JSONObject().put("name","AAC 解码与连续重采样").put("status","skipped").put("detail","编码未生成可用文件"));
        } finally { OmniModels.remove(folder); }
        report.put("checks",checks).put("note","此自检只使用生成的测试音和小型 ONNX 图，不录音、不联网。麦克风、蓝牙路由、视频轨道、各语音模型推理与音质仍需单独验收。");
        return report;
    }

    static void encode(float[] samples, File output) throws Exception {
        MediaCodec encoder = null; MediaMuxer muxer = null; boolean started = false;
        try {
            MediaFormat format = MediaFormat.createAudioFormat("audio/mp4a-latm",24000,1);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE,64000);
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
            encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); encoder.start();
            muxer = new MediaMuxer(output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int cursor = 0, track = -1; boolean inputEnd = false, outputEnd = false;
            long deadline = System.nanoTime() + 15000000000L;
            while (!outputEnd) {
                if (System.nanoTime() > deadline) throw new IOException("AAC 自检编码超时");
                if (!inputEnd) {
                    int index = encoder.dequeueInputBuffer(10000);
                    if (index >= 0) {
                        ByteBuffer input = encoder.getInputBuffer(index); input.clear(); input.order(ByteOrder.LITTLE_ENDIAN);
                        int count = Math.min(input.remaining()/2,samples.length-cursor);
                        for (int n = 0; n < count; n++) input.putShort((short)Math.round(samples[cursor+n]*32768));
                        encoder.queueInputBuffer(index,0,count*2,(long)cursor*1000000/24000,
                                count==0 ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                        cursor += count; inputEnd = count==0;
                    }
                }
                int index = encoder.dequeueOutputBuffer(info,10000);
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (started) throw new IOException("AAC 编码格式重复改变");
                    track = muxer.addTrack(encoder.getOutputFormat()); muxer.start(); started = true;
                } else if (index >= 0) {
                    if (info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) {
                        if (!started) throw new IOException("AAC 编码没有返回格式");
                        ByteBuffer buffer = encoder.getOutputBuffer(index); buffer.position(info.offset); buffer.limit(info.offset+info.size);
                        muxer.writeSampleData(track,buffer,info);
                    }
                    outputEnd = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0; encoder.releaseOutputBuffer(index,false);
                }
            }
            if (!started) throw new IOException("AAC 未生成音轨");
            muxer.stop(); started = false;
        } finally {
            if (encoder != null) { try { encoder.stop(); } catch (Exception ignored) { } encoder.release(); }
            if (muxer != null) { if (started) try { muxer.stop(); } catch (Exception ignored) { } muxer.release(); }
        }
    }
}
