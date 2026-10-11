package com.voice.studio;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;
import org.json.*;

public class ModelImportCancellationTest {
    static int assertions;
    static void check(boolean ok) { assertions++; if (!ok) throw new AssertionError(); }
    static JSONObject manifest(byte[] payload) throws Exception {
        StringBuilder hash = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(payload)) hash.append(String.format(Locale.US, "%02x", b & 255));
        return new JSONObject().put("files", new JSONArray().put(new JSONObject()
                .put("name", "model.onnx").put("size", payload.length).put("sha256", hash.toString())));
    }
    static byte[] zip(byte[] data) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) { zip.putNextEntry(new ZipEntry("model.onnx")); zip.write(data); zip.closeEntry(); }
        return bytes.toByteArray();
    }
    static void cancelled(File root, byte[] packed, JSONObject manifest, String stage) throws Exception {
        File existing = new File(root, "existing"); existing.mkdir();
        byte[] prior = "old damaged model stays until successful activation".getBytes("UTF-8");
        Files.write(new File(existing, "model.onnx").toPath(), prior);
        AtomicBoolean stop = new AtomicBoolean(stage.equals("before")); boolean stopped = false;
        try {
            OmniModels.install(new ByteArrayInputStream(packed), existing, manifest, (done, total, message) -> {
                if (stage.equals("during") && done > 0 || stage.equals("before-activation") && message.equals("准备启用模型")) stop.set(true);
            }, stop::get);
        } catch (InterruptedIOException e) { stopped = true; }
        check(stopped); check(!new File(existing, "verified").exists());
        check(Arrays.equals(prior, Files.readAllBytes(new File(existing, "model.onnx").toPath())));
        check(root.listFiles().length == 1); check(!new File(root, "existing.backup").exists());
        OmniModels.remove(existing);
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("import-cancellation").toFile();
        byte[] payload = new byte[9 * 1024 * 1024]; Arrays.fill(payload, (byte)13);
        JSONObject manifest = manifest(payload); byte[] packed = zip(payload);
        try {
            cancelled(root, packed, manifest, "before");
            cancelled(root, packed, manifest, "during");
            cancelled(root, packed, manifest, "before-activation");
            File target = new File(root, "complete");
            long[] observed = {0, 0}; List<String> messages = new ArrayList<>();
            OmniModels.install(new ByteArrayInputStream(packed), target, manifest, (done, total, message) -> {
                check(done >= observed[0]); check(done <= total); check(total == payload.length);
                observed[0] = done; observed[1] = total; messages.add(message);
            }, () -> false);
            check(OmniModels.ready(target, manifest)); check(observed[0] == observed[1]);
            check(messages.contains("准备启用模型")); check(root.listFiles().length == 1);
            // A retry immediately after cancellation uses the same manifest and clean target.
            File retry = new File(root, "retry");
            OmniModels.install(new ByteArrayInputStream(packed), retry, manifest, message -> {});
            check(OmniModels.ready(retry, manifest));
            System.out.println(assertions + " cancellation/progress assertions passed: before extraction, during extraction, before activation, old-file preservation and retry.");
        } finally { OmniModels.remove(root); }
    }
}
