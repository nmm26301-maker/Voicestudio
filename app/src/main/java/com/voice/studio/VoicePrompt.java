package com.voice.studio;

import java.io.*;

final class VoicePrompt {
    final String text;
    final long[] codes;
    final int frames;
    final double rms;

    VoicePrompt(String text, long[] codes, int frames, double rms) throws IOException {
        if (text == null
                || text.trim().isEmpty()
                || text.length() > 2000
                || frames < 75
                || frames > 500
                || codes.length != 8 * frames
                || !Double.isFinite(rms)
                || rms <= 0) throw new IOException("参考音色无效");
        for (long code : codes) if (code < 0 || code > 1023) throw new IOException("音色编码无效");
        this.text = text;
        this.codes = codes;
        this.frames = frames;
        this.rms = rms;
    }

    String identity() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(text);out.writeInt(frames);out.writeDouble(rms);
            for (long code : codes) out.writeShort((int)code);
        }
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
        StringBuilder result = new StringBuilder();
        for (byte value : hash) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }

    void save(File path) throws IOException {
        try (DataOutputStream out =
                new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) {
            out.writeInt(0x56504331);
            out.writeUTF(text);
            out.writeInt(frames);
            out.writeDouble(rms);
            for (long code : codes) out.writeShort((int) code);
        }
    }

    static VoicePrompt load(File path) throws IOException {
        try (DataInputStream in =
                new DataInputStream(new BufferedInputStream(new FileInputStream(path)))) {
            if (in.readInt() != 0x56504331) throw new IOException("音色文件不受支持");
            String text = in.readUTF();
            int frames = in.readInt();
            double rms = in.readDouble();
            if (frames < 75 || frames > 500) throw new IOException("音色时长无效");
            long[] codes = new long[8 * frames];
            for (int i = 0; i < codes.length; i++) codes[i] = in.readUnsignedShort();
            if (in.read() != -1) throw new IOException("音色数据不完整");
            return new VoicePrompt(text, codes, frames, rms);
        }
    }
}
