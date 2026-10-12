package com.voice.studio;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GeneratedEngineTest {
    static int checks;
    static void check(boolean value,String label) { checks++;if(!value)throw new AssertionError(label); }
    static String text() {
        StringBuilder out=new StringBuilder();
        for(int i=0;i<7;i++)out.append("A calm voice walks across the stars. We keep the same voice. ");
        return out.toString();
    }
    static void compare(File models,File folder,String text,VoicePrompt ref,int steps,float speed)throws Exception {
        OmniEngine engine=new OmniEngine(); LegacyOmniEngine156 old=new LegacyOmniEngine156();
        File before=new File(folder,"before.wav"),after=new File(folder,"after.wav");
        List<String> progress=new ArrayList<>();
        old.generate(models,text,"en",speed,before,new AtomicBoolean(),m->{},ref,"None",steps,42);
        engine.generate(models,text,"en",speed,after,new AtomicBoolean(),progress::add,ref,"None",steps,42);
        int chunks=OmniEngine.split(text,speed,ref).size();
        check(engine.chunkCount==chunks,"all chunks generated");
        int first=ref==null&&engine.automaticPrompt!=null?1:0;
        int expected=first+(chunks-first+127)/128;
        check(engine.backboneLoads==expected&&engine.decoderLoads==expected,"native sessions actually reused");
        if (chunks>32) {
            check(engine.backboneLoads==old.backboneLoads,"same model scheduling as 1.5.6");
            check(engine.decodedPinnedChunks>0 && engine.decoderOutputCopyBytes>0,"actual decoder reuses observed output buffers");
            System.out.println("Decoder reuse: pinned chunks="+engine.decodedPinnedChunks+", copied bytes="+engine.decoderOutputCopyBytes);
            System.out.println("Native reload fixture: "+chunks+" chunks, old loads="+(old.backboneLoads+old.decoderLoads)+
                    ", new loads="+(engine.backboneLoads+engine.decoderLoads)+"; not real model timing.");
        }
        check(engine.prePinnedChunks==chunks && engine.outputCopyBytes==0,"first-run logits use direct pinned storage");
        check(engine.lastGenerationMillis>0&&engine.loadNanos>0&&engine.inferenceNanos>0&&engine.decodeNanos>0,
                "stage timings reflect executed operations");
        check(engine.modelRunNanos>0 && engine.samplingNanos>0 &&
                engine.inferenceNanos>=engine.modelRunNanos+engine.samplingNanos,
                "separate model/sampler timings are actual subsets of inference");
        if (Runtime.getRuntime().availableProcessors()>1)
            check(engine.samplingWorkers>=2 && engine.samplingWorkers<=4 && engine.parallelSamplingRounds>0,
                    "native engine actually uses bounded parallel scoring");
        check(progress.stream().anyMatch(p->p.contains("本机合成 · 0 /")),"start is zero completed steps");
        check(progress.stream().filter(p->p.contains("本机合成 · "+steps+" / "+steps)).count()==chunks,
                "each final step reported after run");
        try(PcmAudio.Reader a=new PcmAudio.Reader(before);PcmAudio.Reader b=new PcmAudio.Reader(after)) {
            check(a.rate==b.rate&&a.frames==b.frames,"duration, sample rate and pauses preserved");
            for(double at=0;at<a.seconds();at+=30) {
                float[] x=a.read(at,Math.min(30,a.seconds()-at));float[] y=b.read(at,Math.min(30,b.seconds()-at));
                check(x.length==y.length,"same sample count");
                for(int i=0;i<x.length;i++)check(x[i]==y[i],"exact same PCM as previous release");
            }
        }
        if(ref==null && old.automaticPrompt!=null) {
            check(engine.automaticPrompt!=null,"automatic anchor established");
            check(engine.automaticPrompt.text.equals(old.automaticPrompt.text)&&
                    Arrays.equals(engine.automaticPrompt.codes,old.automaticPrompt.codes),"same first anchor text and codes");
            check(engine.automaticPrompt.rms==old.automaticPrompt.rms,"same reference loudness");
        } else check(engine.automaticPrompt==null,"provided reference not replaced");
        check(engine.running==null,"native run handle released");
    }
    public static void main(String[] args)throws Exception {
        File models=new File(args[0]); File folder=Files.createTempDirectory("staged-engine").toFile();
        try {
            compare(models,folder,"Hello there.",null,16,1);
            compare(models,folder,text(),null,16,1);
            VoicePrompt ref=new VoicePrompt("A calm voice walks across the stars. We keep the same voice.",new long[8*150],150,.05);
            compare(models,folder,text(),ref,16,1.3f);
            compare(models,folder,"Hello there.",ref,32,1);
            StringBuilder longText=new StringBuilder();
            for(int i=0;i<80;i++)longText.append("A calm voice walks across the stars. We keep the same voice. ");
            String longInput=longText.toString();
            int longChunks=OmniEngine.split(longInput,1,ref).size();
            check(longChunks>32 && longChunks<=128,"fixture crosses old reload boundary");
            compare(models,folder,longInput,ref,8,1);
            int previousRuns=0;
            for (int steps : new int[]{8,16,32}) {
                OmniEngine counted = new OmniEngine();
                counted.generate(models,"Hello there.","en",1,new File(folder,"counted.wav"),
                        new AtomicBoolean(),m->{},ref,"None",steps,42);
                check(counted.backboneRuns==steps,"native model run count matches chosen steps");
                if (previousRuns>0) check(counted.backboneRuns==previousRuns*2,"doubling steps doubles actual model runs");
                previousRuns=counted.backboneRuns;
            }
            for (VoicePrompt voice : new VoicePrompt[]{null, ref}) {
                OmniEngine fast = new OmniEngine(); File wav = new File(folder,"eight.wav");
                List<String> updates = new ArrayList<>();
                fast.generate(models,text(),"en",1,wav,new AtomicBoolean(),updates::add,voice,"None",8,42);
                int chunks = OmniEngine.split(text(),1,voice).size();
                check(fast.chunkCount==chunks && chunks>1,"8 steps supports multi-chunk synthesis");
                check(updates.stream().filter(p->p.contains("本机合成 · 8 / 8 步")).count()==chunks,
                        "every 8-step chunk finishes");
                check(updates.stream().noneMatch(p->p.matches(".*本机合成 · (9|1[0-9]|2[0-9]|3[0-9]) /.*")),
                        "8-step mode performs no later step");
                try(PcmAudio.Reader pcm=new PcmAudio.Reader(wav)) {
                    check(pcm.rate==24000 && pcm.seconds()>0,"8-step output has valid PCM");
                }
                check(voice==null ? fast.automaticPrompt!=null : fast.automaticPrompt==null,
                        "8-step automatic/reference voice ownership");
                check(fast.running==null && fast.modelRunNanos>0 && fast.samplingNanos>0,
                        "8-step model/sampler executed and run handle released");
            }
            OmniEngine engine=new OmniEngine();AtomicBoolean cancel=new AtomicBoolean();
            boolean invalid=false;
            try {
                engine.generate(models,"Hello there.","en",1,new File(folder,"invalid.wav"),cancel,m->{},ref,"None",4,42);
            } catch(IOException expected) { invalid=true; }
            check(invalid && !new File(folder,"invalid.wav").exists(),"unsupported steps rejected before file or model allocation");
            boolean failed=false;
            try {
                engine.generate(models,text(),"en",1,new File(folder,"cancelled.wav"),cancel,
                        m->{ if(m.contains("本机合成 · 1 /")) { cancel.set(true);engine.cancel(); } },ref,"None",16,42);
            } catch(IOException expected) { failed=true; }
            check(failed&&engine.decoderLoads==0&&engine.running==null,"cancelled inference closes session before any decoder");
            cancel.set(false);
            engine.generate(models,"Hello there.","en",1,new File(folder,"retry.wav"),cancel,m->{},ref,"None",16,42);
            check(engine.lastGenerationMillis>0&&engine.backboneLoads==1&&engine.decoderLoads==1,"same engine retries after cancellation");
        } finally { for(File file:folder.listFiles())file.delete();folder.delete(); }
        System.out.println("Native staged engine: "+checks+" checks passed against frozen 1.5.6. Synthetic graphs, not speech weights / phone benchmark.");
    }
}
