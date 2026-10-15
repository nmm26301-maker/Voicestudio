package com.voice.studio;
import java.io.IOException;
import org.json.JSONObject;
/** A single explicitly selected service. No auto provider and no credentials in jobs. */
final class CloudConnection {
    static boolean openModel(String model){for(String id:CloudDeploymentConfig.ENGINES)if(id.equals(model))return true;return false;}
    static String scope(String protocol,String model,String endpoint,String provider)throws Exception{
        if("official".equals(protocol))return CloudConnectionPolicy.secretScope(model,endpoint);
        if(!openModel(model))throw new IOException("请单独选择一个引擎，不能自动选择或混用平台");
        if("gradio".equals(protocol)){
            if(!"qwen3tts".equals(model)||!"hf".equals(provider))throw new IOException("此 Space 连接仅用于 Qwen3-TTS");
            GradioSpeechClient.base(endpoint);return "gpu-qwen3tts-hf";
        }
        if(!"rest".equals(protocol))throw new IOException("未知调用方式");
        CloudConnectionPolicy.gateway(endpoint);
        if("modal".equals(provider)||"lightning".equals(provider)){
            CloudDeploymentConfig.endpoint(provider,model,endpoint);return "gpu-"+model+"-"+provider;
        }
        if(!"custom".equals(provider))throw new IOException("此独立服务的平台配置无效");
        return "gateway-"+model;
    }
    static boolean allowedScope(String scope){
        if(scope==null)return false;
        for(String id:new String[]{"official-fish","official-elevenlabs","official-minimax","official-minimax-cn","official-cartesia"})if(id.equals(scope))return true;
        for(String model:CloudDeploymentConfig.ENGINES){
            if(scope.equals("gateway-"+model)||scope.equals("gpu-"+model+"-modal")||scope.equals("gpu-"+model+"-lightning"))return true;
        }
        return scope.equals("gpu-qwen3tts-hf");
    }
    static JSONObject create(String model,String protocol,String provider,String endpoint,String voice,String profile)throws Exception{
        String address="official".equals(protocol)?CloudConnectionPolicy.validate(model,endpoint):"gradio".equals(protocol)?GradioSpeechClient.base(endpoint):CloudConnectionPolicy.gateway(endpoint);
        String id=scope(protocol,model,address,provider);
        String reference=CloudConnectionPolicy.referenceProfile(protocol,model,profile==null?"":profile);
        return new JSONObject().put("id",id).put("model",model).put("protocol",protocol).put("provider",provider)
            .put("endpoint",address).put("apiVoice",voice==null||voice.isEmpty()?"auto":voice).put("profile",reference);
    }
    static JSONObject job(JSONObject record,String text,String language,String languageName,float speed,int steps,long seed)throws Exception{
        String model=record.getString("model"),protocol=record.getString("protocol"),provider=record.getString("provider"),endpoint=record.getString("endpoint");
        String id=scope(protocol,model,endpoint,provider);if(!id.equals(record.optString("id")))throw new IOException("独立平台连接标识不一致");
        String voice=record.optString("apiVoice","auto"),profile=CloudConnectionPolicy.referenceProfile(protocol,model,record.optString("profile"));
        AcceleratedSpeechClient.request(text,model,voice,language,speed,steps,seed);
        if(protocol.equals("official")&&profile.isEmpty())OfficialSpeechClient.body(model,voice,"配置检查",speed);
        if(!protocol.equals("official")&&profile.isEmpty())throw new IOException(CloudEngineCatalog.label(model)+" 请先在自己的连接中选择参考录音");
        return new JSONObject().put("type","tts").put("engine","accelerated").put("deduplicate",true)
            .put("connectionId",id).put("model",model).put("protocol",protocol).put("provider",provider).put("endpoint",endpoint)
            .put("apiVoice",voice).put("profile",profile).put("text",text).put("language",language).put("languageName",languageName)
            .put("speed",speed).put("steps",steps).put("seed",seed).put("voice",CloudEngineCatalog.label(model));
    }
    static String bind(JSONObject job,JSONObject saved)throws Exception{
        String id=scope(job.optString("protocol","rest"),job.optString("model"),job.getString("endpoint"),job.optString("provider","custom"));
        if(job.has("connectionId")&&!id.equals(job.getString("connectionId")))throw new IOException("任务所属平台不一致，已停止调用");
        if(saved==null||!id.equals(saved.optString("id"))||!job.getString("endpoint").equals(saved.optString("endpoint")))
            throw new IOException(CloudEngineCatalog.label(job.optString("model"))+" 的独立连接尚未保存，或该平台地址已修改；请保存后新建任务");
        return id;
    }
}
