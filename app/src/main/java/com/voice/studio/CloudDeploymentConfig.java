package com.voice.studio;
import java.io.IOException;
import java.net.URI;
import org.json.JSONObject;
/** Only deployment output, never a dashboard URL or an embedded access key. */
final class CloudDeploymentConfig {
    static final String MODAL_WORKFLOW="https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-modal-engine.yml";
    static final String HF_WORKFLOW="https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-hf-zerogpu.yml";
    static final String[] ENGINES={"qwen3tts","indextts25","cosyvoice3","chatterboxv3","gptsovits"};
    static final String[] LABELS={"Qwen3-TTS","IndexTTS 2.5","CosyVoice 3","Chatterbox V3","GPT-SoVITS"};
    static String endpoint(String platform,String input)throws Exception {return endpoint(platform,"qwen3tts",input);}
    static String endpoint(String platform,String engine,String input)throws Exception {
        boolean known=false;for(String id:ENGINES)if(id.equals(engine))known=true;
        if(!known)throw new IOException("不支持此云端部署引擎");
        if(!platform.equals("modal")&&!platform.equals("lightning"))throw new IOException("不支持此部署平台");
        String value=input==null?"":input.trim();
        if(value.length()>16384)throw new IOException("部署信息过长，请仅粘贴 Web Endpoint URL");
        if(value.startsWith("{")){
            JSONObject j=new JSONObject(value);
            if(j.optInt("schema_version")!=1 || !platform.equals(j.optString("provider")) ||
                !engine.equals(j.optString("engine")) || !"rest".equals(j.optString("protocol")))
                throw new IOException("部署信息与所选平台或引擎不一致，请检查引擎选择");
            value=j.getString("url");
        }
        String result=CloudConnectionPolicy.gateway(value);
        String host=new URI(result).getHost().toLowerCase(java.util.Locale.ROOT);
        if(platform.equals("modal") && !host.endsWith(".modal.run"))
            throw new IOException("Modal 请填写部署生成的 *.modal.run Web Endpoint，不是控制台网址");
        if(host.equals("lightning.ai")||host.equals("huggingface.co")||host.equals("github.com")||host.equals("modal.com"))
            throw new IOException("这是平台页面，请复制实际运行服务的 HTTPS 地址");
        return result;
    }
}
