package com.voice.studio;

import java.nio.*;
import java.util.*;

/** Exact decisions/RNG comparison to the immediately preceding release. */
public class SamplerSpeedTest {
    static int checks;
    static volatile long sink;
    static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    static FloatBuffer buffer(int size, int kind) {
        if (kind == 0) return FloatBuffer.allocate(size);
        // A sliced native buffer catches accidental use of the parent's origin.
        FloatBuffer parent = ByteBuffer.allocateDirect((size + 11) * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        parent.position(11);
        FloatBuffer result = parent.slice();
        return kind == 2 ? result.asReadOnlyBuffer() : result;
    }
    static void compare(int frames, int prefix, int steps, int kind, int mode) {
        int sequence = frames + prefix, total = 8 * frames;
        FloatBuffer writable = buffer(16 * sequence * 1025, kind == 2 ? 1 : kind);
        Random values = new Random(154 + mode);
        FloatBuffer logits = kind == 2 ? writable.asReadOnlyBuffer() : writable;
        logits.position(Math.min(23, logits.capacity()));
        int position = logits.position(), limit = logits.limit();
        long[] before = new long[total], after = new long[total];
        Arrays.fill(before, 1024);
        Arrays.fill(after, 1024);
        LegacySampler153 old = new LegacySampler153(total);
        OmniStepSampler fast = new OmniStepSampler(total);
        Random r1 = new Random(42), r2 = new Random(42);
        int remaining = total;
        for (int step = 0; step < steps; step++) {
            for (int i = 0; i < writable.capacity(); i++) writable.put(i,
                    mode == 0 ? 0 : (values.nextFloat() - .5f) * (mode == 1 ? 10 : 2000));
            double a = .1 * step / steps / (1 - .9 * step / steps);
            double b = .1 * (step + 1) / steps / (1 - .9 * (step + 1) / steps);
            int k = step == steps - 1 ? remaining : Math.min(remaining, (int)Math.ceil(total * (b - a)));
            int n1 = old.update(logits, before, frames, sequence, prefix, k, r1);
            int n2 = fast.update(logits, after, frames, sequence, prefix, k, r2);
            check(n1 == n2, "selected count");
            check(Arrays.equals(before, after), "every token at every step");
            check(r1.nextLong() == r2.nextLong(), "RNG continuation");
            check(logits.position() == position && logits.limit() == limit, "caller buffer unchanged");
            remaining -= n1;
        }
        check(remaining == 0, "all codes filled");
        for (long code : after) check(code >= 0 && code < 1024, "valid code");
    }
    static void edges() {
        int frames = 3, prefix = 2, sequence = frames + prefix, total = 8 * frames;
        FloatBuffer logits = buffer(16 * sequence * 1025, 1);
        Random values = new Random(154);
        for (int i = 0; i < logits.capacity(); i++) logits.put(i, (values.nextFloat() - .5f) * 12);
        for (int k : new int[]{0, 1, 6, 12, 24, 99}) {
            long[] before = new long[total];
            for (int i = 0; i < total; i++) before[i] = i % 2 == 0 ? 1024 : i;
            long[] after = before.clone();
            Random r1 = new Random(7), r2 = new Random(7);
            int n1 = new LegacySampler153(total).update(logits, before, frames, sequence, prefix, k, r1);
            int n2 = new OmniStepSampler(total).update(logits, after, frames, sequence, prefix, k, r2);
            check(n1 == n2 && Arrays.equals(before, after), "partial/zero/all/oversized selection");
            check(r1.nextLong() == r2.nextLong(), "edge RNG");
        }
        long[] done = new long[total];
        check(new OmniStepSampler(total).update(logits, done, frames, sequence, prefix, 0, new Random()) == 0,
                "nothing masked");
    }
    static long trajectory(boolean legacy, FloatBuffer logits, int frames, int prefix, int steps) {
        int total = 8 * frames, sequence = frames + prefix, remaining = total;
        long[] codes = new long[total]; Arrays.fill(codes, 1024);
        Random random = new Random(42);
        LegacySampler153 old = legacy ? new LegacySampler153(total) : null;
        OmniStepSampler fast = legacy ? null : new OmniStepSampler(total);
        long start = System.nanoTime();
        for (int step = 0; step < steps; step++) {
            double a = .1 * step / steps / (1 - .9 * step / steps);
            double b = .1 * (step + 1) / steps / (1 - .9 * (step + 1) / steps);
            int k = step == steps - 1 ? remaining : Math.min(remaining, (int)Math.ceil(total * (b - a)));
            if (k <= 0) continue;
            remaining -= legacy ? old.update(logits, codes, frames, sequence, prefix, k, random)
                    : fast.update(logits, codes, frames, sequence, prefix, k, random);
        }
        long elapsed = System.nanoTime() - start;
        check(remaining == 0, "benchmark completed trajectory");
        sink ^= Arrays.hashCode(codes);
        return elapsed;
    }
    static void benchmark() {
        System.out.println("{\"scope\":\"Host Java complete sampler trajectories; no ONNX/model load/decode/phone inference\",\"warmups\":3,\"samples\":7,\"results\":[");
        boolean first = true;
        for (int kind : new int[]{0, 1}) for (int frames : new int[]{100, 250}) for (int steps : new int[]{16, 32}) {
            int prefix = 150;
            FloatBuffer logits = buffer(16 * (frames + prefix) * 1025, kind);
            Random values = new Random(154);
            for (int i = 0; i < logits.capacity(); i++) logits.put(i, (values.nextFloat() - .5f) * 10);
            for (int w = 0; w < 3; w++) {
                trajectory(true, logits, frames, prefix, steps);
                trajectory(false, logits, frames, prefix, steps);
            }
            long[] old = new long[7], fast = new long[7];
            // Alternate order to reduce systematic warm-cache/order effects.
            for (int i = 0; i < 7; i++) {
                if (i % 2 == 0) {
                    old[i] = trajectory(true, logits, frames, prefix, steps);
                    fast[i] = trajectory(false, logits, frames, prefix, steps);
                } else {
                    fast[i] = trajectory(false, logits, frames, prefix, steps);
                    old[i] = trajectory(true, logits, frames, prefix, steps);
                }
            }
            Arrays.sort(old); Arrays.sort(fast);
            if (!first) System.out.println(","); first = false;
            System.out.printf(Locale.US,
                    "{\"buffer\":\"%s\",\"frames\":%d,\"prefixFrames\":%d,\"steps\":%d,\"oldMedianMillis\":%.3f,\"newMedianMillis\":%.3f,\"reductionPercent\":%.2f}",
                    kind == 0 ? "heap" : "native-direct", frames, prefix, steps,
                    old[3] / 1e6, fast[3] / 1e6, 100.0 * (1.0 - (double)fast[3] / old[3]));
            System.out.flush();
        }
        System.out.println("\n]}");
    }
    public static void main(String[] args) {
        if (args.length > 0 && "--benchmark".equals(args[0])) { benchmark(); return; }
        for (int frames : new int[]{1, 4, 17}) for (int prefix : new int[]{0, 3, 150})
            for (int steps : new int[]{8, 16, 32}) for (int kind = 0; kind < 3; kind++)
                for (int mode = 0; mode < 3; mode++) compare(frames, prefix, steps, kind, mode);
        edges();
        System.out.println("Sampler 1.5.4: " + checks + " checks passed against frozen 1.5.3; exact tokens, RNG and buffer state at 8/16/32 steps.");
    }
}
