package com.voice.studio;

import java.io.*;
import java.text.Normalizer;
import java.util.*;

/** Native BPE tokenizer using the unmodified upstream vocabulary and merge order. */
final class NllbTokenizer {
    final String[] pieces;
    final Map<String, Integer> vocab = new HashMap<>();
    final SortedMap<String, Integer> languages = new TreeMap<>();
    final Map<Integer, String> replacements = new HashMap<>();
    final long[] keys = new long[1 << 20];
    final int[] ranks = new int[1 << 20];

    NllbTokenizer(File file) throws IOException {
        try (DataInputStream in =
                new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != 0x4e4c4231) throw new IOException("翻译分词器格式不符");
            int count = bounded(in.readInt(), 256204, 256204);
            pieces = new String[count];
            for (int i = 0; i < count; i++) {
                int id = bounded(in.readInt(), 0, count - 1);
                String piece = read(in);
                if (pieces[id] != null) throw new IOException("重复分词编号");
                pieces[id] = piece;
                vocab.put(piece, id);
            }
            int merges = bounded(in.readInt(), 0, 600000);
            for (int i = 0; i < merges; i++) {
                int left = bounded(in.readInt(), 0, count - 1),
                        right = bounded(in.readInt(), 0, count - 1);
                long key = ((long) left << 32) | right;
                int at = slot(key);
                while (ranks[at] != 0 && keys[at] != key) at = (at + 1) & (keys.length - 1);
                if (ranks[at] == 0) {
                    keys[at] = key;
                    ranks[at] = i + 1;
                }
            }
            int langs = bounded(in.readInt(), 1, 300);
            for (int i = 0; i < langs; i++)
                languages.put(read(in), bounded(in.readInt(), 0, count - 1));
            int replace = bounded(in.readInt(), 0, 100000);
            for (int i = 0; i < replace; i++) replacements.put(in.readInt(), read(in));
            if (in.read() != -1) throw new IOException("分词器有多余数据");
        }
    }

    static int bounded(int n, int min, int max) throws IOException {
        if (n < min || n > max) throw new IOException("分词器损坏");
        return n;
    }

    static String read(DataInputStream in) throws IOException {
        int n = bounded(in.readInt(), 0, 65536);
        byte[] bytes = new byte[n];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    int slot(long key) {
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        return (int) key & (keys.length - 1);
    }

    int rank(int left, int right) {
        long key = ((long) left << 32) | right;
        int at = slot(key);
        while (ranks[at] != 0) {
            if (keys[at] == key) return ranks[at] - 1;
            at = (at + 1) & (keys.length - 1);
        }
        return Integer.MAX_VALUE;
    }

    int language(String code) throws IOException {
        Integer id = languages.get(code);
        if (id == null) throw new IOException("翻译语言不受支持：" + code);
        return id;
    }

    long[] encode(String text, String language) throws IOException {
        StringBuilder mapped = new StringBuilder();
        for (int at = 0; at < text.length(); ) {
            int cp = text.codePointAt(at);
            at += Character.charCount(cp);
            String replace = replacements.get(cp);
            if (replace == null) mapped.appendCodePoint(cp);
            else mapped.append(replace);
        }
        String normalized =
                Normalizer.normalize(mapped, Normalizer.Form.NFKC).replaceAll(" {2,}", " ");
        String spaced = normalized.replace(' ', '▁');
        if (!spaced.startsWith("▁")) spaced = "▁" + spaced;
        List<Integer> tokens = new ArrayList<>();
        tokens.add(language(language));
        int begin = 0;
        for (int at = 1; at <= spaced.length(); at++)
            if (at == spaced.length() || spaced.charAt(at) == '▁') {
                bpe(spaced.substring(begin, at), tokens);
                begin = at;
            }
        tokens.add(2);
        long[] result = new long[tokens.size()];
        for (int i = 0; i < result.length; i++) result[i] = tokens.get(i);
        return result;
    }

    void bpe(String word, List<Integer> out) {
        List<Integer> ids = new ArrayList<>();
        List<String> raw = new ArrayList<>();
        for (int at = 0; at < word.length(); ) {
            int cp = word.codePointAt(at);
            at += Character.charCount(cp);
            String piece = new String(Character.toChars(cp));
            raw.add(piece);
            ids.add(vocab.getOrDefault(piece, 3));
        }
        while (ids.size() > 1) {
            int best = Integer.MAX_VALUE, where = -1;
            for (int i = 0; i < ids.size() - 1; i++) {
                if (ids.get(i) == 3 || ids.get(i + 1) == 3) continue;
                int rank = rank(ids.get(i), ids.get(i + 1));
                if (rank < best) {
                    best = rank;
                    where = i;
                }
            }
            if (where < 0) break;
            String joined = raw.get(where) + raw.get(where + 1);
            Integer id = vocab.get(joined);
            if (id == null) break;
            ids.set(where, id);
            raw.set(where, joined);
            ids.remove(where + 1);
            raw.remove(where + 1);
        }
        for (int id : ids) {
            if (id == 3 && !out.isEmpty() && out.get(out.size() - 1) == 3) continue;
            out.add(id);
        }
    }

    String decode(List<Integer> ids) {
        StringBuilder text = new StringBuilder();
        for (int id : ids)
            if (id > 3
                    && id < pieces.length
                    && !languages.containsValue(id)
                    && !pieces[id].equals("<mask>")) text.append(pieces[id]);
        return text.toString().replace('▁', ' ').trim();
    }
}
