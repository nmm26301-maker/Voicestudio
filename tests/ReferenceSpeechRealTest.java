package com.voice.studio;import java.io.*;import java.util.concurrent.atomic.AtomicBoolean;
public class ReferenceSpeechRealTest {public static void main(String[] args)throws Exception{
 File models=new File(args[0]),out=new File(args[1]);
 String text="愿你今天遇见的，都是温柔的小事。放慢脚步，也别忘了听听自己的声音。";
 OmniEngine control=new OmniEngine();File original=new File(out,"reference-control.wav");
 control.generate(models,text,"zh",1,original,new AtomicBoolean(),m->{},null,"None",32,42);
 double seconds;try(PcmAudio.Reader r=new PcmAudio.Reader(original)){seconds=r.seconds();}
 VoicePrompt good=new OmniCodec().encode(models,original,text,0,seconds,new AtomicBoolean(),m->{});
 if(good.frames!=(int)Math.round(seconds*25))throw new AssertionError("Reference encoder changed duration");
 StringBuilder wrong=new StringBuilder();for(int i=0;i<330;i++)wrong.append('啊');File legacy=new File(out,"bad-reference.vpc");
 try(DataOutputStream f=new DataOutputStream(new FileOutputStream(legacy))){f.writeInt(0x56504331);f.writeUTF(wrong.toString());f.writeInt(good.frames);f.writeDouble(good.rms);for(long code:good.codes)f.writeShort((int)code);}
 VoicePrompt restored=VoicePrompt.load(legacy);if(!OmniDuration.needsTranscript(restored.text,restored.frames))throw new AssertionError("Automatic repair candidate not detected");
 if(Math.abs(OmniEngine.duration(text,1,restored)-OmniDuration.frames(text))>1e-9)throw new AssertionError("Wrong metadata still truncates new target");
 System.out.println("Legacy voice accepted; target duration falls back independently until automatic transcript repair.");
 OmniEngine e=new OmniEngine();e.generate(models,text,"zh",1,new File(out,"valid-reference-after.wav"),new AtomicBoolean(),System.out::println,good,"None",16,42,new File(out,"after-cache"));
 try(PcmAudio.Reader r=new PcmAudio.Reader(new File(out,"valid-reference-after.wav"))){if(r.seconds()<5)throw new AssertionError("Long sentence truncated");System.out.println("VALID after seconds="+r.seconds()+" chunks="+e.chunkCount+" backboneRuns="+e.backboneRuns);}
 System.out.println("HOST after milliseconds="+e.lastGenerationMillis);
}}
