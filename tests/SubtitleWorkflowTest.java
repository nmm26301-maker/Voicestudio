package com.voice.studio;

import android.content.Context;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

public final class SubtitleWorkflowTest {
    static int checks;
    static void ok(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    static JSONArray segments(String text) throws Exception {
        return new JSONArray().put(new JSONObject().put("start", 0).put("end", 1).put("text", text));
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("subtitle-workflow").toFile();
        Context context = new Context() { public File getFilesDir() { return root; } };
        String id = "11111111-1111-4111-8111-111111111111";
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            JSONArray unsorted = segments("第一句");
            unsorted.getJSONObject(0).put("start", 2).put("end", 3).put("speaker", 2).put("profile", "voice");
            unsorted.put(segments("你好😀\n第二行").getJSONObject(0));
            JSONArray normalized = SubtitleDocument.normalize(unsorted);
            ok(normalized.getJSONObject(0).getDouble("start") == 0);
            ok(unsorted.getJSONObject(0).getDouble("start") == 2);
            ok(normalized.getJSONObject(1).getInt("speaker") == 2);
            ok(normalized.getJSONObject(1).getString("profile").equals("voice"));
            ok(SubtitleDocument.srt(normalized).contains("00:00:00,000 --> 00:00:01,000"));
            ok(SubtitleDocument.srt(normalized).contains("你好😀\n第二行"));
            ok(SubtitleDocument.plain(normalized).equals("你好😀\n第二行\n第一句\n"));
            ok(SubtitleDocument.stamp(3599.9996).equals("01:00:00,000"));
            JSONObject original = new JSONObject().put("id", id).put("state", "done").put("title", "保留标题")
                    .put("revision", 10).put("segments", normalized).put("audioSeconds", 4).put("custom", "keep");
            StudioFiles.save(StudioFiles.job(context,id), original);
            JSONObject saved = StudioFiles.replaceSegments(context,id,10,segments("修改成功"));
            ok(saved.optLong("revision") > 10); ok(saved.getString("custom").equals("keep"));
            ok(saved.getString("title").equals("保留标题")); ok(!saved.has("subtitleExportsPending"));
            ok(StudioFiles.text(StudioFiles.output(context,id,"txt")).equals("修改成功\n"));
            ok(new JSONArray(StudioFiles.text(StudioFiles.output(context,id,"json"))).getJSONObject(0).getString("text").equals("修改成功"));
            try { StudioFiles.replaceSegments(context,id,10,segments("旧对话框")); throw new AssertionError(); }
            catch (IOException expected) { checks++; }
            ok(StudioFiles.read(StudioFiles.job(context,id)).getJSONArray("segments").getJSONObject(0).getString("text").equals("修改成功"));
            long revision = saved.getLong("revision");
            JSONArray overlap = segments("A"); overlap.put(new JSONObject().put("start",.5).put("end",2).put("text","B"));
            for (JSONArray invalid : new JSONArray[]{overlap,segments("负时间"),segments("零长度"),segments("超时长"),segments("非有限数")}) {
                if (invalid != overlap) {
                    JSONObject item = invalid.getJSONObject(0);
                    String label = item.getString("text");
                    if (label.equals("负时间")) item.put("start",-1);
                    if (label.equals("零长度")) item.put("end",0);
                    if (label.equals("超时长")) item.put("end",5);
                    if (label.equals("非有限数")) item.put("end","Infinity");
                }
                try { StudioFiles.replaceSegments(context,id,revision,invalid); throw new AssertionError(); }
                catch (Exception expected) { checks++; }
            }
            ok(StudioFiles.read(StudioFiles.job(context,id)).getLong("revision") == revision);
            File blocked = new File(StudioFiles.output(context,id,"srt").getPath()+".new"); blocked.mkdir();
            try { StudioFiles.replaceSegments(context,id,revision,segments("导出故障保留")); throw new AssertionError(); }
            catch (IOException expected) { ok(expected.getMessage().contains("字幕已保存")); }
            JSONObject pending = StudioFiles.read(StudioFiles.job(context,id));
            ok(pending.getBoolean("subtitleExportsPending"));
            ok(pending.getJSONArray("segments").getJSONObject(0).getString("text").equals("导出故障保留"));
            blocked.delete(); StudioFiles.recoverInterrupted(context);
            JSONObject repaired = StudioFiles.read(StudioFiles.job(context,id));
            ok(!repaired.has("subtitleExportsPending"));
            ok(StudioFiles.text(StudioFiles.output(context,id,"srt")).contains("导出故障保留"));
            ok(repaired.getString("state").equals("done"));
            long simultaneous = repaired.getLong("revision");
            CountDownLatch start = new CountDownLatch(1); AtomicInteger wins = new AtomicInteger(), conflicts = new AtomicInteger();
            Future<?>[] writers = new Future<?>[2];
            for (int n = 0; n < 2; n++) {
                final String value = "并发"+n;
                writers[n] = pool.submit(() -> {
                    try { start.await(); StudioFiles.replaceSegments(context,id,simultaneous,segments(value)); wins.incrementAndGet(); }
                    catch (IOException expected) { conflicts.incrementAndGet(); }
                    catch (Exception failure) { throw new RuntimeException(failure); }
                });
            }
            start.countDown(); for (Future<?> writer : writers) writer.get(10,TimeUnit.SECONDS);
            ok(wins.get()==1); ok(conflicts.get()==1);
            JSONObject current = StudioFiles.read(StudioFiles.job(context,id));
            ok(StudioFiles.text(StudioFiles.output(context,id,"txt")).equals(SubtitleDocument.plain(current.getJSONArray("segments"))));
            ok(current.getLong("revision") > simultaneous);
            System.out.println(checks + " assertions passed: subtitle ordering/Unicode, stale edit conflict, export repair and concurrent commit (host adapters)");
        } finally { pool.shutdownNow(); OmniModels.remove(root); }
    }
}
