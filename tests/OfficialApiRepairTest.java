package com.voice.studio;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
/** Independent provider fixtures exercise real app request and diagnostic paths. */
public final class OfficialApiRepairTest {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 interface Call{void run()throws Exception;}
 static String failure(Call call)throws Exception{try{call.run();throw new AssertionError("Expected failure");}catch(IOException e){checks++;return e.getMessage();}}
 static final class Wire extends HttpURLConnection{
  byte[] bytes;int status;String type;boolean closed;ByteArrayOutputStream sent=new ByteArrayOutputStream();
  Wire(URL url,String data,int status)throws Exception{super(url);bytes=data.getBytes("UTF-8");this.status=status;type="application/json";}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
  public int getResponseCode(){return status;}public String getContentType(){return type;}
  public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public InputStream getErrorStream(){return new ByteArrayInputStream(bytes);}
  public OutputStream getOutputStream(){return sent;}
 }
 static String voices()throws Exception{return new JSONObject().put("base_resp",new JSONObject().put("status_code",0)).put("system_voice",new JSONArray().put(new JSONObject().put("voice_id","male-qn-qingse").put("voice_name","青年男声"))).put("voice_cloning",new JSONArray().put(new JSONObject().put("voice_id","my_clone"))).toString();}
 public static void main(String[] args)throws Exception{
  for(String id:new String[]{"fish","elevenlabs","minimax","cartesia"})check(CloudConnectionPolicy.selectOfficial("qwen3tts",id,false),"Changed platform must choose its official API");
  for(String id:new String[]{"auto","qwen3tts","indextts25","cosyvoice3","chatterboxv3","gptsovits"})check(!CloudConnectionPolicy.selectOfficial("elevenlabs",id,true),"OSS needs gateway");
  check(!CloudConnectionPolicy.selectOfficial("elevenlabs","elevenlabs",false),"Keep explicitly selected gateway for same engine");
  for(String model:new String[]{"elevenlabs","minimax","cartesia"})check(CloudConnectionPolicy.referenceProfile("official",model,"recorded-profile").isEmpty(),"Registered voice API must not inherit the global recorder profile");
  check(CloudConnectionPolicy.referenceProfile("official","fish","recorded-profile").equals("recorded-profile"),"Keep working Fish inline reference");
  check(CloudConnectionPolicy.referenceProfile("rest","qwen3tts","recorded-profile").equals("recorded-profile"),"GPU retains reference");
  String cn="https://api.minimax.cn/v1/t2a_v2",intl="https://api.minimax.io/v1/t2a_v2";
  check(CloudConnectionPolicy.endpoint("minimax",true).equals(cn),"China endpoint");check(CloudConnectionPolicy.endpoint("minimax",false).equals(intl),"International endpoint preserved");
  check(CloudConnectionPolicy.secretScope("minimax",cn).equals("official-minimax-cn"),"CN key scope");check(CloudConnectionPolicy.secretScope("minimax",intl).equals("official-minimax"),"Old IO key scope preserved");
  failure(()->CloudConnectionPolicy.validate("minimax","https://api.minimax.cn.evil.invalid/v1/t2a_v2"));failure(()->CloudConnectionPolicy.gateway(cn));
  for(String address:new String[]{cn,intl}){
   List<Wire> wires=new ArrayList<>();
   OfficialVoiceCatalog catalog=new OfficialVoiceCatalog(url->{try{Wire w=new Wire(url,voices(),200);wires.add(w);return w;}catch(Exception e){throw new IOException(e);}});
   check(catalog.minimax(address,"region-key").ids.size()==2,"Both presets and clones parsed");Wire w=wires.get(0);
   check(w.getURL().toString().equals(address.replace("t2a_v2","get_voice")),"No region failover or TTS request");check(w.getRequestMethod().equals("POST"),"Documented query uses POST");
   JSONObject sent=new JSONObject(w.sent.toString("UTF-8"));check(sent.length()==1&&sent.getString("voice_type").equals("all"),"Query contains no synthesis text");
   check(w.getRequestProperty("Authorization").equals("Bearer region-key")&&w.closed&&!w.getInstanceFollowRedirects(),"Auth, close, no redirect");
   check(catalog.verify("minimax",address,"region-key","my_clone").contains("未提交合成"),"No false synthesis acceptance");
   check(failure(()->catalog.verify("minimax",address,"region-key","missing")).contains("没有此 Voice ID"),"Unknown voice explained");
  }
  for(String engine:new String[]{"elevenlabs","cartesia"}){
   String voice=engine.equals("elevenlabs")?"JBFqnCBsd6RMkjVDRZzb":"123e4567-e89b-12d3-a456-426614174000";
   String result=new JSONObject().put(engine.equals("elevenlabs")?"voice_id":"id",voice).toString();List<Wire> wires=new ArrayList<>();
   OfficialVoiceCatalog catalog=new OfficialVoiceCatalog(url->{try{Wire w=new Wire(url,result,200);wires.add(w);return w;}catch(Exception e){throw new IOException(e);}});
   check(catalog.verify(engine,CloudConnectionPolicy.endpoint(engine),"account-key",voice).contains("未提交合成"),"Real voice check separated from TTS");
   check(wires.size()==1&&wires.get(0).getRequestMethod().equals("GET")&&wires.get(0).sent.size()==0,"Read-only GET only");
   check(wires.get(0).getURL().getPath().endsWith("/voices/"+voice),"Actual voice lookup");
  }
  String secret="private-fixture-key-123";
  JSONObject error=new JSONObject().put("detail",new JSONObject().put("status","invalid_api_key").put("message","Rejected "+secret+" Bearer hidden-other-key"));
  for(int status:new int[]{400,401,402,403,404,422,429,500}){
   Wire w=new Wire(new URL("https://api.elevenlabs.io"),error.toString(),status);
   AcceleratedSpeechClient.RequestRejected e=OfficialSpeechClient.rejection("elevenlabs",status,w,secret,new AtomicBoolean());
   check(e.status==status&&e.getMessage().contains("invalid_api_key")&&e.getMessage().contains("ElevenLabs"),"Status and provider code retained");
   check(!e.getMessage().contains(secret)&&!e.getMessage().contains("hidden-other-key"),"Credentials redacted");
   check(e.definitelyRejected()==(status!=500&&status!=402),"Uncertain 5xx stays blocked from automatic repeat");
  }
  JSONObject miniError=new JSONObject().put("base_resp",new JSONObject().put("status_code",1008).put("status_msg","insufficient balance "+secret));
  check(OfficialSpeechClient.errorMessage("minimax",200,miniError,secret).contains("1008"),"HTTP200 business error retained");
  check(!OfficialSpeechClient.errorMessage("minimax",200,miniError,secret).contains(secret),"MiniMax redacted");
  JSONObject sonic=OfficialSpeechClient.body("cartesia","123e4567-e89b-12d3-a456-426614174000","hello",.85f);
  check(Math.abs(sonic.getJSONObject("generation_config").getDouble("speed")-.85)<.0001,"Cartesia supported speed sent");
  failure(()->OfficialSpeechClient.body("cartesia","123e4567-e89b-12d3-a456-426614174000","hello",.5f));
  // Provider rejection travels through the actual pipeline, not just a parser.
  File dir=Files.createTempDirectory("api-repair").toFile();try{
   int[] calls={0};OfficialSpeechClient denied=new OfficialSpeechClient(url->{Wire w;try{w=new Wire(url,miniError.toString(),200);}catch(Exception e){throw new IOException(e);}calls[0]++;return w;},(i,o,c,p)->{throw new AssertionError("Never decode rejected response");},null,null);
   JSONObject job=new JSONObject().put("protocol","official").put("provider","custom").put("model","minimax").put("endpoint",cn).put("apiVoice","male-qn-qingse").put("text","新文字").put("speed",1);
   File out=new File(dir,"out.wav"),cache=new File(dir,"cache");String message=failure(()->RemoteSpeechPipeline.generate(job,secret,null,null,cache,out,new AtomicBoolean(),m->{},new GradioSpeechClient(),denied));
   check(message.contains("1008")&&message.contains("insufficient balance")&&!message.contains(secret),"Business error reaches task without leaking key");
   failure(()->RemoteSpeechPipeline.generate(job,secret,null,null,cache,out,new AtomicBoolean(),m->{},new GradioSpeechClient(),denied));check(calls[0]==1&&!out.exists(),"No automatic repeated synthesis or partial output");
  }finally{delete(dir);}
  System.out.println("API repair: "+checks+" routing/region/read-only/error/redaction/no-repeat checks passed; no live account synthesis.");
 }
 static void delete(File f){if(f.isDirectory())for(File child:f.listFiles())delete(child);f.delete();}
}
