package com.voice.studio;

import java.io.*;

/** A completed, decodable audio file can be registered again without repeating inference or an API call. */
final class SynthesisOutput {
    interface Inspector { long durationMillis(File file, String format) throws Exception; }
    static long existing(File file, String format, Inspector inspector) throws Exception {
        if (!file.exists()) return 0;
        try {
            AudioChecks.validate(file, format);
            long duration = inspector.durationMillis(file, format);
            if (duration <= 0) throw new IOException("音频无法解码");
            return duration;
        } catch (Exception failure) {
            throw new IOException("已生成音频无法复用，请删除此任务后重新生成。原文件已保留（"
                    + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()) + "）", failure);
        }
    }
}
