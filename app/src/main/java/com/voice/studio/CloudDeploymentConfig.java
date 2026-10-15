package com.voice.studio;
import java.io.IOException;
import java.net.URI;
import org.json.JSONObject;
/** Only deployment output, never a dashboard URL or an embedded access key. */
final class CloudDeploymentConfig {
    static final String MODAL_WORKFLOW="https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-modal-qwen.yml";
    static final String HF_WORKFLOW="https://github.com/nmm26301-maker/Voicestudio/actions/workflows/deploy-hf-zerogpu.yml";
    static String endpoint(String platform,String input)throws Exception {
        String value=input==null?"":input.trim();
        if(value.length()>16384)throw new IOException("部署信息过长，请仅粘贴 Web Endpoint URL");
        if(value.startsWith("{")){
            JSONObject j=new JSONObject(value);
            if(j.optInt("schema_version")!=1 || !platform.equals(j.optString("provider")) ||
                !"qwen3tts".equals(j.optString("engine")) || !"rest".equals(j.optString("protocol")))
                throw new IOException("部署信息与所选 Qwen3-TTS 平台不一致");
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
