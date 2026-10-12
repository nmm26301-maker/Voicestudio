package com.voice.studio;
import java.io.File;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class LiveGradioSpeech {
 public static void main(String[] args)throws Exception {
  File out=new File(args[0]);if(out.exists())throw new IllegalArgumentException("new output path required");long start=System.nanoTime();
  long duration=new GradioSpeechClient(url -> {
    String proxy=System.getenv("HTTPS_PROXY");
    if(proxy==null||proxy.isEmpty())return (HttpURLConnection)url.openConnection();
    URI p=URI.create(proxy);return (HttpURLConnection)url.openConnection(new Proxy(Proxy.Type.HTTP,new InetSocketAddress(p.getHost(),p.getPort())));
   }).generate(GradioSpeechClient.OFFICIAL,"","你好，欢迎来到声音工作室。今天测试免费 GPU 的完整语音合成。","Chinese",1,16,args.length>1?new File(args[1]):null,args.length>1?"你好，欢迎来到声音工作室。今天测试免费 GPU 的完整语音合成。":null,out,new AtomicBoolean(),System.out::println);
  System.out.println("HF live duration="+duration+"ms elapsed="+(System.nanoTime()-start)/1000000+"ms bytes="+out.length()+"; file validation, not semantic acceptance or phone benchmark");
 }
}
