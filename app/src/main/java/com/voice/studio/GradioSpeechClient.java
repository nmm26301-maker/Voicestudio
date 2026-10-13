package com.voice.studio;

import java.io.*;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** OmniVoice Gradio 6 call/SSE protocol. No account rotation or automatic paid fallback. */
final class GradioSpeechClient {
    static final String OFFICIAL="https://k2-fsa-omnivoice.hf.space";
    private final AcceleratedSpeechClient.Connections connections;
    volatile HttpURLConnection active;
    GradioSpeechClient(){this(url -> (HttpURLConnection)url.openConnection());}
    GradioSpeechClient(AcceleratedSpeechClient.Connections connections){this.connections=connections;}
    void cancel(){HttpURLConnection c=active;if(c!=null)c.disconnect();}
    static String base(String address) throws IOException {
        try {
            URI u=new URI(address==null?"":address.trim());
            if(!"https".equalsIgnoreCase(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null ||
                    u.getQuery()!=null || u.getFragment()!=null || !(u.getPath()==null || u.getPath().isEmpty() || u.getPath().equals("/")))
                throw new IOException("请填写 Space 的 HTTPS 运行地址，如 https://作者-空间名.hf.space；不是介绍页地址");
            return "https://"+u.getRawAuthority();
        }catch(URISyntaxException e){throw new IOException("Space 地址格式无效");}
    }
    static URL fileUrl(String origin,JSONObject file) throws Exception {
        String value=file.optString("url");
        if(value.isEmpty())throw new IOException("Space 没有返回音频地址");
        URI root=new URI(base(origin)),u=root.resolve(value);
        if(!"https".equalsIgnoreCase(u.getScheme()) || u.getHost()==null || !u.getHost().equalsIgnoreCase(root.getHost()) ||
                u.getPort()!=root.getPort() || u.getUserInfo()!=null || u.getFragment()!=null || u.getQuery()!=null ||
                !u.getPath().startsWith("/gradio_api/file="))
            throw new IOException("Space 返回了异常音频地址，请检查部署；不会向其他网站转发密钥");
        return u.toURL();
    }
    static JSONObject request(String text,String language,float speed,int steps,JSONObject reference) throws Exception {
        return request(text,language,speed,steps,reference,null);
    }
    static JSONObject request(String text,String language,float speed,int steps,JSONObject reference,String referenceText) throws Exception {
        AcceleratedSpeechClient.request(text,"","auto",language,speed,steps,42);
        if(referenceText!=null&&referenceText.length()>2000)throw new IOException("参考文字过长");
        JSONArray data=new JSONArray().put(text).put(language==null||language.isEmpty()?"Auto":language);
        if(reference!=null)data.put(reference).put(referenceText==null||referenceText.isEmpty()?JSONObject.NULL:referenceText).put("");
        data.put(steps).put(2.0).put(true).put(speed).put(JSONObject.NULL).put(true).put(true);
        if(reference==null)for(int i=0;i<6;i++)data.put("Auto");
        return new JSONObject().put("data",data);
    }
    private HttpURLConnection open(URL url,String key,String method,AtomicBoolean cancelled) throws Exception {
        AcceleratedSpeechClient.check(cancelled);
        HttpURLConnection c=connections.open(url);active=c;
        c.setConnectTimeout(15000);c.setReadTimeout(180000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
        if(!key.isEmpty())c.setRequestProperty("Authorization","Bearer "+key);
        return c;
    }
    private void close(HttpURLConnection c){if(active==c)active=null;c.disconnect();}
    private static void status(HttpURLConnection c) throws Exception {
        int n=c.getResponseCode();if(n!=200)throw new IOException("免费 GPU HTTP "+n+"："+
                (n==401||n==403?"请检查 HF Token 权限，或打开官方页面登录":n==429?"额度不足或排队繁忙，请查看平台额度":"Space 暂不可用，请打开平台页面检查状态"));
    }
    private static String json(InputStream in,AtomicBoolean cancelled) throws Exception {
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];int n;
        while((n=in.read(x))!=-1){AcceleratedSpeechClient.check(cancelled);if(b.size()+n>1024*1024)throw new IOException("Space 响应过大");b.write(x,0,n);}
        return b.toString("UTF-8");
    }
    JSONObject uploadReference(String address,String key,File ref,AtomicBoolean cancelled) throws Exception {
        AcceleratedSpeechClient.token(key);return upload(base(address),key,ref,cancelled);
    }
    private JSONObject upload(String origin,String key,File ref,AtomicBoolean cancelled) throws Exception {
        if(!ref.isFile() || ref.length()>2*1024*1024)throw new IOException("参考录音不存在或超过 2 MiB");
        String boundary="VoiceStudio"+UUID.randomUUID().toString().replace("-","");
        byte[] head=("--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\"reference.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes("UTF-8");
        byte[] tail=("\r\n--"+boundary+"--\r\n").getBytes("UTF-8");
        HttpURLConnection c=open(new URL(origin+"/gradio_api/upload"),key,"POST",cancelled);
        try {
            c.setDoOutput(true);c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
            c.setFixedLengthStreamingMode(head.length+ref.length()+tail.length);
            try(OutputStream out=c.getOutputStream();InputStream in=new FileInputStream(ref)) {
                out.write(head);byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1){AcceleratedSpeechClient.check(cancelled);out.write(x,0,n);}out.write(tail);
            }
            status(c);String path;
            try(InputStream in=c.getInputStream()){path=new JSONArray(json(in,cancelled)).getString(0);}
            if(path.length()>4096 || !path.startsWith("/"))throw new IOException("参考音频上传响应无效");
            return new JSONObject().put("path",path).put("orig_name","reference.wav").put("meta",new JSONObject().put("_type","gradio.FileData"));
        }finally{close(c);}
    }
    static JSONArray events(InputStream in,AtomicBoolean cancelled,AcceleratedSpeechClient.Progress progress) throws Exception {
        Reader r=new InputStreamReader(in,"UTF-8");StringBuilder line=new StringBuilder(),data=new StringBuilder();String event="";
        int ch,total=0;long deadline=System.nanoTime()+600000000000L;
        while((ch=r.read())!=-1) {
            AcceleratedSpeechClient.check(cancelled);
            if(++total>1024*1024 || System.nanoTime()>deadline)throw new IOException("GPU 排队超时，请稍后重试");
            if(ch!='\n'){if(ch!='\r')line.append((char)ch);if(line.length()>256*1024)throw new IOException("Space 事件过大");continue;}
            String s=line.toString();line.setLength(0);
            if(s.isEmpty()) {
                if(event.equals("complete")){JSONArray result=new JSONArray(data.toString());if(result.length()<1 || result.isNull(0))throw new IOException("Space 未生成音频，请检查文本、额度或录音");return result;}
                if(event.equals("error"))throw new IOException("Space 合成失败或免费额度不足，请打开官方页面查看详情");
                event="";data.setLength(0);
            }else if(s.startsWith("event:")) {event=s.substring(6).trim();if(event.equals("heartbeat")||event.equals("generating"))progress.update("免费 GPU 排队或合成中…");}
            else if(s.startsWith("data:")){if(data.length()>0)data.append('\n');data.append(s.substring(5).trim());}
        }
        throw new IOException("GPU 连接中断，没有收到完整结果；可以重新生成");
    }
    private static final class DownloadStatus extends IOException {
        private static final long serialVersionUID=1L;
        final int code;
        DownloadStatus(int code){super("音频下载 HTTP "+code+"，已保存生成结果，可稍后继续任务");this.code=code;}
    }
    private static boolean retryDownload(IOException error) {
        if(error instanceof DownloadStatus){int code=((DownloadStatus)error).code;return code==502||code==503||code==504;}
        return error instanceof SocketTimeoutException || error instanceof SocketException || error instanceof EOFException;
    }
    private static void retryPause(int attempt,AtomicBoolean cancelled) throws InterruptedIOException {
        for(int i=0;i<attempt*5;i++) {
            AcceleratedSpeechClient.check(cancelled);
            try{Thread.sleep(50);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new InterruptedIOException("已暂停生成");}
        }
        AcceleratedSpeechClient.check(cancelled);
    }
    /** Retry only a known audio GET; synthesis POSTs and queue streams are never replayed. */
    private void download(URL url,String key,File output,AtomicBoolean cancelled,AcceleratedSpeechClient.Progress progress) throws Exception {
        for(int attempt=0;attempt<3;attempt++) {
            HttpURLConnection download=null;
            try {
                download=open(url,key,"GET",cancelled);
                int code=download.getResponseCode();if(code!=200)throw new DownloadStatus(code);
                String type=download.getContentType();
                if(type!=null&&(type.toLowerCase(java.util.Locale.ROOT).contains("json")||type.toLowerCase(java.util.Locale.ROOT).contains("text")))throw new IOException("Space 返回了文字，没有返回 WAV 音频");
                long expected=download.getContentLengthLong();
                if(expected>64L*1024*1024)throw new IOException("音频超过 64 MiB，请拆分文本");
                progress.update("接收完整 GPU 音频…");
                try(InputStream in=download.getInputStream();OutputStream out=new FileOutputStream(output)) {
                    long total=0;byte[] x=new byte[32768];int n;
                    while((n=in.read(x))!=-1){AcceleratedSpeechClient.check(cancelled);total+=n;if(total>64L*1024*1024)throw new IOException("音频超过 64 MiB，请拆分文本");out.write(x,0,n);}
                    if(expected>=0&&total!=expected)throw new EOFException("音频下载未完成");
                }
                return;
            }catch(IOException error) {
                output.delete();AcceleratedSpeechClient.check(cancelled);
                if(attempt==2||!retryDownload(error))throw error;
                // Release the previous connection before waiting, including on cancellation.
                if(download!=null){close(download);download=null;}
                progress.update("下载连接中断，正在重试 "+(attempt+1)+" / 2（无需重新合成）…");
                retryPause(attempt+1,cancelled);
            }finally{if(download!=null)close(download);}
        }
    }
    long generate(String address,String key,String text,String language,float speed,int steps,File reference,
            File output,AtomicBoolean cancelled,AcceleratedSpeechClient.Progress progress) throws Exception {
        return generate(address,key,text,language,speed,steps,reference,null,output,cancelled,progress);
    }
    interface Checkpoint { JSONObject data(); void save() throws Exception; }
    long generate(String address,String key,String text,String language,float speed,int steps,File reference,String referenceText,
            File output,AtomicBoolean cancelled,AcceleratedSpeechClient.Progress progress) throws Exception {
        String origin=base(address);AcceleratedSpeechClient.token(key);
        request(text,language,speed,steps,null,referenceText);
        JSONObject uploaded=null;if(reference!=null){progress.update("上传所选参考录音…");uploaded=upload(origin,key,reference,cancelled);}
        JSONObject state=new JSONObject();
        return generatePrepared(origin,key,text,language,speed,steps,uploaded,referenceText,output,cancelled,progress,
            new Checkpoint(){public JSONObject data(){return state;}public void save(){}});
    }
    long generatePrepared(String address,String key,String text,String language,float speed,int steps,JSONObject uploaded,String referenceText,
            File output,AtomicBoolean cancelled,AcceleratedSpeechClient.Progress progress,Checkpoint checkpoint) throws Exception {
        String origin=base(address);AcceleratedSpeechClient.token(key);boolean complete=false;
        JSONObject state=checkpoint.data();request(text,language,speed,steps,uploaded,referenceText);
        try {
            String route=uploaded==null?"_design_fn":"_clone_fn";
            JSONObject audio=state.optJSONObject("audio");
            if(audio==null) {
                String phase=state.optString("phase");
                if(phase.equals("submitted")||phase.equals("submitting"))throw new RemoteSpeechPipeline.ResubmitRequired();
                state.put("phase","submitting");checkpoint.save();
                byte[] body=request(text,language,speed,steps,uploaded,referenceText).toString().getBytes("UTF-8");
                HttpURLConnection post=open(new URL(origin+"/gradio_api/call/"+route),key,"POST",cancelled);String id;
                try {
                    post.setDoOutput(true);post.setFixedLengthStreamingMode(body.length);post.setRequestProperty("Content-Type","application/json");
                    progress.update("提交免费 GPU 合成任务…");try(OutputStream out=post.getOutputStream()){out.write(body);}
                    int code=post.getResponseCode();
                    if(code==400||code==401||code==403||code==404||code==422||code==429){state.remove("phase");checkpoint.save();}
                    status(post);
                    try(InputStream in=post.getInputStream()){id=new JSONObject(json(in,cancelled)).getString("event_id");}
                    if(!id.matches("[a-zA-Z0-9_-]{1,128}"))throw new IOException("Space 任务编号无效");
                    state.put("phase","submitted").put("event_id",id);checkpoint.save();
                }finally{close(post);}
                JSONArray result;HttpURLConnection queue=open(new URL(origin+"/gradio_api/call/"+route+"/"+id),key,"GET",cancelled);
                try {status(queue);progress.update("免费 GPU 排队或合成中…");try(InputStream in=queue.getInputStream()){result=events(in,cancelled,progress);}}finally{close(queue);}
                audio=result.getJSONObject(0);fileUrl(origin,audio);state.put("audio",audio).put("phase","download");checkpoint.save();
            }
            download(fileUrl(origin,audio),key,output,cancelled,progress);
            long millis;
            try {millis=AcceleratedSpeechClient.validate(output,text,cancelled);}
            catch(AcceleratedSpeechClient.IncompleteSpeech error) {
                state.remove("audio");state.put("phase","submitted");checkpoint.save();throw error;
            }
            complete=true;return millis;
        }finally{if(!complete)output.delete();}
    }
}
