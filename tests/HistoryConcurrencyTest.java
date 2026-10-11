package com.voice.studio;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class HistoryConcurrencyTest {
    static int checks;
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    static String id(int n) { return String.format(Locale.US, "%08d-1111-4111-8111-111111111111", n); }
    static JSONObject work(int n) throws Exception { return new JSONObject().put("id", id(n)).put("file", id(n)+".wav").put("title", "作品"+n).put("created", n); }
    static JSONObject lookup(JSONArray rows, String id) throws Exception {
        for(int n=0;n<rows.length();n++) if(rows.getJSONObject(n).optString("id").equals(id)) return rows.getJSONObject(n);
        throw new AssertionError("Missing work: "+id);
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("studio-history").toFile();
        Context context = new Context() { public File getFilesDir() { return root; } };
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            JSONObject stale = work(1);
            StudioFiles.history(context, stale);
            StudioFiles.history(context, work(2));
            StudioFiles.editHistory(context, id(1), "title", "已重命名");
            JSONArray records = StudioFiles.historyList(context);
            check(records.length()==2, "A stale foreground edit must preserve a new background work");
            check(lookup(records,id(2)).getString("title").equals("作品2"), "Unrelated new work unchanged");
            File audio = StudioFiles.output(context,id(1),"wav"); Files.write(audio.toPath(),new byte[]{11,22,33});
            StudioFiles.editHistory(context,id(1),"deletedAt",123L);
            check(audio.isFile() && audio.length()==3, "Trash must retain audio");
            check(lookup(StudioFiles.historyList(context),id(1)).getLong("deletedAt")==123, "Trash metadata persisted");
            StudioFiles.editHistory(context,id(1),"deletedAt",0L);
            check(audio.isFile(), "Restore retains the existing audio");
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for(int thread=0;thread<4;thread++) {
                final int worker=thread;
                futures.add(pool.submit(() -> { start.await(); for(int n=0;n<20;n++) StudioFiles.history(context,work(100+worker*20+n));return null; }));
            }
            futures.add(pool.submit(() -> { start.await();for(int n=0;n<40;n++){StudioFiles.editHistory(context,id(1),"favorite",n%2==0);StudioFiles.editHistory(context,id(1),"title","编辑"+n);}return null; }));
            start.countDown();for(Future<?> task:futures) task.get(30,TimeUnit.SECONDS);
            records=StudioFiles.historyList(context);check(records.length()==82,"Concurrent append/edit lost records");
            Set<String> unique=new HashSet<>();for(int n=0;n<records.length();n++)check(unique.add(records.getJSONObject(n).getString("id")),"Duplicate history record");
            StudioFiles.editHistory(context,id(1),"title","最终标题");StudioFiles.editHistory(context,id(1),"favorite",true);StudioFiles.editHistory(context,id(1),"deletedAt",999L);
            StudioFiles.history(context,stale);
            JSONObject updated=lookup(StudioFiles.historyList(context),id(1));
            check(updated.getString("title").equals("最终标题"),"Late generation overwrote rename");
            check(updated.getBoolean("favorite"),"Late generation overwrote favorite");
            check(updated.getLong("deletedAt")==999,"Late generation resurrected trash");
            JSONObject detached=lookup(StudioFiles.historyList(context),id(2));detached.put("title","未保存的编辑");
            check(lookup(StudioFiles.historyList(context),id(2)).getString("title").equals("作品2"),"Read snapshot aliases stored record");
            boolean rejected=false;try{StudioFiles.editHistory(context,id(999),"title","不应复活");}catch(IOException e){rejected=true;}check(rejected,"Edit must not recreate deleted work");
            File history=new File(root,"history.json");check(history.renameTo(new File(history.getPath()+".bak")),"Make backup-only fixture");
            StudioFiles.history(context,work(3));check(StudioFiles.historyList(context).length()==83,"Appending lost backup-only history");
            StudioService.active=id(100);rejected=false;try{StudioFiles.deleteJob(context,id(1));}catch(IOException e){rejected=true;}check(rejected&&audio.isFile(),"Permanent deletion must protect active queue");StudioService.active="";
            StudioFiles.deleteJob(context,id(1));check(!audio.exists(),"Permanent deletion removes internal audio");
            check(StudioFiles.historyList(context).length()==82,"Deletion must preserve concurrent new works");
            Files.write(history.toPath(),"{broken".getBytes("UTF-8"));
            rejected=false;try{StudioFiles.history(context,work(4));}catch(JSONException e){rejected=true;}
            check(rejected,"Corrupt history must not be silently overwritten");
            check(new String(Files.readAllBytes(history.toPath()),"UTF-8").equals("{broken"),"Corrupt original retained for recovery");
            System.out.println(checks+" assertions passed: concurrent appends/edits, stale refresh, trash/restore, backup recovery and guarded deletion (host file adapters).");
        } finally {pool.shutdownNow();StudioService.active="";OmniModels.remove(root);}
    }
}
