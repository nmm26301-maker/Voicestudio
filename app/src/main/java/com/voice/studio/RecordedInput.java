package com.voice.studio;

import java.io.File;
import java.io.IOException;

final class RecordedInput {
    /** Promote only a finalized WAV with at least one second of complete samples. */
    static void preserve(File temporary, File destination) throws IOException {
        try (PcmAudio.Reader reader = new PcmAudio.Reader(temporary)) {
            if (reader.frames < reader.rate) throw new IOException("录音不足 1 秒");
        }
        if (destination.exists()) throw new IOException("已保存录音不可覆盖");
        if (!temporary.renameTo(destination)) throw new IOException("无法保存录音");
    }
}
