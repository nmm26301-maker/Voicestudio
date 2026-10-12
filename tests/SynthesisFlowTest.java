package com.voice.studio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;

public class SynthesisFlowTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    interface Attempt { void run() throws Exception; }
    static void reject(Attempt work) throws Exception {
        try { work.run(); throw new AssertionError("expected failure"); } catch (IOException expected) { checks++; }
    }
    // Original splitter retained only as an independent oracle for multi-language behavior.
    static double boost(double value) { return value<50 ? 50*Math.pow(value/50,1.0/3) : value; }
    static double oldDuration(String text, float speed, VoicePrompt ref) {
        return (ref == null ? OmniDuration.frames(text) : boost(ref.frames * OmniDuration.total(text) / Math.max(1,OmniDuration.total(ref.text)))) / speed;
    }
    static List<String> original(String text, float speed, VoicePrompt ref) {
        List<String> out=new ArrayList<>(); String remaining=text.trim();
        while (!remaining.isEmpty()) {
            if (oldDuration(remaining,speed,ref)<=180) {out.add(remaining);break;}
            int limit=0,strong=0,comma=0,space=0;
            for(int at=0;at<remaining.length();) {
                int cp=remaining.codePointAt(at),next=at+Character.charCount(cp);
                if(oldDuration(remaining.substring(0,next),speed,ref)>180 && limit>0) break;
                limit=next;
                if("。！？!?\n.".indexOf(cp)>=0)strong=next;
                else if("，,；;：:".indexOf(cp)>=0)comma=next;
                else if(Character.isWhitespace(cp))space=next;
                at=next;
            }
            int cut=strong>0?strong:comma>0?comma:space>0?space:limit;
            if(cut<1)cut=Character.charCount(remaining.codePointAt(0));
            String piece=remaining.substring(0,cut).trim();if(!piece.isEmpty())out.add(piece);
            remaining=remaining.substring(cut).trim();
        }
        return out;
    }
    static String repeat(String word,int count) {StringBuilder b=new StringBuilder();while(count-->0)b.append(word);return b.toString();}
    static void segments() throws Exception {
        VoicePrompt reference=new VoicePrompt("这是参考语音，用来保持说话节奏。",new long[8*100],100,.2);
        String[] samples={"你好，欢迎使用语音工作室。",repeat("长文本不应遗漏任何内容。还有下一句！",80),
                repeat("A sentence, and another one. ",60),repeat("𠀀𠀁😀你好abc，\n",120),
                repeat("مرحبا بكم في استوديو الصوت. ",60),repeat("e\u0301 café\t spaces\n",80),repeat("a",12000),
                "   \n\t ",repeat("ـ",400),repeat("1234567890。",100),"世界🌏😊"};
        for(String sample:samples)for(float speed:new float[]{.5f,1,1.5f,2})for(VoicePrompt ref:new VoicePrompt[]{null,reference}) {
            List<String> parts=OmniSegments.split(sample,speed,ref);
            check(parts.equals(original(sample,speed,ref)),"segment behavior changed: "+sample.substring(0,Math.min(25,sample.length())));
            for(String part:parts) {
                check(!part.isEmpty(),"empty segment");
                boolean intact = true;
                for(int at=0;at<part.length();at++) {
                    char c=part.charAt(at);
                    if(Character.isHighSurrogate(c)) {if(at+1>=part.length() || !Character.isLowSurrogate(part.charAt(at+1)))intact=false;at++;}
                    else if(Character.isLowSurrogate(c))intact=false;
                }
                check(intact,"split surrogate pair");
                check(OmniSegments.duration(OmniDuration.total(part),speed,ref)<=180.000001 || part.codePointCount(0,part.length())==1,"duration exceeds budget");
            }
        }
        Random random=new Random(31);String[] alphabet={"你","好","a"," ","。","𠀀","😀","\n",",","3","é"};
        for(int trial=0;trial<80;trial++) {
            StringBuilder b=new StringBuilder();for(int n=0;n<200+trial;n++)b.append(alphabet[random.nextInt(alphabet.length)]);
            check(OmniSegments.split(b.toString(),1,null).equals(original(b.toString(),1,null)),"random oracle mismatch");
        }
        String longest=repeat("Hello world. 你好！",650);long began=System.nanoTime();
        for(int n=0;n<15;n++)OmniSegments.split(longest,1,reference);
        long modern=System.nanoTime()-began;began=System.nanoTime();
        for(int n=0;n<15;n++)original(longest,1,reference);
        long old=System.nanoTime()-began;
        System.out.println("Host segmentation benchmark (not phone inference): old="+old/1e6+" ms, new="+modern/1e6+" ms");
    }
    static void requests() throws Exception {
        JSONObject a=new JSONObject().put("type","tts").put("engine","fish").put("text","hello").put("reference","abc")
                .put("model","s2.1-pro-free").put("state","queued").put("deduplicate",true);
        JSONObject b=new JSONObject(a.toString());b.remove("state");
        check(SynthesisRequests.same(a,b),"same queued request must coalesce");
        for(String state:new String[]{"running","done","failed","paused"}) {
            a.put("state",state);check(SynthesisRequests.same(a,b)==state.equals("running"),"wrong request state");
        }
        a.put("state","queued");
        for(String property:new String[]{"text","reference","model","engine"}) {
            JSONObject changed=new JSONObject(b.toString()).put(property,"changed");
            check(!SynthesisRequests.same(a,changed),"changed setting must create new task");
        }
        check(!SynthesisRequests.same(a,new JSONObject(b.toString()).put("speed",1.5)),"rate matters");
        check(!SynthesisRequests.same(a,new JSONObject(b.toString()).put("deduplicate",false)),"intentional repeated batch items preserved");
        a=new JSONObject().put("type","tts").put("text","hello").put("state","queued").put("deduplicate",true);
        b=new JSONObject(a.toString()).put("engine","omni").put("speed",1).put("language","zh").put("style","None").put("steps",32).put("seed",42);
        check(SynthesisRequests.same(a,b),"legacy default Omni settings match");
        for(String property:new String[]{"language","profile","style"}) {
            check(!SynthesisRequests.same(a,new JSONObject(b.toString()).put(property,"different")),"local voice settings matter");
        }
        check(!SynthesisRequests.same(a,new JSONObject(b.toString()).put("steps",16)),"sampling steps matter");
        check(!SynthesisRequests.same(a,new JSONObject(b.toString()).put("seed",123)),"seed matters");
        check(!SynthesisRequests.same(a,new JSONObject(b.toString()).put("type","book")),"book chapters not coalesced");
    }
    static void outputs() throws Exception {
        File dir=Files.createTempDirectory("synthesis-output-").toFile(), file=new File(dir,"completed.mp3");
        final int[] inspected={0};
        check(SynthesisOutput.existing(file,"mp3",(f,t)->{inspected[0]++;return 1000;})==0,"missing means synthesize");
        check(inspected[0]==0,"missing file must not be decoded");
        byte[] audio=new byte[128];audio[0]='I';audio[1]='D';audio[2]='3';Files.write(file.toPath(),audio);
        check(SynthesisOutput.existing(file,"mp3",(f,t)->{inspected[0]++;return 1234;})==1234,"completed MP3 must be reused");
        check(Arrays.equals(Files.readAllBytes(file.toPath()),audio),"reuse must not modify audio");
        reject(()->SynthesisOutput.existing(file,"mp3",(f,t)->0));
        reject(()->SynthesisOutput.existing(file,"mp3",(f,t)->{throw new IOException("decoder error");}));
        check(file.exists(),"decoder failure preserves file for recovery");
        Files.write(file.toPath(),new byte[128]);
        reject(()->SynthesisOutput.existing(file,"mp3",(f,t)->1000));
        check(file.length()==128,"bad final output must not be overwritten silently");
        Files.write(file.toPath(),new byte[10]);
        reject(()->SynthesisOutput.existing(file,"mp3",(f,t)->1000));
        file.delete();
        File wav=new File(dir,"completed.wav");byte[] wave=new byte[44];
        wave[0]='R';wave[1]='I';wave[2]='F';wave[3]='F';wave[8]='W';wave[9]='A';wave[10]='V';wave[11]='E';
        Files.write(wav.toPath(),wave);
        check(SynthesisOutput.existing(wav,"wav",(f,t)->1000)==1000,"local WAV reused too");
        check(wav.exists(),"local WAV retained");
        reject(()->SynthesisOutput.existing(dir,"wav",(f,t)->1000));
        wav.delete();dir.delete();
    }
    public static void main(String[] args) throws Exception {
        segments();requests();outputs();
        System.out.println("Synthesis flow: "+checks+" assertions passed (host only).");
    }
}
