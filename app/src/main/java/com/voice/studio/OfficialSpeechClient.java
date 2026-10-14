package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
/** Three fixed official APIs, no redirects, no repeat POST; Android decodes to PCM. */
final class OfficialSpeechClient extends AcceleratedSpeechClient {
    interface Decoder {void decode(File input,File output,AtomicBoolean cancelled,Progress progress)throws Exception;}
    private final Connections connections;
    private final Decoder decoder;
    private final File reference;
    private final String transcript;
    OfficialSpeechClient(Decoder decoder,File reference,String transcript){this(url->(HttpURLConnection)url.openConnection(),decoder,reference,transcript);}
    OfficialSpeechClient(Connections connections,Decoder decoder,File reference,String transcript){this.connections=connections;this.decoder=decoder;this.reference=reference;this.transcript=transcript;}
    static JSONObject body(String engine,String voice,String text,float speed)throws Exception{
        if(text==null||text.trim().isEmpty()||text.length()>180)throw new IOException("官方 API 每段最多 180 字");
        if(!Float.isFinite(speed)||speed<.5||speed>2)throw new IOException("语速需为 0.5—2.0");
        if(voice==null||voice.isEmpty()||voice.equals("auto"))throw new IOException("请填写该平台的音色 ID");
        if(engine.equals("fish"))return FishClient.request("validation", "s2.1-pro-free",text,voice,speed);
        if(engine.equals("elevenlabs")){
            if(!voice.matches("[a-zA-Z0-9]{1,120}"))throw new IOException("ElevenLabs 音色 ID 无效");
            if(speed<.7||speed>1.2)throw new IOException("ElevenLabs 官方直连语速需为 0.7—1.2");
            return new JSONObject().put("text",text).put("model_id","eleven_multilingual_v2")
                .put("voice_settings",new JSONObject().put("speed",speed));
        }
        if(engine.equals("cartesia")){
            if(!voice.matches("[a-zA-Z0-9-]{10,120}"))throw new IOException("Cartesia Sonic 音色 ID 无效");
            if(speed != 1)throw new IOException("Cartesia Sonic 官方直连当前支持 1.0× 语速，请在创作页设为 1.0");
            return new JSONObject().put("model_id","sonic-3").put("transcript",text)
                .put("voice",voice).put("language","zh")
                .put("output_format",new JSONObject().put("container","wav")
                    .put("encoding","pcm_s16le").put("sample_rate",24000));
        }
        if(!engine.equals("minimax"))throw new IOException("所选引擎不支持官方直连");
        if(voice.length()>200)throw new IOException("MiniMax 音色 ID 过长");
        return new JSONObject().put("model","speech-2.8-hd").put("text",text).put("stream",false).put("output_format","hex")
            .put("language_boost","auto").put("voice_setting",new JSONObject().put("voice_id",voice).put("speed",speed).put("vol",1).put("pitch",0))
            .put("audio_setting",new JSONObject().put("sample_rate",24000).put("bitrate",128000).put("format","mp3").put("channel",1));
    }
    static byte[] decodeHex(String value)throws IOException{
        if(value==null||value.isEmpty()||value.length()%2!=0||value.length()>8*1024*1024)throw new IOException("MiniMax 音频数据无效或过大");
        byte[] bytes=new byte[value.length()/2];
        for(int i=0;i<bytes.length;i++){int a=Character.digit(value.charAt(i*2),16),b=Character.digit(value.charAt(i*2+1),16);if(a<0||b<0)throw new IOException("MiniMax 返回了无效音频编码");bytes[i]=(byte)((a<<4)|b);}
        return bytes;
    }
    long generate(String address,String key,String model,String voice,String text,String language,float speed,
            int steps,long seed,File output,AtomicBoolean cancelled,Progress progress)throws Exception{
        String root=CloudConnectionPolicy.validate(model,address);token(key);if(key.isEmpty())throw new IOException("请领取并填写平台 API Key");
        boolean inline=reference!=null;
        if(inline&&!model.equals("fish"))throw new IOException("此平台录音克隆请先在平台创建 Voice ID");
        byte[] body;String type="application/json";
        if(inline){
            if(text==null||text.trim().isEmpty()||text.length()>180||!Float.isFinite(speed)||speed<.5||speed>2)throw new IOException("无效的 Fish 合成参数");
            check(cancelled);byte[] wav=readBounded(new FileInputStream(reference),5*1024*1024,cancelled);
            AudioChecks.validate(reference,"wav");
            try(PcmAudio.Reader reader=new PcmAudio.Reader(reference)){reader.validateSamples(cancelled);if(reader.seconds()<3||reader.seconds()>10.01)throw new IOException("参考录音需为 3—10 秒");}
            body=FishInlineMessage.encode(text,wav,transcript,speed);type="application/msgpack";
        }else body=body(model,voice,text,speed).toString().getBytes("UTF-8");
        String target=model.equals("elevenlabs")?root+"/"+voice+"?output_format=mp3_44100_128":root;
        check(cancelled);HttpURLConnection c=connections.open(new URL(target));active=c;
        File downloaded=new File(output.getPath()+".provider."+(model.equals("cartesia")?"wav":"mp3"));boolean complete=false;
        try{
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(180000);c.setRequestMethod("POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);
            c.setRequestProperty("Content-Type",type);
            if(model.equals("cartesia")){
                c.setRequestProperty("X-API-Key",key);
                c.setRequestProperty("Cartesia-Version","2025-04-16");
                c.setRequestProperty("Accept","audio/wav");
            }else c.setRequestProperty(model.equals("elevenlabs")?"xi-api-key":"Authorization",model.equals("elevenlabs")?key:"Bearer "+key);
            if(model.equals("fish"))c.setRequestProperty("model","s2.1-pro-free");
            progress.update("连接 "+CloudEngineCatalog.label(model)+" 官方 API…");
            try(OutputStream out=c.getOutputStream()){check(cancelled);out.write(body);}
            int status=c.getResponseCode();check(cancelled);if(status!=200)throw new RequestRejected(status);
            String contentType=c.getContentType();
            if(!model.equals("minimax")&&contentType!=null&&(contentType.toLowerCase(Locale.ROOT).contains("json")||contentType.toLowerCase(Locale.ROOT).contains("text")))throw new IOException("平台未返回音频");
            if(model.equals("minimax")){
                JSONObject result=new JSONObject(new String(readBounded(c.getInputStream(),8*1024*1024,cancelled),"UTF-8"));
                if(result.getJSONObject("base_resp").getInt("status_code")!=0)throw new IOException("MiniMax 拒绝合成，请检查平台密钥、额度与音色");
                JSONObject data=result.getJSONObject("data");if(data.optInt("status",2)!=2)throw new IOException("MiniMax 尚未返回完整音频");
                byte[] audio=decodeHex(data.getString("audio"));try(OutputStream out=new FileOutputStream(downloaded)){out.write(audio);}
            }else{
                try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(downloaded)){byte[] buffer=new byte[32768];long total=0;int n;while((n=in.read(buffer))!=-1){check(cancelled);total+=n;if(total>4L*1024*1024)throw new IOException("平台返回音频过大");out.write(buffer,0,n);}}
            }
            check(cancelled);
            if(model.equals("cartesia")){
                // Sonic bytes already returns WAV; don't pass it to an MP3 decoder.
                // Validate the exact expected PCM format for the long-text merger.
                AudioChecks.validate(downloaded,"wav");
                try(PcmAudio.Reader reader=new PcmAudio.Reader(downloaded)){
                    if(reader.rate!=24000||reader.channels!=1||reader.encoding!=1||reader.width!=2)
                        throw new IOException("Cartesia 返回的格式不是 24kHz 单声道 PCM16 WAV");
                    reader.validateSpeech(cancelled);
                }
                try(InputStream in=new FileInputStream(downloaded);OutputStream out=new FileOutputStream(output)){
                    byte[] buffer=new byte[32768];int n;
                    while((n=in.read(buffer))!=-1){check(cancelled);out.write(buffer,0,n);}
                }
            }else{
                AudioChecks.validate(downloaded,"mp3");
                progress.update("接收完整音频并转换为 WAV…");
                decoder.decode(downloaded,output,cancelled,progress);
            }
            long duration=validate(output,text,cancelled);complete=true;return duration;
        }finally{active=null;c.disconnect();downloaded.delete();if(!complete)output.delete();}
    }
    static byte[] readBounded(InputStream stream,int limit,AtomicBoolean cancelled)throws Exception{
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1){check(cancelled);if(out.size()+n>limit)throw new IOException("平台响应过大");out.write(buffer,0,n);}check(cancelled);return out.toByteArray();}
    }
}
