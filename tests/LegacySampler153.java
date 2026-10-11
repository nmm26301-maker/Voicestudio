package com.voice.studio;

import java.nio.FloatBuffer;
import java.util.*;

/** Frozen 1.5.3 sampler from source tree 81ffb05c; only class name changed. */
final class LegacySampler153 {
    final int[] predictions;
    final double[] scores, values = new double[1025];
    final PriorityQueue<Integer> best;
    LegacySampler153(int count) {
        predictions = new int[count]; scores = new double[count];
        best = new PriorityQueue<>((i,j) -> Double.compare(scores[i],scores[j]));
    }
    int update(FloatBuffer logits, long[] codes, int frames, int sequence, int target, int k, Random random) {
        best.clear(); Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        for(int c=0;c<8;c++) for(int t=0;t<frames;t++) {
            int index=c*frames+t;
            if(codes[index]!=1024) continue;
            int ci=(c*sequence+target+t)*1025, ui=((8+c)*sequence+t)*1025;
            double max=Double.NEGATIVE_INFINITY, bestValue=Double.NEGATIVE_INFINITY; int pred=0;
            for(int v=0;v<1025;v++) {
                double value=3.0*logits.get(ci+v)-2.0*logits.get(ui+v);
                values[v]=value;
                if(v<1024 && value>bestValue) { pred=v; bestValue=value; }
                max=Math.max(max,value);
            }
            double sum=0; for(int v=0;v<1025;v++) sum+=Math.exp(values[v]-max);
            predictions[index]=pred;
            scores[index]=(bestValue-max-Math.log(sum)-5*c)/5.0
                    -Math.log(-Math.log(Math.max(1e-10,Math.min(1-1e-10,random.nextDouble()))));
            best.add(index); if(best.size()>k) best.poll();
        }
        for(int index:best) codes[index]=predictions[index];
        return best.size();
    }
}
