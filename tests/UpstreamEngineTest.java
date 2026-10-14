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
    File frozen=new File(folder,"frozen.wav");LegacyEngine1511 before=new LegacyEngine1511();
    before.generate(models,text,"zh",1,frozen,new AtomicBoolean(),m->{},ref,"None",steps,42);
    boolean shortQuality=steps==32&&text.equals("你好。");
    if(!shortQuality)check(Arrays.equals(Files.readAllBytes(frozen.toPath()),Files.readAllBytes(output.toPath())),"same entire native WAV as frozen 1.5.11 outside short quality safeguard");
    else check(output.length()>frozen.length(),"32-step two-character clone uses larger actual generated PCM budget");
    check(engine.bufferedPcmChunks==engine.chunkCount&&engine.pcmWriteNanos>0,"native decoder actually uses bounded PCM output");
    List<String> parts=OmniEngine.split(text,1,ref);long samples=0;
    for(String part:parts) {
     double raw=ref.frames*OmniDuration.total(part)/OmniDuration.total(ref.text);
     double expected=raw<50?50*Math.pow(raw/50,1.0/3):raw;
     samples+=(shortQuality?Math.max(50,Math.max(1,(int)expected)):Math.max(1,(int)expected))*960L;
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
   // Automatic multi-segment anchor parity includes RMS, code ownership and silence joins.
   StringBuilder longer=new StringBuilder();for(int i=0;i<35;i++)longer.append("你好，这是完整的声音创作测试。欢迎来到声音工作室。 ");
   for(VoicePrompt voice:new VoicePrompt[]{null,ref}) {
    OmniEngine current=new OmniEngine();LegacyEngine1511 frozen=new LegacyEngine1511();File a=new File(folder,"new-long.wav"),b=new File(folder,"old-long.wav");
    current.generate(models,longer.toString(),"zh",1,a,new AtomicBoolean(),m->{},voice,"None",8,42);
    frozen.generate(models,longer.toString(),"zh",1,b,new AtomicBoolean(),m->{},voice,"None",8,42);
    check(current.chunkCount>1,"multi-segment fixture exercised");
    check(Arrays.equals(Files.readAllBytes(a.toPath()),Files.readAllBytes(b.toPath())),"automatic/reference multi-segment native PCM parity");
    check(current.bufferedPcmChunks==current.chunkCount,"every native chunk streamed to bounded writer");
    if(frozen.automaticPrompt!=null) {
     check(current.automaticPrompt!=null,"automatic anchor still available");
     check(current.automaticPrompt.rms==frozen.automaticPrompt.rms&&Arrays.equals(current.automaticPrompt.codes,frozen.automaticPrompt.codes),"automatic anchor RMS/codes unchanged");
    }
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
