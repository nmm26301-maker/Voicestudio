package com.voice.studio;

/** Port of k2-fsa OmniVoice RuleDurationEstimator, Apache-2.0, Han Zhu/Xiaomi 2026. */
final class OmniDuration {
  static final int[] LIMITS = {
    687, 1023, 1327, 1423, 1535, 1919, 2207, 2303, 2431, 2559, 2687, 2815, 2943, 3071, 3199, 3327,
    3455, 3583, 3839, 4095, 4255, 4351, 4607, 4991, 5023, 5119, 5759, 5791, 5887, 5919, 5951, 5983,
    6015, 6143, 6319, 6399, 6479, 6623, 6655, 6687, 6831, 7039, 7103, 7167, 7247, 7295, 7311, 7359,
    7375, 7423, 7551, 7615, 7679, 7935, 12447, 12543, 12591, 12687, 40959, 42191, 42239, 42559,
    42655, 42751, 43007, 43055, 43135, 43231, 43263, 43311, 43359, 43391, 43487, 43519, 43615,
    43647, 43743, 43775, 43823, 43887, 43967, 44031, 55215, 64255, 65023, 65135, 65279, 65519
  };
  static final double[] WEIGHTS = {
    1.0, 1.0, 1.0, 1.0, 1.5, 1.5, 1.5, 1.5, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.5,
    1.8, 1.8, 1.0, 2.5, 3.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.8, 1.0, 1.0, 1.8, 1.8,
    1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.8, 1.0, 1.0, 1.8, 1.8, 1.0, 1.0, 1.0, 1.0, 2.2, 2.2, 3.0,
    2.5, 3.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.8, 1.0, 1.8, 1.8, 1.8, 1.8, 2.5, 1.8, 1.8, 1.8, 1.8,
    1.8, 1.8, 3.0, 1.0, 1.0, 1.8, 2.5, 3.0, 1.5, 1.0, 1.5, 1.0
  };

  static double weight(int c) {
    if (c == 32) return .2;
    if (c == 0x640) return 0;
    int type = Character.getType(c);
    if (type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK
        || type == Character.ENCLOSING_MARK) return 0;
    if ((type >= Character.DASH_PUNCTUATION && type <= Character.OTHER_PUNCTUATION)
        || (type >= Character.MATH_SYMBOL && type <= Character.OTHER_SYMBOL)
        || type == Character.INITIAL_QUOTE_PUNCTUATION
        || type == Character.FINAL_QUOTE_PUNCTUATION) return .5;
    if (Character.isSpaceChar(c)) return .2;
    if (type == Character.DECIMAL_DIGIT_NUMBER
        || type == Character.LETTER_NUMBER
        || type == Character.OTHER_NUMBER) return 3.5;
    for (int i = 0; i < LIMITS.length; i++) if (c <= LIMITS[i]) return WEIGHTS[i];
    return c > 0x20000 ? 3 : 1;
  }

  static double total(String s) {
    double w = 0;
    for (int i = 0; i < s.length(); ) {
      int c = s.codePointAt(i);
      w += weight(c);
      i += Character.charCount(c);
    }
    return w;
  }

  static double boost(double estimate) {
    return estimate < 50 ? 50 * Math.pow(estimate / 50, 1.0 / 3) : estimate;
  }

  static double frames(String text) {
    double e = total(text) * 25 / total("Nice to meet you.");
    return boost(e);
  }
}
