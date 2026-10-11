package com.voice.studio;
import java.io.*;
import java.nio.file.*;
public class DubClipTest {
    public static void main(String[] args) throws Exception {
        File file = Files.createTempFile("dub-complete", ".wav").toFile();
        try {
            try (PcmAudio.Writer writer = new PcmAudio.Writer(file, 24000)) {
                writer.silence(60);
                float[] tail = new float[24000]; java.util.Arrays.fill(tail, .25f); writer.write(tail);
            }
            try (PcmAudio.Reader reader = new PcmAudio.Reader(file)) {
                float[] full = AudioTiming.readCompleteClip(reader);
                if (full.length != 61 * 24000) throw new AssertionError("the last second was truncated");
                if (Math.abs(full[60 * 24000 + 12000] - .25f) > .001f) throw new AssertionError("spoken tail lost");
            }
            try (PcmAudio.Writer writer = new PcmAudio.Writer(file, 8000)) { writer.silence(151); }
            try (PcmAudio.Reader reader = new PcmAudio.Reader(file)) {
                try { AudioTiming.readCompleteClip(reader); throw new AssertionError("oversized clip should fail before allocation"); }
                catch (IOException expected) { }
            }
            System.out.println("3 dubbing assertions passed: complete 61-second clip, retained tail, oversized clip rejection.");
        } finally { file.delete(); }
    }
}
