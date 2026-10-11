package com.voice.studio;

import java.util.*;

public final class StreamingResamplerTest {
    static int checks;
    static void ok(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    static void compare(float[] source, int input, int output, int block) {
        float[] reference = PcmAudio.resample(source, input, output);
        StreamingResampler resampler = new StreamingResampler(input, output);
        float[] streamed = new float[reference.length]; int cursor = 0;
        Random random = new Random(173);
        for (int at = 0; at < source.length;) {
            int take = Math.min(source.length - at, block == 0 ? random.nextInt(2048) + 1 : block);
            float[] part = resampler.push(Arrays.copyOfRange(source, at, at + take));
            System.arraycopy(part, 0, streamed, cursor, part.length); cursor += part.length; at += take;
        }
        float[] finalPart = resampler.finish();
        System.arraycopy(finalPart, 0, streamed, cursor, finalPart.length); cursor += finalPart.length;
        ok(cursor == reference.length);
        double error = 0;
        for (int n = 0; n < reference.length; n++) error = Math.max(error, Math.abs(reference[n] - streamed[n]));
        ok(error < 1e-6);
        ok(resampler.tail.length == 0);
        try { resampler.push(new float[1]); throw new AssertionError(); } catch (IllegalStateException expected) { checks++; }
    }
    public static void main(String[] args) {
        float[] source = new float[14081];
        for (int n = 0; n < source.length; n++) source[n] = (float)(.6 * Math.sin(n * .09) + .2 * Math.sin(n * .7));
        for (int[] rates : new int[][]{{44100,24000},{48000,24000},{16000,24000},{24000,44100},{44100,44100},{192000,8000},{8000,192000}})
            for (int block : new int[]{1,17,1024,0}) compare(source,rates[0],rates[1],block);
        for (int size : new int[]{0,1,15,16,17,31,32,33}) compare(Arrays.copyOf(source,size),44100,24000,1);
        StreamingResampler longRun = new StreamingResampler(44100,24000);
        int total = 0; float[] block = new float[1024];
        for (int n = 0; n < 1000; n++) total += longRun.push(block).length;
        total += longRun.finish().length;
        ok(total == Math.round(1024000.0 * 24000 / 44100));
        try { new StreamingResampler(0,24000); throw new AssertionError(); } catch (IllegalArgumentException expected) { checks++; }
        System.out.println(checks + " assertions passed: arbitrary chunk boundaries, sinc continuity, exact global duration, final drain");
    }
}
