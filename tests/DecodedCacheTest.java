package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class DecodedCacheTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void remove(File f){if(f.isDirectory())for(File c:f.listFiles())remove(c);f.delete();}
 public static void main(String[] args)throws Exception {
  File root=Files.createTempDirectory("decoded-cache").toFile(),folder=new File(root,"cache"),source=new File(root,"source.wav"),output=new File(root,"out.wav");folder.mkdirs();
  try {
   OmniDecodedCache cache=new OmniDecodedCache(folder);long[] codes=new long[8*75];String key=OmniDecodedCache.key("input-digest",codes);
   check(!key.equals(OmniDecodedCache.key("other-input",codes)),"synthesis inputs bound");codes[0]=1;check(!key.equals(OmniDecodedCache.key("input-digest",codes)),"exact token changes bound");codes[0]=0;
   VoicePrompt anchor=new VoicePrompt("自动音色参考",codes,75,.2);float[] pcm=new float[75*960];for(int i=0;i<pcm.length;i++)pcm[i]=(float)Math.sin(i*.05)*.2f;
   long offset;
   try(PcmAudio.Writer writer=new PcmAudio.Writer(source,24000)) {
    writer.write(new float[]{.1f,.2f});offset=writer.f.getFilePointer();writer.write(pcm);long end=writer.f.getFilePointer();
    check(cache.save(0,key,75,anchor,writer,offset,new AtomicBoolean()),"completed PCM saved");check(writer.f.getFilePointer()==end,"source cursor restored after cache copy");
   }
   OmniDecodedCache.Entry entry=cache.read(0,key,75,true,new AtomicBoolean());check(entry!=null&&entry.rms==anchor.rms&&entry.anchor,"pre-fade anchor RMS exact");
   check(cache.read(0,"wrong",75,true,new AtomicBoolean())==null,"wrong input key ignored");
   try(PcmAudio.Writer writer=new PcmAudio.Writer(source,24000)) {
    writer.silence(3);check(cache.save(2,key,75,null,writer,44,new AtomicBoolean()),"legacy silent PCM fixture saved with valid hash");
   }
   OmniDecodedCache.Entry silence=cache.read(2,key,75,false,new AtomicBoolean());
   check(silence!=null,"valid hash alone does not establish audible speech");
   try(PcmAudio.Writer writer=new PcmAudio.Writer(output,24000)) {
    writer.write(new float[]{.2f,.3f});long length=writer.f.length();boolean rejected=false;
    try{OmniDecodedCache.append(writer,silence,new AtomicBoolean(),true);}catch(OmniPcmWriter.SilentSpeech expected){rejected=true;}
    check(rejected&&writer.count==2&&writer.f.length()==length,"silent cached speech rejected with full rollback");
   }
   try(PcmAudio.Writer writer=new PcmAudio.Writer(source,24000)) {
    writer.write(new float[]{.1f,.2f});writer.write(pcm);
   }
   byte[] original=Files.readAllBytes(cache.path(0).toPath());
   try(PcmAudio.Writer writer=new PcmAudio.Writer(output,24000)){OmniDecodedCache.append(writer,entry,new AtomicBoolean());check(writer.count==pcm.length,"all cached samples appended");}
   byte[] from=Files.readAllBytes(source.toPath()),to=Files.readAllBytes(output.toPath());check(Arrays.equals(Arrays.copyOfRange(from,(int)offset,from.length),Arrays.copyOfRange(to,44,to.length)),"cached PCM byte-for-byte append without repeated fade");
   boolean cancelled=false;try{cache.read(0,key,75,true,new AtomicBoolean(true));}catch(InterruptedIOException expected){cancelled=true;}check(cancelled&&cache.path(0).exists(),"cancelled validation preserves reusable cache");
   try(PcmAudio.Writer writer=new PcmAudio.Writer(output,24000)){
    writer.write(new float[]{.2f});long oldLength=writer.f.length(),oldCount=writer.count;boolean failed=false;
    try{OmniDecodedCache.append(writer,entry,new AtomicBoolean(true));}catch(InterruptedIOException expected){failed=true;}
    check(failed&&writer.count==oldCount&&writer.f.length()==oldLength,"cancelled append preserves prior segment");
    try(RandomAccessFile f=new RandomAccessFile(cache.path(0),"rw")){f.setLength(entry.offset+40000);}
    failed=false;try{OmniDecodedCache.append(writer,entry,new AtomicBoolean());}catch(IOException expected){failed=true;}
    check(failed&&writer.count==oldCount&&writer.f.length()==oldLength,"partial read failure rolls back earlier copied blocks");
   }
   for(int at:new int[]{0,80,original.length/2,original.length-1}){
    byte[] bad=original.clone();bad[at]^=1;Files.write(cache.path(0).toPath(),bad);cache=new OmniDecodedCache(folder);
    check(cache.read(0,key,75,true,new AtomicBoolean())==null&&!cache.path(0).exists(),"corrupt header/data/hash never appended");
   }
   Files.write(cache.path(0).toPath(),original);cache=new OmniDecodedCache(folder);check(cache.read(0,key,75,false,new AtomicBoolean())==null,"wrong anchor mode invalidates decoded output");
   Files.write(cache.path(0).toPath(),Arrays.copyOf(original,original.length-3));cache=new OmniDecodedCache(folder);check(cache.read(0,key,75,true,new AtomicBoolean())==null,"truncated cached tail rejected");
   File budget=new File(folder,"part-9999.pcm");try(RandomAccessFile f=new RandomAccessFile(budget,"rw")){f.setLength(OmniDecodedCache.LIMIT);}
   cache=new OmniDecodedCache(folder);try(PcmAudio.Writer writer=new PcmAudio.Writer(source,24000)){
    writer.write(pcm);long end=writer.f.getFilePointer();check(!cache.save(1,key,75,anchor,writer,44,new AtomicBoolean()),"64 MiB cache budget skips new storage");
    check(writer.f.getFilePointer()==end&&!cache.path(1).exists(),"budget never prevents successful audio output");
   }
  }finally{remove(root);}
  System.out.println("Decoded cache: "+checks+" exact-PCM/anchor/hash/cancel/rollback/budget checks passed.");
 }
}
