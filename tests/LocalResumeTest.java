package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class LocalResumeTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void remove(File f){if(f.isDirectory())for(File c:f.listFiles())remove(c);f.delete();}
 static String text(){StringBuilder t=new StringBuilder();for(int i=0;i<12;i++)t.append("你好，这是完整的声音创作测试。欢迎来到声音工作室。 ");return t.toString();}
 static void run(OmniEngine e,File models,String text,VoicePrompt ref,File out,File cache,AtomicBoolean cancel,OmniEngine.Progress progress,int steps,long seed)throws Exception{
  e.generate(models,text,"zh",1,out,cancel,progress,ref,"None",steps,seed,cache);
 }
 public static void main(String[] args)throws Exception {
  File models=new File(args[0]),root=Files.createTempDirectory("local-resume").toFile();
  try {
   VoicePrompt reference=new VoicePrompt("你好，欢迎来到声音工作室。",new long[8*150],150,.05);
   for(VoicePrompt ref:new VoicePrompt[]{null,reference}) {
    File cache=new File(root,ref==null?"automatic":"reference"),out=new File(root,"output.wav"),expected=new File(root,"expected.wav");
    OmniEngine fresh=new OmniEngine();run(fresh,models,text(),ref,expected,null,new AtomicBoolean(),m->{},8,42);check(fresh.chunkCount>=3,"fixture has multiple chunks");
    OmniEngine paused=new OmniEngine();AtomicBoolean cancel=new AtomicBoolean();boolean stopped=false;
    try{run(paused,models,text(),ref,out,cache,cancel,m->{if(m.startsWith("第 3 /")&&m.contains("本机合成 · 1 /")){cancel.set(true);paused.cancel();}},8,42);}catch(IOException expectedError){stopped=true;}
    check(stopped&&paused.running==null,"interrupted native run closed");
    check(new File(cache,"part-0000.tokens").isFile()&&new File(cache,"part-0001.tokens").isFile()&&!new File(cache,"part-0002.tokens").exists(),"only fully inferred chunks persisted");
    OmniEngine resumed=new OmniEngine();run(resumed,models,text(),ref,out,cache,new AtomicBoolean(),m->{},8,42);
    check(resumed.reusedEncodingChunks==2&&resumed.backboneRuns==8*(resumed.chunkCount-2),"resume skips exact completed model rounds");
    check(Arrays.equals(Files.readAllBytes(out.toPath()),Files.readAllBytes(expected.toPath())),"resumed complete WAV equals uninterrupted WAV");
    if(fresh.automaticPrompt!=null)check(resumed.automaticPrompt!=null&&fresh.automaticPrompt.rms==resumed.automaticPrompt.rms&&Arrays.equals(fresh.automaticPrompt.codes,resumed.automaticPrompt.codes),"automatic anchor survives recomputation-free resume");
    OmniEngine all=new OmniEngine();run(all,models,text(),ref,out,cache,new AtomicBoolean(),m->{},8,42);
    check(all.reusedEncodingChunks==all.chunkCount&&all.backboneLoads==0&&all.backboneRuns==0,"fully cached recovery does not load or run backbone");
    check(all.reusedDecodedChunks==all.chunkCount&&all.decoderLoads==0&&all.decoderRuns==0,"fully cached recovery skips decoder model too");
    check(Arrays.equals(Files.readAllBytes(out.toPath()),Files.readAllBytes(expected.toPath())),"all-cached replay preserves PCM and last sentence length");
    try(RandomAccessFile f=new RandomAccessFile(new File(cache,"part-0001.tokens"),"rw")){f.seek(100);f.write(9);}
    OmniEngine repair=new OmniEngine();run(repair,models,text(),ref,out,cache,new AtomicBoolean(),m->{},8,42);
    check(repair.reusedEncodingChunks==repair.chunkCount-1&&repair.backboneRuns==8,"only corrupted chunk recomputed");
    check(Arrays.equals(Files.readAllBytes(out.toPath()),Files.readAllBytes(expected.toPath())),"corruption repair retains identical full output");
    try(RandomAccessFile f=new RandomAccessFile(new File(cache,"part-0001.pcm"),"rw")){f.seek(100);f.write(7);}
    OmniEngine audioRepair=new OmniEngine();run(audioRepair,models,text(),ref,out,cache,new AtomicBoolean(),m->{},8,42);
    check(audioRepair.backboneRuns==0&&audioRepair.decoderRuns==1&&audioRepair.reusedDecodedChunks==audioRepair.chunkCount-1,"only corrupted PCM segment decoded again");
    check(Arrays.equals(Files.readAllBytes(out.toPath()),Files.readAllBytes(expected.toPath())),"decoded corruption repair preserves entire WAV");
    File interruptedCache=new File(root,ref==null?"auto-decode":"ref-decode");AtomicBoolean stopDecode=new AtomicBoolean();OmniEngine decodePause=new OmniEngine();boolean decodeStopped=false;
    try{run(decodePause,models,text(),ref,out,interruptedCache,stopDecode,m->{if(m.startsWith("第 3 /")&&m.contains("本机解码音频")){stopDecode.set(true);decodePause.cancel();}},8,42);}catch(IOException error){decodeStopped=true;}
    check(decodeStopped&&new File(interruptedCache,"part-0000.pcm").isFile()&&new File(interruptedCache,"part-0001.pcm").isFile()&&!new File(interruptedCache,"part-0002.pcm").exists(),"only complete decoded PCM survives cancelled decode");
    OmniEngine decodeResume=new OmniEngine();run(decodeResume,models,text(),ref,out,interruptedCache,new AtomicBoolean(),m->{},8,42);
    check(decodeResume.backboneRuns==0&&decodeResume.reusedDecodedChunks==2&&decodeResume.decoderRuns==decodeResume.chunkCount-2,"decode resume skips both saved PCM segments and all inferred tokens");
    check(Arrays.equals(Files.readAllBytes(out.toPath()),Files.readAllBytes(expected.toPath())),"decode-stage interruption/recovery equals uninterrupted WAV");
    OmniEngine changed=new OmniEngine();run(changed,models,text(),ref,out,cache,new AtomicBoolean(),m->{},16,42);
    check(changed.reusedEncodingChunks==0&&changed.backboneRuns==16*changed.chunkCount,"changed steps recompute every segment");
    System.out.println("Local resume fixture "+(ref==null?"automatic":"reference")+": chunks="+all.chunkCount+", first resume reused="+resumed.reusedEncodingChunks+", full cache backbone loads/runs="+all.backboneLoads+"/"+all.backboneRuns+", decoder loads/runs="+all.decoderLoads+"/"+all.decoderRuns+"; authored graph, not phone speed.");
   }
  }finally{remove(root);}
  System.out.println("Native local resume: "+checks+" interruption/cache/PCM/anchor/corruption/parameter checks passed.");
 }
}
