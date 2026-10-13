package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
public class SynthesisPreviewTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void remove(File f){if(f.isDirectory())for(File c:f.listFiles())remove(c);f.delete();}
 public static void main(String[] args)throws Exception {
  File root=Files.createTempDirectory("synthesis-preview").toFile(),jobs=new File(root,"jobs");jobs.mkdirs();String id="12345678-1234-1234-1234-123456789012";
  File folder=new File(jobs,id);folder.mkdirs();File source=new File(root,"source.wav"),preview=new File(folder,"preview.wav");
  try {
   float[] samples=new float[24000];for(int i=0;i<samples.length;i++)samples[i]=(float)Math.sin(i*.07)*.2f;
   try(PcmAudio.Writer w=new PcmAudio.Writer(source,24000)) {
    w.write(samples);long cursor=w.f.getFilePointer(),count=w.count;
    SynthesisPreview.write(preview,w,44,samples.length,new AtomicBoolean());
    check(cursor==w.f.getFilePointer()&&count==w.count,"preview never changes complete output cursor or frame count");
    try(PcmAudio.Reader reader=new PcmAudio.Reader(preview)){check(reader.frames==samples.length&&reader.rate==24000,"complete playable WAV before parent writer closes");}
    byte[] saved=Files.readAllBytes(preview.toPath());boolean failed=false;
    try{SynthesisPreview.write(preview,w,44,samples.length,new AtomicBoolean(true));}catch(InterruptedIOException expected){failed=true;}
    check(failed&&Arrays.equals(saved,Files.readAllBytes(preview.toPath()))&&cursor==w.f.getFilePointer(),"cancel leaves previous atomic preview intact");
    check(!new File(preview+".pending").exists(),"no pending preview published");
   }
   try(PcmAudio.Reader a=new PcmAudio.Reader(source);PcmAudio.Reader b=new PcmAudio.Reader(preview)){check(Arrays.equals(a.read(0,1),b.read(0,1)),"preview sample values equal completed source segment");}
   check(new String(Files.readAllBytes(preview.toPath()),"ISO-8859-1").contains("VoiceStudio AI-generated speech"),"synthetic metadata present");
   JSONObject task=new JSONObject().put("id",id).put("type","tts").put("engine","omni").put("previewReady",true).put("state","running");
   for(String state:new String[]{"running","paused","failed","queued","done","idle"}) {
    task.put("state",state);check((SynthesisPreview.file(jobs,task)!=null)==Arrays.asList("running","paused","failed").contains(state),"preview state gate "+state);
   }
   task.put("state","running").put("previewReady",false);check(SynthesisPreview.file(jobs,task)==null,"stale file hidden until current generation publishes readiness");task.put("previewReady",true);
   for(String value:new String[]{"../outside","",id+"/../../outside"}){task.put("id",value);check(SynthesisPreview.file(jobs,task)==null,"path traversal rejected");}task.put("id",id);
   task.put("type","clone");check(SynthesisPreview.file(jobs,task)==null,"reference recordings not presented as generated previews");task.put("type","tts").put("engine","fish");check(SynthesisPreview.file(jobs,task)==null,"Fish has no partial audio preview");
   File gpu=new File(folder,"gpu");gpu.mkdirs();Files.write(new File(gpu,"part-0001.wav").toPath(),Files.readAllBytes(preview.toPath()));Files.write(new File(gpu,"part-0000.wav.pending").toPath(),Files.readAllBytes(preview.toPath()));
   task.put("engine","accelerated");check(SynthesisPreview.file(jobs,task).getName().equals("part-0001.wav"),"only completed remote audio; blank first segment supported");
   File target=new File(folder,"directory-preview");target.mkdir();boolean failed=false;
   try(PcmAudio.Writer w=new PcmAudio.Writer(source,24000)){w.write(samples);long cursor=w.f.getFilePointer();try{SynthesisPreview.write(target,w,44,samples.length,new AtomicBoolean());}catch(IOException expected){failed=true;}check(cursor==w.f.getFilePointer(),"failed optional publication keeps source cursor");}
   check(failed&&!new File(target+".pending").exists(),"failed publication leaves no partial candidate");
  }finally{remove(root);}
  System.out.println("Synthesis preview: "+checks+" WAV/PCM/atomic/cancel/readiness/path/state checks passed.");
 }
}
