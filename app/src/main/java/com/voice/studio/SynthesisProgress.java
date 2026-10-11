package com.voice.studio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only reports a fraction when the engine supplies actual inference steps. */
final class SynthesisProgress {
    private static final Pattern STEPS = Pattern.compile("本机合成 · ([0-9]+) / ([0-9]+) 步");
    static float fraction(String stage) {
        Matcher m = STEPS.matcher(stage == null ? "" : stage);
        if (!m.find()) return -1;
        try {
            int done = Integer.parseInt(m.group(1)), total = Integer.parseInt(m.group(2));
            return total > 0 && done >= 0 && done <= total ? (float) done / total : -1;
        } catch (NumberFormatException ignored) { return -1; }
    }
    static boolean active(String state) { return "queued".equals(state) || "running".equals(state); }
}
