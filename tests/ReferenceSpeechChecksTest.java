package com.voice.studio;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Counterexamples for valid-but-truncated WAVs and mismatched legacy voice prompts. */
public class ReferenceSpeechChecksTest {
    static int checks;
    interface Checked { void run() throws Exception; }
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static void reject(Checked action, String message) throws Exception {
        try { action.run(); } catch (IOException expected) { check(expected.getMessage().contains(message), expected.getMessage()); return; }
        throw new AssertionError("Expected rejection: " + message);
    }
    public static void main(String[] args) throws Exception {
        String target = "愿你今天遇见的，都是温柔的小事。放慢脚步，也别忘了听听自己的声音。";
        StringBuilder paragraph = new StringBuilder(); for (int i=0;i<330;i++) paragraph.append('啊');
        String wrong = paragraph.toString();
        // This ratio actually yielded 1.28s with the shipped native graphs before this fix.
        reject(() -> new VoicePrompt(wrong, new long[8*157], 157, .1), "参考文字与录音时长明显不匹配");
        for (int frames : new int[]{75,150,250,500})
            reject(() -> OmniDuration.checkReference(wrong, frames), "参考文字与录音时长明显不匹配");
        reject(() -> OmniDuration.checkReference("……！？", 150), "没有可朗读内容");
        reject(() -> OmniDuration.checkOutput(target, 1, 1.28), "明显短于输入文字");
        reject(() -> OmniDuration.checkOutput(target, 1, 1.4), "明显短于输入文字");
        for (float speed : new float[]{.5f,1,1.5f,2}) {
            OmniDuration.checkOutput(target,speed,6.28/speed); checks++;
            OmniDuration.checkOutput("你好。",speed,1.2/speed); checks++;
            reject(() -> OmniDuration.checkOutput(target,speed,1.28/speed), "明显短于输入文字");
        }
        for (String text : new String[]{"你好，欢迎来到声音工作室。", "Welcome to VoiceStudio.", "مرحبا بالعالم", "こんにちは世界", "𠀀你好", "一二三四12345"}) {
            new VoicePrompt(text,new long[8*150],150,.1);checks++;
            OmniDuration.checkOutput(text,1,OmniDuration.frames(text)/25);checks++;
        }
        reject(() -> OmniDuration.checkOutput("！！",1,1), "没有可朗读");
        reject(() -> OmniDuration.checkOutput(target,Float.NaN,1), "时长或语速无效");
        reject(() -> OmniDuration.checkOutput(target,1,Double.NaN), "时长或语速无效");
        reject(() -> OmniDuration.checkOutput(target,1,0), "时长或语速无效");
        File folder=Files.createTempDirectory("reference-checks").toFile();
        try {
            File legacy=new File(folder,"legacy.vpc");
            try(DataOutputStream out=new DataOutputStream(new FileOutputStream(legacy))) {
                out.writeInt(0x56504331);out.writeUTF(wrong);out.writeInt(157);out.writeDouble(.1);
                for(int i=0;i<8*157;i++)out.writeShort(0);
            }
            byte[] before=Files.readAllBytes(legacy.toPath());
            reject(() -> VoicePrompt.load(legacy), "参考文字与录音时长明显不匹配");
            check(Arrays.equals(before,Files.readAllBytes(legacy.toPath())),"invalid old profile retained for correction");
            VoicePrompt good=new VoicePrompt(target,new long[8*157],157,.1);File valid=new File(folder,"valid.vpc");good.save(valid);
            check(VoicePrompt.load(valid).text.equals(target),"existing valid VPC1 is still compatible");
            File shortWav=new File(folder,"short.wav");
            try(PcmAudio.Writer writer=new PcmAudio.Writer(shortWav,24000)){writer.silence(1.28);}
            AudioChecks.validate(shortWav,"wav");check(shortWav.length()>32,"counterexample is a structurally valid WAV");
            try(PcmAudio.Reader r=new PcmAudio.Reader(shortWav)) {
                reject(() -> OmniDuration.checkOutput(target,1,r.seconds()),"明显短于输入文字");
            }
        } finally {for(File f:folder.listFiles())f.delete();folder.delete();}
        System.out.println("Reference and local output checks: "+checks+" passed; plausibility check, not semantic acceptance.");
    }
}
