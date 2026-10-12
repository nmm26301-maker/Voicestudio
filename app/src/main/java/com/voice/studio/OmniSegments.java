package com.voice.studio;

import java.util.*;

/** Duration-based splitting without rescanning every prefix; code-point boundaries are preserved. */
final class OmniSegments {
    static final double UNIT = 25 / OmniDuration.total("Nice to meet you.");
    static double frames(double weight) {
        double estimate = weight * UNIT;
        return estimate < 50 ? 50 * Math.pow(estimate / 50, 1.0 / 3) : estimate;
    }
    static double duration(double weight, float speed, VoicePrompt ref) {
        return (ref == null ? frames(weight) : OmniDuration.boost(ref.frames * weight / Math.max(1, OmniDuration.total(ref.text)))) / speed;
    }
    static double estimate(double weight, float speed, double referenceScale) {
        return (referenceScale == 0 ? frames(weight) : OmniDuration.boost(referenceScale * weight)) / speed;
    }
    static List<String> split(String text, float speed, VoicePrompt ref) {
        List<String> result = new ArrayList<>();
        String input = text.trim();
        // Each code point is weighted once. This also avoids large temporary prefix strings.
        double[] prefix = new double[input.length() + 1];
        for (int at = 0; at < input.length();) {
            int cp = input.codePointAt(at), next = at + Character.charCount(cp);
            prefix[next] = prefix[at] + OmniDuration.weight(cp); at = next;
        }
        double referenceScale = ref == null ? 0 : ref.frames / Math.max(1, OmniDuration.total(ref.text));
        int start = 0;
        while (start < input.length()) {
            if (estimate(prefix[input.length()] - prefix[start], speed, referenceScale) <= 180) {
                String last = input.substring(start).trim(); if (!last.isEmpty()) result.add(last); break;
            }
            int limit = start, strong = start, comma = start, space = start;
            for (int at = start; at < input.length();) {
                int cp = input.codePointAt(at), next = at + Character.charCount(cp);
                if (estimate(prefix[next] - prefix[start], speed, referenceScale) > 180 && limit > start) break;
                limit = next;
                if ("。！？!?\n.".indexOf(cp) >= 0) strong = next;
                else if ("，,；;：:".indexOf(cp) >= 0) comma = next;
                else if (Character.isWhitespace(cp)) space = next;
                at = next;
            }
            int cut = strong > start ? strong : comma > start ? comma : space > start ? space : limit;
            if (cut <= start) cut = start + Character.charCount(input.codePointAt(start));
            String piece = input.substring(start, cut).trim(); if (!piece.isEmpty()) result.add(piece);
            start = cut;
            // Match String.trim used by the original splitter without rebuilding the remainder.
            while (start < input.length() && input.charAt(start) <= ' ') start++;
        }
        return result;
    }
}
