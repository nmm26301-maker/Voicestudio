package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.*;
public final class OfficialVoiceCatalogTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    interface Action{void run()throws Exception;}
    static void reject(Action a)throws Exception{boolean failed=false;try{a.run();}catch(Exception expected){failed=true;}check(failed);}
    static class Fake extends HttpURLConnection {
        byte[] bytes;int status=200;boolean closed;
        Fake(URL url,String body)throws Exception{super(url);bytes=body.getBytes("UTF-8");}
        public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
        public int getResponseCode(){return status;}public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
        public OutputStream getOutputStream(){throw new AssertionError("Voice listing must not submit synthesis");}
    }
    public static void main(String[] args)throws Exception{
        List<Fake> calls=new ArrayList<>();
        OfficialVoiceCatalog eleven=new OfficialVoiceCatalog(url->{try{
            Fake c=new Fake(url,"{\"voices\":[{\"voice_id\":\"Voice123\",\"name\":\"我的声音\"},{\"voice_id\":\"../bad\"},{\"voice_id\":\"Voice123\"}],\"has_more\":true,\"next_page_token\":\"next & ?\"}");if(url.getQuery().contains("next_page_token="))c.bytes="{\"voices\":[],\"has_more\":false}".getBytes("UTF-8");calls.add(c);return c;
        }catch(Exception e){throw new IOException(e);}});
        OfficialVoiceCatalog.Page p=eleven.list("elevenlabs","read-only-fixture","");
        check(p.ids.equals(Collections.singletonList("Voice123")));check(p.names.get(0).startsWith("我的声音"));check(p.next.equals("next & ?"));
        Fake c=calls.get(0);check(c.getURL().toString().equals("https://api.elevenlabs.io/v2/voices?page_size=100"));
        check(c.getRequestMethod().equals("GET"));check(c.getRequestProperty("xi-api-key").equals("read-only-fixture"));check(c.closed&&!c.getInstanceFollowRedirects());
        eleven.list("elevenlabs","read-only-fixture",p.next);check(calls.get(1).getURL().toString().endsWith("next_page_token=next+%26+%3F"));
        OfficialVoiceCatalog cartesia=new OfficialVoiceCatalog(url->{try{Fake f=new Fake(url,"{\"data\":[{\"id\":\"123e4567-e89b-12d3-a456-426614174000\",\"name\":\"Voice\"}],\"has_more\":false}");calls.add(f);return f;}catch(Exception e){throw new IOException(e);}});
        check(cartesia.list("cartesia","cartesia-key","").ids.size()==1);c=calls.get(2);
        check(c.getRequestProperty("Authorization").equals("Bearer cartesia-key"));check(c.getRequestProperty("Cartesia-Version").equals("2026-08-14"));check(c.getURL().getHost().equals("api.cartesia.ai"));
        reject(()->eleven.list("cartesia","", ""));reject(()->eleven.list("fish","key", ""));reject(()->eleven.list("elevenlabs","bad\nkey", ""));
        for(int code:new int[]{301,401,403,500}){
            Fake bad=new Fake(new URL("https://api.cartesia.ai/voices"),"{}");bad.status=code;
            reject(()->new OfficialVoiceCatalog(url->bad).list("cartesia","key",""));check(bad.closed&&!bad.getInstanceFollowRedirects());
        }
        Fake huge=new Fake(new URL("https://api.cartesia.ai/voices"),String.join("",Collections.nCopies(2*1024*1024+1,"x")));
        reject(()->new OfficialVoiceCatalog(url->huge).list("cartesia","key",""));check(huge.closed);
        check(CloudDeploymentConfig.endpoint("modal","https://user--voice.modal.run").endsWith("/v1/audio/speech"));
        check(CloudDeploymentConfig.endpoint("modal","{\"schema_version\":1,\"provider\":\"modal\",\"engine\":\"qwen3tts\",\"protocol\":\"rest\",\"url\":\"https://user--voice.modal.run\"}").startsWith("https://user--voice.modal.run"));
        for(String id:CloudDeploymentConfig.ENGINES){
            String json="{\"schema_version\":1,\"provider\":\"modal\",\"engine\":\""+id+"\",\"protocol\":\"rest\",\"url\":\"https://user--voice.modal.run\"}";
            check(CloudDeploymentConfig.endpoint("modal",id,json).endsWith("/v1/audio/speech"));
            if(!id.equals("qwen3tts"))reject(()->CloudDeploymentConfig.endpoint("modal","qwen3tts",json));
        }
        reject(()->CloudDeploymentConfig.endpoint("modal","fish","https://user--voice.modal.run"));
        for(String bad:new String[]{"https://modal.com/apps","http://user--voice.modal.run","https://user--voice.modal.run?key=x","https://huggingface.co/spaces/user/space","https://api.cartesia.ai/tts/bytes","https://key@user--voice.modal.run"})reject(()->CloudDeploymentConfig.endpoint("modal",bad));
        reject(()->CloudDeploymentConfig.endpoint("lightning","{\"schema_version\":1,\"provider\":\"modal\",\"engine\":\"qwen3tts\",\"protocol\":\"rest\",\"url\":\"https://user--voice.modal.run\"}"));
        System.out.println("Voice IDs and deployment configuration: "+checks+" read-only/pagination/auth/redirect/bounds/endpoint checks passed.");
    }
}
