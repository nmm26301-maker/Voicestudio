package com.voice.studio;

import java.io.*;

/** Bounded sparse PCM amplitude overview, never decoding full files on the UI thread. */
final class WaveformEnvelope {
    static float[] read(File file) throws IOException {
        float[] result=new float[48];
        try(PcmAudio.Reader reader=new PcmAudio.Reader(file)) {
            double length=reader.seconds(), window=Math.min(.012,length/result.length);
            for(int i=0;i<result.length;i++) {
                double start=Math.max(0,(i+.5)*length/result.length-window*.5);
                float[] samples=reader.read(start,window); float peak=0;
                for(float sample:samples)peak=Math.max(peak,Math.abs(sample));
                result[i]=Math.min(1,peak);
            }
        }
        return result;
    }
}
