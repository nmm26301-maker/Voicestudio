package com.voice.studio;

import ai.onnxruntime.*;

import org.json.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class WhisperEngine {
    static class Segment {
        final double start, end;
        final String text;

        Segment(double s, double e, String t) {
            start = s;
            end = e;
            text = t;
        }
    }

    final File models;
    final WhisperMel mel;
    final JSONObject config;
    final Map<Integer, String> vocab = new HashMap<>();
    final Map<Integer, Integer> byteMap = new HashMap<>();
    volatile OrtSession.RunOptions running;

    WhisperEngine(File models, InputStream filters) throws Exception {
        this.models = models;
        mel = new WhisperMel(filters);
        config = new JSONObject(read(new File(models, "generation_config.json")));
        JSONObject tokens =
                new JSONObject(read(new File(models, "tokenizer.json")))
                        .getJSONObject("model")
                        .getJSONObject("vocab");
        for (Iterator<String> it = tokens.keys(); it.hasNext(); ) {
            String key = it.next();
            vocab.put(tokens.getInt(key), key);
        }
        List<Integer> bytes = new ArrayList<>();
        for (int b = 33; b <= 126; b++) bytes.add(b);
        for (int b = 161; b <= 172; b++) bytes.add(b);
        for (int b = 174; b <= 255; b++) bytes.add(b);
        int extra = 0;
        for (int b = 0; b < 256; b++) if (!bytes.contains(b)) byteMap.put(256 + extra++, b);
        for (int b : bytes) byteMap.put(b, b);
    }

    void cancel() {
        try {
            if (running != null) running.setTerminate(true);
        } catch (Exception ignored) {
        }
    }

    static String read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) >= 0) {
                if (out.size() + n > 20000000) throw new IOException("配置文件过大");
                out.write(b, 0, n);
            }
            return out.toString("UTF-8");
        }
    }

    String decode(List<Integer> ids) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int id : ids) {
            String token = vocab.get(id);
            if (token == null || token.startsWith("<|")) continue;
            for (int at = 0; at < token.length(); ) {
                int cp = token.codePointAt(at);
                at += Character.charCount(cp);
                Integer value = byteMap.get(cp);
                if (value != null) b.write(value);
            }
        }
        return b.toString("UTF-8").trim();
    }

    List<Segment> transcribe(
            File wav,
            String language,
            boolean english,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        List<Segment> all = new ArrayList<>();
        JSONObject langs = config.getJSONObject("lang_to_id");
        Set<Integer> languageIds = new HashSet<>();
        for (Iterator<String> it = langs.keys(); it.hasNext(); )
            languageIds.add(langs.getInt(it.next()));
        Set<Integer> suppressed = new HashSet<>();
        JSONArray sup = config.optJSONArray("suppress_tokens");
        if (sup != null) for (int i = 0; i < sup.length(); i++) suppressed.add(sup.getInt(i));
        try (OrtSession.SessionOptions options = OmniCodec.options();
                OrtSession.RunOptions run = new OrtSession.RunOptions();
                OrtSession encoder =
                        env.createSession(
                                new File(models, "encoder_model_quantized.onnx").getAbsolutePath(),
                                options);
                OrtSession decoder =
                        env.createSession(
                                new File(models, "decoder_model_merged_quantized.onnx")
                                        .getAbsolutePath(),
                                options);
                PcmAudio.Reader reader = new PcmAudio.Reader(wav)) {
            running = run;
            double offset = 0;
            int iterations = 0;
            while (offset < reader.seconds() && iterations++ < Math.ceil(reader.seconds()) + 1) {
                OmniEngine.check(cancel);
                double seconds = Math.min(30, reader.seconds() - offset);
                float[] samples =
                        PcmAudio.resample(reader.read(offset, seconds), reader.rate, 16000);
                if (PcmAudio.rms(samples) < .002) {
                    offset += seconds;
                    continue;
                }
                progress.update(
                        "本机转写 · "
                                + (int) offset
                                + " / "
                                + (int) Math.ceil(reader.seconds())
                                + " 秒");
                try (OnnxTensor features = OnnxTensor.createTensor(env, mel.features(samples));
                        OrtSession.Result encoded =
                                encoder.run(
                                        Collections.singletonMap("input_features", features),
                                        run)) {
                    OnnxTensor hidden = (OnnxTensor) encoded.get(0);
                    int lang;
                    if (language.equals("auto")) {
                        try (OrtSession.Result detection =
                                step(env, decoder, hidden, new long[] {50258}, null, null, run)) {
                            FloatBuffer logits =
                                    ((OnnxTensor) detection.get("logits").get()).getFloatBuffer();
                            int vocabulary = logits.remaining();
                            double best = -Double.MAX_VALUE;
                            lang = 50259;
                            for (int id : languageIds)
                                if (id < vocabulary && logits.get(id) > best) {
                                    best = logits.get(id);
                                    lang = id;
                                }
                        }
                    } else {
                        String key = "<|" + language + "|>";
                        if (!langs.has(key)) throw new IOException("识别语言不受支持");
                        lang = langs.getInt(key);
                    }
                    long[] input = {50258, lang, english ? 50358 : 50359};
                    OrtSession.Result first = null, previous = null;
                    List<Integer> ids = new ArrayList<>();
                    int lastTimestamp = 50364, repeat = 0, lastToken = -1;
                    try {
                        for (int token = 0; token < 440; token++) {
                            OmniEngine.check(cancel);
                            OrtSession.Result next =
                                    step(env, decoder, hidden, input, first, previous, run);
                            if (previous != null && previous != first) previous.close();
                            previous = next;
                            if (first == null) first = next;
                            OnnxTensor logitsTensor = (OnnxTensor) next.get("logits").get();
                            long[] shape = logitsTensor.getInfo().getShape();
                            int count = (int) shape[shape.length - 1];
                            FloatBuffer logits = logitsTensor.getFloatBuffer();
                            int start = logits.remaining() - count;
                            double best = -Double.MAX_VALUE;
                            int chosen = 50257;
                            for (int id = 0; id < count; id++) {
                                if (suppressed.contains(id)
                                        || languageIds.contains(id)
                                        || id == 50258
                                        || id == 50358
                                        || id == 50359
                                        || id == 50363) continue;
                                if (id >= 50364
                                        && (id < lastTimestamp
                                                || id > 50364 + (int) Math.ceil(seconds / .02)))
                                    continue;
                                if (token == 0 && (id == 50257 || id == 220 || id > 50414))
                                    continue;
                                double value = logits.get(start + id);
                                if (value > best) {
                                    best = value;
                                    chosen = id;
                                }
                            }
                            if (chosen == 50257) break;
                            if (chosen == lastToken) repeat++;
                            else repeat = 0;
                            if (repeat > 16) throw new IOException("识别重复，请缩短或更换清晰音频");
                            lastToken = chosen;
                            ids.add(chosen);
                            if (chosen >= 50364) lastTimestamp = chosen;
                            input = new long[] {chosen};
                        }
                    } finally {
                        if (previous != null && previous != first) previous.close();
                        if (first != null) first.close();
                    }
                    List<Integer> words = new ArrayList<>();
                    double start = 0, lastEnd = 0;
                    for (int token : ids) {
                        if (token >= 50364) {
                            double time = Math.min(seconds, (token - 50364) * .02);
                            if (!words.isEmpty()) {
                                String text = decode(words);
                                if (!text.isEmpty())
                                    all.add(
                                            new Segment(
                                                    offset + start,
                                                    offset + Math.max(start + .02, time),
                                                    text));
                                words.clear();
                                lastEnd = time;
                            }
                            start = time;
                        } else if (token < 50257) words.add(token);
                    }
                    if (!words.isEmpty()) {
                        String text = decode(words);
                        if (!text.isEmpty()) {
                            all.add(new Segment(offset + start, offset + seconds, text));
                            lastEnd = seconds;
                        }
                    }
                    if (lastEnd > 1 && seconds - lastEnd > 1) {
                        float[] tail =
                                Arrays.copyOfRange(
                                        samples,
                                        Math.min(samples.length, (int) (lastEnd * 16000)),
                                        samples.length);
                        if (PcmAudio.rms(tail) > .003) {
                            offset += lastEnd;
                            continue;
                        }
                    }
                }
                offset += seconds;
            }
        } finally {
            running = null;
        }
        return all;
    }

    OrtSession.Result step(
            OrtEnvironment env,
            OrtSession decoder,
            OnnxTensor hidden,
            long[] input,
            OrtSession.Result first,
            OrtSession.Result previous,
            OrtSession.RunOptions run)
            throws Exception {
        List<OnnxTensor> owned = new ArrayList<>();
        Map<String, OnnxTensor> feed = new HashMap<>();
        try {
            OnnxTensor
                    ids =
                            OnnxTensor.createTensor(
                                    env, LongBuffer.wrap(input), new long[] {1, input.length}),
                    branch =
                            OnnxTensor.createTensor(
                                    env,
                                    ByteBuffer.wrap(new byte[] {(byte) (first == null ? 0 : 1)}),
                                    new long[] {1},
                                    OnnxJavaType.BOOL);
            owned.add(ids);
            owned.add(branch);
            feed.put("input_ids", ids);
            feed.put("encoder_hidden_states", hidden);
            feed.put("use_cache_branch", branch);
            for (String name : decoder.getInputNames())
                if (name.startsWith("past_key_values")) {
                    OnnxTensor cache;
                    if (first == null) {
                        cache =
                                OnnxTensor.createTensor(
                                        env, FloatBuffer.allocate(0), new long[] {1, 6, 0, 64});
                        owned.add(cache);
                    } else {
                        OrtSession.Result source = name.contains("encoder") ? first : previous;
                        cache =
                                (OnnxTensor)
                                        source.get(name.replace("past_key_values", "present"))
                                                .get();
                    }
                    feed.put(name, cache);
                }
            return decoder.run(feed, run);
        } finally {
            for (OnnxTensor tensor : owned) tensor.close();
        }
    }

    static String srt(List<Segment> segments) {
        StringBuilder s = new StringBuilder();
        int n = 0;
        for (Segment x : segments)
            s.append(++n)
                    .append('\n')
                    .append(stamp(x.start))
                    .append(" --> ")
                    .append(stamp(x.end))
                    .append('\n')
                    .append(x.text)
                    .append("\n\n");
        return s.toString();
    }

    static String stamp(double seconds) {
        long m = Math.max(0, Math.round(seconds * 1000));
        return String.format(
                Locale.US,
                "%02d:%02d:%02d,%03d",
                m / 3600000,
                m / 60000 % 60,
                m / 1000 % 60,
                m % 1000);
    }
}
