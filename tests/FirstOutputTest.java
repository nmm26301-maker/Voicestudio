package com.voice.studio;

import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class FirstOutputTest {
    static int checks;
    static volatile long sink;
    static void check(boolean ok,String label) { checks++;if(!ok)throw new AssertionError(label); }
    static OrtSession.SessionOptions options() throws OrtException {
        OrtSession.SessionOptions options=new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(1);options.setInterOpNumThreads(1);return options;
    }
    static void compare(OrtEnvironment env,File path,int sequence,String kind) throws Exception {
        long[] ids=new long[16*sequence],att=new long[2*sequence],pos=new long[2*sequence];
        byte[] mask=new byte[2*sequence];Arrays.fill(att,1);Arrays.fill(mask,(byte)1);
        try (OrtSession.SessionOptions options=options();
                OrtSession session=env.createSession(path.getAbsolutePath(),options);
                OrtSession.RunOptions run=new OrtSession.RunOptions();
                OmniTensorRun fast=new OmniTensorRun(env,sequence,ids,mask,att,pos);
                LegacyTensorRun155 old=new LegacyTensorRun155(env,sequence,ids,mask,att,pos)) {
            for(int step=0;step<6;step++) {
                for(int i=0;i<ids.length;i++)ids[i]=(step%3)*21+i%137;
                FloatBuffer a=old.run(session,run,ids),b=fast.run(session,run,ids);
                check(a.remaining()==b.remaining(),"same dynamic output length");
                check(b.isReadOnly(),"caller cannot corrupt pinned buffer");
                for(int i=0;i<a.remaining();i++)check(a.get(i)==b.get(i),"first and later outputs equal frozen 1.5.5");
                if("static".equals(kind)) check(fast.metadataPinned && fast.copiedOutputBytes==0,"static output pre-pinned without copying");
                if("unnamed".equals(kind)) check(!fast.metadataPinned && fast.copiedOutputBytes>0,"unnamed dimensions use actual output shape");
                if("half".equals(kind)) check(!fast.metadataPinned && fast.copiedOutputBytes==(long)(step+1)*a.remaining()*4,"half conversion remains compatible");
                if("nonconforming".equals(kind)) check(!fast.metadataPinned && fast.metadataShapeFallbacks==1,
                        "badly reused symbol recovers via actual shape exactly once");
            }
        }
    }
    static void cancelledFirstRun(OrtEnvironment env,File path) throws Exception {
        int sequence=7;long[] ids=new long[16*sequence],att=new long[2*sequence],pos=new long[2*sequence];byte[] mask=new byte[2*sequence];
        try (OrtSession.SessionOptions options=options();
                OrtSession session=env.createSession(path.getAbsolutePath(),options);
                OrtSession.RunOptions run=new OrtSession.RunOptions();
                OmniTensorRun tensors=new OmniTensorRun(env,sequence,ids,mask,att,pos)) {
            run.setTerminate(true);boolean cancelled=false;
            try { tensors.run(session,run,ids); } catch(OrtException expected) { cancelled=true; }
            check(cancelled && tensors.metadataShapeFallbacks==0,"cancel does not probe or rerun native inference");
            run.setTerminate(false);ids[0]=123;
            check(tensors.run(session,run,ids).get(0)==123,"preallocated buffers retry after cancellation");
            check(tensors.metadataPinned && tensors.copiedOutputBytes==0,"retry remains direct");
        }
    }
    static long first(OrtEnvironment env,OrtSession session,int sequence,boolean legacy) throws Exception {
        long[] ids=new long[16*sequence],att=new long[2*sequence],pos=new long[2*sequence];byte[] mask=new byte[2*sequence];
        Arrays.fill(ids,1024);Arrays.fill(att,1);Arrays.fill(mask,(byte)1);
        long start=System.nanoTime();
        try(OrtSession.RunOptions run=new OrtSession.RunOptions()) {
            if(legacy) try(LegacyTensorRun155 tensors=new LegacyTensorRun155(env,sequence,ids,mask,att,pos)) {
                FloatBuffer output=tensors.run(session,run,ids);
                sink+=(long)output.get(0)+output.remaining();
            } else try(OmniTensorRun tensors=new OmniTensorRun(env,sequence,ids,mask,att,pos)) {
                FloatBuffer output=tensors.run(session,run,ids);
                check(tensors.metadataPinned && tensors.copiedOutputBytes==0,"benchmark actually avoids copy");
                sink+=(long)output.get(0)+output.remaining();
            }
        }
        return System.nanoTime()-start;
    }
    static void benchmark(OrtEnvironment env,File path) throws Exception {
        System.out.println("{\"scope\":\"Host native authored broadcast graph; first tensor setup/run/close with loaded session; excludes real model loads/phone\",\"warmups\":4,\"samples\":9,\"results\":[");
        boolean first=true;
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(1);options.setInterOpNumThreads(1);
            try(OrtSession session=env.createSession(path.getAbsolutePath(),options)) {
                for(int sequence:new int[]{64,250,850}) {
                    for(int w=0;w<4;w++){first(env,session,sequence,true);first(env,session,sequence,false);}
                    long[] a=new long[9],b=new long[9];
                    for(int i=0;i<9;i++) {
                        if(i%2==0){a[i]=first(env,session,sequence,true);b[i]=first(env,session,sequence,false);}
                        else {b[i]=first(env,session,sequence,false);a[i]=first(env,session,sequence,true);}
                    }
                    Arrays.sort(a);Arrays.sort(b);
                    if(!first)System.out.println(",");first=false;
                    System.out.printf(Locale.US,"{\"sequence\":%d,\"oldMedianMillis\":%.3f,\"newMedianMillis\":%.3f,\"reductionPercent\":%.2f,\"firstCopyBytesAvoided\":%d}",
                            sequence,a[4]/1e6,b[4]/1e6,100*(1-(double)b[4]/a[4]),16L*sequence*1025*4);
                    System.out.flush();
                }
            }
        }
        System.out.println("\n]}");
    }
    public static void main(String[] args) throws Exception {
        OrtEnvironment env=OrtEnvironment.getEnvironment();env.setTelemetry(false);
        if(args.length>1 && "--benchmark".equals(args[1])) { benchmark(env,new File(args[0]));return; }
        File folder=new File(args[0]);
        for(String kind:new String[]{"static","unnamed","half","nonconforming"})
            compare(env,new File(folder,kind+".onnx"),"static".equals(kind)?7:13,kind);
        cancelledFirstRun(env,new File(folder,"static.onnx"));
        System.out.println("Native first output: "+checks+" checks passed against frozen 1.5.5; metadata/static pins, unknown/half/bad-symbol fallback, exact outputs and first-run cancellation/retry.");
    }
}
