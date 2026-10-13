package com.voice.studio;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
public class RemoteSpeechPipelineTest {
 static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 interface Action{void run()throws Exception;}static void reject(Action a)throws Exception{boolean failed=false;try{a.run();}catch(Exception e){failed=true;}check(failed,"expected rejection");}
 static class Fake extends HttpURLConnection {
  byte[] data;boolean fail,closed;int status=200;ByteArrayOutputStream body=new ByteArrayOutputStream();
  Fake(byte[] bytes)throws Exception{super(new URL("https://fixture.invalid"));data=bytes;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
  public int getResponseCode(){return status;}public String getContentType(){return "audio/wav";}
  public OutputStream getOutputStream(){return body;}
  public InputStream getInputStream()throws IOException{if(fail)throw new IOException("simulated connection loss");return new ByteArrayInputStream(data);}
 }
 static Fake json(String s)throws Exception{return new Fake(s.getBytes("UTF-8"));}
 static void add(ArrayDeque<Fake> q,byte[] audio)throws Exception {
  q.add(json("{\"event_id\":\"fixture123\"}"));
  q.add(json("event: complete\ndata: [{\"url\":\"https://fixture.invalid/gradio_api/file=/tmp/audio.wav\"},\"Done\"]\n\n"));
  q.add(new Fake(audio));
 }
 static JSONObject job(String text,String protocol)throws Exception{return new JSONObject().put("text",text).put("endpoint",protocol.equals("gradio")?"https://fixture.invalid":"https://fixture.invalid/v1/audio/speech")
  .put("protocol",protocol).put("provider",protocol.equals("gradio")?"hf":"modal").put("model","k2-fsa/OmniVoice").put("apiVoice","auto").put("language","zh").put("languageName","Chinese").put("steps",16);}
 static void remove(File f){if(f.isDirectory())for(File c:f.listFiles())remove(c);f.delete();}
 public static void main(String[] args)throws Exception {
  Random random=new Random(2026);String alphabet="中文。！\n abc123😀𠀀";
  for(int t=0;t<300;t++) {
   StringBuilder text=new StringBuilder("头");int length=random.nextInt(11000);for(int i=0;i<length;i++){
    int at=random.nextInt(alphabet.codePointCount(0,alphabet.length()));int pos=alphabet.offsetByCodePoints(0,at);int point=alphabet.codePointAt(pos);if(text.length()+Character.charCount(point)>11998)break;text.appendCodePoint(point);
   }text.append("尾");
   List<String> split=RemoteSpeechPipeline.split(text.toString(),180);check(String.join("",split).equals(text.toString()),"lossless Unicode/sentence splitting");
   for(String s:split){check(!Character.isHighSurrogate(s.charAt(s.length()-1))&&!Character.isLowSurrogate(s.charAt(0)),"surrogate pairs stay together");}
  }
  reject(()->RemoteSpeechPipeline.split(" ",180));reject(()->RemoteSpeechPipeline.split(String.join("",Collections.nCopies(12001,"字")),180));
  check(String.join("",RemoteSpeechPipeline.split("字"+String.join("",Collections.nCopies(11000," "))+"尾",180)).endsWith("尾"),"large internal whitespace preserved");
  File root=Files.createTempDirectory("remote-pipeline").toFile(),a=new File(root,"a.wav"),b=new File(root,"b.wav"),out=new File(root,"result.wav");
  try {
   try(PcmAudio.Writer w=new PcmAudio.Writer(a,24000)){float[] x=new float[24000*6];Arrays.fill(x,.1f);w.write(x);}
   try(PcmAudio.Writer w=new PcmAudio.Writer(b,24000)){float[] x=new float[24000*6];Arrays.fill(x,.2f);w.write(x);}
   byte[] first=Files.readAllBytes(a.toPath()),second=Files.readAllBytes(b.toPath());
   String text=String.join("",Collections.nCopies(12,"全文必须保留最后一句。"))+"最后一个人物也要完整说完这句话。";
   // Ensure exactly two realistic text chunks independent of punctuation placement.
   text=String.join("",Collections.nCopies(190,"字"))+"终";
   JSONObject task=job(text,"gradio");List<String> parts=RemoteSpeechPipeline.split(text,180);check(parts.size()==2,"two-chunk control");
   ArrayDeque<Fake> q=new ArrayDeque<>();List<String> urls=new ArrayList<>();List<Fake> opened=new ArrayList<>();
   AcceleratedSpeechClient.Connections transport=u->{urls.add(u.toString());Fake f=q.remove();opened.add(f);return f;};
   GradioSpeechClient client=new GradioSpeechClient(transport);AcceleratedSpeechClient rest=new AcceleratedSpeechClient(transport);
   File folder=new File(root,"checkpoint");q.add(json("[\"/tmp/reference.wav\"]"));add(q,first);add(q,second);Fake bad=q.getLast();bad.fail=true;
   reject(()->RemoteSpeechPipeline.generate(task,"fixture-token",a,"参考文字",folder,out,new AtomicBoolean(),m->{},client,rest));
   check(!out.exists()&&new File(folder,"part-0000.wav").isFile()&&!new File(folder,"part-0001.wav").exists(),"completed chunk preserved, partial output removed");
   check(urls.stream().filter(u->u.endsWith("/upload")).count()==1,"one reference upload for both chunks");
   check(!RemoteSpeechSession.needsResubmit(folder),"known audio URL can retry download without new GPU call");
   JSONObject state=RemoteSpeechSession.read(new File(folder,"session.json"));check(state.getJSONArray("segments").getJSONObject(1).getString("phase").equals("download"),"audio URL persisted before transfer");
   int before=urls.size();q.add(new Fake(second));
   check(RemoteSpeechPipeline.generate(task,"fixture-token",a,"参考文字",folder,out,new AtomicBoolean(),m->{},client,rest)==12000,"download-only recovery duration");
   check(urls.size()==before+1&&urls.get(before).contains("/file="),"no reference re-upload or POST on download recovery");
   byte[] joined=Files.readAllBytes(out.toPath());check(Arrays.equals(Arrays.copyOfRange(joined,44,44+(first.length-44)),Arrays.copyOfRange(first,44,first.length)),"first chunk PCM bytes unchanged");
   check(Arrays.equals(Arrays.copyOfRange(joined,44+(first.length-44),44+(first.length-44)+(second.length-44)),Arrays.copyOfRange(second,44,second.length)),"second chunk PCM bytes unchanged; no samples lost at boundary");
   out.delete();before=urls.size();check(RemoteSpeechPipeline.generate(task,"fixture-token",a,"参考文字",folder,out,new AtomicBoolean(),m->{},client,rest)==12000&&urls.size()==before,"all completed chunks reused without any HTTP");
   out.delete();JSONObject changed=new JSONObject(task.toString()).put("steps",32);reject(()->RemoteSpeechPipeline.generate(changed,"fixture-token",a,"参考文字",folder,out,new AtomicBoolean(),m->{},client,rest));check(urls.size()==before,"changed settings cannot mix cached output");
   File uncertain=new File(root,"uncertain");add(q,first);q.add(json("{\"event_id\":\"fixture123\"}"));Fake missing=json("");missing.fail=true;q.add(missing);
   reject(()->RemoteSpeechPipeline.generate(task,"",null,null,uncertain,out,new AtomicBoolean(),m->{},client,rest));check(RemoteSpeechSession.needsResubmit(uncertain),"interrupted SSE submission is ambiguous");
   before=urls.size();reject(()->RemoteSpeechPipeline.generate(task,"",null,null,uncertain,out,new AtomicBoolean(),m->{},client,rest));check(urls.size()==before,"ambiguous submission not silently repeated");
   RemoteSpeechSession.allowResubmit(uncertain);add(q,second);check(RemoteSpeechPipeline.generate(task,"",null,null,uncertain,out,new AtomicBoolean(),m->{},client,rest)==12000,"explicit resubmit recovers remaining chunk");
   check(urls.size()==before+3,"explicit retry only resubmits unfinished chunk");out.delete();
   AtomicBoolean cancel=new AtomicBoolean();reject(()->RemoteSpeechPipeline.generate(task,"",null,null,uncertain,out,cancel,m->{if(m.contains("正在合并"))cancel.set(true);},client,rest));check(!out.exists(),"cancel during final join leaves no successful partial WAV");
   check(new File(uncertain,"part-0000.wav").isFile()&&new File(uncertain,"part-0001.wav").isFile(),"cancelled join preserves both chunks");
   File shortFile=new File(root,"short.wav");try(PcmAudio.Writer w=new PcmAudio.Writer(shortFile,24000)){float[] x=new float[24000];Arrays.fill(x,.1f);w.write(x);}
   JSONObject shortTask=job(String.join("",Collections.nCopies(100,"字")),"gradio");File shortFolder=new File(root,"short-state");add(q,Files.readAllBytes(shortFile.toPath()));
   reject(()->RemoteSpeechPipeline.generate(shortTask,"",null,null,shortFolder,out,new AtomicBoolean(),m->{},client,rest));
   check(RemoteSpeechSession.needsResubmit(shortFolder),"one-word-length server result needs explicit re-synthesis, not repeated download");
   check(RemoteSpeechSession.read(new File(shortFolder,"session.json")).getJSONArray("segments").getJSONObject(0).optJSONObject("audio")==null,"bad short audio URL removed");
   check(!RemoteSpeechSession.read(new File(folder,"session.json")).toString().contains("fixture-token"),"checkpoints contain no credentials");
   // REST path stores chunks too and needs consent after an uncertain direct POST.
   File restFolder=new File(root,"rest");JSONObject restTask=job(String.join("",Collections.nCopies(2100,"字")),"rest");File longWav=new File(root,"rest-long.wav");try(PcmAudio.Writer w=new PcmAudio.Writer(longWav,24000)){float[] x=new float[24000*70];Arrays.fill(x,.1f);w.write(x);}
   byte[] restAudio=Files.readAllBytes(longWav.toPath());q.add(new Fake(restAudio));Fake lost=new Fake(second);lost.fail=true;q.add(lost);
   reject(()->RemoteSpeechPipeline.generate(restTask,"",null,null,restFolder,out,new AtomicBoolean(),m->{},client,rest));check(RemoteSpeechSession.needsResubmit(restFolder),"REST failed transfer is not retried automatically");
   RemoteSpeechSession.allowResubmit(restFolder);q.add(new Fake(second));check(RemoteSpeechPipeline.generate(restTask,"",null,null,restFolder,out,new AtomicBoolean(),m->{},client,rest)==76000,"REST preserves completed chunk on retry");out.delete();
   try(RandomAccessFile f=new RandomAccessFile(new File(folder,"part-0000.wav"),"rw")){f.seek(44);f.write(7);}
   reject(()->RemoteSpeechPipeline.generate(task,"fixture-token",a,"参考文字",folder,out,new AtomicBoolean(),m->{},client,rest));check(!out.exists(),"corrupt cached PCM rejected");
   for(String protocol:new String[]{"gradio","rest"})for(int code:new int[]{401,429}) {
    File rejected=new File(root,protocol+code);Fake response=json("{}");response.status=code;q.add(response);
    reject(()->RemoteSpeechPipeline.generate(job("接口检查文字",protocol),"",null,null,rejected,out,new AtomicBoolean(),m->{},client,rest));
    check(!RemoteSpeechSession.needsResubmit(rejected),"known authentication/quota rejection has no uncertain GPU submission");
   }
   for(Fake f:opened)check(f.closed&&!f.getInstanceFollowRedirects(),"all mocked network handles closed and redirects disabled");
   check(q.isEmpty(),"every planned mock was used; no hidden additional requests");
  }finally{remove(root);}
  System.out.println("Remote pipeline: "+checks+" Unicode/checkpoint/transport/PCM/consent checks passed; mocked speech only.");
 }
}
