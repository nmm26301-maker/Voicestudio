package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class EncodingCacheTest {
 static int checks;static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 static void remove(File f){if(f.isDirectory())for(File c:f.listFiles())remove(c);f.delete();}
 public static void main(String[] args)throws Exception {
  File root=Files.createTempDirectory("encoding-cache").toFile(),models=new File(root,"models"),folder=new File(root,"cache");models.mkdirs();
  try {
   File tokenizer=new File(models,"tokenizer.json"),graph=new File(models,"graph.onnx");Files.write(tokenizer.toPath(),"{}".getBytes("UTF-8"));Files.write(graph.toPath(),new byte[10]);
   OmniEncodingCache cache=new OmniEncodingCache(folder,models);long[] codes=new long[8*250];for(int i=0;i<codes.length;i++)codes[i]=i%1024;
   VoicePrompt ref=new VoicePrompt("参考文字",new long[8*100],100,.05);
   String key=cache.key("完整文字","zh",1,ref,"None",16,42);check(cache.read(0,key,250)==null,"empty cache is a miss");cache.save(0,key,250,codes);
   check(Arrays.equals(cache.read(0,key,250).codes,codes),"exact codes recovered");
   for(String different:new String[]{cache.key("其他文字","zh",1,ref,"None",16,42),cache.key("完整文字","en",1,ref,"None",16,42),cache.key("完整文字","zh",1.5f,ref,"None",16,42),cache.key("完整文字","zh",1,ref,"Happy",16,42),cache.key("完整文字","zh",1,ref,"None",8,42),cache.key("完整文字","zh",1,ref,"None",16,43),cache.key("完整文字","zh",1,null,"None",16,42)})check(cache.read(0,different,250)==null,"settings changes invalidate cached tokens");
   ref.codes[0]=1;check(!key.equals(cache.key("完整文字","zh",1,ref,"None",16,42)),"reference acoustic codes part of identity");
   File pending=new File(cache.path(0)+".pending");Files.write(pending.toPath(),new byte[500]);check(cache.read(0,key,250)!=null,"pending crash file cannot replace completed entry");pending.delete();
   byte[] original=Files.readAllBytes(cache.path(0).toPath());
   for(int position:new int[]{0,80,original.length/2,original.length-1}){byte[] bad=original.clone();bad[position]^=1;Files.write(cache.path(0).toPath(),bad);check(cache.read(0,key,250)==null&&!cache.path(0).exists(),"corrupted bytes trigger recomputation, never decoder input");}
   Files.write(cache.path(0).toPath(),Arrays.copyOf(original,original.length-2));check(cache.read(0,key,250)==null,"truncated cache rejected");
   cache.save(0,key,250,codes);check(cache.read(0,key,249)==null,"wrong frame count rejected");
   Files.write(cache.path(0).toPath(),new byte[17001]);check(cache.read(0,key,250)==null,"bounded cache input");
   boolean failed=false;codes[0]=1024;try{cache.save(0,key,250,codes);}catch(IOException expected){failed=true;}check(failed&&!cache.path(0).exists(),"unfinished mask token never cached");codes[0]=0;
   graph.setLastModified(graph.lastModified()+10000);check(!cache.model.equals(new OmniEncodingCache(folder,models).model),"graph replacement metadata invalidates cache");
   String afterGraph=new OmniEncodingCache(folder,models).model;
   long timestamp=tokenizer.lastModified();Files.write(tokenizer.toPath(),"[]".getBytes("UTF-8"));tokenizer.setLastModified(timestamp);check(!afterGraph.equals(new OmniEncodingCache(folder,models).model),"same-size/same-date tokenizer changes caught by full hash");
   check(!new String(original,"ISO-8859-1").contains("完整文字"),"cache contains digest and acoustic codes, no raw text");
  }finally{remove(root);}
  System.out.println("Encoding cache: "+checks+" integrity/identity/pending-file/mask/bounds checks passed.");
 }
}
