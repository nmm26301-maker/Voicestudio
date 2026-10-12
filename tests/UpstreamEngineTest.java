package com.voice.studio;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class UpstreamEngineTest {
 static int checks;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception {
  File models=new File(args[0]),folder=Files.createTempDirectory("upstream-engine").toFile();
  VoicePrompt ref=new VoicePrompt("你好，欢迎来到声音工作室。",new long[8*150],150,.05);
  try {
   for(int steps:new int[]{8,16,32})for(String text:new String[]{"你好。","这是一次完整中文合成测试。欢迎来到声音工作室。"}) {
    OmniEngine engine=new OmniEngine();File output=new File(folder,"current.wav");
    List<String> progress=new ArrayList<>();
    engine.generate(models,text,"zh",1,output,new AtomicBoolean(),progress::add,ref,"None",steps,42);
    List<String> parts=OmniEngine.split(text,1,ref);long samples=0;
    for(String part:parts) {
     double raw=ref.frames*OmniDuration.total(part)/OmniDuration.total(ref.text);
     double expected=raw<50?50*Math.pow(raw/50,1.0/3):raw;
     samples+=Math.max(1,(int)expected)*960L;
    }
    samples+=(parts.size()-1)*2880L;
    try(PcmAudio.Reader reader=new PcmAudio.Reader(output)) {
     check(reader.rate==24000&&reader.frames==samples,"native decoder length uses corrected target duration and complete segments");
     for(float value:reader.read(0,reader.seconds()))check(Float.isFinite(value),"finite WAV PCM");
    }
    check(engine.chunkCount==parts.size()&&engine.backboneRuns==steps*parts.size(),"all chunks and model steps executed");
    check(engine.running==null,"native handles released");
    check(progress.stream().filter(p->p.contains("本机合成 · "+steps+" / "+steps)).count()==parts.size(),"each segment completes");
   }
   OmniEngine engine=new OmniEngine();AtomicBoolean cancel=new AtomicBoolean();boolean failed=false;
   try {engine.generate(models,"你好。","zh",1,new File(folder,"cancel.wav"),cancel,m->{if(m.contains("本机合成 · 1 /")){cancel.set(true);engine.cancel();}},ref,"None",16,42);}
   catch(IOException expected){failed=true;}
   check(failed&&engine.running==null&&engine.decoderLoads==0,"cancel closes native encoder before decoding");
   cancel.set(false);engine.generate(models,"你好。","zh",1,new File(folder,"retry.wav"),cancel,m->{},ref,"None",16,42);
   check(engine.running==null&&engine.lastGenerationMillis>0,"same engine retries with corrected reference prompt");
   System.out.println("Upstream native flow: "+checks+" checks passed; authored graph, corrected duration/output length/cancel/retry; no speech quality claim.");
  }finally{for(File f:folder.listFiles())f.delete();folder.delete();}
 }
}
