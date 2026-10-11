package com.voice.studio;

import android.content.*;
import android.util.AtomicFile;

import org.json.*;

import java.io.*;
import java.util.*;

final class StudioFiles {
    private static boolean modelWork;
    static volatile long historyRevision;

    static synchronized boolean beginModelWork(Context c) {
        if (modelWork || jobsPending(c)) return false;
        modelWork = true;
        return true;
    }

    static synchronized void endModelWork() { modelWork = false; }
    static synchronized boolean modelWork() { return modelWork; }

    static synchronized void recoverInterrupted(Context context) {
        JSONArray jobs = list(context);
        for (int n = 0; n < jobs.length(); n++) {
            JSONObject job = jobs.optJSONObject(n);
            String state = job.optString("state");
            if (state.equals("queued") || state.equals("running")) {
                try {
                    job.put("state", "paused")
                            .put("progress", "上次进程已结束，点击继续可恢复任务")
                            .put("interrupted", true);
                    save(job(context, job.getString("id")), job);
                } catch (Exception ignored) { }
            }
            if (job.optBoolean("subtitleExportsPending"))
                try { ensureSubtitles(context, job.getString("id")); } catch (Exception ignored) { }
        }
    }

    static File dir(Context c, String name) {
        File d = new File(c.getFilesDir(), name);
        d.mkdirs();
        return d;
    }

    static File job(Context c, String id) throws IOException {
        if (!id.matches("[a-f0-9-]{36}")) throw new IOException("任务编号无效");
        return new File(dir(c, "jobs"), id + ".json");
    }

    static File output(Context c, String id, String ext) {
        return new File(dir(c, "audio"), id + "." + ext);
    }

    static synchronized String text(File f) throws IOException {
        try (InputStream in = new AtomicFile(f).openRead();
                ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            byte[] x = new byte[8192];
            int n;
            while ((n = in.read(x)) >= 0) {
                if (b.size() + n > 10000000) throw new IOException("文本过大");
                b.write(x, 0, n);
            }
            return b.toString("UTF-8");
        }
    }

    static JSONObject read(File f) throws Exception {
        return new JSONObject(text(f));
    }

    static synchronized void save(File f, JSONObject json) throws Exception {
        AtomicFile a = new AtomicFile(f);
        FileOutputStream out = a.startWrite();
        try {
            out.write(json.toString().getBytes("UTF-8"));
            a.finishWrite(out);
        } catch (Exception e) {
            a.failWrite(out);
            throw e;
        }
    }

    static synchronized void writeText(File file, String text) throws IOException {
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream out = atomic.startWrite();
        try {
            out.write(text.getBytes("UTF-8"));
            atomic.finishWrite(out);
            if (file.getName().equals("history.json")) historyRevision++;
        } catch (IOException e) {
            atomic.failWrite(out);
            throw e;
        }
    }

    static boolean pending(Context c) {
        return modelWork() || jobsPending(c);
    }

    static boolean jobsPending(Context c) {
        if (!StudioService.active.isEmpty()) return true;
        JSONArray jobs = list(c);
        for (int i = 0; i < jobs.length(); i++) {
            String state = jobs.optJSONObject(i).optString("state");
            if (state.equals("queued") || state.equals("running")) return true;
        }
        return false;
    }

    static JSONArray list(Context c) {
        List<JSONObject> all = new ArrayList<>();
        File[] files = dir(c, "jobs").listFiles();
        Set<String> names = new HashSet<>();
        if (files != null)
            for (File entry : files) {
                String name = entry.getName();
                if (name.endsWith(".json.bak")) name = name.substring(0, name.length() - 4);
                if (name.endsWith(".json") && names.add(name))
                    try {
                        all.add(read(new File(dir(c, "jobs"), name)));
                    } catch (Exception ignored) {
                    }
            }
        all.sort((a, b) -> Long.compare(b.optLong("created"), a.optLong("created")));
        JSONArray j = new JSONArray();
        for (JSONObject x : all) j.put(x);
        return j;
    }

    static JSONObject manifest(Context c, String name) throws Exception {
        try (InputStream in = c.getAssets().open(name + "-model-manifest.json");
                ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            byte[] x = new byte[8192];
            int n;
            while ((n = in.read(x)) >= 0) b.write(x, 0, n);
            return new JSONObject(b.toString("UTF-8"));
        }
    }

    static boolean ready(Context c, String name) {
        try {
            return OmniModels.ready(new File(c.getFilesDir(), name + "-model"), manifest(c, name));
        } catch (Exception e) {
            return false;
        }
    }

    static VoicePrompt voice(Context c, String name) throws IOException {
        if (name.isEmpty()) return null;
        if (!name.matches("[a-f0-9-]{36}")) throw new IOException("音色编号无效");
        return VoicePrompt.load(new File(dir(c, "profiles"), name + ".vpc"));
    }

    static synchronized JSONObject replaceSegments(Context c, String id, long expectedRevision, JSONArray segments) throws Exception {
        JSONObject current = read(job(c, id));
        if (current.optLong("revision") != expectedRevision)
            throw new IOException("字幕已在其他操作中改变，请重新打开最新字幕再编辑");
        JSONArray normalized = SubtitleDocument.normalize(segments);
        double duration = current.optDouble("audioSeconds", 0);
        if (duration > 0 && normalized.length() > 0
                && normalized.getJSONObject(normalized.length() - 1).getDouble("end") > duration + .01)
            throw new IOException("字幕时间超出原音轨，请缩短结束时间");
        current.put("segments", normalized).put("revision", Math.max(System.currentTimeMillis(), expectedRevision + 1))
                .put("subtitleExportsPending", true);
        save(job(c, id), current);
        try { return ensureSubtitles(c, id); }
        catch (Exception failure) { throw new IOException("字幕已保存，但导出文件未同步，请重新打开字幕结果", failure); }
    }

    static synchronized JSONObject ensureSubtitles(Context c, String id) throws Exception {
        JSONObject current = read(job(c, id));
        if (!current.optBoolean("subtitleExportsPending")) return current;
        JSONArray segments = current.getJSONArray("segments");
        writeText(output(c, id, "txt"), SubtitleDocument.plain(segments));
        writeText(output(c, id, "srt"), SubtitleDocument.srt(segments));
        writeText(output(c, id, "json"), segments.toString());
        current.remove("subtitleExportsPending"); save(job(c, id), current);
        return current;
    }

    static synchronized void deleteJob(Context context, String id) throws Exception {
        if (pending(context)) throw new IOException("请先暂停并等待整个队列停止，再删除任务");
        File record = job(context, id);
        JSONArray jobs = list(context);
        for (int n = 0; n < jobs.length(); n++) {
            JSONObject item = jobs.getJSONObject(n);
            if (!item.optString("id").equals(id)
                    && (item.optString("sourceJob").equals(id)
                            || item.optString("groupedSource").equals(id)))
                throw new IOException("有其他分组或配音任务引用此任务，请先删除关联任务");
        }
        JSONArray old = historyList(context), next = new JSONArray();
        for (int n = 0; n < old.length(); n++)
            if (!old.getJSONObject(n).optString("id").equals(id)) next.put(old.getJSONObject(n));
        File[] results = dir(context, "audio").listFiles();
        if (results != null)
            for (File file : results)
                if (file.getName().startsWith(id + ".") || file.getName().startsWith(id + "-a."))
                    if (!file.delete()) throw new IOException("部分结果删除失败，请重试");
        OmniModels.remove(new File(dir(context, "jobs"), id));
        if (!record.delete() && record.exists()) throw new IOException("任务删除失败");
        writeText(new File(context.getFilesDir(), "history.json"), next.toString());
    }

    static synchronized JSONArray historyList(Context c) throws Exception {
        File file = new File(c.getFilesDir(), "history.json");
        if (!file.isFile() && !new File(file.getPath() + ".bak").isFile()) return new JSONArray();
        return new JSONArray(text(file));
    }

    static synchronized void editHistory(Context c, String id, String property, Object value) throws Exception {
        if (!property.equals("title") && !property.equals("favorite") && !property.equals("deletedAt"))
            throw new IOException("作品属性无效");
        JSONArray all = historyList(c);
        boolean found = false;
        for (int n = 0; n < all.length(); n++) {
            JSONObject item = all.getJSONObject(n);
            if (item.optString("id").equals(id)) { item.put(property, value); found = true; break; }
        }
        if (!found) throw new IOException("作品已移除，请刷新列表");
        writeText(new File(c.getFilesDir(), "history.json"), all.toString());
    }

    static synchronized void history(Context c, JSONObject job) throws Exception {
        JSONArray old = historyList(c), next = new JSONArray();
        JSONObject fresh = new JSONObject(job.toString());
        for (int i = 0; i < old.length(); i++) {
            JSONObject previous = old.getJSONObject(i);
            if (previous.optString("id").equals(fresh.optString("id"))) {
                for (String property : new String[]{"favorite", "title", "deletedAt"})
                    if (previous.has(property)) fresh.put(property, previous.get(property));
            }
        }
        next.put(fresh);
        for (int i = 0; i < old.length(); i++)
            if (!old.getJSONObject(i).optString("id").equals(fresh.optString("id"))) next.put(old.getJSONObject(i));
        writeText(new File(c.getFilesDir(), "history.json"), next.toString());
    }
}
