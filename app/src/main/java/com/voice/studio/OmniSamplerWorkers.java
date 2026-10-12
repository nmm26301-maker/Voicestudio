package com.voice.studio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Generation-scoped workers; all readers finish before logits are reused or closed. */
final class OmniSamplerWorkers implements AutoCloseable {
    interface Work { void range(int start, int end, double[] scratch) throws Exception; }
    private final int maximum;
    private final AtomicBoolean cancelled;
    private final ThreadLocal<double[]> scratch = ThreadLocal.withInitial(() -> new double[1025]);
    private ExecutorService executor;
    private boolean closed;
    int parallelRounds, largestWorkerCount;

    OmniSamplerWorkers(int maximum, AtomicBoolean cancelled) {
        this.maximum = Math.max(1, Math.min(4, maximum));
        this.cancelled = cancelled;
    }
    void check() throws IOException {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new IOException("已取消生成");
    }
    void run(int count, Work work) throws IOException {
        if (closed) throw new IllegalStateException("Sampling workers closed");
        check();
        if (count == 0) return;
        int workers = Math.min(maximum, count / 64);
        if (workers < 2) {
            try { work.range(0, count, scratch.get()); }
            catch (Exception error) { rethrow(error); }
            check();
            return;
        }
        if (executor == null) executor = Executors.newFixedThreadPool(maximum, task -> {
            Thread thread = new Thread(task, "VoiceStudio-sampler");
            thread.setDaemon(true);
            return thread;
        });
        parallelRounds++;
        largestWorkerCount = Math.max(largestWorkerCount, workers);
        List<Future<?>> futures = new ArrayList<>(workers);
        Throwable failure = null;
        boolean interrupted = false;
        try {
            for (int i = 0; i < workers; i++) {
                final int start = count * i / workers, end = count * (i + 1) / workers;
                futures.add(executor.submit(() -> {
                    check();
                    work.range(start, end, scratch.get());
                    check();
                    return null;
                }));
            }
        } catch (Throwable error) { failure = error; }
        // Future cancellation alone does not stop a buffer reader. Join every task,
        // including after an interrupted caller or a failure in another worker.
        for (Future<?> future : futures) {
            boolean joined = false;
            while (!joined) try {
                future.get(); joined = true;
            } catch (InterruptedException error) {
                interrupted = true;
            } catch (ExecutionException error) {
                if (failure == null) failure = error.getCause();
                joined = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("已取消生成");
        }
        rethrow(failure);
        check();
    }
    private static void rethrow(Throwable error) throws IOException {
        if (error == null) return;
        if (error instanceof IOException) throw (IOException)error;
        if (error instanceof RuntimeException) throw (RuntimeException)error;
        if (error instanceof Error) throw (Error)error;
        throw new IOException("音频采样失败", error);
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        // run() has already joined all tasks; shutdown releases idle worker threads.
        if (executor != null) executor.shutdown();
        scratch.remove();
    }
}
