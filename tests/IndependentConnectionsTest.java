package com.voice.studio;
import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.json.*;
/** Uses the actual connection store with in-memory prefs and a scope-tracing vault. */
public final class IndependentConnectionsTest {
 static int checks;
 static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 interface Action{void run()throws Exception;}
 static void fail(Action a)throws Exception{try{a.run();throw new AssertionError("Expected refusal");}catch(IOException e){checks++;}}
 static final class Vault implements CloudConnectionStore.Credentials{
  Map<String,String> data=new HashMap<>();String last;
  public String read(String id){last=id;return data.getOrDefault(id,"");}
  public void write(String id,String key){data.put(id,key);}
 }
 static SharedPreferences preferences(Map<String,Object> values){
  ClassLoader loader=IndependentConnectionsTest.class.getClassLoader();
  return (SharedPreferences)Proxy.newProxyInstance(loader,new Class[]{SharedPreferences.class},(p,m,a)->{
   String n=m.getName();if(n.equals("contains"))return values.containsKey(a[0]);if(n.equals("getAll"))return new HashMap<>(values);
   if(n.startsWith("get"))return values.getOrDefault(a[0],a[1]);
   if(n.equals("edit"))return Proxy.newProxyInstance(loader,new Class[]{SharedPreferences.Editor.class},(e,em,ea)->{
    String en=em.getName();if(en.startsWith("put")){values.put((String)ea[0],ea[1]);return e;}if(en.equals("remove")){values.remove(ea[0]);return e;}
    if(en.equals("clear")){values.clear();return e;}if(en.equals("commit"))return true;return null;
   });return null;
  });
 }
 static JSONObject task(CloudConnectionStore store)throws Exception{return store.job("独立平台测试，末句。","zh","Chinese",1,32,42);}
 public static void main(String[] args)throws Exception{
  Map<String,Object> data=new HashMap<>();Vault vault=new Vault();CloudConnectionStore store=new CloudConnectionStore(preferences(data),vault);
  List<JSONObject> records=new ArrayList<>(),jobs=new ArrayList<>();Map<String,String> keys=new HashMap<>();
  for(String model:new String[]{"elevenlabs","minimax","cartesia"}){
   String voice=model.equals("cartesia")?"123e4567-e89b-12d3-a456-426614174000":"Voice123";
   JSONObject r=CloudConnection.create(model,"official","custom",CloudConnectionPolicy.endpoint(model),voice,"global-recording-must-not-leak");
   store.save(r,model+"-only-key");records.add(r);JSONObject job=task(store);jobs.add(job);keys.put(r.getString("id"),model+"-only-key");
   check(job.getString("model").equals(model)&&job.getString("profile").isEmpty(),"Only selected API / registered voice");
  }
  JSONObject china=CloudConnection.create("minimax","official","custom",CloudConnectionPolicy.endpoint("minimax",true),"cnVoice","");
  store.save(china,"china-only-key");records.add(china);jobs.add(task(store));keys.put(china.getString("id"),"china-only-key");
  for(String model:CloudDeploymentConfig.ENGINES){
   JSONObject r=CloudConnection.create(model,"rest","custom","https://"+model+".example.invalid","auto","00000000-0000-0000-0000-000000000001");
   store.save(r,model+"-gpu-key");records.add(r);jobs.add(task(store));keys.put(r.getString("id"),model+"-gpu-key");
  }
  // The same model on another host must retain a separate key and reference.
  for(String provider:new String[]{"modal","lightning"})for(String model:CloudDeploymentConfig.ENGINES){
   String endpoint="https://"+model+(provider.equals("modal")?".modal.run":".lightning.example.invalid");
   JSONObject r=CloudConnection.create(model,"rest",provider,endpoint,"auto","00000000-0000-0000-0000-000000000002");
   String key=provider+"-"+model+"-only-key";store.save(r,key);records.add(r);jobs.add(task(store));keys.put(r.getString("id"),key);
  }
  JSONObject space=CloudConnection.create("qwen3tts","gradio","hf","https://owner-qwen.hf.space","auto","00000000-0000-0000-0000-000000000003");
  store.save(space,"");records.add(space);jobs.add(task(store));keys.put(space.getString("id"),"");
  String selected=store.active().getString("id");
  // Deliberately corrupt every obsolete global setting: queued jobs must ignore them.
  data.put("accelerated-url","https://wrong.invalid");data.put("accelerated-protocol","wrong");data.put("cloud-profile","other-platform-profile");data.put("accelerated-provider","wrong");
  for(int i=0;i<jobs.size();i++){
   JSONObject job=jobs.get(i),record=records.get(i);String scope=job.getString("connectionId");
   check(store.keyForJob(job).equals(keys.get(scope)),"Queue reads only owning platform key after switching all other services");
   check(vault.last.equals(scope),"No key lookup against another platform");check(job.getString("endpoint").equals(record.getString("endpoint")),"Own address snapshot retained");
   check(keys.get(scope).isEmpty()||!job.toString().contains(keys.get(scope)),"No API key stored in job JSON");
   check(job.getString("apiVoice").equals(record.getString("apiVoice"))&&job.getString("profile").equals(record.getString("profile")),"Voice and reference belong to the original connection");
  }
  check(store.active().getString("id").equals(selected),"Executing queue doesn't switch active provider");
  JSONObject eleven=jobs.get(0);store.activate(records.get(1));check(store.keyForJob(eleven).equals("elevenlabs-only-key"),"Eleven job survives selecting MiniMax");
  check(store.forModel("elevenlabs").getString("apiVoice").equals("Voice123"),"Independent voice retained");
  JSONObject forged=new JSONObject(eleven.toString()).put("connectionId",china.getString("id"));fail(()->store.keyForJob(forged));
  JSONObject changed=CloudConnection.create("qwen3tts","rest","custom","https://new-qwen.invalid","auto","ref");store.save(changed,"new-qwen-key");
  JSONObject oldQwen=jobs.stream().filter(j->j.optString("connectionId").equals("gateway-qwen3tts")).findFirst().get();fail(()->store.keyForJob(oldQwen));
  JSONObject modalQwen=jobs.stream().filter(j->j.optString("connectionId").equals("gpu-qwen3tts-modal")).findFirst().get();
  check(store.keyForJob(modalQwen).equals("modal-qwen3tts-only-key"),"Editing custom Qwen cannot invalidate Modal Qwen");
  check(store.keyForJob(jobs.get(jobs.size()-1)).isEmpty(),"Anonymous HF never borrows a saved official or GPU key");
  // Changing Qwen affects Qwen only; the original Eleven task remains runnable.
  check(store.keyForJob(eleven).equals("elevenlabs-only-key"),"Unrelated model edits cannot invalidate official task");
  fail(()->CloudConnection.create("auto","rest","custom","https://gateway.invalid","auto","ref"));
  fail(()->CloudConnection.create("minimax","rest","custom","https://gateway.invalid","voice",""));
  // Exact legacy migration copies just one saved model, never all models or Fish's key.
  Map<String,Object> legacy=new HashMap<>();legacy.put("engine","fish");legacy.put("accelerated-model","cosyvoice3");legacy.put("accelerated-url","https://cosy-only.invalid/v1/audio/speech");legacy.put("accelerated-protocol","rest");legacy.put("cloud-profile","profile-cosy");
  Vault oldVault=new Vault();oldVault.data.put("accelerated","old-cosy-key");oldVault.data.put("official-elevenlabs","eleven-existing-key");
  CloudConnectionStore migrated=new CloudConnectionStore(preferences(legacy),oldVault);migrated.migrateCurrent();
  check(oldVault.data.get("gateway-cosyvoice3").equals("old-cosy-key"),"Copy only exact legacy owner");
  check(!oldVault.data.containsKey("gateway-qwen3tts")&&!oldVault.data.containsKey("gateway-gptsovits"),"No broad shared-key migration");
  check(legacy.get("engine").equals("fish"),"Working original Fish remains selected");
  check(oldVault.data.get("official-elevenlabs").equals("eleven-existing-key"),"Other platform credentials untouched");
  migrated.migrateCurrent();check(oldVault.data.size()==3,"Migration is idempotent");
  System.out.println("Independent connections: "+checks+" real-store queue/key/voice/region/migration/isolation checks passed; credentials and preferences injected, no live APIs.");
 }
}
