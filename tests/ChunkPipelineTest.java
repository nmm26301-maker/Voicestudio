package com.voice.studio;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkPipelineTest {
    static int checks;
    static void check(boolean value,String label) { checks++; if(!value)throw new AssertionError(label); }
    static class Fixture implements OmniChunkPipeline.Factory<Integer,Integer> {
        final AtomicBoolean cancelled;
        final List<Integer> output=new ArrayList<>();
        final List<Integer> inputs=new ArrayList<>();
        int active, encoders, decoders, anchorAt, failEncode=-1, failDecode=-1,
                cancelEncode=-1, cancelDecode=-1, cancelAfterEncoder=-1, maxPending, pending;
        Fixture(AtomicBoolean cancelled,int anchorAt) { this.cancelled=cancelled;this.anchorAt=anchorAt; }
        public OmniChunkPipeline.Encoder<Integer,Integer> encoder() {
            check(active==0,"decoder released before encoder");active=1;encoders++;
            return new OmniChunkPipeline.Encoder<Integer,Integer>() {
                public Integer encode(int index,Integer voice)throws Exception {
                    check(active==1,"encoding owns session");
                    if(index==failEncode)throw new IOException("encode failure");
                    inputs.add(voice==null?-1:voice);
                    if(index==cancelEncode)cancelled.set(true);
                    maxPending=Math.max(maxPending,++pending);
                    return index*37+(voice==null?0:voice);
                }
                public void close() {
                    check(active==1,"encoder closed exactly once");active=0;
                    if(encoders==cancelAfterEncoder)cancelled.set(true);
                }
            };
        }
        public OmniChunkPipeline.Decoder<Integer,Integer> decoder() {
            check(active==0,"backbone released before decoder");active=2;decoders++;
            return new OmniChunkPipeline.Decoder<Integer,Integer>() {
                public Integer decode(int index,Integer codes,Integer voice)throws Exception {
                    check(active==2,"decoding owns session");
                    if(index==failDecode)throw new IOException("decode failure");
                    check(codes==index*37+(voice==null?0:voice),"codes and reference stay paired");
                    output.add(index);pending--;
                    if(index==cancelDecode)cancelled.set(true);
                    return index>=anchorAt?1000+index:null;
                }
                public void close() { check(active==2,"decoder closed exactly once");active=0; }
            };
        }
    }
    public static void main(String[] args)throws Exception {
        for(int count:new int[]{0,1,2,3,10,31,32,33,64,65,110})
            for(int anchor:new int[]{0,1,3,200}) for(boolean provided:new boolean[]{false,true}) {
                AtomicBoolean cancel=new AtomicBoolean();Fixture f=new Fixture(cancel,anchor);
                Integer ref=provided?777:null;
                Integer result=OmniChunkPipeline.run(count,ref,cancel,f);
                int single=provided?0:Math.min(count,anchor+1);
                int remaining=count-single;
                int loads=single+(remaining+31)/32;
                check(f.encoders==loads&&f.decoders==loads,"expected bounded load count");
                check(f.maxPending<=32,"bounded code staging");check(f.active==0,"no resident sessions");
                check(f.output.size()==count,"no lost or duplicate chunks");
                for(int i=0;i<count;i++) {
                    check(f.output.get(i)==i,"original output order");
                    check(f.inputs.get(i)==(provided?777:i<=anchor?-1:1000+anchor),"first usable voice stays fixed");
                }
                check(Objects.equals(result,provided?Integer.valueOf(777):count>anchor?Integer.valueOf(1000+anchor):null),"returned automatic anchor");
            }
        for(int failure=0;failure<6;failure++) {
            AtomicBoolean cancel=new AtomicBoolean();Fixture f=new Fixture(cancel,0);
            if(failure==0)cancel.set(true);
            if(failure==1)f.failEncode=3;
            if(failure==2)f.failDecode=3;
            if(failure==3)f.cancelEncode=3;
            if(failure==4)f.cancelDecode=3;
            if(failure==5)f.cancelAfterEncoder=1;
            boolean failed=false;
            try { OmniChunkPipeline.run(10,777,cancel,f); } catch(IOException expected) { failed=true; }
            check(failed,"failure/cancellation propagated");check(f.active==0,"failure closes owned session");
            if(failure==0||failure==3||failure==5)check(f.decoders==0,"cancelled codes never decoded");
        }
        System.out.println("Staged chunk pipeline: "+checks+" checks passed; 10 reference chunks use 2 loads vs 20, automatic anchor uses 4 vs 20. Counts are not phone timing.");
    }
}
