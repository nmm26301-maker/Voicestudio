package com.voice.studio;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ParallelSamplerTest {
    static int checks;
    static volatile long sink;
    static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    static FloatBuffer logits(int frames, int prefix, int kind, int mode) {
        int size=16*(frames+prefix)*1025;
        FloatBuffer data=kind==0 ? FloatBuffer.allocate(size) : ByteBuffer.allocateDirect((size+13)*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        if (kind!=0) { data.position(13); data=data.slice(); }
        Random random=new Random(155);
        for (int i=0;i<size;i++) data.put(i,mode==0 ? 0 : (random.nextFloat()-.5f)*(mode==1 ? 12 : 2000));
        if (mode==3) for (int i=0;i<size;i+=503) data.put(i,i%2==0 ? Float.NaN : Float.POSITIVE_INFINITY);
        data.position(23);
        return kind==2 ? data.asReadOnlyBuffer() : data;
    }
    static Random rng(boolean tied) {
        return tied ? new Random(42) { public double nextDouble() { return .5; } } : new Random(42);
    }
    static void compare(int frames, int prefix, int steps, int kind, int mode, int threads, boolean tied) throws Exception {
        FloatBuffer data=logits(frames,prefix,kind,mode);
        int total=8*frames, remaining=total, position=data.position(), limit=data.limit();
        long[] a=new long[total], b=new long[total]; Arrays.fill(a,1024);Arrays.fill(b,1024);
        Random r1=rng(tied), r2=rng(tied);
        LegacySampler154 old=new LegacySampler154(total);
        try (OmniSamplerWorkers workers=new OmniSamplerWorkers(threads,new AtomicBoolean())) {
            OmniStepSampler fast=new OmniStepSampler(total,workers);
            for (int step=0;step<steps;step++) {
                double x=.1*step/steps/(1-.9*step/steps), y=.1*(step+1)/steps/(1-.9*(step+1)/steps);
                int k=step==steps-1 ? remaining : Math.min(remaining,(int)Math.ceil(total*(y-x)));
                int n1=old.update(data,a,frames,frames+prefix,prefix,k,r1);
                int n2=fast.update(data,b,frames,frames+prefix,prefix,k,r2);
                check(n1==n2,"same accepted count");check(Arrays.equals(a,b),"same tokens after every step");
                check(r1.nextLong()==r2.nextLong(),"same RNG state");
                check(data.position()==position && data.limit()==limit,"caller buffer unchanged");
                remaining-=n1;
            }
            check(remaining==0,"complete trajectory");
            check(workers.largestWorkerCount<=Math.min(4,Math.max(1,threads)),"worker cap");
            if (total>=128 && threads>1) check(workers.parallelRounds>0,"parallel path actually executed");
        }
    }
    static void edges() throws Exception {
        int frames=40,prefix=150,total=8*frames;
        FloatBuffer data=logits(frames,prefix,2,1);
        try (OmniSamplerWorkers workers=new OmniSamplerWorkers(4,new AtomicBoolean())) {
            OmniStepSampler fast=new OmniStepSampler(total,workers);
            for (int k:new int[]{0,1,64,160,320,999}) {
                long[] a=new long[total];for (int i=0;i<total;i++) a[i]=i%2==0 ? 1024 : i%1024;
                long[] b=a.clone();Random r1=new Random(42),r2=new Random(42);
                int n1=new LegacySampler154(total).update(data,a,frames,frames+prefix,prefix,k,r1);
                int n2=fast.update(data,b,frames,frames+prefix,prefix,k,r2);
                check(n1==n2 && Arrays.equals(a,b),"partial, zero, all and excess k");
                check(r1.nextLong()==r2.nextLong(),"edge RNG state");
            }
            long[] done=new long[total];
            check(fast.update(data,done,frames,frames+prefix,prefix,0,new Random())==0,"already filled");
        }
        AtomicBoolean cancelled=new AtomicBoolean(true);
        try (OmniSamplerWorkers workers=new OmniSamplerWorkers(4,cancelled)) {
            long[] codes=new long[total];Arrays.fill(codes,1024);long[] before=codes.clone();
            Random random=new Random(42);boolean failed=false;
            try { new OmniStepSampler(total,workers).update(data,codes,frames,frames+prefix,prefix,10,random); }
            catch (IOException expected) { failed=true; }
            check(failed && Arrays.equals(before,codes),"cancel does not publish partial tokens");
            check(random.nextLong()==new Random(42).nextLong(),"cancel does not advance RNG");
            cancelled.set(false);
            check(new OmniStepSampler(total,workers).update(data,codes,frames,frames+prefix,prefix,total,random)==total,
                    "same workers reusable after cancellation");
        }
    }
    static void joinSafety(boolean interrupt) throws Exception {
        AtomicBoolean cancelled=new AtomicBoolean();
        OmniSamplerWorkers workers=new OmniSamplerWorkers(4,cancelled);
        CountDownLatch ready=new CountDownLatch(interrupt ? 4 : 3), release=new CountDownLatch(1), firstFailure=new CountDownLatch(1);
        AtomicInteger completed=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
        AtomicBoolean preservedInterrupt=new AtomicBoolean();
        Thread runner=new Thread(() -> {
            try {
                workers.run(256,(start,end,scratch) -> {
                    if (!interrupt && start==0) { ready.await();firstFailure.countDown();throw new IOException("fixture failure"); }
                    ready.countDown();release.await();completed.incrementAndGet();
                });
            } catch (Throwable error) { failure.set(error);preservedInterrupt.set(Thread.currentThread().isInterrupted()); }
        });
        runner.start();check(ready.await(5,TimeUnit.SECONDS),"readers started");
        if (interrupt) runner.interrupt(); else check(firstFailure.await(5,TimeUnit.SECONDS),"worker failure triggered");
        runner.join(50);check(runner.isAlive(),"caller waits for every buffer reader after error/interrupt");
        release.countDown();runner.join(5000);
        check(!runner.isAlive() && failure.get() instanceof IOException,"error delivered after joining readers");
        check(completed.get()==(interrupt ? 4 : 3),"all sibling readers exited");
        if (interrupt) check(preservedInterrupt.get(),"caller interrupt restored");
        workers.close();workers.close();boolean closed=false;
        try { workers.run(1,(start,end,scratch)->{}); } catch (IllegalStateException expected) { closed=true; }
        check(closed,"closed workers reject work");
    }
    static void scratchReuse() throws Exception {
        ConcurrentMap<Thread,double[]> seen=new ConcurrentHashMap<>();AtomicBoolean mismatch=new AtomicBoolean();
        try (OmniSamplerWorkers workers=new OmniSamplerWorkers(4,new AtomicBoolean())) {
            for (int round=0;round<12;round++) workers.run(256,(start,end,scratch)->{
                double[] previous=seen.putIfAbsent(Thread.currentThread(),scratch);
                if (previous!=null && previous!=scratch) mismatch.set(true);
                for (int i=0;i<scratch.length;i++) scratch[i]=start+i;
            });
            check(!mismatch.get() && seen.size()<=4,"generation reuses bounded scratch and threads");
            check(workers.parallelRounds==12 && workers.largestWorkerCount==4,"actual worker diagnostics");
        }
    }
    static long trajectory(boolean legacy, FloatBuffer data, int frames, int prefix, int steps,
            OmniSamplerWorkers workers) throws Exception {
        int total=8*frames,remaining=total;
        long[] codes=new long[total];Arrays.fill(codes,1024);Random random=new Random(42);
        LegacySampler154 old=legacy ? new LegacySampler154(total) : null;
        OmniStepSampler fast=legacy ? null : new OmniStepSampler(total,workers);
        long start=System.nanoTime();
        for (int step=0;step<steps;step++) {
            double a=.1*step/steps/(1-.9*step/steps),b=.1*(step+1)/steps/(1-.9*(step+1)/steps);
            int k=step==steps-1 ? remaining : Math.min(remaining,(int)Math.ceil(total*(b-a)));
            if (k<=0) continue;
            remaining-=legacy ? old.update(data,codes,frames,frames+prefix,prefix,k,random)
                    : fast.update(data,codes,frames,frames+prefix,prefix,k,random);
        }
        long elapsed=System.nanoTime()-start;
        check(remaining==0,"benchmark completed");sink^=Arrays.hashCode(codes);
        return elapsed;
    }
    static void benchmark() throws Exception {
        System.out.println("{\"scope\":\"Host warm complete Java sampling trajectories vs frozen 1.5.4; excludes ONNX/loading/decode/phone\",\"warmups\":4,\"samples\":9,\"maximumWorkers\":4,\"results\":[");
        boolean first=true;
        for (int frames:new int[]{100,250}) for (int steps:new int[]{8,16,32}) {
            int prefix=150;FloatBuffer data=logits(frames,prefix,2,1);
            try (OmniSamplerWorkers workers=new OmniSamplerWorkers(4,new AtomicBoolean())) {
                for (int i=0;i<4;i++) { trajectory(true,data,frames,prefix,steps,workers);trajectory(false,data,frames,prefix,steps,workers); }
                long[] a=new long[9],b=new long[9];
                for (int i=0;i<9;i++) {
                    if (i%2==0) { a[i]=trajectory(true,data,frames,prefix,steps,workers);b[i]=trajectory(false,data,frames,prefix,steps,workers); }
                    else { b[i]=trajectory(false,data,frames,prefix,steps,workers);a[i]=trajectory(true,data,frames,prefix,steps,workers); }
                }
                Arrays.sort(a);Arrays.sort(b);
                if (!first) System.out.println(",");first=false;
                System.out.printf(Locale.US,"{\"frames\":%d,\"prefixFrames\":%d,\"steps\":%d,\"oldMedianMillis\":%.3f,\"newMedianMillis\":%.3f,\"reductionPercent\":%.2f}",
                        frames,prefix,steps,a[4]/1e6,b[4]/1e6,100*(1-(double)b[4]/a[4]));
                System.out.flush();
            }
        }
        System.out.println("\n]}");
    }
    public static void main(String[] args) throws Exception {
        if (args.length>0 && "--benchmark".equals(args[0])) { benchmark();return; }
        for (int frames:new int[]{1,32,100}) for (int steps:new int[]{8,16,32})
            for (int kind=0;kind<3;kind++) for (int mode=0;mode<3;mode++)
                for (int threads:new int[]{1,2,4}) compare(frames,3,steps,kind,mode,threads,false);
        compare(250,150,32,2,1,4,false);
        compare(40,150,16,2,0,4,true);
        compare(40,150,16,2,3,4,false);
        edges();joinSafety(false);joinSafety(true);scratchReuse();
        System.out.println("Parallel sampler: "+checks+" checks passed against frozen 1.5.4; exact token/RNG/buffer state, cancellation, interruption, joined failures and bounded worker reuse.");
    }
}
