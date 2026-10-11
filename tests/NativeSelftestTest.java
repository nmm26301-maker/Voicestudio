package com.voice.studio;
import java.nio.file.*;
public final class NativeSelftestTest {
    public static void main(String[] args) throws Exception {
        byte[] graph = Files.readAllBytes(Paths.get(args[0]));
        String result = DeviceChecks.nativeOnnx(graph);
        if (!result.contains("通过")) throw new AssertionError();
        System.out.println("Native Identity graph inference passed using host ONNX Runtime JNI; Android execution remains separate.");
    }
}
