package com.voice.studio;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StagedEngineTest {
    static int checks;
    static void check(boolean value,String label) { checks++;if(!value)throw new AssertionError(label); }
    static String text() {
        StringBuilder out=new StringBuilder();
        for(int i=0;i<7;i++)out.append("A calm voice walks across the stars. We keep the same voice. ");
        return out.toString();
    }
    static void compare(File models,File folder,String text,VoicePrompt ref,int steps,float speed)throws Exception {
        OmniEngine engine=new OmniEngine(); LegacyOmniEngine140 old=new LegacyOmniEngine140();
        File before=new File(folder,"before.wav"),after=new File(folder,"after.wav");
        List<String> progress=new ArrayList<>();
        old.generate(models,text,"en",speed,before,new AtomicBoolean(),m->{},ref,"None",steps,42);
        engine.generate(models,text,"en",speed,after,new AtomicBoolean(),progress::add,ref,"None",steps,42);
        int chunks=OmniEngine.split(text,speed,ref).size();
        check(engine.chunkCount==chunks,"all chunks generated");
        int first=ref==null&&engine.automaticPrompt!=null?1:0;
        int expected=first+(chunks-first+31)/32;
        check(engine.backboneLoads==expected&&engine.decoderLoads==expected,"native sessions actually reused");
        check(engine.lastGenerationMillis>0&&engine.loadNanos>0&&engine.inferenceNanos>0&&engine.decodeNanos>0,
                "stage timings reflect executed operations");
        check(progress.stream().anyMatch(p->p.contains("本机合成 · 0 /")),"start is zero completed steps");
        check(progress.stream().filter(p->p.contains("本机合成 · "+steps+" / "+steps)).count()==chunks,
                "each final step reported after run");
        try(PcmAudio.Reader a=new PcmAudio.Reader(before);PcmAudio.Reader b=new PcmAudio.Reader(after)) {
            check(a.rate==b.rate&&a.frames==b.frames,"duration, sample rate and pauses preserved");
            for(double at=0;at<a.seconds();at+=30) {
                float[] x=a.read(at,Math.min(30,a.seconds()-at));float[] y=b.read(at,Math.min(30,b.seconds()-at));
                check(x.length==y.length,"same sample count");
                for(int i=0;i<x.length;i++)check(Math.abs(x[i]-y[i])<=2f/32768,"direct PCM avoids second quantization; within 2 LSB");
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
            OmniEngine engine=new OmniEngine();AtomicBoolean cancel=new AtomicBoolean();
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
        System.out.println("Native staged engine: "+checks+" checks passed against frozen 1.4.0. Synthetic graphs, not speech weights / phone benchmark.");
    }
}
