package com.voice.studio;
import android.content.Context;
import java.io.*;
import java.nio.file.*;
import org.json.*;
public class StudioRecoveryTest {
    static int checks;
    static void check(boolean ok, String message) { checks++; if(!ok) throw new AssertionError(message); }
    static String id(int n) { return String.format("%08d-1111-4111-8111-111111111111",n); }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("studio-recovery").toFile();
        Context context = new Context() { public File getFilesDir() { return root; } };
        try {
            String[] states = {"running","queued","done","failed","paused"};
            for(int n=0;n<states.length;n++) StudioFiles.save(StudioFiles.job(context,id(n)),new JSONObject().put("id",id(n)).put("state",states[n]).put("created",n).put("chaptersDone",3));
            File result = StudioFiles.output(context,id(0),"wav");Files.write(result.toPath(),new byte[]{1,2,3});
            check(StudioFiles.pending(context),"Interrupted jobs initially block tasks");
            StudioFiles.recoverInterrupted(context);
            check(!StudioFiles.pending(context),"Recovery must unblock model operations");
            for(int n=0;n<states.length;n++) {
                JSONObject j=StudioFiles.read(StudioFiles.job(context,id(n)));
                check(j.getString("state").equals(n<2?"paused":states[n]),"Only running/queued jobs paused");
                check(j.getInt("chaptersDone")==3,"Chapter checkpoints retained");
            }
            check(result.isFile()&&result.length()==3,"Results retained");
            check(StudioFiles.list(context).length()==5,"Job records retained");
            check(StudioFiles.beginModelWork(context),"First maintenance starts");
            check(StudioFiles.pending(context),"Maintenance blocks destructive or inference work");
            check(!StudioFiles.beginModelWork(context),"Concurrent maintenance rejected");
            StudioFiles.endModelWork();check(!StudioFiles.pending(context),"Maintenance releases gate");
            StudioService.active=id(0);check(!StudioFiles.beginModelWork(context),"Active inference blocks maintenance");StudioService.active="";
            File record=StudioFiles.job(context,id(2));check(record.renameTo(new File(record.getPath()+".bak")),"Create interrupted-write fixture");
            check(StudioFiles.list(context).length()==5,"Backup-only job remains visible");
            check(record.exists(),"Read restores backup");
            check(StudioFiles.read(record).getString("state").equals("done"),"Completed backup job unchanged");
            StudioFiles.recoverInterrupted(context);check(StudioFiles.list(context).length()==5,"Repeated recovery keeps records");
            System.out.println(checks+" recovery assertions passed with host Context/AtomicFile adapters; Android lifecycle is not simulated.");
        } finally { StudioService.active="";StudioFiles.endModelWork();OmniModels.remove(root); }
    }
}
