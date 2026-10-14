package com.voice.studio;

import java.io.*;
import java.text.Normalizer;
import java.util.Locale;
import org.json.JSONObject;

/** One-time recording transcript checks, bound to the exact saved prompt and crop. */
final class ReferenceTranscript {
    static String identity(VoicePrompt voice, File source, double start, double seconds) throws Exception {
        return voice.identity() + ":" + source.getCanonicalPath() + ":" + source.length()
                + ":" + source.lastModified() + ":" + Double.toString(start) + ":" + Double.toString(seconds);
    }
    static boolean checked(JSONObject meta, VoicePrompt voice, File source, double start, double seconds) throws Exception {
        return identity(voice, source, start, seconds).equals(meta.optString("referenceTranscriptCheckedFor"));
    }
    static String select(String provided, String recognized, int frames) throws IOException {
        if (recognized == null || recognized.trim().isEmpty() || recognized.length() > 2000
                || OmniDuration.needsTranscript(recognized, frames))
            throw new IOException("没有识别到可靠的参考文字，请核对录音内容");
        if (provided == null || provided.trim().isEmpty() || OmniDuration.needsTranscript(provided, frames))
            return recognized.trim();
        String a = normalize(provided), b = normalize(recognized);
        // Keep hand-corrected names and small ASR errors; repair substantially different content.
        int[] left = a.codePoints().toArray(), right = b.codePoints().toArray();
        int[] row = new int[right.length + 1];
        for (int j = 0; j <= right.length; j++) row[j] = j;
        for (int i = 1; i <= left.length; i++) {
            int diagonal = row[0];row[0] = i;
            for (int j = 1; j <= right.length; j++) {
                int previous = row[j];
                row[j] = Math.min(diagonal + (left[i-1] == right[j-1] ? 0 : 1),
                        Math.min(row[j] + 1, row[j-1] + 1));
                diagonal = previous;
            }
        }
        return row[right.length] > Math.max(left.length, right.length) * .35
                ? recognized.trim() : provided;
    }
    private static String normalize(String text) {
        StringBuilder result = new StringBuilder();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        for (int at = 0; at < normalized.length();) {
            int cp = normalized.codePointAt(at);at += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) result.appendCodePoint(cp);
        }
        return result.toString();
    }
}
