package com.voice.studio;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import org.json.JSONObject;
/** Non-secret per-service records plus separately encrypted, per-service keys. */
final class CloudConnectionStore {
    interface Credentials {String read(String scope)throws Exception;void write(String scope,String key)throws Exception;}
    final SharedPreferences prefs;final Credentials credentials;
    CloudConnectionStore(Context c){this(c.getSharedPreferences("studio",0),new Credentials(){
        public String read(String scope)throws Exception{return new SecretStore(c,scope).read();}
        public void write(String scope,String key)throws Exception{new SecretStore(c,scope).save(key);}
    });}
    CloudConnectionStore(SharedPreferences prefs,Credentials credentials){this.prefs=prefs;this.credentials=credentials;}
    JSONObject get(String id)throws Exception{
        if(!CloudConnection.allowedScope(id))throw new IOException("未知独立平台");
        String value=prefs.getString("connection."+id,"");return value.isEmpty()?null:new JSONObject(value);
    }
    JSONObject active()throws Exception{String id=prefs.getString("active-cloud-connection","");return id.isEmpty()?null:get(id);}
    JSONObject forModel(String model)throws Exception{String id=prefs.getString("connection-choice."+model,"");return id.isEmpty()?null:get(id);}
    String key(String id)throws Exception{return credentials.read(id);}
    void save(JSONObject record,String key)throws Exception{
        String id=CloudConnection.scope(record.getString("protocol"),record.getString("model"),record.getString("endpoint"),record.getString("provider"));
        if(!id.equals(record.getString("id")))throw new IOException("平台连接标识无效");
        AcceleratedSpeechClient.token(key);if(key.isEmpty()&&!record.getString("protocol").equals("gradio"))throw new IOException("请填写这个平台自己的密钥");
        credentials.write(id,key);
        if(!prefs.edit().putString("connection."+id,record.toString()).putString("connection-choice."+record.getString("model"),id).commit())throw new IOException("独立连接保存失败");
        activate(record);
    }
    void activate(JSONObject r)throws Exception{
        if(!prefs.edit().putString("active-cloud-connection",r.getString("id")).putString("engine","accelerated")
            .putString("cloud-engine",r.getString("model")).putString("accelerated-model",r.getString("model"))
            .putString("accelerated-url",r.getString("endpoint")).putString("accelerated-protocol",r.getString("protocol"))
            .putString("accelerated-provider",r.getString("provider")).putString("accelerated-voice",r.optString("apiVoice","auto")).commit())throw new IOException("平台选择保存失败");
    }
    JSONObject job(String text,String language,String languageName,float speed,int steps,long seed)throws Exception{
        JSONObject record=active();if(record==null)throw new IOException("请在音色页单独配置并选择一个平台");
        return CloudConnection.job(record,text,language,languageName,speed,steps,seed);
    }
    String keyForJob(JSONObject job)throws Exception{
        String id=CloudConnection.scope(job.optString("protocol","rest"),job.optString("model"),job.getString("endpoint"),job.optString("provider","custom"));
        // Existing official jobs already had separate key scopes and fixed hosts.
        if(!job.has("connectionId")&&job.optString("protocol").equals("official"))return key(id);
        CloudConnection.bind(job,get(id));return key(id);
    }
    void migrateCurrent()throws Exception{
        if(active()!=null)return;
        String model=prefs.getString("accelerated-model","auto"),endpoint=prefs.getString("accelerated-url","");
        if(model.equals("auto")||endpoint.isEmpty())return;
        String protocol=prefs.getString("accelerated-protocol","rest"),provider=prefs.getString("accelerated-provider","custom");
        JSONObject record=CloudConnection.create(model,protocol,provider,endpoint,prefs.getString("accelerated-voice","auto"),prefs.getString(protocol.equals("gradio")?"gpu-hf-profile":"cloud-profile",""));
        String oldScope=protocol.equals("official")?CloudConnectionPolicy.secretScope(model,endpoint):protocol.equals("gradio")?"gpu-hf":provider.equals("modal")?"gpu-modal":"accelerated";
        String secret=credentials.read(oldScope);
        String oldEngine=prefs.getString("engine","accelerated");save(record,secret);
        if(oldEngine.equals("fish"))prefs.edit().putString("engine","fish").apply();
    }
}
