package com.voice.studio;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.zip.*;import org.json.*;
public class OmniModelsTest {
 static byte[] payload="test model".getBytes();static int assertions;
 static void check(boolean ok){assertions++;if(!ok)throw new AssertionError();}
 static JSONObject manifest()throws Exception{StringBuilder hex=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(payload))hex.append(String.format("%02x",b&255));return new JSONObject().put("files",new JSONArray().put(new JSONObject().put("name","model.onnx").put("size",payload.length).put("sha256",hex.toString())));}
 static byte[] zip(String name,byte[] data)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry(name));z.write(data);z.closeEntry();}return out.toByteArray();}
 static void reject(File root,String name,byte[] data)throws Exception{File dir=new File(root,"case-"+assertions);boolean failed=false;try{OmniModels.install(new ByteArrayInputStream(zip(name,data)),dir,manifest(),p->{});}catch(IOException e){failed=true;}check(failed);check(!dir.exists());check(root.listFiles().length==1);}
 public static void main(String[] args)throws Exception{File root=Files.createTempDirectory("omni-model-tests").toFile();try{
  File valid=new File(root,"valid");check(!OmniModels.ready(valid,manifest()));OmniModels.install(new ByteArrayInputStream(zip("model.onnx",payload)),valid,manifest(),p->{});check(OmniModels.ready(valid,manifest()));
  reject(root,"../model.onnx",payload);reject(root,"model.onnx",new byte[payload.length]);reject(root,"model.onnx",new byte[payload.length+1]);reject(root,"README.txt",new byte[0]);
  try(FileOutputStream out=new FileOutputStream(new File(valid,"model.onnx"))){out.write(0);}check(!OmniModels.ready(valid,manifest()));
  byte[] whole=zip("model.onnx",payload);int split=whole.length/2;
  InputStream joined=new SequenceInputStream(new ByteArrayInputStream(Arrays.copyOfRange(whole,0,split)),new ByteArrayInputStream(Arrays.copyOfRange(whole,split,whole.length)));
  File parts=new File(root,"joined");OmniModels.install(joined,parts,manifest(),p->{});check(OmniModels.ready(parts,manifest()));
  // Same-size corruption must be detected by full verification, then repaired without clearing app data.
  File bad=new File(root,"repair");OmniModels.install(new ByteArrayInputStream(whole),bad,manifest(),m->{});
  try(FileOutputStream out=new FileOutputStream(new File(bad,"model.onnx"))){out.write(new byte[payload.length]);}
  check(OmniModels.ready(bad,manifest()));boolean corrupted=false;
  try{OmniModels.verify(bad,manifest(),m->{});}catch(IOException e){corrupted=true;}
  check(corrupted);check(!OmniModels.ready(bad,manifest()));
  boolean badInstall=false;try{OmniModels.install(new ByteArrayInputStream(zip("model.onnx",new byte[payload.length])),bad,manifest(),m->{});}catch(IOException e){badInstall=true;}
  check(badInstall);check(new File(bad,"model.onnx").exists());
  OmniModels.install(new ByteArrayInputStream(whole),bad,manifest(),m->{});OmniModels.verify(bad,manifest(),m->{});check(OmniModels.ready(bad,manifest()));
  check(!new File(root,"repair.backup").exists());
  File stale=new File(root,"import-11111111-1111-4111-8111-111111111111");stale.mkdir();
  File backup=new File(root,"omni-model.backup");check(bad.renameTo(backup));
  OmniModels.recoverImports(root);check(!stale.exists());check(new File(root,"omni-model").exists());check(!backup.exists());
  OmniModels.verify(new File(root,"omni-model"),manifest(),m->{});
  System.out.println(assertions+" model import assertions passed: integrity, size, path rejection, missing files and cleanup.");
 }finally{OmniModels.remove(root);}}
}
