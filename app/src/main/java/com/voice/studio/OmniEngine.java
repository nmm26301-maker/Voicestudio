package com.voice.studio;

import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native CPU inference; staged chunks reuse sessions without holding both large graphs. */
final class OmniEngine {
    interface Progress { void update(String message); }
    static final OmniTokenizerCache tokenizerCache = new OmniTokenizerCache();
    long lastGenerationMillis, tokenizerNanos, loadNanos, inferenceNanos, decodeNanos, modelRunNanos, samplingNanos;
    int backboneLoads, decoderLoads, chunkCount, backboneRuns, samplingWorkers, parallelSamplingRounds;
    int prePinnedChunks, outputShapeFallbacks;
    long outputCopyBytes, pcmWriteNanos;
    int bufferedPcmChunks, reusedEncodingChunks, reusedDecodedChunks, decoderRuns;
    VoicePrompt automaticPrompt;
    volatile OrtSession.RunOptions running;

    void cancel() {
        OrtSession.RunOptions r = running;
        if (r != null) try { r.setTerminate(true); } catch (Exception ignored) {}
    }

    void generate(File modelDir, String text, String language, float speed, File output,
            AtomicBoolean cancelled, Progress progress) throws Exception {
        generate(modelDir, text, language, speed, output, cancelled, progress, null, "None", 16, 42);
    }

    void generate(File modelDir, String text, String language, float speed, File output,
            AtomicBoolean cancelled, Progress progress, VoicePrompt ref, String instruct,
            int steps, long seed) throws Exception {
        generate(modelDir,text,language,speed,output,cancelled,progress,ref,instruct,steps,seed,null);
    }

    void generate(File modelDir,String text,String language,float speed,File output,
            AtomicBoolean cancelled,Progress progress,VoicePrompt ref,String instruct,
            int steps,long seed,File checkpoint) throws Exception {
        generate(modelDir,text,language,speed,output,cancelled,progress,ref,instruct,steps,seed,checkpoint,null);
    }

    void generate(File modelDir,String text,String language,float speed,File output,
            AtomicBoolean cancelled,Progress progress,VoicePrompt ref,String instruct,
            int steps,long seed,File checkpoint,File preview) throws Exception {
        if (text == null || text.trim().isEmpty() || text.length() > 12000)
            throw new IOException("文本需为 1—12000 字");
        if (!Float.isFinite(speed) || speed < .5 || speed > 2) throw new IOException("语速无效");
        if (!OmniDuration.hasSpeech(text)) throw new IOException("文本中没有可朗读的文字");
        if (steps != 8 && steps != 16 && steps != 32) throw new IOException("请选择 8、16 或 32 步");
        long generationStart = System.nanoTime();
        lastGenerationMillis = tokenizerNanos = loadNanos = inferenceNanos = decodeNanos = 0;
        modelRunNanos = samplingNanos = 0;
        backboneLoads = decoderLoads = 0;
        backboneRuns = 0;
        samplingWorkers = parallelSamplingRounds = 0;
        prePinnedChunks = outputShapeFallbacks = 0;outputCopyBytes = 0;
        automaticPrompt = null;
        decodedPinnedChunks = 0;decoderOutputCopyBytes = 0;pcmWriteNanos=0;bufferedPcmChunks=0;reusedEncodingChunks=0;reusedDecodedChunks=decoderRuns=0;
        List<String> parts = split(text, speed, ref);
        chunkCount = parts.size();
        boolean automatic = ref == null;
        check(cancelled);
        progress.update("读取本机分词器…");
        long tokenStart = System.nanoTime();
        OmniTokenizer tokenizer = tokenizerCache.get(new File(modelDir, "tokenizer.json"));
        tokenizerNanos += System.nanoTime() - tokenStart;
        check(cancelled);
        OmniEncodingCache cache=checkpoint==null?null:new OmniEncodingCache(checkpoint,modelDir);
        OmniDecodedCache decodedCache=cache==null?null:new OmniDecodedCache(cache.folder);
        check(cancelled);
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                OmniSamplerWorkers workers = new OmniSamplerWorkers(Runtime.getRuntime().availableProcessors(), cancelled);
                OrtSession.RunOptions run = new OrtSession.RunOptions();
                PcmAudio.Writer writer = new PcmAudio.Writer(output, 24000)) {
            options.setIntraOpNumThreads(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())));
            options.setInterOpNumThreads(1);
            options.setMemoryPatternOptimization(false);
            running = run;
            VoicePrompt anchor = OmniChunkPipeline.run(parts.size(), ref, cancelled,
                    new OmniChunkPipeline.Factory<EncodedChunk, VoicePrompt>() {
                public OmniChunkPipeline.Encoder<EncodedChunk, VoicePrompt> encoder() throws Exception {
                    check(cancelled);
                    return new OmniChunkPipeline.Encoder<EncodedChunk, VoicePrompt>() {
                        OrtSession session;
                        public EncodedChunk encode(int index, VoicePrompt voice) throws Exception {
                            check(cancelled);
                            String part=parts.get(index),instruction=voice==null?instruct:"None";
                            float rate=automatic&&voice!=null?1:speed;
                            int frames=frameCount(part,language,rate,voice,steps);
                            String key=cache==null?null:cache.key(part,language,rate,voice,instruction,steps,seed);
                            OmniEncodingCache.Entry saved=cache==null?null:cache.read(index,key,frames);
                            check(cancelled);
                            if(saved!=null){reusedEncodingChunks++;progress.update("第 "+(index+1)+" / "+parts.size()+" 段 · 复用已完成的本机推理…");return new EncodedChunk(part,saved.codes,saved.frames,key);}
                            if(session==null) {
                                progress.update("加载本机合成模型…");long began=System.nanoTime();
                                session=env.createSession(new File(modelDir,"omnivoice.qint8_per_channel.onnx").getAbsolutePath(),options);
                                loadNanos+=System.nanoTime()-began;backboneLoads++;
                            }
                            long start=System.nanoTime();EncodedChunk result;
                            try {
                                result=encodeChunk(env,session,run,tokenizer,part,language,rate,cancelled,
                                    m->progress.update("第 "+(index+1)+" / "+parts.size()+" 段 · "+m),voice,instruction,steps,seed,workers);
                            }finally{inferenceNanos+=System.nanoTime()-start;}
                            check(cancelled);
                            if(cache!=null){cache.save(index,key,result.frames,result.codes);check(cancelled);}
                            return new EncodedChunk(result.text,result.codes,result.frames,key);
                        }
                        public void close() throws Exception {if(session!=null)session.close();}
                    };
                }
                public OmniChunkPipeline.Decoder<EncodedChunk, VoicePrompt> decoder() throws Exception {
                    check(cancelled);
                    return new OmniChunkPipeline.Decoder<EncodedChunk, VoicePrompt>() {
                        OrtSession session;OmniDecoderRun buffers;
                        public VoicePrompt decode(int index, EncodedChunk chunk, VoicePrompt voice) throws Exception {
                            check(cancelled);long start=System.nanoTime(),loadBefore=loadNanos;
                            try {
                                String key=decodedCache==null?null:OmniDecodedCache.key(chunk.key,chunk.codes);
                                boolean anchorEligible=voice==null&&chunk.frames>=75&&chunk.text.length()<=2000;
                                OmniDecodedCache.Entry saved=decodedCache==null?null:decodedCache.read(index,key,chunk.frames,anchorEligible,cancelled);
                                VoicePrompt result;
                                if(saved!=null) {
                                    progress.update("第 "+(index+1)+" / "+parts.size()+" 段 · 复用已完成的本机音频…");
                                    OmniDecodedCache.append(writer,saved,cancelled,OmniDuration.hasSpeech(chunk.text));reusedDecodedChunks++;
                                    result=saved.anchor?new VoicePrompt(chunk.text,chunk.codes.clone(),chunk.frames,saved.rms):null;
                                }else {
                                    if(session==null) {
                                        progress.update("加载本机音频解码器…");long began=System.nanoTime();
                                        session=env.createSession(new File(modelDir,"higgs_decoder.onnx").getAbsolutePath(),options);
                                        loadNanos+=System.nanoTime()-began;decoderLoads++;buffers=new OmniDecoderRun(env);
                                    }
                                    progress.update("第 "+(index+1)+" / "+parts.size()+" 段 · 本机解码音频…");
                                    long offset=writer.f.getFilePointer();
                                    result=decodeChunk(buffers,session,run,chunk,voice,writer,cancelled);
                                    if(decodedCache!=null)decodedCache.save(index,key,chunk.frames,result,writer,offset,cancelled);
                                }
                                if(index==0&&preview!=null) {
                                    try {
                                        SynthesisPreview.write(preview,writer,44,chunk.frames*960,cancelled);
                                        progress.update(SynthesisPreview.READY);
                                    }catch(IOException error){if(error instanceof SynthesisPreview.SourceFailure)throw error;check(cancelled);progress.update("片段试听暂不可用，继续生成完整音频…");}
                                }
                                if(index+1<parts.size())writer.silence(.12);
                                check(cancelled);return result;
                            }catch(OmniPcmWriter.SilentSpeech silent) {
                                if(cache!=null)cache.path(index).delete();
                                if(decodedCache!=null)decodedCache.path(index).delete();
                                throw silent;
                            }finally{decodeNanos+=System.nanoTime()-start-(loadNanos-loadBefore);}
                        }
                        public void close() throws Exception {
                            try{if(buffers!=null)buffers.close();}finally{if(session!=null)session.close();}
                        }
                    };
                }
            });
            if (automatic) automaticPrompt = anchor;
            samplingWorkers = workers.largestWorkerCount;
            parallelSamplingRounds = workers.parallelRounds;
        } finally { running = null; }
        check(cancelled);
        try (PcmAudio.Reader complete = new PcmAudio.Reader(output)) {
            OmniDuration.checkOutput(text, speed, complete.seconds(), language, ref != null, steps);
            complete.validateSpeech(cancelled);
        }
        PcmAudio.markSynthetic(output);
        lastGenerationMillis = (System.nanoTime() - generationStart) / 1000000;
    }

    static double duration(String text, float speed, VoicePrompt ref) {
        return OmniSegments.duration(OmniDuration.total(text), speed, ref);
    }

    static int frameCount(String text, String language, float speed, VoicePrompt ref, int steps) {
        return Math.max(Math.max(1,(int)duration(text,speed,ref)),
                (int)Math.ceil(OmniDuration.cloneFloor(text,language,speed,ref!=null,steps)));
    }

    static List<String> split(String text, float speed, VoicePrompt ref) {
        return OmniSegments.split(text, speed, ref);
    }

    private static final class EncodedChunk {
        final String text;
        final long[] codes;
        final int frames;final String key;
        EncodedChunk(String text,long[] codes,int frames){this(text,codes,frames,null);}
        EncodedChunk(String text,long[] codes,int frames,String key){this.text=text;this.codes=codes;this.frames=frames;this.key=key;}
    }

    private EncodedChunk encodeChunk(OrtEnvironment env, OrtSession session, OrtSession.RunOptions run,
            OmniTokenizer tokenizer, String text, String language, float speed,
            AtomicBoolean cancelled, Progress progress, VoicePrompt ref, String instruct,
            int steps, long seed, OmniSamplerWorkers workers) throws Exception {
        check(cancelled);
        long[] prefix = tokenizer.encode(OmniPrompt.build(text, language, instruct, ref));
        int frames = frameCount(text, language, speed, ref, steps),
                P = prefix.length,
                R = ref == null ? 0 : ref.frames,
                target = P + R,
                S = target + frames;
        if (frames > 250 || S > 850) throw new IOException("参考文字或单段过长，请缩短参考文字");
        int total = 8 * frames;
        long[] codes = new long[total];
        Arrays.fill(codes, 1024);
        long[] input = new long[2 * 8 * S],
                attention = new long[2 * S],
                positions = new long[2 * S];
        Arrays.fill(input, 1024);
        byte[] mask = new byte[2 * S];
        Arrays.fill(mask, (byte) 1);
        for (int c = 0; c < 8; c++) System.arraycopy(prefix, 0, input, c * S, P);
        for (int i = 0; i < P; i++) mask[i] = 0;
        if (ref != null)
            for (int c = 0; c < 8; c++) System.arraycopy(ref.codes, c * R, input, c * S + P, R);
        for (int i = 0; i < S; i++) {
            attention[i] = 1;
            positions[i] = i;
            positions[S + i] = i;
            if (i < frames) attention[S + i] = 1;
        }
        int remain = total;
        OmniStepSampler sampler = new OmniStepSampler(total, workers);
        Random random = new Random(seed);
        progress.update("本机合成 · 0 / " + steps + " 步");
        try (OmniTensorRun tensors = new OmniTensorRun(env, S, input, mask, attention, positions)) {
                for (int step = 0; step < steps; step++) {
                    check(cancelled);
                    double a = time((double) step / steps), b = time((double) (step + 1) / steps);
                    int k =
                            step == steps - 1
                                    ? remain
                                    : Math.min(remain, (int) Math.ceil(total * (b - a)));
                    if (k <= 0) { progress.update("本机合成 · " + (step + 1) + " / " + steps + " 步"); continue; }
                    for (int c = 0; c < 8; c++)
                        for (int t = 0; t < frames; t++) {
                            input[c * S + target + t] = codes[c * frames + t];
                            input[(8 + c) * S + t] = codes[c * frames + t];
                        }
                    long runStart = System.nanoTime();
                    FloatBuffer logits = tensors.run(session, run, input);
                    backboneRuns++;
                    modelRunNanos += System.nanoTime() - runStart;
                    long samplingStart = System.nanoTime();
                    remain -= sampler.update(logits, codes, frames, S, target, k, random);
                    samplingNanos += System.nanoTime() - samplingStart;
                    check(cancelled);
                    progress.update("本机合成 · " + (step + 1) + " / " + steps + " 步");
                }
                if (tensors.metadataPinned) prePinnedChunks++;
                outputShapeFallbacks += tensors.metadataShapeFallbacks;
                outputCopyBytes += tensors.copiedOutputBytes;
        }
        return new EncodedChunk(text, codes, frames);
    }

    int decodedPinnedChunks;
    long decoderOutputCopyBytes;

    private VoicePrompt decodeChunk(OmniDecoderRun buffers, OrtSession decoder, OrtSession.RunOptions run,
            EncodedChunk chunk, VoicePrompt ref, PcmAudio.Writer writer, AtomicBoolean cancelled) throws Exception {
        long copied = buffers.copiedOutputBytes;
        int pinned = buffers.pinnedRuns;
        decoderRuns++;
        FloatBuffer samples = buffers.run(decoder, run, chunk.codes, chunk.frames, cancelled);
        decoderOutputCopyBytes += buffers.copiedOutputBytes - copied;
        decodedPinnedChunks += buffers.pinnedRuns - pinned;
        check(cancelled);
        if (samples == null || !samples.hasRemaining()) throw new IOException("模型返回空音频");
        if (samples.remaining() != Math.multiplyExact(chunk.frames, 960))
            throw new IOException("解码音频长度与模型帧数不一致，已停止保存不完整作品");
        boolean anchorEligible=ref==null&&chunk.frames>=75&&chunk.text.length()<=2000;
        double gain=ref!=null&&ref.rms<.1?ref.rms/.1:1;
        long writeStart=System.nanoTime();
        double rms=OmniPcmWriter.write(writer,samples,gain,anchorEligible,cancelled,OmniDuration.hasSpeech(chunk.text));
        pcmWriteNanos+=System.nanoTime()-writeStart;bufferedPcmChunks++;
        VoicePrompt anchor=anchorEligible?new VoicePrompt(chunk.text,chunk.codes.clone(),chunk.frames,Math.max(.1,rms)):null;
        check(cancelled);
        return anchor;
    }

    static double time(double t) {
        return .1 * t / (1 - .9 * t);
    }

    static void check(AtomicBoolean c) throws IOException {
        if (c.get() || Thread.currentThread().isInterrupted()) throw new IOException("已取消生成");
    }

    static void writeWav(File file, FloatBuffer samples, AtomicBoolean cancelled)
            throws IOException {
        int count = samples.remaining();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            out.write("RIFF".getBytes("US-ASCII"));
            le(out, 36 + count * 2, 4);
            out.write("WAVEfmt ".getBytes("US-ASCII"));
            le(out, 16, 4);
            le(out, 1, 2);
            le(out, 1, 2);
            le(out, 24000, 4);
            le(out, 48000, 4);
            le(out, 2, 2);
            le(out, 16, 2);
            out.write("data".getBytes("US-ASCII"));
            le(out, count * 2, 4);
            for (int i = 0; i < count; i++) {
                if ((i & 8191) == 0) check(cancelled);
                float value = samples.get();
                if (!Float.isFinite(value)) throw new IOException("模型返回无效音频");
                le(out, Math.round(Math.max(-1, Math.min(1, value)) * 32767), 2);
            }
        }
    }

    static void le(OutputStream out, int value, int bytes) throws IOException {
        for (int i = 0; i < bytes; i++) out.write((value >> (8 * i)) & 255);
    }
}
