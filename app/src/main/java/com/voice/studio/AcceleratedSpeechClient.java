package com.voice.studio;

import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Explicitly selected OmniVoice-compatible GPU service; never an automatic network fallback. */
final class AcceleratedSpeechClient {
    interface Connections { HttpURLConnection open(URL url) throws IOException; }
    interface Progress { void update(String message); }
    private final Connections connections;
    volatile HttpURLConnection active;
    AcceleratedSpeechClient() { this(url -> (HttpURLConnection)url.openConnection()); }
    AcceleratedSpeechClient(Connections connections) { this.connections = connections; }
    void cancel() { HttpURLConnection value=active;if(value!=null)value.disconnect(); }

    static String endpoint(String configured) throws IOException {
        try {
            URI uri=new URI(configured==null?"":configured.trim());
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null ||
                    uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
                throw new IOException("请填写 HTTPS 加速服务地址，密钥请填写在独立密钥框中");
            String value=uri.toASCIIString();while(value.endsWith("/"))value=value.substring(0,value.length()-1);
            if(value.endsWith("/v1/audio/speech"))return value;
            return value+(value.endsWith("/v1")?"/audio/speech":"/v1/audio/speech");
        } catch(URISyntaxException e) { throw new IOException("加速服务地址格式无效"); }
    }
    static void token(String key) throws IOException {
        if(key==null || key.length()>4096 || (!key.isEmpty() && !key.matches("[!-~]+")))
            throw new IOException("接口密钥含空格或无效字符");
    }
    static JSONObject request(String text,String model,String voice,String language,float speed,int steps,long seed) throws Exception {
        if(text==null || text.trim().isEmpty() || text.length()>12000)throw new IOException("文本需为 1—12000 字");
        if(model==null || model.length()>200 || voice==null || voice.isEmpty() || voice.length()>200)
            throw new IOException("接口模型或音色配置无效");
        if(!Float.isFinite(speed) || speed<.5 || speed>2)throw new IOException("语速需为 0.5—2.0");
        if(steps!=8 && steps!=16 && steps!=32)throw new IOException("请选择 8、16 或 32 步");
        JSONObject body=new JSONObject().put("input",text).put("voice",voice).put("response_format","wav")
                .put("speed",speed).put("num_step",steps).put("seed",seed);
        if(!model.isEmpty())body.put("model",model);
        if(language!=null && !language.isEmpty() && !language.equals("None"))body.put("language",language);
        return body;
    }
    long generate(String address,String key,String model,String voice,String text,String language,float speed,
            int steps,long seed,File output,AtomicBoolean cancelled,Progress progress) throws Exception {
        String target=endpoint(address);token(key);
        byte[] body=request(text,model,voice,language,speed,steps,seed).toString().getBytes("UTF-8");
        check(cancelled);HttpURLConnection connection=connections.open(new URL(target));active=connection;
        boolean complete=false;
        try {
            check(cancelled);connection.setConnectTimeout(15000);connection.setReadTimeout(180000);
            connection.setInstanceFollowRedirects(false);connection.setRequestMethod("POST");
            connection.setDoOutput(true);connection.setFixedLengthStreamingMode(body.length);
            connection.setRequestProperty("Content-Type","application/json");
            connection.setRequestProperty("Accept","audio/wav");
            if(!key.isEmpty())connection.setRequestProperty("Authorization","Bearer "+key);
            progress.update("连接加速服务…");
            try(OutputStream stream=connection.getOutputStream()){check(cancelled);stream.write(body);}
            int status=connection.getResponseCode();check(cancelled);
            if(status!=200)throw new IOException("加速接口 HTTP "+status+"："+(status==401||status==403?"请检查接口密钥":status==429?"服务繁忙，请稍后重试":"请检查服务状态、模型与音色配置"));
            String type=connection.getContentType();
            if(type!=null && (type.toLowerCase(Locale.ROOT).contains("json") || type.toLowerCase(Locale.ROOT).contains("text")))
                throw new IOException("加速服务返回了文字或错误消息，没有返回 WAV 音频");
            progress.update("服务器正在合成并接收完整音频…");
            try(InputStream input=connection.getInputStream();OutputStream stream=new FileOutputStream(output)) {
                byte[] buffer=new byte[32768];long total=0;int n;
                while((n=input.read(buffer))!=-1){check(cancelled);total+=n;if(total>64L*1024*1024)throw new IOException("接口音频超过 64 MiB，请拆分文本");stream.write(buffer,0,n);}
            }
            long millis=validate(output,text,cancelled);complete=true;return millis;
        } finally {active=null;connection.disconnect();if(!complete)output.delete();}
    }
    static long validate(File output,String text,AtomicBoolean cancelled) throws Exception {
        check(cancelled);AudioChecks.validate(output,"wav");
        long millis;
        try(PcmAudio.Reader reader=new PcmAudio.Reader(output)) {
            millis=Math.round(reader.seconds()*1000);
            if(millis<=0 || (OmniDuration.total(text)>=30 && millis<500))
                throw new IOException("接口返回的音频过短，可能只合成了开头，请检查服务器的完整文本处理");
            for(double at=0;at<reader.seconds();at+=30){check(cancelled);reader.read(at,Math.min(30,reader.seconds()-at));}
        }
        check(cancelled);PcmAudio.markSynthetic(output);return millis;
    }
    static void check(AtomicBoolean cancelled) throws InterruptedIOException {
        if(cancelled.get() || Thread.currentThread().isInterrupted())throw new InterruptedIOException("已暂停生成");
    }
}
