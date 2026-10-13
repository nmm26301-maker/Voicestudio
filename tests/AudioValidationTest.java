package com.voice.studio;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class AudioValidationTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 interface Action{void run()throws Exception;}
 static void reject(Action action)throws Exception{try{action.run();throw new AssertionError("expected invalid audio");}catch(IOException expected){checks++;}}
 static void wav(File path,int encoding,int bits,int channels,byte[] data)throws Exception {
  try(RandomAccessFile f=new RandomAccessFile(path,"rw")) {
   f.setLength(0);f.writeBytes("RIFF");PcmAudio.writeLE(f,36+data.length,4);f.writeBytes("WAVEfmt ");PcmAudio.writeLE(f,16,4);
   PcmAudio.writeLE(f,encoding,2);PcmAudio.writeLE(f,channels,2);PcmAudio.writeLE(f,24000,4);PcmAudio.writeLE(f,24000*channels*(bits/8),4);
   PcmAudio.writeLE(f,channels*(bits/8),2);PcmAudio.writeLE(f,bits,2);f.writeBytes("data");PcmAudio.writeLE(f,data.length,4);f.write(data);
  }
 }
 static void validate(File path)throws Exception{try(PcmAudio.Reader r=new PcmAudio.Reader(path)){r.validateSamples(new AtomicBoolean());}}
 static long run(File path,boolean legacy,int repeats)throws Exception{
  long began=System.nanoTime();for(int i=0;i<repeats;i++)try(PcmAudio.Reader r=new PcmAudio.Reader(path)){
   if(legacy)for(double at=0;at<r.seconds();at+=30)r.read(at,Math.min(30,r.seconds()-at));
   else r.validateSamples(new AtomicBoolean());
  }return System.nanoTime()-began;
 }
 public static void main(String[] args)throws Exception {
  File dir=Files.createTempDirectory("pcm-validation").toFile(),path=new File(dir,"fixture.wav");
  try {
   Random random=new Random(1511);
   for(int bits:new int[]{16,24,32})for(int channels:new int[]{1,2,8}) {
    byte[] data=new byte[24001*channels*(bits/8)];random.nextBytes(data);wav(path,1,bits,channels,data);
    byte[] before=Files.readAllBytes(path.toPath());validate(path);
    try(PcmAudio.Reader r=new PcmAudio.Reader(path)){check(r.read(0,r.seconds()).length==24001,"decoder agrees on frame count");}
    check(Arrays.equals(before,Files.readAllBytes(path.toPath())),"validation never changes PCM");
    reject(()->{try(PcmAudio.Reader r=new PcmAudio.Reader(path)){r.validateSamples(new AtomicBoolean(true));}});
   }
   byte[] floats=new byte[24001*8*4];ByteBuffer b=ByteBuffer.wrap(floats).order(ByteOrder.LITTLE_ENDIAN);
   for(int i=0;i<floats.length/4;i++)b.putFloat(i*4,random.nextFloat()*2-1);
   wav(path,3,32,8,floats);validate(path);checks++;
   for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})for(int at:new int[]{0,32768, floats.length-4}) {
    float old=b.getFloat(at);b.putFloat(at,bad);wav(path,3,32,8,floats);reject(()->validate(path));b.putFloat(at,old);
   }
   for(int bits:new int[]{16,24,32}){wav(path,1,bits,2,new byte[13]);reject(()->validate(path));}
   wav(path,1,17,1,new byte[12]);reject(()->validate(path));
   wav(path,1,16,1,new byte[48000]);
   try(PcmAudio.Reader r=new PcmAudio.Reader(path);RandomAccessFile f=new RandomAccessFile(path,"rw")) {
    f.setLength(44+30000);reject(()->r.validateSamples(new AtomicBoolean()));
   }
   // Host-only benchmark: exact 5-minute mono PCM16. No network/model inference.
   wav(path,1,16,1,new byte[24000*300*2]);run(path,true,2);run(path,false,2);
   double[] old=new double[7],fast=new double[7];for(int i=0;i<7;i++){old[i]=run(path,true,1)/1e6;fast[i]=run(path,false,1)/1e6;}
   Arrays.sort(old);Arrays.sort(fast);
   System.out.printf(Locale.ROOT,"Host 300s PCM16 validation median: previous=%.3f ms; bounded=%.3f ms; buffer=32768 bytes (not phone/GPU inference speed).%n",old[3],fast[3]);
  }finally{path.delete();dir.delete();}
  System.out.println("Audio validation: "+checks+" format/full-read/nonfinite/cancellation checks passed.");
 }
}
