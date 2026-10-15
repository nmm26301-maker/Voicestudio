package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
/** Read-only voice ID listing. No previews, speech POSTs or redirects. */
final class OfficialVoiceCatalog {
    static final class Page {
        final List<String> ids=new ArrayList<>(),names=new ArrayList<>();String next="";
    }
    final CloudHealthClient.Connections connections;
    OfficialVoiceCatalog(){this(url->(HttpURLConnection)url.openConnection());}
    OfficialVoiceCatalog(CloudHealthClient.Connections c){connections=c;}
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
            if(status!=200)throw new IOException("读取音色 HTTP "+status+"；请检查 API Key 的音色读取权限（未提交合成）");
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
