package com.voice.studio;

import java.util.regex.Pattern;

/** Qwen pre-tokenization without the unsupported Android flag 256. */
final class OmniWordPattern {
    // Unicode White_Space, explicit so host Java and Android ICU split identically.
    private static final String SPACE = "[\\t-\\r \\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]";
    static Pattern compile() {
        String expression = "(?iu:'s|'t|'re|'ve|'m|'ll|'d)|[^\\r\\n\\p{L}\\p{N}]?\\p{L}+|\\p{N}| ?[^\\s\\p{L}\\p{N}]+[\\r\\n]*|\\s*[\\r\\n]+|\\s+(?!\\S)|\\s+";
        return Pattern.compile(expression.replace("\\s", SPACE).replace("\\S", "[^" + SPACE.substring(1, SPACE.length() - 1) + "]"));
    }
}
