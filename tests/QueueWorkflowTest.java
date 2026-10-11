package com.voice.studio;

import android.content.Context;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Real queue/file decisions; Android services and microphone hardware are not simulated. */
public class QueueWorkflowTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    interface Operation { void run() throws Exception; }
    static void rejects(Operation operation, String message) throws Exception {
        try { operation.run(); throw new AssertionError(message); } catch (IOException expected) { checks++; }
    }
    static String id(int n) { return String.format("%08d-1111-4111-8111-111111111111", n); }
    static JSONObject task(int n, String type, String state) throws Exception {
        return new JSONObject().put("id", id(n)).put("type", type).put("state", state).put("created", n);
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("studio-queue").toFile();
        Context c = new Context() { public File getFilesDir() { return root; } };
        try {
            JSONObject paused = task(1, "book", "paused").put("started", 1).put("finished", 2)
                    .put("failureStage", "old").put("failureDetail", "old").put("interrupted", true).put("chaptersDone", 7);
            StudioFiles.save(StudioFiles.job(c, id(1)), paused);
            check(StudioFiles.beginModelWork(c), "maintenance starts before queue admission");
            rejects(() -> StudioFiles.requeue(c, id(1)), "retry during model removal must fail");
            check(StudioFiles.read(StudioFiles.job(c, id(1))).getString("state").equals("paused"), "failed admission preserves old state");
            StudioFiles.endModelWork();
            JSONObject queued = StudioFiles.requeue(c, id(1));
            check(queued.getString("state").equals("queued"), "retry queued");
            for (String key : new String[]{"started", "finished", "failureStage", "failureDetail", "interrupted"})
                check(!queued.has(key), "clear stale retry metadata: " + key);
            check(queued.getInt("chaptersDone") == 7, "chapter checkpoint survives retry");
            rejects(() -> StudioFiles.requeue(c, id(1)), "double retry must not queue twice");
            check(!StudioFiles.beginModelWork(c), "queued job blocks model removal");
            JSONObject mic = task(2, "dictation", "paused"); StudioFiles.save(StudioFiles.job(c, id(2)), mic);
            rejects(() -> StudioFiles.requeue(c, id(2)), "live microphone must not sit behind a queue");
            File recording = new File(StudioFiles.dir(c, "jobs"), id(2) + "/input.wav");
            recording.getParentFile().mkdirs(); Files.write(recording.toPath(), new byte[]{1});
            StudioFiles.requeue(c, id(2));
            check(recording.isFile(), "retry saved recording preserves input and can queue");
            StudioFiles.save(StudioFiles.job(c, id(1)), task(1, "book", "done"));
            StudioFiles.save(StudioFiles.job(c, id(2)), task(2, "dictation", "paused"));
            recording.delete(); StudioService.active = id(9);
            rejects(() -> StudioFiles.requeue(c, id(2)), "live microphone rejects active worker");
            StudioService.active = ""; StudioFiles.requeue(c, id(2));
            check(StudioFiles.read(StudioFiles.job(c, id(2))).getString("state").equals("queued"), "idle microphone retry admitted");
            StudioFiles.save(StudioFiles.job(c, id(2)), task(2, "dictation", "done"));
            rejects(() -> StudioFiles.requeue(c, id(2)), "finished jobs cannot resume");
            rejects(() -> StudioFiles.requeue(c, "../escape"), "invalid id rejected");
            Files.write(new File(StudioFiles.job(c, id(2)).getPath() + ".new").toPath(), new byte[]{1});
            StudioFiles.deleteJob(c, id(2));
            check(!new File(StudioFiles.job(c, id(2)).getPath() + ".new").exists(), "task deletion cleans interrupted writes");
            File voiceInput = new File(StudioFiles.dir(c, "audio"), id(300) + ".wav");
            Files.write(voiceInput.toPath(), new byte[]{1});
            StudioFiles.save(StudioFiles.job(c, id(3)), task(3, "clone", "done").put("referenceOwned", true).put("referenceAudio", voiceInput.getName()));
            File retained = new File(StudioFiles.dir(c, "profiles"), id(3) + ".reference.wav"); Files.write(retained.toPath(), new byte[]{2});
            StudioFiles.deleteJob(c, id(3));
            check(!voiceInput.exists(), "owned immutable queue input cleaned with task");
            check(retained.isFile(), "deleting clone task preserves profile reference recording");
            StudioFiles.save(StudioFiles.job(c, id(4)), task(4, "extractVoices", "done"));
            File person = new File(StudioFiles.dir(c, "audio"), id(4) + "-1.wav"); Files.write(person.toPath(), new byte[]{1});
            File unrelated = new File(StudioFiles.dir(c, "audio"), id(5) + "-1.wav"); Files.write(unrelated.toPath(), new byte[]{1});
            StudioFiles.save(StudioFiles.job(c, id(6)), task(6, "clone", "paused").put("sourceJob", id(4)).put("referenceAudio", person.getName()));
            rejects(() -> StudioFiles.deleteJob(c, id(4)), "pending character clone must retain speaker extraction");
            StudioFiles.deleteJob(c, id(6));
            check(person.exists(), "unowned shared speaker input preserved when clone task deleted");
            StudioFiles.deleteJob(c, id(4));
            check(!person.exists(), "per-speaker exports cleaned when parent task deleted");
            check(unrelated.exists(), "unrelated speaker exports retained");
            // A real race between maintenance and a new task must admit exactly one side.
            for (int n = 0; n < 30; n++) {
                final int number = n + 100;
                CountDownLatch start = new CountDownLatch(1);
                AtomicBoolean maintenance = new AtomicBoolean(), admitted = new AtomicBoolean();
                AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread one = new Thread(() -> { try { start.await(); maintenance.set(StudioFiles.beginModelWork(c)); } catch (Throwable t) { failure.set(t); } });
                Thread two = new Thread(() -> { try { start.await(); StudioFiles.queue(c, task(number, "tts", "queued")); admitted.set(true); }
                    catch (IOException expected) { } catch (Throwable t) { failure.set(t); } });
                one.start(); two.start(); start.countDown(); one.join(); two.join();
                if (failure.get() != null) throw new AssertionError(failure.get());
                check(maintenance.get() != admitted.get(), "model maintenance and queue cannot both win");
                StudioFiles.endModelWork();
                if (admitted.get()) { StudioFiles.save(StudioFiles.job(c, id(number)), task(number, "tts", "done")); StudioFiles.deleteJob(c, id(number)); }
            }
            System.out.println(checks + " queue workflow assertions passed (host filesystem; no Android service simulation).");
        } finally { StudioService.active = ""; StudioFiles.endModelWork(); OmniModels.remove(root); }
    }
}
