package com.voice.studio;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PcmWriterTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static double previous(PcmAudio.Writer writer,FloatBuffer source,double gain,boolean rms)throws Exception {
        float[] pcm=new float[source.remaining()];source.get(pcm);
        if(gain!=1)for(int i=0;i<pcm.length;i++)pcm[i]*=gain;
        double value=rms?PcmAudio.rms(pcm):0;
        PcmAudio.fade(pcm);writer.write(pcm);return value;
    }
    static FloatBuffer buffer(float[] values,boolean direct) {
        FloatBuffer b=direct?ByteBuffer.allocateDirect((values.length+9)*4).order(ByteOrder.nativeOrder()).asFloatBuffer():FloatBuffer.allocate(values.length+9);
        b.position(5);b.put(values);b.limit(5+values.length);b.position(5);return b.asReadOnlyBuffer();
    }
    static void same(File root,float[] values,double gain,boolean rms,boolean direct)throws Exception {
        File a=new File(root,"old.wav"),b=new File(root,"new.wav");FloatBuffer x=buffer(values,direct),y=buffer(values,direct);
        double expected,actual;
        try(PcmAudio.Writer w=new PcmAudio.Writer(a,24000)){expected=previous(w,x,gain,rms);}
        try(PcmAudio.Writer w=new PcmAudio.Writer(b,24000)){actual=OmniPcmWriter.write(w,y,gain,rms,new AtomicBoolean());}
        check(Double.doubleToLongBits(expected)==Double.doubleToLongBits(actual),"RMS bit-for-bit match "+values.length);
        check(Arrays.equals(Files.readAllBytes(a.toPath()),Files.readAllBytes(b.toPath())),"entire WAV byte match "+values.length);
        check(y.position()==y.limit(),"only requested buffer range consumed");
    }
    static long bench(File path,FloatBuffer source,boolean old)throws Exception {
        source.position(5);long began=System.nanoTime();try(PcmAudio.Writer w=new PcmAudio.Writer(path,24000)){
            if(old)previous(w,source,.5,false);else OmniPcmWriter.write(w,source,.5,false,new AtomicBoolean());
        }return System.nanoTime()-began;
    }
    public static void main(String[] args)throws Exception {
        File root=Files.createTempDirectory("pcm-writer").toFile(),output=new File(root,"rollback.wav");
        try {
            Random random=new Random(1512);
            for(int count:new int[]{0,1,2,3,239,240,479,480,481,4095,4096,4097,8193,16383,16384,16385,240000}) {
                float[] values=new float[count];for(int i=0;i<count;i++)values[i]=(random.nextFloat()-.5f)*4;
                for(double gain:new double[]{0,.1,.5,1})for(boolean direct:new boolean[]{false,true})for(boolean rms:new boolean[]{false,true})same(root,values,gain,rms,direct);
            }
            float[] edge={Float.MIN_VALUE,-Float.MIN_VALUE,0,-0f,1,-1,.00001f,1.0001f,-1.0001f,Float.MAX_VALUE,-Float.MAX_VALUE};
            for(double gain:new double[]{0,.1,1})same(root,edge,gain,true,true);
            for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
                float[] values=new float[32769];Arrays.fill(values,.1f);values[32768]=bad;
                FloatBuffer source=buffer(values,true);
                try(PcmAudio.Writer w=new PcmAudio.Writer(output,24000)) {
                    w.write(new float[]{.2f,.3f});long bytes=w.f.length(),count=w.count;boolean failed=false;
                    try{OmniPcmWriter.write(w,source,1,false,new AtomicBoolean());}catch(IOException expected){failed=true;}
                    check(failed,"nonfinite rejected after earlier full blocks");check(w.f.length()==bytes&&w.count==count,"failed chunk rolled back, earlier samples retained");
                    check(source.position()==5,"failed source range reset");
                }
            }
            for(boolean interrupt:new boolean[]{false,true}) {
                FloatBuffer source=buffer(new float[8193],true);AtomicBoolean cancel=new AtomicBoolean(!interrupt);
                try(PcmAudio.Writer w=new PcmAudio.Writer(output,24000)) {
                    if(interrupt)Thread.currentThread().interrupt();boolean failed=false;
                    try{OmniPcmWriter.write(w,source,1,false,cancel);}catch(InterruptedIOException expected){failed=true;}finally{Thread.interrupted();}
                    check(failed&&w.count==0&&w.f.length()==44&&source.position()==5,"cancel/interruption leaves no partial samples");
                }
            }
            // Monitor a real output write to cancel after at least one completed block.
            float[] large=new float[2400000];Arrays.fill(large,.1f);FloatBuffer input=buffer(large,true);AtomicBoolean cancel=new AtomicBoolean();
            try(PcmAudio.Writer w=new PcmAudio.Writer(output,24000)) {
                Thread watcher=new Thread(()->{try{while(w.f.length()<=44&&!Thread.currentThread().isInterrupted())Thread.yield();cancel.set(true);}catch(IOException e){throw new RuntimeException(e);}});
                watcher.start();boolean failed=false;
                try{OmniPcmWriter.write(w,input,1,false,cancel);}catch(InterruptedIOException expected){failed=true;}finally{watcher.interrupt();watcher.join(5000);}
                check(failed&&!watcher.isAlive(),"mid-write cancellation stops and joins monitor");check(w.count==0&&w.f.length()==44&&input.position()==5,"mid-write cancellation rolls back chunk");
            }
            FloatBuffer inputBench=buffer(new float[240000],true);File benchmark=new File(root,"bench.wav");
            for(int i=0;i<8;i++){bench(benchmark,inputBench,true);bench(benchmark,inputBench,false);}
            double[] old=new double[15],fast=new double[15];for(int i=0;i<15;i++){old[i]=bench(benchmark,inputBench,true)/1e6;fast[i]=bench(benchmark,inputBench,false)/1e6;}
            Arrays.sort(old);Arrays.sort(fast);System.out.printf(Locale.ROOT,"Host 10s decoder PCM write median: previous=%.3f ms; bounded=%.3f ms; reusable sample+byte buffers=%d bytes (not model/phone speed).%n",old[7],fast[7],OmniPcmWriter.BUFFER_BYTES);
        }finally{for(File f:root.listFiles())f.delete();root.delete();}
        System.out.println("Bounded PCM writer: "+checks+" exact-WAV/RMS/range/rollback/cancellation checks passed.");
    }
}
