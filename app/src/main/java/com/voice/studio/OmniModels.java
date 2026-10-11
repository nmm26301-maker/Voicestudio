package com.voice.studio;

import org.json.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import java.util.function.BooleanSupplier;

/** Strict, local-only model package import. Fixed filenames; SHA-256 before activation. */
final class OmniModels {
    interface InstallProgress { void update(long done, long total, String message); }
    static void checkCancel(BooleanSupplier cancelled) throws InterruptedIOException {
        if (cancelled.getAsBoolean()) throw new InterruptedIOException("已取消安装");
    }
    static void recoverImports(File root) {
        File[] files = root.listFiles();
        if (files != null) for (File file : files)
            if (file.isDirectory() && file.getName().matches("import-[a-f0-9-]{36}")) remove(file);
        for (String module : new String[]{"omni", "clone", "whisper", "translation", "separation", "speaker"}) {
            File target = new File(root, module + "-model"), backup = new File(root, module + "-model.backup");
            if (backup.exists() && !target.exists()) backup.renameTo(target);
            else if (backup.exists() && new File(target, "verified").isFile()) remove(backup);
        }
    }

    static synchronized void verify(File dir, JSONObject manifest, OmniEngine.Progress progress) throws Exception {
        try {
            JSONArray files = manifest.getJSONArray("files");
            for (int i = 0; i < files.length(); i++) {
                JSONObject entry = files.getJSONObject(i);
                File file = new File(dir, entry.getString("name"));
                if (!file.isFile() || file.length() != entry.getLong("size")) throw new IOException("模型缺少文件或大小不符，请重新导入");
                progress.update("检查文件 " + (i + 1) + " / " + files.length());
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] bytes = new byte[131072];
                    int count;
                    while ((count = in.read(bytes)) != -1) hash.update(bytes, 0, count);
                }
                StringBuilder hex = new StringBuilder();
                for (byte b : hash.digest()) hex.append(String.format(Locale.US, "%02x", b & 255));
                if (!hex.toString().equals(entry.getString("sha256"))) throw new IOException("模型内容损坏，请重新导入");
            }
            try (OutputStream out = new FileOutputStream(new File(dir, "verified"))) { out.write(1); }
        } catch (Exception e) {
            new File(dir, "verified").delete();
            throw e;
        }
    }

    static boolean ready(File dir, JSONObject manifest) {
        try {
            if (!new File(dir, "verified").isFile()) return false;
            JSONArray list = manifest.getJSONArray("files");
            for (int i = 0; i < list.length(); i++) {
                JSONObject f = list.getJSONObject(i);
                File p = new File(dir, f.getString("name"));
                if (!p.isFile() || p.length() != f.getLong("size")) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static synchronized void install(
            InputStream source, File dir, JSONObject manifest, OmniEngine.Progress progress)
            throws Exception {
        install(source, dir, manifest, (done, total, message) -> progress.update(message), () -> false);
    }

    static synchronized void install(InputStream source, File dir, JSONObject manifest,
            InstallProgress progress, BooleanSupplier cancelled) throws Exception {
        checkCancel(cancelled);
        if (ready(dir, manifest)) throw new IOException("模型已存在，无需重复导入");
        File staging = new File(dir.getParentFile(), "import-" + UUID.randomUUID());
        if (!staging.mkdirs()) throw new IOException("无法创建模型目录");
        Map<String, JSONObject> expected = new HashMap<>();
        JSONArray files = manifest.getJSONArray("files");
        for (int i = 0; i < files.length(); i++) {
            JSONObject f = files.getJSONObject(i);
            expected.put(f.getString("name"), f);
        }
        Set<String> found = new HashSet<>();
        long done = 0, last = 0, total = 0;
        for (int i = 0; i < files.length(); i++) total += files.getJSONObject(i).getLong("size");
        if (staging.getUsableSpace() < total + 64 * 1024 * 1024L) {
            remove(staging);
            throw new IOException("手机空间不足，请至少预留 " + ((total + 64 * 1024 * 1024L) / 1024 / 1024) + " MiB");
        }
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(source))) {
            progress.update(0, total, "正在读取模型包");
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                checkCancel(cancelled);
                String name = e.getName();
                if (e.isDirectory()) throw new IOException("模型包目录结构不符");
                JSONObject f = expected.get(name);
                if (f == null) {
                    if (name.equals("NOTICE.txt") || name.equals("README.txt")) {
                        byte[] b = new byte[8192];
                        long n = 0;
                        int len;
                        while ((len = zip.read(b)) != -1) {
                            checkCancel(cancelled);
                            n += len;
                            if (n > 65536) throw new IOException("模型说明文件过大");
                        }
                        continue;
                    }
                    throw new IOException("模型包包含未知文件");
                }
                if (!found.add(name)) throw new IOException("模型包包含重复文件");
                progress.update(done, total, "安装文件 " + found.size() + " / " + expected.size() + "：" + name);
                long size = f.getLong("size"), count = 0;
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                try (OutputStream out =
                        new BufferedOutputStream(new FileOutputStream(new File(staging, name)))) {
                    byte[] b = new byte[131072];
                    int n;
                    while ((n = zip.read(b)) != -1) {
                        checkCancel(cancelled);
                        count += n;
                        if (count > size) throw new IOException("模型文件大小不符");
                        out.write(b, 0, n);
                        hash.update(b, 0, n);
                        done += n;
                        if (done - last >= 8 * 1024 * 1024) {
                            last = done;
                            progress.update(done, total,
                                    "正在导入 · "
                                            + (done / 1024 / 1024)
                                            + " / "
                                            + (total / 1024 / 1024)
                                            + " MiB");
                        }
                    }
                }
                StringBuilder hex = new StringBuilder();
                for (byte b : hash.digest()) hex.append(String.format(Locale.US, "%02x", b & 255));
                if (count != size || !hex.toString().equals(f.getString("sha256")))
                    throw new IOException("校验失败：" + name + "。请重新下载对应模块的完整模型包");
                progress.update(done, total, "文件校验通过 " + found.size() + " / " + expected.size());
            }
            if (found.size() != expected.size()) throw new IOException("模型包缺少必要文件");
            progress.update(done, total, "准备启用模型");
            checkCancel(cancelled);
            try (OutputStream out = new FileOutputStream(new File(staging, "verified"))) {
                out.write(1);
            }
            File backup = new File(dir.getParentFile(), dir.getName() + ".backup");
            if (backup.exists()) throw new IOException("存在待恢复的模型备份，请重新打开应用后导入");
            boolean hadPrevious = dir.exists();
            if (hadPrevious && !dir.renameTo(backup)) throw new IOException("无法替换损坏模型，请重试");
            if (!staging.renameTo(dir)) {
                if (hadPrevious && !backup.renameTo(dir))
                    throw new IOException("模型激活失败，原文件保留于 " + backup.getName());
                throw new IOException("无法保存模型");
            }
            remove(backup);
        } finally {
            remove(staging);
        }
    }

    static void remove(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) for (File c : files) remove(c);
        }
        f.delete();
    }
}
