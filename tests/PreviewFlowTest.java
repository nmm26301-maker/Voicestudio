package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class PreviewFlowTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void remove(File f){if(f.isDirectory())for(File c:f.listFiles())remove(c);f.delete();}
 public static void main(String[] args)throws Exception {
  File models=new File(args[0]),root=Files.createTempDirectory("preview-native").toFile();
  try {
   String text=LocalResumeTest.text();VoicePrompt reference=new VoicePrompt("你好，欢迎来到声音工作室。",new long[8*150],150,.05);
   for(VoicePrompt voice:new VoicePrompt[]{null,reference}) {
    File output=new File(root,"new.wav"),expected=new File(root,"old.wav"),preview=new File(root,"preview.wav"),cache=new File(root,voice==null?"automatic":"reference");
    OmniEngine plain=new OmniEngine();plain.generate(models,text,"zh",1,expected,new AtomicBoolean(),m->{},voice,"None",8,42);
    OmniEngine engine=new OmniEngine();int[] ready={0};
    engine.generate(models,text,"zh",1,output,new AtomicBoolean(),m->{if(m.equals(SynthesisPreview.READY)){
     ready[0]++;check(preview.isFile(),"preview published before readiness signal");
     check(engine.bufferedPcmChunks+engine.reusedDecodedChunks==1,"preview available after first decoded chunk");
     try(PcmAudio.Reader r=new PcmAudio.Reader(preview)){check(r.frames>0&&r.frames<=240000,"validated complete first segment");}catch(Exception e){throw new RuntimeException(e);}
    }},voice,"None",8,42,cache,preview);
    check(ready[0]==1,"one preview notification per generation");
    check(Arrays.equals(Files.readAllBytes(output.toPath()),Files.readAllBytes(expected.toPath())),"preview adds no PCM/fade/pause changes to complete WAV");
    check(engine.backboneLoads==plain.backboneLoads&&engine.decoderLoads==plain.decoderLoads,"preview does not force extra native model reloads");
    AtomicBoolean cancel=new AtomicBoolean();boolean stopped=false;
    try{new OmniEngine().generate(models,text,"zh",1,output,cancel,m->{if(m.equals(SynthesisPreview.READY))cancel.set(true);},voice,"None",8,42,cache,preview);}catch(IOException e){stopped=true;}
    check(stopped&&preview.isFile(),"cancel after audition readiness retains finalized preview");
   }
  }finally{remove(root);}
  System.out.println("Native preview flow: "+checks+" readiness/PCM/model-load/cancel checks passed; authored graph, no device playback claim.");
 }
}
