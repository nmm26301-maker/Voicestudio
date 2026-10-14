package com.voice.studio;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** Sequential, lossless text chunks. Completed WAVs survive cancellation and network failure. */
final class RemoteSpeechPipeline {
    static final class ResubmitRequired extends IOException {
        private static final long serialVersionUID=1L;
        ResubmitRequired(){super("该段已提交服务器但结果未收到。已完成段会保留，重新提交可能再次消耗 GPU 额度。");}
    }
    static List<String> split(String text,int limit) throws IOException {
        if(text==null||text.trim().isEmpty()||text.length()>12000||limit<2)throw new IOException("文本需为 1—12000 字");
        List<String> result=new ArrayList<>();int at=0;
        while(at<text.length()) {
            int end=Math.min(text.length(),at+limit);
            if(end<text.length()) {
                if(Character.isHighSurrogate(text.charAt(end-1)))end--;
                int minimum=at+limit/2;
                for(int i=end-1;i>=minimum;i--)if("。！？；.!?;\n".indexOf(text.charAt(i))>=0){end=i+1;break;}
            }
            String part=text.substring(at,end);at=end;
            if(part.trim().isEmpty()&&!result.isEmpty())result.set(result.size()-1,result.get(result.size()-1)+part);
            else result.add(part);
        }
        if(result.size()>256)throw new IOException("分段过多，请缩短文本");return result;
    }
    static long generate(JSONObject job,String key,File reference,String referenceText,File folder,File output,
            AtomicBoolean cancelled,AcceleratedSpeechClient.Progress progress,GradioSpeechClient gradio,AcceleratedSpeechClient rest) throws Exception {
        boolean useGradio=job.optString("protocol","rest").equals("gradio");String text=job.getString("text");
        List<String> parts=split(text,useGradio?180:job.optString("model").equals("k2-fsa/OmniVoice")?2000:180);
        RemoteSpeechSession session=new RemoteSpeechSession(folder,RemoteSpeechSession.identity(job,reference,referenceText));
        JSONObject uploaded=session.data.optJSONObject("reference");
        if(reference!=null&&uploaded==null&&useGradio) {
            progress.update("上传所选参考片段（本任务仅一次）…");
            uploaded=gradio.uploadReference(job.getString("endpoint"),key,reference,cancelled);session.data.put("reference",uploaded);session.save();
        }
        if(reference!=null&&!useGradio&&uploaded==null){
            progress.update("上传参考录音到你配置的 HTTPS 云端网关…");
            uploaded=new CloudReferenceClient().upload(job.getString("endpoint"),key,reference,referenceText,cancelled);
            session.data.put("reference",uploaded);session.save();
        }
        boolean complete=false;
        try {
            for(int i=0;i<parts.size();i++) {
                AcceleratedSpeechClient.check(cancelled);String part=parts.get(i);final int index=i;
                JSONObject state=session.segment(i);File cached=session.audio(i);
                if(state.optBoolean("complete")) {
                    if(!cached.isFile()||!RemoteSpeechSession.hash(cached).equals(state.optString("sha256")))throw new IOException("已保存的第 "+(i+1)+" 段不完整，请重新创建任务");
                    progress.update("已保存 "+(i+1)+" / "+parts.size()+" 段 · 复用已完成音频…");continue;
                }
                if(part.trim().isEmpty()){state.put("blank",true);session.save();continue;}
                File pending=new File(cached.getPath()+".pending");
                try {
                    AcceleratedSpeechClient.Progress stage=m->progress.update("已保存 "+index+" / "+parts.size()+" 段 · 第 "+(index+1)+" 段 · "+m);
                    long duration;
                    if(useGradio) {
                        final JSONObject current=state;
                        duration=gradio.generatePrepared(job.getString("endpoint"),key,part,job.optString("languageName","Auto"),
                            (float)job.optDouble("speed",1),job.optInt("steps",32),uploaded,referenceText,pending,cancelled,stage,
                            new GradioSpeechClient.Checkpoint(){public JSONObject data(){return current;}public void save()throws Exception{session.save();}});
                    }else {
                        String phase=state.optString("phase");if(phase.equals("submitted")||phase.equals("submitting"))throw new ResubmitRequired();
                        state.put("phase","submitting");session.save();
                        try {
                            duration=rest.generate(job.getString("endpoint"),key,job.optString("model"),uploaded==null?job.optString("apiVoice","auto"):uploaded.getString("voice"),part,
                                job.optString("language","zh"),(float)job.optDouble("speed",1),job.optInt("steps",32),job.optLong("seed",42),pending,cancelled,stage);
                        }catch(AcceleratedSpeechClient.RequestRejected error){if(error.definitelyRejected()){state.remove("phase");session.save();}throw error;}
                    }
                    AcceleratedSpeechClient.check(cancelled);
                    if(!pending.renameTo(cached))throw new IOException("保存 GPU 分段失败");
                    state.put("complete",true).put("phase","complete").put("duration",duration).put("sha256",RemoteSpeechSession.hash(cached));session.save();
                    progress.update("已保存 "+(i+1)+" / "+parts.size()+" 段 · 音频已校验…");
                }finally{pending.delete();}
            }
            progress.update("已保存 "+parts.size()+" / "+parts.size()+" 段 · 正在合并完整音频…");
            merge(session,parts.size(),output,cancelled);
            long duration=AcceleratedSpeechClient.validate(output,text,cancelled);complete=true;return duration;
        }finally{if(!complete)output.delete();}
    }
    static void merge(RemoteSpeechSession session,int count,File output,AtomicBoolean cancelled) throws Exception {
        int actual=0,only=-1;
        for(int i=0;i<count;i++)if(!session.segment(i).optBoolean("blank")){actual++;only=i;}
        if(actual==1) {
            if(!session.segment(only).optBoolean("complete"))throw new IOException("GPU 分段尚未完成");
            try(InputStream in=new FileInputStream(session.audio(only));OutputStream out=new FileOutputStream(output)) {
                byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){AcceleratedSpeechClient.check(cancelled);out.write(b,0,n);}
            }
            return;
        }
        long frames=0;int rate=0;
        for(int i=0;i<count;i++) {
            JSONObject state=session.segment(i);if(state.optBoolean("blank"))continue;
            if(!state.optBoolean("complete"))throw new IOException("GPU 分段尚未全部完成");
            try(PcmAudio.Reader reader=new PcmAudio.Reader(session.audio(i))) {
                if(rate==0)rate=reader.rate;
                if(reader.rate!=rate||reader.channels!=1||reader.encoding!=1||reader.width!=2)throw new IOException("服务返回的分段格式不一致，请使用单声道 PCM16 WAV");
                frames+=reader.frames;
            }
        }
        if(rate==0||frames*2+128>64L*1024*1024)throw new IOException("完整音频过长或为空，请拆分任务");
        // Copy PCM bytes unchanged; avoids repeated float conversion and sample loss at joins.
        try(PcmAudio.Writer writer=new PcmAudio.Writer(output,rate)) {
            byte[] b=new byte[32768];
            for(int i=0;i<count;i++) {
                AcceleratedSpeechClient.check(cancelled);if(session.segment(i).optBoolean("blank"))continue;
                try(PcmAudio.Reader reader=new PcmAudio.Reader(session.audio(i))) {
                    reader.file.seek(reader.offset);long remaining=reader.frames*2;
                    while(remaining>0){AcceleratedSpeechClient.check(cancelled);int n=reader.file.read(b,0,(int)Math.min(b.length,remaining));if(n<=0)throw new IOException("GPU 分段被截断");writer.f.write(b,0,n);remaining-=n;}
                    writer.count+=reader.frames;
                }
            }
        }
    }
}
