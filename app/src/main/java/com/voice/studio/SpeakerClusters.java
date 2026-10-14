package com.voice.studio;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Global spherical clustering; known speaker count bounds fragmentation across utterances. */
final class SpeakerClusters {
    static void check(AtomicBoolean cancel) throws InterruptedIOException {
        if (cancel.get() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("人物分组已暂停");
    }
    static double dot(float[] a, float[] b) {
        double sum=0;for(int d=0;d<a.length;d++)sum+=a[d]*b[d];return sum;
    }
    static float[] normal(double[] sum) {
        double norm=0;for(double v:sum)norm+=v*v;
        if(norm<1e-20)return null;
        float[] center=new float[sum.length];norm=Math.sqrt(norm);
        for(int d=0;d<sum.length;d++)center[d]=(float)(sum[d]/norm);return center;
    }
    static int[] group(List<float[]> input,List<Double> weights,int requested,double threshold,AtomicBoolean cancel) throws IOException {
        if(requested<0||requested>32||!Double.isFinite(threshold)||threshold<.2||threshold>.95||weights.size()!=input.size())
            throw new IOException("人物数量或分组参数无效");
        if(input.isEmpty())return new int[0];
        int dimensions=input.get(0).length;
        if(dimensions==0)throw new IOException("人物声音特征为空");
        List<float[]> features=new ArrayList<>();int first=0;
        for(int n=0;n<input.size();n++) {
            check(cancel);float[] value=input.get(n);
            if(value.length!=dimensions||!Double.isFinite(weights.get(n))||weights.get(n)<=0)throw new IOException("人物声音特征不一致");
            double norm=0;for(float v:value){if(!Float.isFinite(v))throw new IOException("人物声音特征无效");norm+=v*v;}
            if(norm<1e-20)throw new IOException("人物声音特征无效");
            value=value.clone();norm=Math.sqrt(norm);for(int d=0;d<dimensions;d++)value[d]/=norm;features.add(value);
            if(weights.get(n)>weights.get(first))first=n;
        }
        List<float[]> centers=new ArrayList<>();centers.add(features.get(first).clone());
        boolean[] selected=new boolean[features.size()];selected[first]=true;
        int limit=Math.min(requested==0?32:requested,features.size());
        while(centers.size()<limit) {
            check(cancel);int farthest=-1;double lowest=Double.POSITIVE_INFINITY;
            for(int n=0;n<features.size();n++)if(!selected[n]) {
                double best=-2;for(float[] c:centers)best=Math.max(best,dot(features.get(n),c));
                if(best<lowest){lowest=best;farthest=n;}
            }
            if(farthest<0||lowest>1-1e-7||(requested==0&&lowest>=threshold))break;
            selected[farthest]=true;centers.add(features.get(farthest).clone());
        }
        int[] labels=fit(features,weights,centers,cancel);
        // Auto mode reconciles prototypes globally instead of keeping every early fragment.
        if(requested==0)while(centers.size()>1) {
            check(cancel);int a=-1,b=-1;double best=threshold;
            for(int i=0;i<centers.size();i++)for(int j=i+1;j<centers.size();j++) {
                double score=dot(centers.get(i),centers.get(j));if(score>best){best=score;a=i;b=j;}
            }
            if(a<0)break;
            double[] sum=new double[dimensions];
            for(int n=0;n<labels.length;n++)if(labels[n]==a||labels[n]==b)
                for(int d=0;d<dimensions;d++)sum[d]+=features.get(n)[d]*weights.get(n);
            float[] merged=normal(sum);if(merged==null)break;
            centers.set(a,merged);centers.remove(b);labels=fit(features,weights,centers,cancel);
        }
        Map<Integer,Integer> stable=new LinkedHashMap<>();
        for(int n=0;n<labels.length;n++) {
            if(!stable.containsKey(labels[n]))stable.put(labels[n],stable.size()+1);
            labels[n]=stable.get(labels[n]);
        }
        check(cancel);return labels;
    }
    private static int[] fit(List<float[]> features,List<Double> weights,List<float[]> centers,AtomicBoolean cancel) throws IOException {
        int[] labels=new int[features.size()];Arrays.fill(labels,-1);int dimensions=features.get(0).length;
        for(int iteration=0;iteration<64;iteration++) {
            check(cancel);boolean changed=false;
            double[][] sums=new double[centers.size()][dimensions];int[] sizes=new int[centers.size()];
            for(int n=0;n<features.size();n++) {
                if((n&15)==0)check(cancel);int chosen=0;double best=-2;
                for(int c=0;c<centers.size();c++) {double score=dot(features.get(n),centers.get(c));if(score>best){best=score;chosen=c;}}
                changed|=labels[n]!=chosen;labels[n]=chosen;sizes[chosen]++;
                for(int d=0;d<dimensions;d++)sums[chosen][d]+=features.get(n)[d]*weights.get(n);
            }
            for(int c=0;c<centers.size();c++)if(sizes[c]>0) {float[] center=normal(sums[c]);if(center!=null)centers.set(c,center);}
            if(!changed)break;
        }
        return labels;
    }
}
