package com.voice.studio;
import java.io.IOException;

public final class RecordingHealthTest {
    static int checks;
    static void ok(boolean condition) { checks++; if (!condition) throw new AssertionError("check " + checks); }
    public static void main(String[] args) throws Exception {
        RecordingHealth health = new RecordingHealth(16000, 100);
        health.checkInput(10099); ok(!health.enough());
        try { health.checkInput(10100); throw new AssertionError(); } catch (IOException expected) { checks++; }
        health.accept(new short[16000], 16000, 10100);
        ok(health.enough()); ok(health.seconds() == 1);
        health.checkInput(20099); checks++;
        ok(health.hint(10100).contains("静音"));
        health.accept(new short[]{0, -200, 300}, 3, 20100);
        ok(health.hint(20100).contains("接收"));
        ok(health.hint(23100).contains("静音"));
        health.accept(new short[]{Short.MIN_VALUE}, 1, 23101);
        ok(health.clipped); ok(health.hint(23101).contains("偏响"));
        health.accept(new short[]{200}, 0, 30000);
        try { health.checkInput(33101); throw new AssertionError(); } catch (IOException expected) { checks++; }
        ok(!health.limit(3600099)); ok(health.limit(3600100));
        RecordingHealth byFrames = new RecordingHealth(1, 0);
        byFrames.accept(new short[3600], 3600, 10); ok(byFrames.limit(10));
        try { new RecordingHealth(0, 0); throw new AssertionError(); } catch (IllegalArgumentException expected) { checks++; }
        try { health.accept(new short[1], 2, 0); throw new AssertionError(); } catch (IllegalArgumentException expected) { checks++; }
        ok(health.frames == 16004);
        System.out.println(checks + " assertions passed: PCM heartbeat, silent input, signed peak, clipping, recording limit");
    }
}
