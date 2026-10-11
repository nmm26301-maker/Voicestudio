package com.voice.studio;

import android.text.Html;

import org.w3c.dom.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.xml.parsers.*;

final class BookReader {
    static List<String> chapters(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            if (in.read() == 'P' && in.read() == 'K') return epub(file);
        }
        String text = StudioFiles.text(file);
        if (text.length() > 1000000) throw new IOException("书籍超过 100 万字，请分卷");
        List<String> chapters = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\\r?\\n")) {
            if (line.trim().matches("^(第.{1,20}[章节回卷]|Chapter\\s+\\d+|CHAPTER\\s+\\d+).*")
                    && current.length() > 0) {
                append(chapters, current.toString());
                current.setLength(0);
            }
            current.append(line).append('\n');
            if (current.length() > 12000) {
                append(chapters, current.toString());
                current.setLength(0);
            }
        }
        append(chapters, current.toString());
        if (chapters.isEmpty()) throw new IOException("没有可朗读文字");
        return chapters;
    }

    static void append(List<String> chapters, String text) {
        text = text.trim();
        while (!text.isEmpty()) {
            int take = Math.min(12000, text.length());
            if (take < text.length() && Character.isHighSurrogate(text.charAt(take - 1))) take--;
            chapters.add(text.substring(0, take));
            text = text.substring(take).trim();
        }
    }

    static List<String> epub(File file) throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            Document container = xml(read(zip, "META-INF/container.xml"));
            NodeList roots = container.getElementsByTagNameNS("*", "rootfile");
            if (roots.getLength() == 0) throw new IOException("EPUB 容器不完整");
            String opf = ((Element) roots.item(0)).getAttribute("full-path");
            Document packageDoc = xml(read(zip, opf));
            String base = opf.contains("/") ? opf.substring(0, opf.lastIndexOf('/') + 1) : "";
            Map<String, String> paths = new HashMap<>();
            NodeList items = packageDoc.getElementsByTagNameNS("*", "item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                paths.put(item.getAttribute("id"), resolve(base, item.getAttribute("href")));
            }
            List<String> chapters = new ArrayList<>();
            NodeList spine = packageDoc.getElementsByTagNameNS("*", "itemref");
            if (spine.getLength() > 1000) throw new IOException("章节过多，请分卷");
            int total = 0;
            for (int i = 0; i < spine.getLength(); i++) {
                String path = paths.get(((Element) spine.item(i)).getAttribute("idref"));
                if (path == null) continue;
                String html = new String(read(zip, path), "UTF-8");
                String text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                total += text.length();
                if (total > 1000000) throw new IOException("书籍过大，请分卷");
                append(chapters, text);
            }
            if (chapters.isEmpty()) throw new IOException("没有正文；不支持加密 EPUB");
            return chapters;
        }
    }

    static String resolve(String base, String href) throws IOException {
        try {
            int fragment = href.indexOf('#');
            if (fragment >= 0) href = href.substring(0, fragment);
            String decoded = java.net.URLDecoder.decode(href.replace("+", "%2B"), "UTF-8");
            if (decoded.startsWith("/")
                    || decoded.contains(":")
                    || decoded.contains("\\")
                    || decoded.indexOf(0) >= 0) throw new IOException("EPUB 路径无效");
            Deque<String> parts = new ArrayDeque<>();
            for (String piece : (base + decoded).split("/")) {
                if (piece.isEmpty() || piece.equals(".")) continue;
                if (piece.equals("..")) {
                    if (parts.isEmpty()) throw new IOException("EPUB 路径超出容器");
                    parts.removeLast();
                } else parts.addLast(piece);
            }
            return String.join("/", parts);
        } catch (IllegalArgumentException e) {
            throw new IOException("EPUB 路径编码无效", e);
        }
    }

    static byte[] read(ZipFile zip, String path) throws IOException {
        ZipEntry e = zip.getEntry(path);
        if (e == null) throw new IOException("EPUB 缺少文件");
        try (InputStream in = zip.getInputStream(e);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) >= 0) {
                if (out.size() + n > 10000000) throw new IOException("EPUB 章节过大");
                out.write(b, 0, n);
            }
            return out.toByteArray();
        }
    }

    static Document xml(byte[] bytes) throws Exception {
        String text = new String(bytes, "UTF-8"), upper = text.toUpperCase(Locale.ROOT);
        if (text.indexOf('\u0000') >= 0
                || upper.contains("<!DOCTYPE")
                || upper.contains("<!ENTITY")) throw new IOException("EPUB XML 声明不支持");
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setExpandEntityReferences(false);
        DocumentBuilder b = f.newDocumentBuilder();
        b.setEntityResolver(
                (p, s) -> {
                    throw new org.xml.sax.SAXException("不支持外部 XML");
                });
        return b.parse(new org.xml.sax.InputSource(new StringReader(text)));
    }
}
