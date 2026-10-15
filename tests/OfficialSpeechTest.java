package com.voice.studio;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
/** No real credentials, provider request or device codec; injectable transport/decoder. */
public class OfficialSpeechTest {
 static int checks;
 static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 interface Attempt {void run()throws Exception;}
 static void reject(Attempt call)throws Exception{try{call.run();throw new AssertionError("Should reject");}catch(IOException expected){checks++;}}
 static byte[] mp3(){byte[] x=new byte[128];x[0]='I';x[1]='D';x[2]='3';return x;}
 static String hex(byte[] data){StringBuilder b=new StringBuilder();for(byte x:data)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
 static class Fake extends HttpURLConnection {
  final ByteArrayOutputStream sent=new ByteArrayOutputStream();byte[] data;int status=200;String type;boolean closed;AtomicBoolean cancel;
  Fake(URL u,byte[] data,String type){super(u);this.data=data;this.type=type;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
  public int getResponseCode(){return status;}public String getContentType(){return type;}public OutputStream getOutputStream(){return sent;}
  public InputStream getInputStream(){return new ByteArrayInputStream(data){public int read(byte[] b,int off,int len){int n=super.read(b,off,len);if(cancel!=null)cancel.set(true);return n;}};}
 }
 static OfficialSpeechClient.Decoder decoder=(in,out,cancel,progress)->{
  check(Arrays.equals(Files.readAllBytes(in.toPath()),mp3()),"Full downloaded bytes passed to decoder");
  try(PcmAudio.Writer writer=new PcmAudio.Writer(out,24000)){float[] samples=new float[12*24000];for(int i=0;i<samples.length;i++)samples[i]=(float)Math.sin(i*.1)*.2f;writer.write(samples);}
 };
 static String voice(String engine){return engine.equals("fish")?"0123456789abcdef0123456789abcdef":"Voice123";}
 static JSONObject job(String model,String text)throws Exception{return new JSONObject().put("protocol","official").put("provider","custom").put("model",model).put("endpoint",CloudConnectionPolicy.endpoint(model)).put("apiVoice",voice(model)).put("text",text).put("speed",1);}
 public static void main(String[] args)throws Exception{
  File folder=Files.createTempDirectory("official-speech").toFile();File out=new File(folder,"out.wav"),ref=new File(folder,"ref.wav");
  try{
   for(String model:new String[]{"fish","elevenlabs","minimax"}){
    byte[] data=model.equals("minimax")?new JSONObject().put("base_resp",new JSONObject().put("status_code",0)).put("data",new JSONObject().put("status",2).put("audio",hex(mp3()))).toString().getBytes("UTF-8"):mp3();
    List<Fake> calls=new ArrayList<>();OfficialSpeechClient client=new OfficialSpeechClient(url->{Fake c=new Fake(url,data,model.equals("minimax")?"application/json":"audio/mpeg");calls.add(c);return c;},decoder,null,null);
    long duration=client.generate(CloudConnectionPolicy.endpoint(model),"fixture-key",model,voice(model),"新的完整句子，最后一句。","zh",1,16,42,out,new AtomicBoolean(),m->{});
    check(duration==12000,"PCM validated");check(calls.size()==1,"One POST only");Fake c=calls.get(0);check(c.closed&&!c.getInstanceFollowRedirects(),"Closed, no redirect");check(c.getRequestMethod().equals("POST"),"Correct method");
    check(c.getURL().toString().startsWith(CloudConnectionPolicy.endpoint(model)),"Official URL only");
    check((model.equals("elevenlabs")?"fixture-key":"Bearer fixture-key").equals(c.getRequestProperty(model.equals("elevenlabs")?"xi-api-key":"Authorization")),"Correct auth header");
    JSONObject body=new JSONObject(c.sent.toString("UTF-8"));check(body.getString("text").endsWith("最后一句。"),"Full new text in official payload");check(!body.has("token")&&!body.has("api_key"),"Key not in payload");out.delete();
    reject(()->client.generate("https://other.invalid/v1", "fixture-key",model,voice(model),"x","zh",1,16,42,out,new AtomicBoolean(),m->{}));check(calls.size()==1,"Mismatched host never called");
   }
   // Cartesia Sonic returns PCM WAV bytes and must bypass the MP3 decoder.
   String cartesiaId="123e4567-e89b-12d3-a456-426614174000";
   File sonicFile=new File(folder,"cartesia.wav");
   try(PcmAudio.Writer writer=new PcmAudio.Writer(sonicFile,24000)){
     float[] samples=new float[5*24000];for(int i=0;i<samples.length;i++)samples[i]=(float)Math.sin(i*.1)*.18f;writer.write(samples);
   }
   List<Fake> cartesiaCalls=new ArrayList<>();
   OfficialSpeechClient sonic=new OfficialSpeechClient(url->{
       Fake f=new Fake(url,Files.readAllBytes(sonicFile.toPath()),"audio/wav");cartesiaCalls.add(f);return f;
     },(in,file,cancel,progress)->{throw new AssertionError("WAV must bypass MP3 decoder");},null,null);
   long millisSonic=sonic.generate(CloudConnectionPolicy.endpoint("cartesia"),"cartesia-fixture","cartesia",
       cartesiaId,"完整的 Cartesia 文本，最后一句。","zh",1,16,42,out,new AtomicBoolean(),m->{});
   check(millisSonic==5000,"Cartesia WAV preserved");
   check(cartesiaCalls.size()==1&&cartesiaCalls.get(0).closed,"Cartesia one POST");
   Fake cts=cartesiaCalls.get(0);
   check("Bearer cartesia-fixture".equals(cts.getRequestProperty("Authorization")),"Cartesia key header");
   check("2026-08-14".equals(cts.getRequestProperty("Cartesia-Version")),"Cartesia API version");
   JSONObject cbody=new JSONObject(cts.sent.toString("UTF-8"));
   check(cbody.getString("transcript").endsWith("最后一句。"),"Cartesia full text");
   check(cbody.getString("voice").equals(cartesiaId),"Current Cartesia version uses string voice ID");
   check(cbody.getJSONObject("output_format").getString("container").equals("wav"),"Cartesia WAV request");
   reject(()->OfficialSpeechClient.body("cartesia","auto","hello",1));
   reject(()->OfficialSpeechClient.body("cartesia",cartesiaId,"hello",1.6f));
   reject(()->CloudConnectionPolicy.gateway("https://api.cartesia.ai/tts/bytes"));
   out.delete();
   for(String bad:new String[]{"","00g0","fff"})reject(()->OfficialSpeechClient.decodeHex(bad));check(Arrays.equals(OfficialSpeechClient.decodeHex("0041ff"),new byte[]{0,65,-1}),"Hex decoding");
   reject(()->OfficialSpeechClient.body("elevenlabs","Voice123","text",2));reject(()->CloudConnectionPolicy.gateway("https://api.fish.audio/v1/tts"));
   reject(()->CloudConnectionPolicy.endpoint("qwen3tts"));
   check(CloudConnectionPolicy.gateway("https://my.invalid/v1").equals("https://my.invalid/v1/audio/speech"),"Custom gateway normalized");
   for(String model:CloudEngineCatalog.IDS){check(CloudConnectionPolicy.account(model).startsWith("https://"),"Account link HTTPS");check(CloudConnectionPolicy.keys(model).startsWith("https://"),"Key link HTTPS");}
   try(PcmAudio.Writer writer=new PcmAudio.Writer(ref,24000)){float[] samples=new float[5*24000];for(int i=0;i<samples.length;i++)samples[i]=(float)Math.sin(i*.2)*.1f;writer.write(samples);}
   List<Fake> calls=new ArrayList<>();OfficialSpeechClient inline=new OfficialSpeechClient(url->{Fake c=new Fake(url,mp3(),"audio/mpeg");calls.add(c);return c;},decoder,ref,"录音原文。");
   JSONObject job=job("fish","新文字🙂。末句也要保留。"+String.join("",Collections.nCopies(70,"完整段落。")));
   job.put("profile","reference-fixture");File cache=new File(folder,"cache");
   RemoteSpeechPipeline.generate(job,"fixture-key",ref,"录音原文。",cache,out,new AtomicBoolean(),m->{},new GradioSpeechClient(),inline);
   check(calls.size()==RemoteSpeechPipeline.split(job.getString("text"),180).size(),"One official POST for every text chunk");
   check(calls.stream().allMatch(c->c.getURL().toString().equals("https://api.fish.audio/v1/tts")),"No gateway reference upload");
   check(calls.get(0).getRequestProperty("Content-Type").equals("application/msgpack"),"Inline clone uses MessagePack");
   Files.write(Paths.get(args[0]),calls.get(0).sent.toByteArray());
   int previous=calls.size();RemoteSpeechPipeline.generate(job,"fixture-key",ref,"录音原文。",cache,out,new AtomicBoolean(),m->{},new GradioSpeechClient(),inline);check(calls.size()==previous,"Completed speech resumed with zero repeated POSTs");
   File failedCache=new File(folder,"failed-cache");int[] failedCalls={0};OfficialSpeechClient failing=new OfficialSpeechClient(url->{failedCalls[0]++;Fake c=new Fake(url,mp3(),"audio/mpeg");c.status=500;return c;},decoder,null,null);
   JSONObject failed=job("fish","超时不能重复计费。");reject(()->RemoteSpeechPipeline.generate(failed,"fixture-key",null,null,failedCache,out,new AtomicBoolean(),m->{},new GradioSpeechClient(),failing));reject(()->RemoteSpeechPipeline.generate(failed,"fixture-key",null,null,failedCache,out,new AtomicBoolean(),m->{},new GradioSpeechClient(),failing));check(failedCalls[0]==1,"Uncertain submission never repeated");
   AtomicBoolean cancel=new AtomicBoolean(true);reject(()->inline.generate(CloudConnectionPolicy.endpoint("fish"),"fixture-key","fish","auto","取消测试","zh",1,16,42,out,cancel,m->{}));check(calls.size()==previous,"Cancellation before request");
   OfficialSpeechClient wrongTranscript=new OfficialSpeechClient(url->{throw new AssertionError("Missing reference text called provider");},decoder,ref,"");reject(()->wrongTranscript.generate(CloudConnectionPolicy.endpoint("fish"),"fixture-key","fish","auto","新文字","zh",1,16,42,out,new AtomicBoolean(),m->{}));
  }finally{delete(folder);}
  System.out.println("Official APIs: "+checks+" host/mock request, reference, decode-boundary, resume and cancellation checks passed.");
 }
 static void delete(File f){if(f.isDirectory())for(File x:f.listFiles())delete(x);f.delete();}
}
