package com.voice.studio;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
public class AcceleratedSpeechTest {
 static int checks;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 interface Attempt{void run()throws Exception;}
 static void reject(Attempt action)throws Exception{boolean failed=false;try{action.run();}catch(IOException expected){failed=true;}check(failed,"invalid request rejected");}
 static class Fake extends HttpURLConnection {
  byte[] audio;int status=200;String type="audio/wav";boolean disconnected;AtomicBoolean cancel;
  ByteArrayOutputStream sent=new ByteArrayOutputStream();
  Fake(byte[] data)throws Exception{super(new URL("https://example.invalid/v1/audio/speech"));audio=data;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){disconnected=true;}
  public int getResponseCode(){return status;}public String getContentType(){return type;}
  public OutputStream getOutputStream(){return sent;}
  public InputStream getInputStream(){return new ByteArrayInputStream(audio){public int read(byte[] b,int off,int len){int n=super.read(b,off,len);if(cancel!=null)cancel.set(true);return n;}};}
 }
 static long send(Fake connection,File output,AtomicBoolean cancel)throws Exception {
  return new AcceleratedSpeechClient(url->{check(url.toString().endsWith("/v1/audio/speech"),"normalized URL used");return connection;})
    .generate("https://example.invalid/v1","test-key","k2-fsa/OmniVoice","auto","你好，这是一次完整语音生成测试。","zh",1,16,42,output,cancel,m->{});
 }
 public static void main(String[] args)throws Exception {
  for(String url:new String[]{"https://example.invalid","https://example.invalid/","https://example.invalid/v1/","https://example.invalid/v1/audio/speech"})check(AcceleratedSpeechClient.endpoint(url).equals("https://example.invalid/v1/audio/speech"),"equivalent addresses");
  for(String bad:new String[]{"","http://example.invalid","https://user:secret@example.invalid","https://example.invalid?key=secret","https://example.invalid/#frag"})reject(()->AcceleratedSpeechClient.endpoint(bad));
  reject(()->AcceleratedSpeechClient.token("x\r\nsecret"));reject(()->AcceleratedSpeechClient.token("x y"));
  JSONObject body=AcceleratedSpeechClient.request("完整内容\n不能丢掉结尾。","m","auto","zh",1.5f,16,123);
  check(body.getString("input").endsWith("结尾。"),"entire multiline text sent");check(body.getInt("num_step")==16&&body.getLong("seed")==123,"requested settings sent");check(body.getDouble("speed")==1.5,"speed sent");check(!body.has("token")&&!body.has("profile")&&!body.has("ref_audio"),"no key or unrequested reference upload in JSON");
  reject(()->AcceleratedSpeechClient.request("x","m","auto","zh",1,7,42));
  reject(()->AcceleratedSpeechClient.request("x","m","auto","zh",Float.NaN,16,42));
  File dir=Files.createTempDirectory("accelerated-speech").toFile(),wav=new File(dir,"fixture.wav"),out=new File(dir,"pending.wav");
  try {
   try(PcmAudio.Writer w=new PcmAudio.Writer(wav,24000)){float[] x=new float[24000];Arrays.fill(x,.1f);w.write(x);}
   byte[] data=Files.readAllBytes(wav.toPath());Fake success=new Fake(data);
   check(send(success,out,new AtomicBoolean())==1000,"real WAV duration validated");
   check(success.disconnected&&!success.getInstanceFollowRedirects(),"closed and redirects rejected");
   check("Bearer test-key".equals(success.getRequestProperty("Authorization")),"key only in auth header");
   check(new JSONObject(success.sent.toString("UTF-8")).getString("input").contains("完整"),"full request transported");out.delete();
   for(int code:new int[]{301,400,401,403,429,500}){Fake failed=new Fake(data);failed.status=code;reject(()->send(failed,out,new AtomicBoolean()));check(!out.exists()&&failed.disconnected,"error leaves no published output");}
   for(byte[] bad:new byte[][]{new byte[0],new byte[128],Arrays.copyOf(data,100)}){Fake invalid=new Fake(bad);reject(()->send(invalid,out,new AtomicBoolean()));check(!out.exists(),"empty/corrupt/truncated WAV removed");}
   Fake text=new Fake(data);text.type="APPLICATION/JSON";reject(()->send(text,out,new AtomicBoolean()));
   try(PcmAudio.Writer w=new PcmAudio.Writer(wav,24000)){w.write(new float[100]);}
   Fake shortAudio=new Fake(Files.readAllBytes(wav.toPath()));reject(()->send(shortAudio,out,new AtomicBoolean()));check(!out.exists(),"long text with a few milliseconds of audio not accepted");
   AtomicBoolean cancelled=new AtomicBoolean(true);int[] opens={0};reject(()->new AcceleratedSpeechClient(url->{opens[0]++;return success;}).generate("https://example.invalid","","m","auto","x","zh",1,16,42,out,cancelled,m->{}));check(opens[0]==0,"pre-cancelled request never connects");
   cancelled.set(false);Fake interrupted=new Fake(data);interrupted.cancel=cancelled;reject(()->send(interrupted,out,cancelled));check(!out.exists()&&interrupted.disconnected,"cancel during transfer cleans output");
   JSONObject job=new JSONObject().put("type","tts").put("state","queued").put("deduplicate",true).put("engine","accelerated").put("text","你好").put("endpoint","https://example.invalid/v1/audio/speech").put("apiVoice","auto").put("model","m");
   check(SynthesisRequests.same(job,new JSONObject(job.toString())),"same API request deduplicated");
   for(String key:new String[]{"endpoint","model","apiVoice","language"})check(!SynthesisRequests.same(job,new JSONObject(job.toString()).put(key,"changed")),"changed endpoint/voice/model/language distinct");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
  System.out.println("Accelerated API: "+checks+" checks passed; mock transport only, no live GPU/server speed claim.");
 }
}
