package com.voice.studio;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

public final class SpeakerClipsTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    interface Operation { void run() throws Exception; }
    static void rejects(Operation action) throws Exception {
        try { action.run(); throw new AssertionError("Expected rejection"); } catch (IOException expected) { checks++; }
    }
    static JSONObject clip(double start, double end, int speaker) throws Exception {
        return new JSONObject().put("start", start).put("end", end).put("speaker", speaker).put("text", "对白");
    }
    public static void main(String[] args) throws Exception {
        JSONArray input = new JSONArray().put(clip(5, 9, 2)).put(clip(0, 2, 1)).put(clip(2, 5, 0)).put(clip(9, 11, 1));
        Map<Integer, JSONArray> plan = SpeakerClips.plan(input, 11);
        check(plan.size() == 2 && !plan.containsKey(0), "Unknown speech excluded");
        check(plan.get(1).length() == 2, "Nonadjacent same speaker combined");
        check(SpeakerClips.seconds(plan.get(1)) == 4, "Correct total speaker duration");
        check(plan.get(1).getJSONObject(0).getDouble("start") == 0, "Unsorted input sorted");
        check(input.getJSONObject(0).getDouble("start") == 5, "Caller input not mutated");
        rejects(() -> SpeakerClips.plan(new JSONArray().put(clip(0, 3, 1)).put(clip(2, 5, 2)), 10));
        rejects(() -> SpeakerClips.plan(new JSONArray().put(clip(-1, 3, 1)), 10));
        rejects(() -> SpeakerClips.plan(new JSONArray().put(clip(1, 1, 1)), 10));
        rejects(() -> SpeakerClips.plan(new JSONArray().put(clip(0, 11, 1)), 10));
        rejects(() -> SpeakerClips.plan(new JSONArray().put(clip(0, 3, 33)), 10));
        rejects(() -> SpeakerClips.plan(new JSONArray().put(clip(0, 3, 0)), 10));
        rejects(() -> SpeakerClips.plan(input, Double.NaN));
        check(SpeakerClips.plan(new JSONArray().put(clip(0, 10.02, 1)), 10).get(1).getJSONObject(0).getDouble("end") == 10, "Rounding overrun clamped");
        File folder = Files.createTempDirectory("speaker-clips").toFile();
        try {
            File source = new File(folder, "source.wav"), full = new File(folder, "full.wav"), ref = new File(folder, "ref.wav");
            try (PcmAudio.Writer writer = new PcmAudio.Writer(source, 16000)) {
                float[] second = new float[16000];
                for (int n = 0; n < 75; n++) { Arrays.fill(second, n < 2 || n >= 9 ? .25f : -.25f); writer.write(second); }
            }
            try (PcmAudio.Reader reader = new PcmAudio.Reader(source)) {
                long[] count = SpeakerClips.export(reader, plan.get(1), full, ref, new AtomicBoolean());
                check(count[0] == 64000 && count[1] == 64000, "Short disjoint dialogue frame counts");
                try (PcmAudio.Reader out = new PcmAudio.Reader(full)) {
                    float[] samples = out.read(0, 4);
                    for (int n = 0; n < samples.length; n += 101) check(samples[n] > .24f, "Other speakers/gaps excluded");
                }
                JSONArray longClip = new JSONArray().put(clip(9, 74, 1));
                count = SpeakerClips.export(reader, longClip, full, ref, new AtomicBoolean());
                check(count[0] == 65L * 16000, "Complete audio over 60 seconds preserved");
                check(count[1] == 10L * 16000, "Reference capped at 10 seconds");
                try (PcmAudio.Reader out = new PcmAudio.Reader(full); PcmAudio.Reader reference = new PcmAudio.Reader(ref)) {
                    check(out.seconds() == 65 && reference.seconds() == 10, "Actual WAV lengths correct");
                    check(out.read(64.5, .5)[7999] > .24f, "End of long speaker export preserved");
                }
                rejects(() -> SpeakerClips.export(reader, longClip, full, ref, new AtomicBoolean(true)));
            }
        } finally { for (File f : folder.listFiles()) f.delete(); folder.delete(); }
        System.out.println("Speaker extraction: " + checks + " checks passed (real PCM ranges/export; no model inference).");
    }
}
