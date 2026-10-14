package com.voice.studio;
import java.io.*;import java.util.concurrent.atomic.AtomicBoolean;
public class ShortCloneAcceptanceTest {
 public static void main(String[] args)throws Exception {
  File models=new File(args[0]),reference=new File(args[1]),folder=new File(args[2]);folder.mkdirs();
  String referenceText="你好，欢迎来到声音工作室。今天测试免费 GPU 的完整语音合成。";
  double seconds;try(PcmAudio.Reader r=new PcmAudio.Reader(reference)){seconds=r.seconds();}
  VoicePrompt ref=new OmniCodec().encode(models,reference,referenceText,0,seconds,new AtomicBoolean(),System.out::println);
  ref.save(new File(folder,"reference.vpc"));
  StringBuilder wrong=new StringBuilder();for(int i=0;i<330;i++)wrong.append('啊');
  VoicePrompt legacy=new VoicePrompt(wrong.toString(),ref.codes,ref.frames,ref.rms);legacy.save(new File(folder,"legacy.vpc"));
  VoicePrompt restored=VoicePrompt.load(new File(folder,"legacy.vpc"));
  if(!OmniDuration.needsTranscript(restored.text,restored.frames))throw new AssertionError("Repair candidate not identified");
  if(Math.abs(OmniEngine.duration("你好",1,restored)-OmniDuration.frames("你好"))>1e-9)throw new AssertionError("Bad metadata still compresses new text");
  String[] targets={"你好","谢谢","今天的天气很好，我们一起去公园散步。"};
  for(int steps:new int[]{16,32})for(int i=0;i<targets.length;i++) {
   OmniEngine engine=new OmniEngine();File out=new File(folder,"clone-"+steps+"-"+i+".wav");
   engine.generate(models,targets[i],"zh",1,out,new AtomicBoolean(),m->{},ref,"None",steps,42);
   try(PcmAudio.Reader r=new PcmAudio.Reader(out)){System.out.println("CLONE text="+targets[i]+" steps="+steps+" seconds="+r.seconds()+" host_ms="+engine.lastGenerationMillis);if(r.seconds()<=0)throw new AssertionError("Empty clone");}
  }
  System.out.println("All different target texts generated; semantic acceptance requires ASR/listening, not WAV validity.");
 }
}
