package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
public class LiveGradioPipeline {
 public static void main(String[] args)throws Exception {
  File folder=new File(args[0]),output=new File(folder.getParentFile(),"hf-pipeline-1.5.10.wav");
  boolean cached=folder.exists();
  String originalHash=cached?RemoteSpeechSession.hash(output):null;
  if(cached)output=new File(folder.getParentFile(),"hf-pipeline-validated-1.5.10.wav");
  if(output.exists())throw new IOException("fresh result path required");
  String text="你好，欢迎来到声音工作室。今天我们使用免费的计算资源，测试较长文字的完整语音合成。程序会按句子分段处理，再把收到的音频按原来的顺序合并。每一个句子都应该保留，第一段说完后还有后面的内容。如果网络中断，已经完成的片段会保存在手机里。恢复时可以复用这些片段，避免从头再做。我们还要检查英文标点、中文标点和换行是否影响分段。测试结束后，请认真试听开头、中间和结尾，确认最后这句话也能听到。";
  JSONObject job=new JSONObject().put("text",text).put("endpoint",GradioSpeechClient.OFFICIAL).put("protocol","gradio").put("provider","hf")
   .put("model","k2-fsa/OmniVoice").put("apiVoice","auto").put("language","zh").put("languageName","Chinese").put("steps",16);
  final int[] calls={0};AcceleratedSpeechClient.Connections network=u->{if(cached)throw new AssertionError("Unexpected network call while verifying completed cache");calls[0]++;String proxy=System.getenv("HTTPS_PROXY");
   if(proxy==null||proxy.isEmpty())return (HttpURLConnection)u.openConnection();URI p=URI.create(proxy);return (HttpURLConnection)u.openConnection(new Proxy(Proxy.Type.HTTP,new InetSocketAddress(p.getHost(),p.getPort())));};
  long begin=System.nanoTime();long duration=RemoteSpeechPipeline.generate(job,"",null,null,folder,output,new AtomicBoolean(),System.out::println,new GradioSpeechClient(network),new AcceleratedSpeechClient(network));
  if(cached&&!RemoteSpeechSession.hash(output).equals(originalHash))throw new AssertionError("current merge differs from actual live WAV");
  System.out.println((cached?"Prior HF live cache verified with current pipeline; ":"HF live pipeline; ")+"duration="+duration+"ms elapsed="+(System.nanoTime()-begin)/1000000+"ms chunks="+RemoteSpeechPipeline.split(text,180).size()+" HTTP="+calls[0]);
  int initial=calls[0];File resumed=new File(folder.getParentFile(),cached?"hf-pipeline-reused-validated-1.5.10.wav":"hf-pipeline-reused-1.5.10.wav");begin=System.nanoTime();
  RemoteSpeechPipeline.generate(job,"",null,null,folder,resumed,new AtomicBoolean(),System.out::println,new GradioSpeechClient(network),new AcceleratedSpeechClient(network));
  if(calls[0]!=initial||!RemoteSpeechSession.hash(output).equals(RemoteSpeechSession.hash(resumed)))throw new AssertionError("cache recovery differs or sent HTTP");
  System.out.println("Completed cache reuse elapsed="+(System.nanoTime()-begin)/1000000+"ms additionalHTTP=0; no semantic or phone speed acceptance");
 }
}
