package com.voice.studio;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

public class ReferenceTranscriptTest {
    static int checks;
    static void check(boolean ok, String why) { checks++;if (!ok) throw new AssertionError(why); }
    interface Action { void run() throws Exception; }
    static void reject(Action action) throws Exception {
        try { action.run();throw new AssertionError("Expected rejection"); }
        catch (IOException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        String actual = "你好，欢迎来到声音工作室。今天我们一起测试新的音色。";
        String wrong = "山上的树木很多，汽车经过桥梁。请检查这段不同的录音。";
        check(!OmniDuration.needsTranscript(wrong, 150), "old duration heuristic misses plausible-length wrong content");
        check(ReferenceTranscript.select(wrong, actual, 150).equals(actual), "repair wrong content despite plausible duration");
        check(ReferenceTranscript.select(actual, "你好欢迎来到声音工作室今天我们一起测试新的音色", 150).equals(actual), "retain manually corrected punctuation");
        check(ReferenceTranscript.select("今天的天气很好，我们一起去公园散步。", "今天天气很好我们一起去公园散步", 150).startsWith("今天的"), "preserve small recognizer omissions");
        check(ReferenceTranscript.select("Hello WORLD!", "hello world", 150).equals("Hello WORLD!"), "case and punctuation are equivalent");
        check(ReferenceTranscript.select("", actual, 150).equals(actual), "empty manual text uses recognized recording");
        reject(() -> ReferenceTranscript.select(actual, "", 150));
        reject(() -> ReferenceTranscript.select(actual, "……", 150));
        File folder = Files.createTempDirectory("reference-transcript").toFile(), source = new File(folder, "ref.wav");
        try {
            VoicePrompt voice = new VoicePrompt(actual, new long[1200], 150, .1);
            Files.write(source.toPath(), new byte[]{1,2,3});
            JSONObject meta = new JSONObject().put("referenceTranscriptCheckedFor", ReferenceTranscript.identity(voice, source, 2, 6));
            check(ReferenceTranscript.checked(meta, voice, source, 2, 6), "successful check reused for the same voice/crop");
            check(!ReferenceTranscript.checked(meta, voice, source, 0, 6), "different crop rechecked");
            check(!ReferenceTranscript.checked(meta, voice, source, 2, 5), "different duration rechecked");
            check(!ReferenceTranscript.checked(meta, new VoicePrompt(wrong, voice.codes, 150, .1), source, 2, 6), "edited transcript rechecked");
            long[] changed = voice.codes.clone();changed[0] = 1;
            check(!ReferenceTranscript.checked(meta, new VoicePrompt(actual, changed, 150, .1), source, 2, 6), "new acoustic tokens rechecked");
            Files.write(source.toPath(), new byte[]{1,2,3,4});
            check(!ReferenceTranscript.checked(meta, voice, source, 2, 6), "replaced source rechecked");
            File wav = new File(folder, "silence.wav");
            try (PcmAudio.Writer w = new PcmAudio.Writer(wav, 24000)) { w.silence(2); }
            try (PcmAudio.Reader r = new PcmAudio.Reader(wav)) {
                OmniDuration.checkOutput("你好", 1, r.seconds());
                reject(() -> r.validateSpeech(new AtomicBoolean()));
            }
            float[] pcm = new float[48000];for (int i=0;i<pcm.length;i++) pcm[i] = (float)(.0002 * Math.sin(i*.1));
            try (PcmAudio.Writer w = new PcmAudio.Writer(wav, 24000)) { w.write(pcm); }
            byte[] before = Files.readAllBytes(wav.toPath());
            try (PcmAudio.Reader r = new PcmAudio.Reader(wav)) { r.validateSpeech(new AtomicBoolean());checks++;reject(() -> r.validateSpeech(new AtomicBoolean(true))); }
            check(java.util.Arrays.equals(before, Files.readAllBytes(wav.toPath())), "quiet valid PCM retained unchanged");
            try (PcmAudio.Reader r = new PcmAudio.Reader(wav); RandomAccessFile f = new RandomAccessFile(wav,"rw")) {
                f.setLength(100);reject(() -> r.validateSpeech(new AtomicBoolean()));
            }
        } finally { for (File f : folder.listFiles()) f.delete();folder.delete(); }
        System.out.println("Reference transcript and silent-output regression: " + checks + " checks passed; ASR accuracy is separate.");
    }
}
