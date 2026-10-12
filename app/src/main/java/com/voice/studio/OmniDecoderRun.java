package com.voice.studio;

import ai.onnxruntime.*;
import java.io.IOException;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One decoder session owns one reusable input and one observed float output shape. */
final class OmniDecoderRun implements AutoCloseable {
    private final OrtEnvironment env;
    private final Map<String,OnnxTensor> feed = new LinkedHashMap<>();
    private final Map<String,OnnxTensor> pinned = new LinkedHashMap<>();
    private LongBuffer codes;
    private FloatBuffer audio;
    private OnnxTensor input, output;
    private int frames = -1;
    private boolean closed, shapeUnstable;
    int pinnedRuns, shapeFallbacks;
    long copiedOutputBytes;

    OmniDecoderRun(OrtEnvironment env) { this.env = env; }
    private static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }
    FloatBuffer run(OrtSession session, OrtSession.RunOptions options, long[] values,
            int newFrames, AtomicBoolean cancelled) throws OrtException,IOException {
        if (closed) throw new IllegalStateException("Decoder buffers closed");
        check(cancelled);
        if (newFrames <= 0 || values.length != Math.multiplyExact(8, newFrames))
            throw new IOException("解码音频帧无效");
        if (frames != newFrames) {
            discardOutput();
            if (input != null) input.close();
            input = null;feed.clear();codes = null;frames = -1;shapeUnstable = false;
            LongBuffer storage = direct(Math.multiplyExact(values.length, 8)).asLongBuffer();
            input = OnnxTensor.createTensor(env, storage, new long[]{8, 1, newFrames});
            codes = storage;frames = newFrames;feed.put("codes", input);
        }
        codes.rewind();codes.put(values).rewind();
        if (output != null) {
            audio.rewind();
            try (OrtSession.Result result = session.run(feed, Collections.emptySet(), pinned, options)) {
                check(cancelled);pinnedRuns++;
                return audio.asReadOnlyBuffer();
            } catch (OrtException error) {
                // Only an explicit output-shape mismatch may probe a changed output.
                // Cancellation, invalid input and execution errors must not rerun the model.
                String message = error.getMessage();
                if (message == null || !(message.contains("OrtValue shape verification") ||
                        (message.contains("output OrtValue provided for output") &&
                         message.contains("computed output shape")))) throw error;
                check(cancelled);discardOutput();shapeFallbacks++;shapeUnstable = true;
            }
        }
        try (OrtSession.Result result = session.run(feed, options)) {
            check(cancelled);
            OnnxTensor value = (OnnxTensor) result.get(0);
            FloatBuffer first = value.getFloatBuffer();
            if (first == null || !first.hasRemaining()) throw new IOException("模型返回空音频");
            copiedOutputBytes += (long)first.remaining() * 4;
            TensorInfo info = value.getInfo();
            long[] shape = info.getShape().clone();
            long elements = 1;
            for (long dimension : shape) {
                if (dimension <= 0 || dimension > 16777216L / elements)
                    return first.asReadOnlyBuffer();
                elements *= dimension;
            }
            // Half precision and unusually large outputs retain the original copied path.
            if (shapeUnstable || info.type != OnnxJavaType.FLOAT || elements != first.remaining())
                return first.asReadOnlyBuffer();
            FloatBuffer storage = first;
            if (!first.isDirect()) {
                storage = direct((int)(elements * 4)).asFloatBuffer();
                storage.put(first).rewind();
            }
            storage.rewind();
            OnnxTensor tensor = OnnxTensor.createTensor(env, storage, shape);
            audio = storage;output = tensor;
            pinned.put(session.getOutputNames().iterator().next(), tensor);
            return audio.asReadOnlyBuffer();
        }
    }
    private static void check(AtomicBoolean cancelled) throws IOException {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new IOException("已取消生成");
    }
    private void discardOutput() {
        if (output != null) output.close();
        output = null;audio = null;pinned.clear();
    }
    @Override public void close() {
        if (closed) return;closed = true;
        discardOutput();
        if (input != null) input.close();
        input = null;codes = null;feed.clear();
    }
}
