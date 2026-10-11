package com.voice.studio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Stage chunks without keeping the backbone and decoder resident together. */
final class OmniChunkPipeline {
    // Each chunk has at most 8 * 250 int64 codes: staging stays below 500 KiB.
    static final int BATCH_SIZE = 32;

    interface Encoder<C, V> extends AutoCloseable {
        C encode(int index, V voice) throws Exception;
        void close() throws Exception;
    }

    interface Decoder<C, V> extends AutoCloseable {
        V decode(int index, C codes, V voice) throws Exception;
        void close() throws Exception;
    }

    interface Factory<C, V> {
        Encoder<C, V> encoder() throws Exception;
        Decoder<C, V> decoder() throws Exception;
    }

    static <C, V> V run(int count, V initialVoice, AtomicBoolean cancelled,
            Factory<C, V> factory) throws Exception {
        boolean automatic = initialVoice == null;
        V voice = initialVoice;
        for (int start = 0; start < count;) {
            check(cancelled);
            // A generated voice must be decoded before it can anchor following chunks.
            int end = Math.min(count, start + (automatic && voice == null ? 1 : BATCH_SIZE));
            List<C> encoded = new ArrayList<>(end - start);
            try (Encoder<C, V> encoder = factory.encoder()) {
                for (int i = start; i < end; i++) {
                    check(cancelled);
                    encoded.add(encoder.encode(i, voice));
                    check(cancelled);
                }
            }
            check(cancelled);
            try (Decoder<C, V> decoder = factory.decoder()) {
                for (int i = start; i < end; i++) {
                    check(cancelled);
                    V anchor = decoder.decode(i, encoded.get(i - start), voice);
                    encoded.set(i - start, null);
                    if (automatic && voice == null && anchor != null) voice = anchor;
                    check(cancelled);
                }
            }
            start = end;
        }
        return voice;
    }

    static void check(AtomicBoolean cancelled) throws IOException {
        if (cancelled.get() || Thread.currentThread().isInterrupted())
            throw new IOException("已取消生成");
    }
}
