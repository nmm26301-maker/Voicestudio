package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
/** Read-only configuration probe. Does not upload recordings or submit speech. */
final class CloudHealthClient {
    interface Connections { HttpURLConnection open(URL url) throws IOException; }
    final Connections connections;
    CloudHealthClient(){this(url -> (HttpURLConnection)url.openConnection());}
    CloudHealthClient(Connections connections){this.connections=connections;}
    Set<String> check(String address,String key) throws Exception {
        String speech=AcceleratedSpeechClient.endpoint(address);AcceleratedSpeechClient.token(key);
        if(key.isEmpty())throw new IOException("请填写网关密钥");
        URL target=new URL(speech.substring(0,speech.length()-"/audio/speech".length())+"/health");
        HttpURLConnection c=connections.open(target);
        try {
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);
            c.setRequestMethod("GET");c.setRequestProperty("Authorization","Bearer "+key);
            if(c.getResponseCode()!=200)throw new IOException("连接检测 HTTP "+c.getResponseCode()+"；请检查地址和密钥");
            ByteArrayOutputStream b=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){byte[] bytes=new byte[2048];int n;while((n=in.read(bytes))!=-1){if(b.size()+n>32768)throw new IOException("检测响应过大");b.write(bytes,0,n);}}
            JSONObject result=new JSONObject(b.toString("UTF-8"));
            if(!"ok".equals(result.optString("status")))throw new IOException("网关未就绪");
            JSONArray engines=result.getJSONArray("configured");Set<String> ids=new HashSet<>();
            for(int i=0;i<engines.length();i++){String id=engines.getString(i);if(CloudEngineCatalog.known(id))ids.add(id);}
            return ids;
        }finally{c.disconnect();}
    }
}
