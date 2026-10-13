package com.voice.studio;

import java.io.*;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded decoder output conversion. Matches the former gain/RMS/fade/PCM operations exactly. */
final class OmniPcmWriter {
    static final int BLOCK_SAMPLES = 16384;
    static final int BUFFER_BYTES = BLOCK_SAMPLES * 6;
    static double write(PcmAudio.Writer writer,FloatBuffer source,double gain,boolean measureRms,
            AtomicBoolean cancelled) throws IOException {
        int position=source.position(),count=source.remaining();
        if(writer.channels!=1||!Double.isFinite(gain)||gain<0||writer.count+count>1000000000L)
            throw new IOException("解码音频输出参数无效");
        long before=writer.f.getFilePointer(),oldCount=writer.count;
        float[] samples=new float[BLOCK_SAMPLES];byte[] bytes=new byte[BLOCK_SAMPLES*2];
        int fade=Math.min(240,count/2);double square=0;
        try {
            for(int at=0;at<count;) {
                check(cancelled);int take=Math.min(BLOCK_SAMPLES,count-at);source.get(samples,0,take);
                if(gain!=1)for(int i=0;i<take;i++)samples[i]=(float)(samples[i]*gain);
                if(measureRms)for(int i=0;i<take;i++)square+=samples[i]*samples[i];
                // Only the first/last 240 samples fade. Keep branches out of PCM conversion.
                for(int i=0;i<Math.min(take,Math.max(0,fade-at));i++)samples[i]*=(float)(at+i)/fade;
                for(int i=Math.max(0,count-fade-at);i<take;i++)samples[i]*=(float)(count-1-at-i)/fade;
                for(int i=0;i<take;i++) {
                    float value=samples[i];
                    if(!Float.isFinite(value))throw new IOException("模型返回无效音频");
                    int pcm=Math.round(Math.max(-1,Math.min(1,value))*32767);
                    bytes[i*2]=(byte)pcm;bytes[i*2+1]=(byte)(pcm>>8);
                }
                check(cancelled);writer.f.write(bytes,0,take*2);at+=take;
            }
            check(cancelled);writer.count=oldCount+count;
            return measureRms?Math.sqrt(square/Math.max(1,count)):0;
        }catch(IOException|RuntimeException error) {
            source.position(position);writer.count=oldCount;
            try{writer.f.setLength(before);writer.f.seek(before);}catch(IOException rollback){error.addSuppressed(rollback);}
            throw error;
        }
    }
    private static void check(AtomicBoolean cancelled)throws InterruptedIOException {
        if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消生成");
    }
}
