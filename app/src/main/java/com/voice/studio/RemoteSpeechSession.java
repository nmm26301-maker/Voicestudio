package com.voice.studio;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Private per-job checkpoints, without credentials. All network settings are part of identity. */
final class RemoteSpeechSession {
    final File folder;
    final JSONObject data;
    RemoteSpeechSession(File folder,String identity) throws Exception {
        this.folder=folder;
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("无法保存 GPU 分段进度");
        File file=new File(folder,"session.json");data=file.isFile()?read(file):new JSONObject().put("identity",identity).put("segments",new JSONArray());
        if(!identity.equals(data.optString("identity")))throw new IOException("生成参数或参考录音已改变，请创建新任务，避免混合不同音色");
    }
    static JSONObject read(File file) throws Exception {
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("GPU 进度文件过大");out.write(b,0,n);}
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    void save() throws Exception {write(new File(folder,"session.json"),data);}
    static void write(File target,JSONObject value) throws Exception {
        File pending=new File(target.getPath()+".tmp");
        try {
            try(FileOutputStream out=new FileOutputStream(pending)){out.write(value.toString().getBytes("UTF-8"));out.getFD().sync();}
            if(!pending.renameTo(target))throw new IOException("GPU 进度保存失败");
        }finally{pending.delete();}
    }
    JSONObject segment(int index) throws Exception {
        JSONArray list=data.getJSONArray("segments");while(list.length()<=index)list.put(new JSONObject());
        JSONObject item=list.getJSONObject(index);String phase=item.optString("phase");
        if(!Arrays.asList("","submitting","submitted","download","complete").contains(phase))throw new IOException("GPU 进度状态无效");
        if(phase.equals("download")&&item.optJSONObject("audio")==null)throw new IOException("GPU 音频下载进度无效");
        if(phase.equals("complete")&&!item.optBoolean("complete"))throw new IOException("GPU 完成进度无效");
        return item;
    }
    File audio(int index){return new File(folder,String.format(Locale.ROOT,"part-%04d.wav",index));}
    static String hash(File file) throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);}
        return hex(hash.digest());
    }
    static String identity(JSONObject job,File reference,String referenceText) throws Exception {
        StringBuilder out=new StringBuilder();
        for(String key:new String[]{"provider","protocol","endpoint","model","apiVoice","text","language","languageName","profile"})append(out,job.optString(key));
        append(out,Double.toString(job.optDouble("speed",1)));append(out,Integer.toString(job.optInt("steps",32)));append(out,Long.toString(job.optLong("seed",42)));
        append(out,reference==null?"":hash(reference));append(out,referenceText==null?"":referenceText);
        return hex(MessageDigest.getInstance("SHA-256").digest(out.toString().getBytes("UTF-8")));
    }
    private static void append(StringBuilder b,String s){b.append(s.length()).append(':').append(s);}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static boolean needsResubmit(File folder) throws Exception {
        File file=new File(folder,"session.json");if(!file.isFile())return false;
        JSONArray list=read(file).getJSONArray("segments");
        for(int i=0;i<list.length();i++){JSONObject s=list.getJSONObject(i);String phase=s.optString("phase");
            if(!s.optBoolean("complete")&&(phase.equals("submitting")||phase.equals("submitted")))return true;}
        return false;
    }
    static void allowResubmit(File folder) throws Exception {
        File file=new File(folder,"session.json");if(!file.isFile())return;
        JSONObject root=read(file);JSONArray list=root.getJSONArray("segments");
        for(int i=0;i<list.length();i++){JSONObject s=list.getJSONObject(i);String phase=s.optString("phase");
            if(!s.optBoolean("complete")&&(phase.equals("submitting")||phase.equals("submitted"))){s.remove("phase");s.remove("event_id");s.remove("audio");}}
        write(file,root);
    }
}
