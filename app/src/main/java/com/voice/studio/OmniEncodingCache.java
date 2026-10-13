package com.voice.studio;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Private per-task completed acoustic tokens, atomically stored and bound to synthesis inputs. */
final class OmniEncodingCache {
    final File folder;
    final String model;
    static final class Entry {
        final int frames;final long[] codes;
        Entry(int frames,long[] codes){this.frames=frames;this.codes=codes;}
    }
    OmniEncodingCache(File folder,File modelDir)throws Exception {
        this.folder=folder;
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("无法保存本地合成进度");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeUTF("VoiceStudio acoustic cache v1");out.writeUTF(modelDir.getCanonicalPath());
            fingerprint(modelDir,out,0,new int[]{0});
            File tokenizer=new File(modelDir,"tokenizer.json");
            out.writeUTF(hash(tokenizer));
        }
        model=hash(bytes.toByteArray());
    }
    // Installed model content is checked on import. Metadata detects replacement without rehashing GB weights each resume.
    private static void fingerprint(File directory,DataOutputStream out,int depth,int[] files)throws Exception {
        if(depth>8)throw new IOException("模型目录过深");File[] list=directory.listFiles();
        if(list==null)throw new IOException("无法读取模型目录");out.writeInt(list.length);Arrays.sort(list,Comparator.comparing(File::getName));
        for(File f:list){if(++files[0]>4096)throw new IOException("模型文件过多");out.writeUTF(f.getName());out.writeBoolean(f.isDirectory());
            if(f.isDirectory())fingerprint(f,out,depth+1,files);else{out.writeLong(f.length());out.writeLong(f.lastModified());if(f.getName().endsWith(".onnx")&&f.length()<=4L*1024*1024)out.writeUTF(hash(f));}}
    }
    String key(String text,String language,float speed,VoicePrompt ref,String instruct,int steps,long seed)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeUTF(model);out.writeUTF(text);out.writeUTF(language==null?"":language);out.writeFloat(speed);
            out.writeUTF(instruct==null?"":instruct);out.writeInt(steps);out.writeLong(seed);out.writeBoolean(ref!=null);
            if(ref!=null){out.writeUTF(ref.text);out.writeInt(ref.frames);out.writeDouble(ref.rms);for(long code:ref.codes)out.writeLong(code);}
        }
        return hash(bytes.toByteArray());
    }
    File path(int index){return new File(folder,String.format(Locale.ROOT,"part-%04d.tokens",index));}
    Entry read(int index,String key,int frames)throws Exception {
        File file=path(index);if(!file.isFile())return null;
        try {
            if(file.length()<100||file.length()>17000)throw new IOException("本地缓存长度无效");
            byte[] all=new byte[(int)file.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(file))){in.readFully(all);}
            int size=all.length-32;byte[] digest=MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(all,size));
            if(!MessageDigest.isEqual(digest,Arrays.copyOfRange(all,size,all.length)))throw new IOException("本地缓存校验失败");
            try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(all,0,size))) {
                if(in.readInt()!=0x56534331||!in.readUTF().equals(key))return null;
                int stored=in.readInt(),count=in.readInt();if(stored!=frames||stored<1||stored>250||count!=stored*8)throw new IOException("本地缓存形状无效");
                long[] codes=new long[count];for(int i=0;i<count;i++){codes[i]=in.readLong();if(codes[i]<0||codes[i]>=1024)throw new IOException("本地缓存未完成");}
                if(in.available()!=0)throw new IOException("本地缓存含异常尾部");return new Entry(stored,codes);
            }
        }catch(IOException error){file.delete();return null;}
    }
    void save(int index,String key,int frames,long[] codes)throws Exception {
        if(frames<1||frames>250||codes.length!=frames*8)throw new IOException("本地分段形状无效");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(0x56534331);out.writeUTF(key);out.writeInt(frames);out.writeInt(codes.length);
            for(long code:codes){if(code<0||code>=1024)throw new IOException("本地分段尚未完成");out.writeLong(code);}
        }
        byte[] payload=bytes.toByteArray();File target=path(index),pending=new File(target.getPath()+".pending");
        try {
            try(FileOutputStream out=new FileOutputStream(pending)){out.write(payload);out.write(MessageDigest.getInstance("SHA-256").digest(payload));out.getFD().sync();}
            if(!pending.renameTo(target))throw new IOException("本地合成进度保存失败");
        }finally{pending.delete();}
    }
    private static String hash(File file)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
        return hex(digest.digest());
    }
    private static String hash(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
}
