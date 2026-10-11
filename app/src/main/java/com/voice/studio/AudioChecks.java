package com.voice.studio;

import java.io.*;
import java.util.Locale;

/** Pure Java checks shared by downloads and local synthesis. Decoder validation follows. */
final class AudioChecks {
  static void validate(File file, String format) throws IOException {
    if (!file.isFile() || file.length() < 32) throw new IOException("没有生成有效音频，请重试");
    byte[] h = new byte[12];
    try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
      in.readFully(h);
    }
    boolean valid;
    if ("wav".equals(format)) {
      valid =
          h[0] == 'R'
              && h[1] == 'I'
              && h[2] == 'F'
              && h[3] == 'F'
              && h[8] == 'W'
              && h[9] == 'A'
              && h[10] == 'V'
              && h[11] == 'E';
    } else {
      int a = h[1] & 255, b = h[2] & 255;
      valid =
          (h[0] == 'I' && h[1] == 'D' && h[2] == '3')
              || ((h[0] & 255) == 255
                  && (a & 224) == 224
                  && (a & 24) != 8
                  && (a & 6) != 0
                  && (b & 240) != 0
                  && (b & 240) != 240
                  && (b & 12) != 12);
    }
    if (!valid) throw new IOException("返回内容不是有效的音频，请检查服务与语音引擎");
  }

  static String shortTitle(String text) {
    String value = text.replaceAll("\\s+", " ").trim();
    int count = value.codePointCount(0, value.length());
    return count > 26 ? value.substring(0, value.offsetByCodePoints(0, 26)) + "…" : value;
  }

  static String exportName(String title) {
    String value = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
    if (value.isEmpty()) value = "VoiceStudio";
    int count = value.codePointCount(0, value.length());
    return count > 60 ? value.substring(0, value.offsetByCodePoints(0, 60)) : value;
  }

  static boolean localeMatches(Locale voice, Locale selected) {
    if (!voice.getLanguage().equals(selected.getLanguage())) return false;
    return selected.getCountry().isEmpty() || selected.getCountry().equals(voice.getCountry());
  }
}
