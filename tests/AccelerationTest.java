package com.voice.studio;

import java.io.*;
import java.nio.*;
import java.util.*;
import org.json.*;

public class AccelerationTest {
    static int checks;
    static void check(boolean ok,String label) { checks++; if(!ok)throw new AssertionError(label); }
    // Independent retained original sampler, including both logits passes and heap selection.
    static int old(FloatBuffer logits,long[] codes,int frames,int s,int target,int k,Random random) {
        int total=codes.length; int[] predictions=new int[total]; double[] scores=new double[total]; Arrays.fill(scores,Double.NEGATIVE_INFINITY);
        PriorityQueue<Integer> best=new PriorityQueue<>((i,j)->Double.compare(scores[i],scores[j]));
        for(int c=0;c<8;c++)for(int t=0;t<frames;t++) {
            int index=c*frames+t; if(codes[index]!=1024)continue;
            int ci=(c*s+target+t)*1025,ui=((8+c)*s+t)*1025,pred=0; double max=Double.NEGATIVE_INFINITY,bestValue=Double.NEGATIVE_INFINITY;
            for(int v=0;v<1025;v++) { double value=3.0*logits.get(ci+v)-2.0*logits.get(ui+v); if(v<1024 && value>bestValue) { pred=v; bestValue=value; } max=Math.max(max,value); }
            double sum=0;for(int v=0;v<1025;v++)sum+=Math.exp(3.0*logits.get(ci+v)-2.0*logits.get(ui+v)-max);
            double confidence=3.0*logits.get(ci+pred)-2.0*logits.get(ui+pred)-max-Math.log(sum);
            predictions[index]=pred; scores[index]=(confidence-5*c)/5.0-Math.log(-Math.log(Math.max(1e-10,Math.min(1-1e-10,random.nextDouble()))));
            best.add(index); if(best.size()>k)best.poll();
        }
        for(int index:best)codes[index]=predictions[index];return best.size();
    }
    public static void main(String[] args)throws Exception {
        Random values=new Random(140);
        for(int frames:new int[]{1,4,12})for(int steps:new int[]{16,32})for(int mode=0;mode<3;mode++) {
            int target=3,s=frames+target,total=8*frames; FloatBuffer logits=FloatBuffer.allocate(16*s*1025);
            long[] a=new long[total],b=new long[total];Arrays.fill(a,1024);Arrays.fill(b,1024);
            OmniStepSampler sampler=new OmniStepSampler(total);Random r1=new Random(42),r2=new Random(42);int remain=total;
            for(int step=0;step<steps;step++) {
                for(int v=0;v<logits.capacity();v++)logits.put(v,mode==0?0:mode==1?(values.nextFloat()-.5f)*10:(values.nextFloat()-.5f)*2000);
                double t1=(double)step/steps,t2=(double)(step+1)/steps;
                int k=step==steps-1?remain:Math.min(remain,(int)Math.ceil(total*(.1*t2/(1-.9*t2)-.1*t1/(1-.9*t1))));
                if(k==0)continue;
                int n1=old(logits,a,frames,s,target,k,r1),n2=sampler.update(logits,b,frames,s,target,k,r2);
                check(n1==n2,"same selected count");check(Arrays.equals(a,b),"same audio token decisions");remain-=n1;
            }
            check(remain==0,"all codes filled");for(long code:b)check(code>=0&&code<1024,"valid audio codes");
        }
        JSONArray history=new JSONArray().put(new JSONObject().put("id","a").put("title","Hello").put("voice","warm").put("created",2).put("duration",1000).put("favorite",true))
                .put(new JSONObject().put("id","b").put("text","你好世界").put("voice","清澈").put("created",5).put("duration",62000))
                .put(new JSONObject().put("id","trash").put("created",99).put("duration",999999).put("deletedAt",1));
        check(WorksCatalog.select(history,"",false,0).get(0).getString("id").equals("b"),"latest order excludes trash");
        check(WorksCatalog.select(history,"",false,1).get(0).getString("id").equals("a"),"oldest order");
        check(WorksCatalog.select(history,"",false,2).get(0).getString("id").equals("b"),"duration order");
        check(WorksCatalog.select(history,"HELLO",true,0).size()==1,"case insensitive favorite search");
        check(WorksCatalog.select(history,"清澈",false,0).size()==1,"voice search");
        check(WorksCatalog.select(history,"你好",false,0).size()==1,"fallback text search");
        check(WorksCatalog.select(history,"不存在",false,0).isEmpty(),"empty search");
        check(WorksCatalog.totalDuration(history)==63000,"actual non-trash duration");
        check(WorksCatalog.duration(63000).equals("01:03"),"duration format");
        check(WorksCatalog.duration(3723000).equals("1:02:03"),"hour duration");
        File wav=File.createTempFile("waveform140",".wav");
        try {
            float[] signal=new float[24000]; for(int i=0;i<signal.length;i++)signal[i]=(float)(.5*Math.sin(2*Math.PI*440*i/24000));
            try(PcmAudio.Writer writer=new PcmAudio.Writer(wav,24000)) { writer.write(signal); }
            float[] peaks=WaveformEnvelope.read(wav); check(peaks.length==48,"bounded envelope");for(float peak:peaks)check(peak>.45 && peak<=.51,"real tone amplitude");
            Arrays.fill(signal,0);try(PcmAudio.Writer writer=new PcmAudio.Writer(wav,24000)) { writer.write(signal); }
            for(float peak:WaveformEnvelope.read(wav))check(peak==0,"silence remains flat");
        }finally { wav.delete(); }
        System.out.println("Acceleration / archive: "+checks+" checks passed. Sampling decisions match original; no phone speed claim.");
    }
}
