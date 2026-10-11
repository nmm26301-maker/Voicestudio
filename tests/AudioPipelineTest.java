package com.voice.studio;
import java.io.*;import java.util.*;
public class AudioPipelineTest {
 static void check(boolean condition,String text){if(!condition)throw new AssertionError(text);}
 static double pitch(float[] x,int rate){int first=-1,last=-1,count=0;for(int i=rate/4;i<x.length-rate/4;i++)if(x[i-1]<=0&&x[i]>0){if(first<0)first=i;last=i;count++;}return (count-1.0)*rate/(last-first);}
 public static void main(String[] args)throws Exception{
  float[] tone=new float[48000];for(int i=0;i<tone.length;i++)tone[i]=(float)(.2*Math.sin(2*Math.PI*440*i/24000));
  for(double ratio:new double[]{.5,.8,1.25,2}){int frames=(int)(tone.length/ratio);float[] fit=AudioTiming.fit(tone,frames);check(fit.length==frames,"Exact timeline length");double hz=pitch(fit,24000);check(Math.abs(hz-440)<2,"Pitch changed: "+hz);System.out.println("Duration ratio "+ratio+": "+hz+" Hz");}
  float[] resampled=PcmAudio.resample(tone,24000,16000);check(resampled.length==32000,"Resample count");check(Math.abs(pitch(resampled,16000)-440)<1,"Resample pitch");
  File file=File.createTempFile("voices-test-",".wav");try{try(PcmAudio.Writer writer=new PcmAudio.Writer(file,24000)){writer.write(tone);writer.silence(.25);}PcmAudio.markSynthetic(file);try(PcmAudio.Reader reader=new PcmAudio.Reader(file)){check(reader.frames==54000,"WAV count");check(reader.read(1.000125,.1).length==2400,"Sample address");}
   try(RandomAccessFile raw=new RandomAccessFile(file,"rw")){raw.seek(22);raw.write(0);raw.write(0);}boolean rejected=false;try(PcmAudio.Reader ignored=new PcmAudio.Reader(file)){}catch(IOException e){rejected=true;}check(rejected,"Invalid channels must raise IOException");
  }finally{file.delete();}
  boolean rejected=false;try{AudioTiming.fit(tone,1000);}catch(IOException e){rejected=true;}check(rejected,"Impossible duration rejected");
  System.out.println("Audio timing, resampling, synthetic metadata, WAV addressing and malformed WAV checks passed.");
 }
}
