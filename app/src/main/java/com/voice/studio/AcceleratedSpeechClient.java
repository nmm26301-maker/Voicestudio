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

    static final class RequestRejected extends IOException {
        private static final long serialVersionUID=1L;
        final int status;
        RequestRejected(int status){super("加速接口 HTTP "+status+"："+(status==401||status==403?"请检查接口密钥":status==429?"额度不足或服务繁忙，请稍后重试":"请检查服务状态、模型与音色配置"));this.status=status;}
        boolean definitelyRejected(){return status==400||status==401||status==403||status==404||status==413||status==422||status==429;}
    }
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
            if(status!=200)throw new RequestRejected(status);
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
    static final class IncompleteSpeech extends IOException {
        private static final long serialVersionUID=1L;
        IncompleteSpeech(){super("接口返回的音频过短，可能只合成了开头，请检查服务器的完整文本处理");}
    }
    static long minimumDuration(String text) {
        // Loose lower bound, NOT proof of word-for-word correctness. Handle even
        // one/two-character utterances; the old rule skipped them entirely.
        long minimum=0;int spoken=0;
        for(int i=0;i<text.length();) {
            int c=text.codePointAt(i);i+=Character.charCount(c);
            if(!Character.isLetterOrDigit(c))continue;
            spoken++;
            Character.UnicodeScript script=Character.UnicodeScript.of(c);
            boolean syllabic=script==Character.UnicodeScript.HAN
                    || script==Character.UnicodeScript.HIRAGANA
                    || script==Character.UnicodeScript.KATAKANA
                    || script==Character.UnicodeScript.HANGUL;
            minimum+=syllabic?30:11;
        }
        // At 2x speed, 250 ms is a deliberately forgiving short-clone bound.
        return spoken==0?0:Math.max(250,minimum);
    }
    static long validate(File output,String text,AtomicBoolean cancelled) throws Exception {
        check(cancelled);AudioChecks.validate(output,"wav");
        long millis;
        try(PcmAudio.Reader reader=new PcmAudio.Reader(output)) {
            millis=Math.round(reader.seconds()*1000);
            if(millis<=0 || millis<minimumDuration(text))
                throw new IncompleteSpeech();
            reader.validateSamples(cancelled);
        }
        check(cancelled);PcmAudio.markSynthetic(output);return millis;
    }
    static void check(AtomicBoolean cancelled) throws InterruptedIOException {
        if(cancelled.get() || Thread.currentThread().isInterrupted())throw new InterruptedIOException("已暂停生成");
    }
}
