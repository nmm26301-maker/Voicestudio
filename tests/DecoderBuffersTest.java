package com.voice.studio;

import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DecoderBuffersTest {
    static int checks;
    static volatile long sink;
    static void check(boolean ok,String label) { checks++;if(!ok)throw new AssertionError(label); }
    static OrtSession.SessionOptions options()throws OrtException {
        OrtSession.SessionOptions options=new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(1);options.setInterOpNumThreads(1);return options;
    }
    static FloatBuffer legacy(OrtEnvironment env,OrtSession session,OrtSession.RunOptions run,long[] codes,int frames)throws Exception {
        try(OnnxTensor input=OnnxTensor.createTensor(env,LongBuffer.wrap(codes),new long[]{8,1,frames});
                OrtSession.Result result=session.run(Collections.singletonMap("codes",input),run)) {
            return ((OnnxTensor)result.get(0)).getFloatBuffer();
        }
    }
    static void compare(OrtEnvironment env,File path,String kind)throws Exception {
        AtomicBoolean cancel=new AtomicBoolean();
        OmniDecoderRun buffers=new OmniDecoderRun(env);
        try(OrtSession.SessionOptions options=options();OrtSession session=env.createSession(path.getAbsolutePath(),options);
                OrtSession.RunOptions run=new OrtSession.RunOptions();OmniDecoderRun ignored=buffers) {
            for(int frames:new int[]{1,1,50,50,50,250,250,3,3}) {
                long[] codes=new long[8*frames];Arrays.fill(codes,checks%31);
                if(kind.equals("variable"))codes[0]=12+checks%37;
                long[] before=codes.clone();
                FloatBuffer old=legacy(env,session,run,codes,frames),current=buffers.run(session,run,codes,frames,cancel);
                check(current.isReadOnly(),"read only output");
                check(Arrays.equals(before,codes),"caller input unchanged");
                check(old.remaining()==current.remaining(),"observed output shape preserved");
                for(int i=0;i<old.remaining();i++)check(Float.floatToRawIntBits(old.get(i))==Float.floatToRawIntBits(current.get(i)),"exact current decoder samples");
            }
            if(kind.equals("float"))check(buffers.pinnedRuns==5&&buffers.shapeFallbacks==0,"repeated frame lengths actually use pinned output");
            if(kind.equals("half"))check(buffers.pinnedRuns==0&&buffers.shapeFallbacks==0,"half output never incorrectly float-pinned");
            if(kind.equals("variable"))check(buffers.shapeFallbacks==4 && buffers.pinnedRuns==0,"unstable shapes recover once per frame group, then stop retrying");
            int fallbacks=buffers.shapeFallbacks;long copied=buffers.copiedOutputBytes;
            cancel.set(true);boolean failed=false;
            try{buffers.run(session,run,new long[24],3,cancel);}catch(IOException expected){failed=true;}
            check(failed&&buffers.shapeFallbacks==fallbacks&&buffers.copiedOutputBytes==copied,"flag cancellation never runs or probes");
            cancel.set(false);Thread.currentThread().interrupt();failed=false;
            try{buffers.run(session,run,new long[24],3,cancel);}catch(IOException expected){failed=true;}
            check(failed&&Thread.currentThread().isInterrupted(),"interrupt retained without model retry");Thread.interrupted();
            run.setTerminate(true);failed=false;
            try{buffers.run(session,run,new long[24],3,cancel);}catch(OrtException expected){failed=true;}
            check(failed&&buffers.shapeFallbacks==fallbacks,"native cancellation not mistaken for output-shape error");
            run.setTerminate(false);long[] retry=new long[24];retry[0]=19;
            check(buffers.run(session,run,retry,3,cancel).hasRemaining(),"retry succeeds after cancelled native run");
            failed=false;
            try{buffers.run(session,run,new long[7],1,cancel);}catch(IOException expected){failed=true;}
            check(failed,"invalid input length rejected");
        }
        buffers.close();boolean closed=false;
        try{buffers.run(null,null,new long[8],1,new AtomicBoolean());}catch(IllegalStateException expected){closed=true;}
        check(closed,"closed helper rejects use and double close is safe");
    }
    static long timed(OrtEnvironment env,OrtSession session,int frames,boolean old)throws Exception {
        long[] codes=new long[8*frames];Arrays.fill(codes,17);long start=System.nanoTime();
        try(OrtSession.RunOptions run=new OrtSession.RunOptions();OmniDecoderRun buffers=new OmniDecoderRun(env)) {
            for(int i=0;i<12;i++) {
                codes[0]=i;
                FloatBuffer samples=old?legacy(env,session,run,codes,frames):buffers.run(session,run,codes,frames,new AtomicBoolean());
                sink+=samples.remaining()+Float.floatToRawIntBits(samples.get(0));
            }
            if(!old)check(buffers.pinnedRuns==11&&buffers.copiedOutputBytes==frames*480L*4,"benchmark genuinely skips eleven output copies");
        }
        return System.nanoTime()-start;
    }
    static void benchmark(OrtEnvironment env,File path)throws Exception {
        System.out.println("{\"scope\":\"Host authored decoder graph; 12 equal-frame decodes including tensor preparation/copy/close; excludes model load, PCM write, real speech weights and phone\",\"warmups\":6,\"samples\":11,\"cases\":[");
        try(OrtSession.SessionOptions options=options();OrtSession session=env.createSession(path.getAbsolutePath(),options)) {
            boolean comma=false;
            for(int frames:new int[]{50,250,500}) {
                for(int w=0;w<6;w++){timed(env,session,frames,true);timed(env,session,frames,false);}
                long[] a=new long[11],b=new long[11];
                for(int i=0;i<11;i++){if((i&1)==0){a[i]=timed(env,session,frames,true);b[i]=timed(env,session,frames,false);}else{b[i]=timed(env,session,frames,false);a[i]=timed(env,session,frames,true);}}
                Arrays.sort(a);Arrays.sort(b);if(comma)System.out.println(",");comma=true;
                System.out.printf(Locale.US,"{\"frames\":%d,\"oldMedianMs\":%.6f,\"newMedianMs\":%.6f,\"reductionPercent\":%.2f,\"outputCopyBytesAvoided\":%d}",frames,a[5]/1e6,b[5]/1e6,100*(1-(double)b[5]/a[5]),11L*frames*480*4);
            }
        }
        System.out.println("\n]}");
    }
    public static void main(String[] args)throws Exception {
        OrtEnvironment env=OrtEnvironment.getEnvironment();env.setTelemetry(false);File folder=new File(args[0]);
        if(args.length>1){benchmark(env,new File(folder,"float.onnx"));return;}
        for(String kind:new String[]{"float","half","variable"})compare(env,new File(folder,kind+".onnx"),kind);
        System.out.println("Native decoder buffers: "+checks+" checks passed; exact sample bits, input updates, changing frames/content-dependent shape, half output, cancellation/retry and closure.");
    }
}
