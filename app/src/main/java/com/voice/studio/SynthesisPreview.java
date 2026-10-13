package com.voice.studio;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** A separate, complete first-segment WAV. It is never a completed work. */
final class SynthesisPreview {
    static final class SourceFailure extends IOException {
        private static final long serialVersionUID=1L;
        SourceFailure(IOException cause){super("恢复完整音频写入位置失败",cause);}
    }
    static final String READY="首段已完成，可先试听";
    static File file(File jobs,JSONObject job) {
        if(job==null||!job.optBoolean("previewReady")||!job.optString("type","tts").equals("tts"))return null;
        if(!Arrays.asList("running","paused","failed").contains(job.optString("state")))return null;
        String id=job.optString("id");if(!id.matches("[a-f0-9-]{36}"))return null;
        File folder=new File(jobs,id),audio;
        if(job.optString("engine","omni").equals("accelerated"))audio=remote(new File(folder,"gpu"));
        else if(job.optString("engine","omni").equals("omni"))audio=new File(folder,"preview.wav");
        else return null;
        return audio!=null&&audio.isFile()&&audio.length()>44?audio:null;
    }
    static File remote(File folder) {
        File[] files=folder.listFiles(f->f.isFile()&&f.getName().matches("part-[0-9]{4}\\.wav"));
        if(files==null||files.length==0)return null;Arrays.sort(files,Comparator.comparing(File::getName));return files[0];
    }
    static void write(File target,PcmAudio.Writer source,long offset,int samples,AtomicBoolean cancel)throws IOException {
        if(samples<1||samples>240000||source.channels!=1||source.rate!=24000)throw new IOException("试听片段参数无效");
        File pending=new File(target.getPath()+".pending");long cursor=source.f.getFilePointer();
        if(offset<44||cursor-offset!=samples*2L)throw new IOException("试听片段尚未完整");
        try {
            try(PcmAudio.Writer out=new PcmAudio.Writer(pending,24000)) {
                source.f.seek(offset);byte[] block=new byte[32768];long remaining=samples*2L;
                while(remaining>0){check(cancel);int n=(int)Math.min(block.length,remaining);source.f.readFully(block,0,n);out.f.write(block,0,n);remaining-=n;}
                out.count=samples;check(cancel);
            }
            PcmAudio.markSynthetic(pending);
            try(RandomAccessFile verify=new RandomAccessFile(pending,"rw")){verify.getFD().sync();}
            check(cancel);if(!pending.renameTo(target))throw new IOException("无法保存试听片段");
        }finally{try{source.f.seek(cursor);}catch(IOException error){throw new SourceFailure(error);}finally{pending.delete();}}
    }
    private static void check(AtomicBoolean cancel)throws InterruptedIOException {
        if(cancel.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消生成");
    }
}
