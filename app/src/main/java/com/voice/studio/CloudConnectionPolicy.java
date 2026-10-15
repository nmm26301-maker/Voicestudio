package com.voice.studio;
import java.io.IOException;
import java.net.URI;
/** Official addresses are paired with their own wire format, never gateway REST. */
final class CloudConnectionPolicy {
    static boolean official(String id){return id.equals("fish")||id.equals("elevenlabs")||id.equals("minimax")||id.equals("cartesia");}
    static String endpoint(String id)throws IOException{
        if(id.equals("fish"))return "https://api.fish.audio/v1/tts";
        if(id.equals("elevenlabs"))return "https://api.elevenlabs.io/v1/text-to-speech";
        if(id.equals("minimax"))return "https://api.minimax.io/v1/t2a_v2";
        if(id.equals("cartesia"))return "https://api.cartesia.ai/tts/bytes";
        throw new IOException("此开源引擎需要你自己的云端网关");
    }
    static String validate(String id,String address)throws IOException{
        String expected=endpoint(id);
        if(id.equals("minimax")&&"https://api.minimax.cn/v1/t2a_v2".equals(address))return address;
        if(!expected.equals(address))throw new IOException("官方直连地址与所选平台不匹配，请恢复自动填写");
        return expected;
    }
    static String endpoint(String id,boolean china)throws IOException{return id.equals("minimax")&&china?"https://api.minimax.cn/v1/t2a_v2":endpoint(id);}
    static String secretScope(String id,String address)throws IOException{
        validate(id,address);return id.equals("minimax")&&address.contains("api.minimax.cn/")?"official-minimax-cn":"official-"+id;
    }
    static boolean selectOfficial(String previous,String selected,boolean current){return previous.equals(selected)?current:official(selected);}
    static String referenceProfile(String protocol,String model,String selected){
        return "official".equals(protocol)&&!"fish".equals(model)?"":selected;
    }
    static String gateway(String value)throws IOException{
        try{
            String host=new URI(value.trim()).getHost();
            if("api.fish.audio".equalsIgnoreCase(host)||"api.elevenlabs.io".equalsIgnoreCase(host)||"api.minimax.io".equalsIgnoreCase(host)||"api.minimax.cn".equalsIgnoreCase(host)||"api.cartesia.ai".equalsIgnoreCase(host))
                throw new IOException("这是平台官方 API 地址，请选择“官方直连”连接方式");
        }catch(java.net.URISyntaxException e){throw new IOException("网关地址格式无效");}
        return AcceleratedSpeechClient.endpoint(value);
    }
    static String account(String id){
        if(id.equals("fish"))return "https://fish.audio/auth/signup/";
        if(id.equals("elevenlabs"))return "https://elevenlabs.io/app/sign-up";
        if(id.equals("minimax"))return "https://platform.minimax.io/login";
        if(id.equals("cartesia"))return "https://play.cartesia.ai/";
        return "https://huggingface.co/join";
    }
    static String keys(String id){
        if(id.equals("fish"))return "https://fish.audio/app/api-keys/";
        if(id.equals("elevenlabs"))return "https://elevenlabs.io/app/developers/api-keys";
        if(id.equals("minimax"))return "https://platform.minimax.io/console/access";
        if(id.equals("cartesia"))return "https://play.cartesia.ai/keys";
        return "https://huggingface.co/settings/tokens";
    }
    static String documentation(String id){
        if(id.equals("indextts25"))return "https://github.com/index-tts/index-tts";
        if(id.equals("cosyvoice3"))return "https://github.com/FunAudioLLM/CosyVoice";
        if(id.equals("qwen3tts"))return "https://github.com/QwenLM/Qwen3-TTS";
        if(id.equals("chatterboxv3"))return "https://github.com/resemble-ai/chatterbox";
        if(id.equals("gptsovits"))return "https://github.com/RVC-Boss/GPT-SoVITS/blob/main/api_v2.py";
        if(id.equals("cartesia"))return "https://docs.cartesia.ai/api-reference/tts/bytes";
        if(id.equals("fish"))return "https://docs.fish.audio";
        if(id.equals("elevenlabs"))return "https://elevenlabs.io/docs/eleven-api/quickstart";
        if(id.equals("minimax"))return "https://platform.minimax.io/docs/api-reference/speech-t2a-http";
        return "https://github.com/nmm26301-maker/Voicestudio/blob/main/docs/CLOUD_ENGINES.md";
    }
}
