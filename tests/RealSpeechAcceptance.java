package com.voice.studio;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class RealSpeechAcceptance {
 public static void main(String[] args)throws Exception {
  File models=new File(args[0]),folder=new File(args[1]);LegacyOmniEngine157 old=new LegacyOmniEngine157();
  String referenceText="你好，欢迎来到声音工作室。今天我们测试完整的中文语音合成。";
  old.generate(models,referenceText,"zh",1,new File(folder,"reference-actual.wav"),new AtomicBoolean(),m->{},null,"None",16,42);
  VoicePrompt ref=old.automaticPrompt;ref.save(new File(folder,"actual-reference.vpc"));
  String text="你好。";
  File before=new File(folder,"short-before.wav"),after=new File(folder,"short-after.wav");
  old.generate(models,text,"zh",1,before,new AtomicBoolean(),m->{},ref,"None",16,42);
  OmniEngine current=new OmniEngine();current.generate(models,text,"zh",1,after,new AtomicBoolean(),m->{},ref,"None",16,42);
  try(PcmAudio.Reader a=new PcmAudio.Reader(before);PcmAudio.Reader b=new PcmAudio.Reader(after)) {
   System.out.println("REAL reference-backed short Chinese: old_seconds="+a.seconds()+", new_seconds="+b.seconds()+", reference_frames="+ref.frames);
   if(!(b.seconds()>a.seconds()*2))throw new AssertionError("Expected upstream short duration boost");
  }
  current.generate(models,"这是新的完整中文语音合成测试，请把这句话一直读到最后。","zh",1,new File(folder,"full-after.wav"),new AtomicBoolean(),m->{},ref,"None",16,42);
  System.out.println("Real graphs completed; semantic speech assessment still uses ASR/listening and phone validation.");
 }
}
