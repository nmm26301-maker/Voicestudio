package com.voice.studio;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Orders numbered volumes and opens only the volume currently being read. */
final class ModelParts {
    interface Source {
        String name() throws Exception;
        InputStream open() throws Exception;
    }

    private static final Pattern PART = Pattern.compile("^(.+)\\.part(\\d+)(?:\\.bin)?$", Pattern.CASE_INSENSITIVE);

    static InputStream open(List<Source> sources) throws Exception {
        if (sources.isEmpty()) throw new IOException("请选择模型 ZIP 或该模块的全部分包");
        TreeMap<Integer, Source> ordered = new TreeMap<>();
        String base = null;
        for (Source source : sources) {
            String name = source.name();
            if (name == null || name.trim().isEmpty()) throw new IOException("无法读取文件名，请使用手机文件管理器选择");
            name = name.replaceAll("\\s*\\(\\d+\\)", "").trim();
            Matcher match = PART.matcher(name);
            if (!match.matches()) {
                if (sources.size() != 1 || name.toLowerCase(Locale.ROOT).contains(".part"))
                    throw new IOException("请选择同一模块的全部分包，或只选择一个完整 ZIP");
                return new PartsStream(Collections.singletonList(source));
            }
            String prefix = match.group(1).toLowerCase(Locale.ROOT);
            if (base == null) base = prefix;
            if (!base.equals(prefix)) throw new IOException("选中了不同模块的分包，请分开导入");
            int number;
            try { number = Integer.parseInt(match.group(2)); }
            catch (NumberFormatException e) { throw new IOException("分包编号无效"); }
            if (number < 1 || ordered.put(number, source) != null)
                throw new IOException("分包编号重复或无效，请每份只选一次");
        }
        if (ordered.size() < 2) throw new IOException("请同时选择该模块的全部分包");
        int next = 1;
        for (int number : ordered.keySet())
            if (number != next++) throw new IOException("缺少 part" + String.format(Locale.US, "%02d", next - 1) + "，请重新选择全部分包");
        return new PartsStream(new ArrayList<>(ordered.values()));
    }

    private static final class PartsStream extends InputStream {
        final List<Source> sources;
        int index;
        InputStream current;
        boolean closed;
        PartsStream(List<Source> values) { sources = values; }
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 255;
        }
        public int read(byte[] b, int offset, int length) throws IOException {
            if (closed) throw new IOException("模型文件已关闭");
            if (b == null) throw new NullPointerException();
            if (offset < 0 || length < 0 || offset > b.length - length) throw new IndexOutOfBoundsException();
            if (length == 0) return 0;
            while (true) {
                if (current == null) {
                    if (index == sources.size()) return -1;
                    try { current = sources.get(index++).open(); }
                    catch (Exception e) { throw new IOException("无法打开模型分包：" + e.getMessage(), e); }
                    if (current == null) throw new IOException("无法读取模型分包");
                }
                int count = current.read(b, offset, length);
                if (count >= 0) return count;
                current.close();
                current = null;
            }
        }
        public void close() throws IOException {
            closed = true;
            if (current != null) { try { current.close(); } finally { current = null; } }
        }
    }
}
