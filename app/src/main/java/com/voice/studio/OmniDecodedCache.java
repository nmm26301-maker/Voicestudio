package com.voice.studio;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Completed faded PCM16 and pre-fade anchor RMS, bound to exact acoustic tokens. */
final class OmniDecodedCache {
    static final long LIMIT=64L*1024*1024;
    final File folder;
    long bytes;
    static final class Entry {
        final File file;final long offset;final int samples;final double rms;final boolean anchor;
        Entry(File file,long offset,int samples,boolean anchor,double rms){this.file=file;this.offset=offset;this.samples=samples;this.anchor=anchor;this.rms=rms;}
    }
    OmniDecodedCache(File folder)throws IOException {
        this.folder=folder;File[] files=folder.listFiles();if(files==null)throw new IOException("无法读取本地音频进度");
        for(File f:files)if(f.getName().matches("part-[0-9]+\\.pcm"))bytes+=f.length();
    }
    static String key(String input,long[] codes)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");digest.update(input.getBytes("UTF-8"));
        for(long value:codes)for(int i=0;i<8;i++)digest.update((byte)(value>>>(i*8)));
        StringBuilder result=new StringBuilder();for(byte b:digest.digest())result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
    }
    File path(int index){return new File(folder,String.format(Locale.ROOT,"part-%04d.pcm",index));}
    Entry read(int index,String key,int frames,boolean anchor,AtomicBoolean cancel)throws Exception {
        File file=path(index);if(!file.isFile())return null;long length=file.length();
        try {
            if(length<110||length>491520)throw new IOException("本地音频缓存长度无效");
            try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
                if(in.readInt()!=0x56535031)throw new IOException("本地音频缓存头无效");
                if(!in.readUTF().equals(key))return null;
                int stored=in.readInt(),samples=in.readInt();boolean anchored=in.readBoolean();double rms=in.readDouble();long offset=in.getFilePointer();
                if(stored!=frames||frames<1||frames>250||samples!=frames*960||anchored!=anchor||!Double.isFinite(rms)||
                    (anchor?rms<.1:rms!=0)||length!=offset+samples*2L+32)throw new IOException("本地音频缓存形状无效");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");in.seek(0);long remaining=length-32;byte[] block=new byte[32768];
                while(remaining>0){check(cancel);int n=(int)Math.min(block.length,remaining);in.readFully(block,0,n);digest.update(block,0,n);remaining-=n;}
                byte[] expected=new byte[32];in.readFully(expected);check(cancel);
                if(!MessageDigest.isEqual(digest.digest(),expected))throw new IOException("本地音频缓存校验失败");
                return new Entry(file,offset,samples,anchor,rms);
            }
        }catch(InterruptedIOException e){throw e;}catch(IOException e){if(file.delete())bytes-=length;return null;}
    }
    boolean save(int index,String key,int frames,VoicePrompt anchor,PcmAudio.Writer writer,long start,AtomicBoolean cancel)throws Exception {
        File target=path(index),pending=new File(target.getPath()+".pending");long prior=target.isFile()?target.length():0;
        int samples=Math.multiplyExact(frames,960);long end=writer.f.getFilePointer();
        if(frames<1||frames>250||end-start!=samples*2L)throw new IOException("解码段完整性无效");
        if(bytes-prior+samples*2L+128>LIMIT)return false;
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(FileOutputStream raw=new FileOutputStream(pending);DigestOutputStream hashed=new DigestOutputStream(raw,digest);DataOutputStream out=new DataOutputStream(hashed)) {
                out.writeInt(0x56535031);out.writeUTF(key);out.writeInt(frames);out.writeInt(samples);out.writeBoolean(anchor!=null);out.writeDouble(anchor==null?0:anchor.rms);
                writer.f.seek(start);byte[] block=new byte[32768];long remaining=samples*2L;
                while(remaining>0){check(cancel);int n=(int)Math.min(block.length,remaining);writer.f.readFully(block,0,n);out.write(block,0,n);remaining-=n;}
                check(cancel);out.flush();hashed.on(false);out.write(digest.digest());out.flush();raw.getFD().sync();
            }
            check(cancel);if(!pending.renameTo(target))throw new IOException("本地音频缓存保存失败");bytes=bytes-prior+target.length();return true;
        }finally{writer.f.seek(end);pending.delete();}
    }
    static void append(PcmAudio.Writer writer,Entry entry,AtomicBoolean cancel)throws Exception {
        long start=writer.f.getFilePointer(),oldCount=writer.count;
        try(RandomAccessFile in=new RandomAccessFile(entry.file,"r")) {
            in.seek(entry.offset);byte[] block=new byte[32768];long remaining=entry.samples*2L;
            while(remaining>0){check(cancel);int n=(int)Math.min(block.length,remaining);in.readFully(block,0,n);writer.f.write(block,0,n);remaining-=n;}
            check(cancel);writer.count=oldCount+entry.samples;
        }catch(IOException|RuntimeException error){writer.count=oldCount;try{writer.f.setLength(start);writer.f.seek(start);}catch(IOException rollback){error.addSuppressed(rollback);}throw error;}
    }
    private static void check(AtomicBoolean cancel)throws InterruptedIOException {
        if(cancel.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消生成");
    }
}
