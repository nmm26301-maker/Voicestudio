package com.voice.studio;

import java.io.*;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Upload a single reference to the SAME HTTPS origin as the explicit speech gateway. */
final class CloudReferenceClient {
    JSONObject upload(String endpoint,String key,File wav,String transcript,AtomicBoolean cancelled) throws Exception {
        String speech=AcceleratedSpeechClient.endpoint(endpoint);
        if(!speech.endsWith("/audio/speech"))throw new IOException("无效的云端网关地址");
        URL url=new URL(speech.substring(0,speech.length()-"/audio/speech".length())+"/voices/reference");
        if(!wav.isFile()||wav.length()<44||wav.length()>5L*1024*1024)throw new IOException("参考录音须为 5 MiB 以下的 WAV");
        if(transcript!=null&&transcript.length()>2000)throw new IOException("参考录音文字过长");
        AcceleratedSpeechClient.token(key);
        if(key.isEmpty())throw new IOException("云端网关密钥不可为空");
        String boundary="vs-"+UUID.randomUUID().toString().replace("-","");
        byte[] head=("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"reference.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes("UTF-8");
        byte[] tail=("\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"transcript\"\r\n\r\n"+(transcript==null?"":transcript)+"\r\n--"+boundary+"--\r\n").getBytes("UTF-8");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(15000);c.setReadTimeout(120000);c.setRequestMethod("POST");
        c.setDoOutput(true);c.setFixedLengthStreamingMode((long)head.length+wav.length()+tail.length);
        c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
        c.setRequestProperty("Authorization","Bearer "+key);
        try {
            AcceleratedSpeechClient.check(cancelled);
            try(OutputStream out=c.getOutputStream();InputStream in=new FileInputStream(wav)) {
                out.write(head);byte[] data=new byte[16384];int n;
                while((n=in.read(data))!=-1){AcceleratedSpeechClient.check(cancelled);out.write(data,0,n);}
                out.write(tail);
            }
            int status=c.getResponseCode();if(status!=200)throw new IOException("上传参考录音 HTTP "+status+"；请检查网关及费用配置");
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(data.size()+n>32768)throw new IOException("网关响应过大");data.write(b,0,n);}}
            JSONObject result=new JSONObject(data.toString("UTF-8"));String voice=result.optString("voice");
            if(!voice.matches("ref-[a-f0-9-]{36}"))throw new IOException("网关未返回有效参考录音编号");
            return new JSONObject().put("voice",voice);
        }finally{c.disconnect();}
    }
}
