package com.voice.studio;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual graph experiment with a public synthetic control, never a user's recording. */
public class ReferenceRepairAcceptance {
    public static void main(String[] args) throws Exception {
        File models = new File(args[0]), folder = new File(args[1]);folder.mkdirs();
        AtomicBoolean cancel = new AtomicBoolean();
        File recording = new File(folder, "control-reference.wav");
        if (args.length == 2) {
            new OmniEngine().generate(models, "你好，欢迎来到声音工作室。今天我们一起测试新的音色。", "zh", 1,
                    recording, cancel, System.out::println, null, "None", 32, 42);
            return;
        }
        String recognized = args[2];double seconds;
        try (PcmAudio.Reader r = new PcmAudio.Reader(recording)) { seconds = r.seconds(); }
        VoicePrompt encoded = new OmniCodec().encode(models, recording, recognized, 0, seconds, cancel, System.out::println);
        String wrong = "山上的树木很多，汽车经过桥梁。请检查这段不同的录音。";
        VoicePrompt legacy = new VoicePrompt(wrong, encoded.codes, encoded.frames, encoded.rms);
        if (OmniDuration.needsTranscript(wrong, encoded.frames)) throw new AssertionError("Control must bypass old duration heuristic");
        String repaired = ReferenceTranscript.select(wrong, recognized, encoded.frames);
        if (!repaired.equals(recognized)) throw new AssertionError("Failed to repair plausible wrong content");
        VoicePrompt fixed = new VoicePrompt(repaired, legacy.codes, legacy.frames, legacy.rms);
        String[] text = {"你好", "谢谢", "今天的天气很好，我们一起去公园散步。"};
        boolean shortOnly=args.length>3&&args[3].equals("short");
        for (int steps : new int[]{16,32}) for (int i = 0; i < text.length; i++) {
            if(shortOnly&&(steps!=32||i==2))continue;
            OmniEngine engine = new OmniEngine();
            engine.generate(models, text[i], "zh", 1, new File(folder, "fixed-"+steps+"-"+i+".wav"),
                    cancel, m -> {}, fixed, "None", steps, 42);
            System.out.println("GENERATED text="+text[i]+" steps="+steps+" host_ms="+engine.lastGenerationMillis);
        }
        if(!shortOnly)new OmniEngine().generate(models, text[2], "zh", 1, new File(folder, "wrong-reference-16.wav"), cancel,
                m -> {}, legacy, "None", 16, 42);
        System.out.println("Actual reference repair control completed; independent recognition/listening required.");
    }
}
