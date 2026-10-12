package com.voice.studio;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
public class GradioSpeechTest {
 static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 interface Attempt{void run()throws Exception;}static void reject(Attempt a)throws Exception{boolean failed=false;try{a.run();}catch(Exception e){failed=true;}check(failed,"failure expected");}
 static class Fake extends HttpURLConnection {
  byte[] response;int code=200;boolean closed;ByteArrayOutputStream body=new ByteArrayOutputStream();
  Fake(byte[] response)throws Exception{super(new URL("https://fixture.invalid"));this.response=response;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}public int getResponseCode(){return code;}
  public OutputStream getOutputStream(){return body;}public InputStream getInputStream(){return new ByteArrayInputStream(response);}
 }
 static Fake data(String value)throws Exception{return new Fake(value.getBytes("UTF-8"));}
 static JSONObject file(String url)throws Exception{return new JSONObject().put("url",url).put("meta",new JSONObject().put("_type","gradio.FileData"));}
 static String event(String url)throws Exception{return "event: heartbeat\ndata: null\n\nevent: complete\ndata: "+new JSONArray().put(file(url)).put("Done.")+"\n\n";}
 public static void main(String[] args)throws Exception {
  String base="https://fixture.invalid";
  check(GradioSpeechClient.base(base+"/").equals(base),"root normalization");
  for(String bad:new String[]{"http://fixture.invalid","https://user:password@fixture.invalid","https://fixture.invalid?token=x","https://huggingface.co/spaces/a/b"})reject(()->GradioSpeechClient.base(bad));
  JSONObject request=GradioSpeechClient.request("全文\n最后一句。","Chinese",1.25f,16,null);JSONArray d=request.getJSONArray("data");
  check(d.length()==15&&d.getString(0).endsWith("最后一句。"),"complete official design argument order");check(d.getInt(2)==16&&d.getDouble(5)==1.25,"steps and speed position");
  JSONObject uploaded=new JSONObject().put("path","/tmp/reference.wav");JSONArray clone=GradioSpeechClient.request("人物说话","Chinese",1,32,uploaded).getJSONArray("data");
  check(GradioSpeechClient.request("人物说话","Chinese",1,32,uploaded,"参考原文").getJSONArray("data").getString(3).equals("参考原文"),"known reference transcript skips server ASR");
  check(clone.length()==12&&clone.getJSONObject(2).getString("path").endsWith("reference.wav")&&clone.getInt(5)==32,"clone argument order");
  check(GradioSpeechClient.fileUrl(base,file(base+"/gradio_api/file=/tmp/audio.wav")).getHost().equals("fixture.invalid"),"same host audio URL");
  for(String url:new String[]{"https://evil.invalid/gradio_api/file=/x","http://fixture.invalid/gradio_api/file=/x","https://fixture.invalid:99/gradio_api/file=/x","https://user:x@fixture.invalid/gradio_api/file=/x","https://fixture.invalid/wrong"})reject(()->GradioSpeechClient.fileUrl(base,file(url)));
  for(String s:new String[]{"event: error\ndata: null\n\n","event: complete\ndata: [null,\"Error\"]\n\n","event: heartbeat\ndata: null\n\n","event: complete\ndata: rubbish\n\n"})reject(()->GradioSpeechClient.events(new ByteArrayInputStream(s.getBytes("UTF-8")),new AtomicBoolean(),m->{}));
  File dir=Files.createTempDirectory("gradio-tests").toFile(),wav=new File(dir,"input.wav"),out=new File(dir,"pending.wav");
  try{
   try(PcmAudio.Writer w=new PcmAudio.Writer(wav,24000)){float[] x=new float[24000];Arrays.fill(x,.1f);w.write(x);}
   byte[] audio=Files.readAllBytes(wav.toPath());
   for(boolean useReference:new boolean[]{false,true}){
    ArrayDeque<Fake> queue=new ArrayDeque<>();List<Fake> opened=new ArrayList<>();List<String> urls=new ArrayList<>();
    if(useReference)queue.add(data("[\"/tmp/reference.wav\"]"));
    Fake post=data("{\"event_id\":\"123abc\"}");queue.add(post);queue.add(data(event(base+"/gradio_api/file=/tmp/audio.wav")));queue.add(new Fake(audio));
    GradioSpeechClient client=new GradioSpeechClient(url->{urls.add(url.toString());Fake f=queue.remove();opened.add(f);return f;});
    check(client.generate(base,"hf_test","全文与最后一句都要读完。","Chinese",1,16,useReference?wav:null,out,new AtomicBoolean(),m->{})==1000,"SSE complete WAV validated");
    check(queue.isEmpty()&&urls.get(useReference?1:0).endsWith(useReference?"/_clone_fn":"/_design_fn"),"correct endpoint actually selected");
    check(new JSONObject(post.body.toString("UTF-8")).getJSONArray("data").getString(0).contains("最后一句"),"full text transported");
    for(Fake f:opened){check(f.closed&&!f.getInstanceFollowRedirects(),"no leaked connection or redirects");check("Bearer hf_test".equals(f.getRequestProperty("Authorization")),"own key only to chosen same host");}
    if(useReference)check(opened.get(0).body.size()>audio.length,"multipart includes reference bytes");out.delete();
   }
   for(String response:new String[]{event("https://evil.invalid/gradio_api/file=/x"),"event: error\ndata: null\n\n"}){
    ArrayDeque<Fake> queue=new ArrayDeque<>(Arrays.asList(data("{\"event_id\":\"abc\"}"),data(response)));
    reject(()->new GradioSpeechClient(u->queue.remove()).generate(base,"hf_test","全文","Chinese",1,16,null,out,new AtomicBoolean(),m->{}));check(!out.exists(),"failed response never publishes WAV");
   }
   int[] opens={0};reject(()->new GradioSpeechClient(u->{opens[0]++;return null;}).generate(base,"","全文","Chinese",1,16,null,out,new AtomicBoolean(true),m->{}));check(opens[0]==0,"pre-cancelled never submits GPU request");
   String attack="你好\n'\\\"; __import__('os').system('false') # 尾句";
   JSONObject notebook=KaggleTask.notebook(new JSONObject().put("input",attack).put("num_step",16).put("seed",42).put("token","should-not-leak"));
   check(notebook.getInt("nbformat")==4&&notebook.getJSONArray("cells").length()==3,"valid notebook structure");check(!notebook.toString().contains("should-not-leak"),"no secret field exported");
   Files.write(new File(dir,"task.ipynb").toPath(),notebook.toString().getBytes("UTF-8"));
   String code=notebook.getJSONArray("cells").getJSONObject(2).getJSONArray("source").getString(0);
   check(code.contains("torch.cuda.is_available")&&code.contains("VoiceStudio-Kaggle.wav"),"real GPU required and WAV export");
   if(args.length>0)Files.write(Paths.get(args[0]),notebook.toString().getBytes("UTF-8"));
   JSONObject job=new JSONObject().put("deduplicate",true).put("state","queued").put("engine","accelerated").put("text","你好").put("provider","hf").put("protocol","gradio").put("profile","person1");
   for(String key:new String[]{"provider","protocol","profile"})check(!SynthesisRequests.same(job,new JSONObject(job.toString()).put(key,"other")),"different platform/protocol/reference distinct");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
  System.out.println("Free GPU: "+checks+" protocol/transport/notebook checks passed.");
 }
}
