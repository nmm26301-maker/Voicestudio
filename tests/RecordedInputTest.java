package com.voice.studio;
import java.io.*;
import java.nio.file.*;

public final class RecordedInputTest {
    static int checks;
    static void ok(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    static void wav(File file, int frames) throws Exception {
        try (PcmAudio.Writer writer = new PcmAudio.Writer(file, 16000)) { writer.write(new float[frames]); }
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("recorded-input").toFile();
        File pending = new File(root, "input.pending.wav"), saved = new File(root, "input.wav");
        try {
            wav(pending, 15999);
            try { RecordedInput.preserve(pending, saved); throw new AssertionError(); } catch (IOException expected) { checks++; }
            ok(pending.isFile()); ok(!saved.exists());
            wav(pending, 16000); RecordedInput.preserve(pending, saved);
            ok(!pending.exists()); ok(saved.isFile());
            try (PcmAudio.Reader reader = new PcmAudio.Reader(saved)) { ok(reader.frames == 16000); }
            wav(pending, 32000);
            try { RecordedInput.preserve(pending, saved); throw new AssertionError(); } catch (IOException expected) { checks++; }
            try (PcmAudio.Reader reader = new PcmAudio.Reader(saved)) { ok(reader.frames == 16000); }
            ok(pending.isFile()); saved.delete();
            try (FileOutputStream output = new FileOutputStream(pending)) { output.write(new byte[100]); }
            try { RecordedInput.preserve(pending, saved); throw new AssertionError(); } catch (IOException expected) { checks++; }
            ok(!saved.exists());
            System.out.println(checks + " assertions passed: finalized partial recording recovery, short/corrupt input rejection, existing input protection");
        } finally { pending.delete(); saved.delete(); root.delete(); }
    }
}
