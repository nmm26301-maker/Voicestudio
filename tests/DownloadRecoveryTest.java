package com.voice.studio;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class DownloadRecoveryTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static class Fake extends HttpURLConnection {
  byte[] data;int code=200;long length=-1;String type="audio/wav";IOException error;boolean closed;ByteArrayOutputStream body=new ByteArrayOutputStream();
  Fake(byte[] data)throws Exception{super(new URL("https://fixture.invalid"));this.data=data;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
  public int getResponseCode(){return code;}public long getContentLengthLong(){return length;}public String getContentType(){return type;}
  public OutputStream getOutputStream(){return body;}
  public InputStream getInputStream()throws IOException{if(error!=null)throw error;return new ByteArrayInputStream(data);}
 }
 static Fake text(String s)throws Exception{return new Fake(s.getBytes("UTF-8"));}
 public static void main(String[] args)throws Exception {
  File dir=Files.createTempDirectory("download-recovery").toFile(),source=new File(dir,"source.wav"),out=new File(dir,"output.wav");
  try {
   try(PcmAudio.Writer w=new PcmAudio.Writer(source,24000)){float[] x=new float[24000];Arrays.fill(x,.1f);w.write(x);}
   byte[] audio=Files.readAllBytes(source.toPath());
   for(String kind:new String[]{"timeout","reset","short-body","502","503","504","limit","401","403","404","429","html","oversize","cancel","sse","post"}) {
    ArrayDeque<Fake> queue=new ArrayDeque<>();List<Fake> opened=new ArrayList<>();List<String> urls=new ArrayList<>();AtomicBoolean cancel=new AtomicBoolean();
    Fake post=text("{\"event_id\":\"test123\"}"),sse=text("event: complete\ndata: [{\"url\":\"/gradio_api/file=/audio.wav\"}]\n\n"),bad=new Fake(audio);
    queue.add(post);queue.add(sse);
    if(kind.equals("post"))post.error=new SocketTimeoutException("post response lost");
    if(kind.equals("sse"))sse.error=new SocketTimeoutException("queue lost");
    if(Arrays.asList("timeout","limit","cancel").contains(kind))bad.error=new SocketTimeoutException("read timeout");
    if(kind.equals("reset"))bad.error=new SocketException("reset");
    if(kind.equals("short-body"))bad.length=audio.length+500;
    if(kind.matches("[0-9]+"))bad.code=Integer.parseInt(kind);
    if(kind.equals("html"))bad.type="text/html";
    if(kind.equals("oversize"))bad.length=64L*1024*1024+1;
    queue.add(bad);
    boolean success=Arrays.asList("timeout","reset","short-body","502","503","504").contains(kind);
    if(success)queue.add(new Fake(audio));
    if(kind.equals("limit"))for(int i=0;i<2;i++){Fake f=new Fake(audio);f.error=new SocketTimeoutException();queue.add(f);}
    GradioSpeechClient client=new GradioSpeechClient(u->{urls.add(u.toString());Fake f=queue.remove();opened.add(f);return f;});
    boolean failed=false;try {
     long duration=client.generate("https://fixture.invalid","test-token","下载恢复测试。","Chinese",1,16,null,out,cancel,m->{if(kind.equals("cancel")&&m.contains("正在重试"))cancel.set(true);});
     check(duration==1000,"complete WAV duration");
    }catch(IOException expected){failed=true;}
    check(failed!=success,"correct result for "+kind);check(out.exists()==success,"no failed partial output "+kind);
    check(opened.stream().filter(f->f.getRequestMethod().equals("POST")).count()==1,"no synthesis resubmission "+kind);
    long downloads=urls.stream().filter(u->u.contains("/file=")).count();
    check(downloads==(success?2:kind.equals("limit")?3:kind.equals("post")||kind.equals("sse")?0:1),"bounded GET-only retry "+kind);
    for(Fake f:opened)check(f.closed&&!f.getInstanceFollowRedirects()&&"Bearer test-token".equals(f.getRequestProperty("Authorization")),"connection closed; same-host credentials preserved");
    out.delete();
   }
  }finally{source.delete();out.delete();dir.delete();}
  System.out.println("Download recovery: "+checks+" transient/permanent/cancel/no-repeat-POST checks passed (mock transport).");
 }
}
