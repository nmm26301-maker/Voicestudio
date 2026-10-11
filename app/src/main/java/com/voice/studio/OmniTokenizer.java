package com.voice.studio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import org.json.*;

/** Qwen byte-level BPE. The model tokenizer.json is the source of truth. */
final class OmniTokenizer {
  private final Map<String, Integer> vocab = new HashMap<>(), ranks = new HashMap<>();
  private final Map<String, Integer> special = new HashMap<>();
  private final String[] bytes = new String[256];
  private final Pattern words = OmniWordPattern.compile();
  private final Pattern specials;

  OmniTokenizer(File file) throws Exception {
    JSONObject json = new JSONObject(new String(read(file), StandardCharsets.UTF_8));
    JSONObject model = json.getJSONObject("model"), v = model.getJSONObject("vocab");
    Iterator<String> keys = v.keys();
    while (keys.hasNext()) {
      String k = keys.next();
      vocab.put(k, v.getInt(k));
    }
    JSONArray merges = model.getJSONArray("merges");
    for (int i = 0; i < merges.length(); i++) {
      JSONArray pair = merges.getJSONArray(i);
      ranks.put(pair.getString(0) + "\u0000" + pair.getString(1), i);
    }
    JSONArray added = json.getJSONArray("added_tokens");
    List<String> patterns = new ArrayList<>();
    for (int i = 0; i < added.length(); i++) {
      JSONObject t = added.getJSONObject(i);
      special.put(t.getString("content"), t.getInt("id"));
      patterns.add(Pattern.quote(t.getString("content")));
    }
    specials = Pattern.compile(String.join("|", patterns));
    int extra = 256;
    for (int i = 0; i < 256; i++) {
      int c = ((i >= 33 && i <= 126) || (i >= 161 && i <= 172) || i >= 174) ? i : extra++;
      bytes[i] = new String(Character.toChars(c));
    }
  }

  long[] encode(String input) {
    String s = Normalizer.normalize(input, Normalizer.Form.NFC);
    List<Long> ids = new ArrayList<>();
    Matcher m = specials.matcher(s);
    int end = 0;
    while (m.find()) {
      ordinary(s.substring(end, m.start()), ids);
      ids.add((long) special.get(m.group()));
      end = m.end();
    }
    ordinary(s.substring(end), ids);
    long[] out = new long[ids.size()];
    for (int i = 0; i < out.length; i++) out[i] = ids.get(i);
    return out;
  }

  private void ordinary(String s, List<Long> out) {
    Matcher m = words.matcher(s);
    while (m.find()) {
      byte[] raw = m.group().getBytes(StandardCharsets.UTF_8);
      List<String> parts = new ArrayList<>();
      for (byte b : raw) parts.add(bytes[b & 255]);
      while (parts.size() > 1) {
        int best = Integer.MAX_VALUE, at = -1;
        for (int i = 0; i < parts.size() - 1; i++) {
          Integer r = ranks.get(parts.get(i) + "\u0000" + parts.get(i + 1));
          if (r != null && r < best) {
            best = r;
            at = i;
          }
        }
        if (at < 0) break;
        String left = parts.get(at), right = parts.get(at + 1);
        for (int i = 0; i < parts.size() - 1; ) {
          if (parts.get(i).equals(left) && parts.get(i + 1).equals(right)) {
            parts.set(i, left + right);
            parts.remove(i + 1);
          } else i++;
        }
      }
      for (String p : parts) {
        Integer id = vocab.get(p);
        if (id == null) throw new IllegalArgumentException("Tokenizer vocabulary mismatch");
        out.add((long) id);
      }
    }
  }

  static byte[] read(File f) throws IOException {
    try (InputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[32768];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
      return out.toByteArray();
    }
  }
}
