package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
/** Read-only voice ID listing. No previews, speech POSTs or redirects. */
final class OfficialVoiceCatalog {
    static final class Page {
        final List<String> ids=new ArrayList<>(),names=new ArrayList<>();String next="";
    }
    final CloudHealthClient.Connections connections;
    OfficialVoiceCatalog(){this(url->(HttpURLConnection)url.openConnection());}
    OfficialVoiceCatalog(CloudHealthClient.Connections c){connections=c;}
    String verify(String engine,String address,String key,String voice)throws Exception{
        CloudConnectionPolicy.validate(engine,address);AcceleratedSpeechClient.token(key);
        if(key.isEmpty())throw new IOException("请先填写本平台 API Key");
        OfficialSpeechClient.body(engine,voice,"检查",1);
        if(engine.equals("minimax")){
            if(!minimax(address,key).ids.contains(voice))throw new IOException("MiniMax 密钥可用，但账号返回的音色列表没有此 Voice ID；请读取平台音色或更换 ID");
        }else if(engine.equals("elevenlabs")||engine.equals("cartesia")){
            boolean eleven=engine.equals("elevenlabs");
            HttpURLConnection c=connections.open(new URL((eleven?"https://api.elevenlabs.io/v1/voices/":"https://api.cartesia.ai/voices/")+voice));
            try{
                c.setRequestMethod("GET");c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);
                c.setRequestProperty(eleven?"xi-api-key":"Authorization",eleven?key:"Bearer "+key);
                if(!eleven)c.setRequestProperty("Cartesia-Version","2026-08-14");
                c.setRequestProperty("Accept","application/json");int status=c.getResponseCode();
                if(status!=200)throw OfficialSpeechClient.rejection(engine,status,c,key,new AtomicBoolean());
                JSONObject result=new JSONObject(new String(OfficialSpeechClient.readBounded(c.getInputStream(),2*1024*1024,new AtomicBoolean()),"UTF-8"));
                if(!voice.equals(result.optString(eleven?"voice_id":"id")))throw new IOException("平台未返回所选音色；请重新读取 Voice ID");
            }finally{c.disconnect();}
        }else return "Fish 地址与参数已检查；请使用原有 Fish 合成入口";
        return CloudEngineCatalog.label(engine)+" 认证与音色查询成功；未提交合成，TTS 权限、余额和音质仍需实际生成确认";
    }
    Page minimax(String address,String key)throws Exception{
        String approved=CloudConnectionPolicy.validate("minimax",address);AcceleratedSpeechClient.token(key);
        if(key.isEmpty())throw new IOException("请填写对应地区的 MiniMax API Key");
        String host=approved.substring(0,approved.indexOf("/v1/"));
        HttpURLConnection c=connections.open(new URL(host+"/v1/get_voice"));
        try{
            c.setRequestMethod("POST");c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setDoOutput(true);
            c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json");
            byte[] body="{\"voice_type\":\"all\"}".getBytes("UTF-8");c.setFixedLengthStreamingMode(body.length);
            try(OutputStream out=c.getOutputStream()){out.write(body);}
            int status=c.getResponseCode();if(status!=200)throw OfficialSpeechClient.rejection("minimax",status,c,key,new AtomicBoolean());
            JSONObject result=new JSONObject(new String(OfficialSpeechClient.readBounded(c.getInputStream(),2*1024*1024,new AtomicBoolean()),"UTF-8"));
            if(result.getJSONObject("base_resp").getInt("status_code")!=0)throw new IOException(OfficialSpeechClient.errorMessage("minimax",200,result,key));
            Page page=new Page();Set<String> seen=new HashSet<>();
            for(String group:new String[]{"system_voice","voice_cloning","voice_generation"}){
                JSONArray items=result.optJSONArray(group);if(items==null)continue;if(items.length()>5000)throw new IOException("平台音色列表异常");
                for(int i=0;i<items.length();i++){
                    JSONObject item=items.getJSONObject(i);String id=item.optString("voice_id");
                    if(id.isEmpty()||id.length()>200||id.matches(".*[\\p{Cntrl}].*")||!seen.add(id))continue;
                    String name=item.optString("voice_name",id).replaceAll("[\\r\\n\\t]"," ");if(name.length()>100)name=name.substring(0,100);
                    page.ids.add(id);page.names.add(name+"\n"+id);
                }
            }
            return page;
        }finally{c.disconnect();}
    }
    Page list(String engine,String key,String cursor)throws Exception {
        AcceleratedSpeechClient.token(key);if(key.isEmpty())throw new IOException("请先填写本平台 API Key，再读取音色 ID");
        boolean eleven=engine.equals("elevenlabs");
        if(!eleven&&!engine.equals("cartesia"))throw new IOException("此平台请使用预置 ID 或官方音色库入口");
        if(cursor==null)cursor="";if(cursor.length()>4096)throw new IOException("音色分页标记过长");
        String address=eleven?"https://api.elevenlabs.io/v2/voices?page_size=100":"https://api.cartesia.ai/voices?limit=100";
        if(!cursor.isEmpty())address+=(eleven?"&next_page_token=":"&starting_after=")+URLEncoder.encode(cursor,"UTF-8");
        HttpURLConnection c=connections.open(new URL(address));
        try{
            c.setRequestMethod("GET");c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);
            c.setRequestProperty(eleven?"xi-api-key":"Authorization",eleven?key:"Bearer "+key);
            if(!eleven)c.setRequestProperty("Cartesia-Version","2026-08-14");
            c.setRequestProperty("Accept","application/json");int status=c.getResponseCode();
            if(status!=200)throw OfficialSpeechClient.rejection(engine,status,c,key,new AtomicBoolean());
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] b=new byte[8192];int n;
                while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("音色列表过大");out.write(b,0,n);}
            }
            JSONObject j=new JSONObject(out.toString("UTF-8"));JSONArray items=j.getJSONArray(eleven?"voices":"data");
            if(items.length()>300)throw new IOException("音色列表条数异常");Page page=new Page();Set<String> seen=new HashSet<>();
            for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);String id=item.optString(eleven?"voice_id":"id");
                if(!id.matches(eleven?"[a-zA-Z0-9]{1,120}":"[a-zA-Z0-9-]{10,120}")||!seen.add(id))continue;
                String name=item.optString("name",id).replaceAll("[\\r\\n\\t]"," ");
                if(name.length()>100)name=name.substring(0,100);
                page.ids.add(id);page.names.add(name+"\n"+id);
            }
            if(j.optBoolean("has_more")){
                String next=j.optString(eleven?"next_page_token":"next_page","");
                if(next.isEmpty()||next.length()>4096||next.equals(cursor))throw new IOException("平台音色分页信息无效");page.next=next;
            }
            return page;
        }finally{c.disconnect();}
    }
}
