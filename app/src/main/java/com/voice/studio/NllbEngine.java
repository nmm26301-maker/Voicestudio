package com.voice.studio;

import ai.onnxruntime.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** INT8 NLLB inference. Encoder is closed before decoder loading to reduce peak RAM. */
final class NllbEngine {
    final File models;
    final NllbTokenizer tokenizer;
    volatile OrtSession.RunOptions running;

    NllbEngine(File models) throws IOException {
        this.models = models;
        tokenizer = new NllbTokenizer(new File(models, "tokenizer.bin"));
    }

    void cancel() {
        try {
            if (running != null) running.setTerminate(true);
        } catch (Exception ignored) {
        }
    }

    String translate(
            String text,
            String source,
            String target,
            AtomicBoolean cancel,
            OmniEngine.Progress progress)
            throws Exception {
        if (text.trim().isEmpty()) return "";
        tokenizer.language(source);
        int targetId = tokenizer.language(target);
        if (source.equals(target)) return text;
        long[] ids = tokenizer.encode(text, source);
        if (ids.length > 400) throw new IOException("翻译段落过长，请拆成较短句子");
        long[] mask = new long[ids.length];
        Arrays.fill(mask, 1);
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = OmniCodec.options();
                OrtSession.RunOptions run = new OrtSession.RunOptions()) {
            running = run;
            OmniEngine.check(cancel);
            progress.update("离线翻译 · 编码文字");
            float[] hidden;
            try (OrtSession encoder =
                            env.createSession(
                                    new File(models, "encoder_model_quantized.onnx")
                                            .getAbsolutePath(),
                                    options);
                    OnnxTensor input =
                            OnnxTensor.createTensor(
                                    env, LongBuffer.wrap(ids), new long[] {1, ids.length});
                    OnnxTensor attention =
                            OnnxTensor.createTensor(
                                    env, LongBuffer.wrap(mask), new long[] {1, ids.length})) {
                Map<String, OnnxTensor> feed = new HashMap<>();
                feed.put("input_ids", input);
                feed.put("attention_mask", attention);
                try (OrtSession.Result result = encoder.run(feed, run)) {
                    FloatBuffer buffer = ((OnnxTensor) result.get(0)).getFloatBuffer();
                    hidden = new float[buffer.remaining()];
                    buffer.get(hidden);
                }
            }
            OmniEngine.check(cancel);
            try (OrtSession decoder =
                            env.createSession(
                                    new File(models, "decoder_model_merged_quantized.onnx")
                                            .getAbsolutePath(),
                                    options);
                    OnnxTensor encoderHidden =
                            OnnxTensor.createTensor(
                                    env,
                                    FloatBuffer.wrap(hidden),
                                    new long[] {1, ids.length, 1024});
                    OnnxTensor encoderMask =
                            OnnxTensor.createTensor(
                                    env, LongBuffer.wrap(mask), new long[] {1, ids.length})) {
                OrtSession.Result first = null, previous = null;
                List<Integer> output = new ArrayList<>();
                long[] input = {2};
                boolean ended = false;
                int last = -1, repeat = 0;
                try {
                    for (int step = 0; step < 256; step++) {
                        OmniEngine.check(cancel);
                        OrtSession.Result next =
                                step(
                                        env,
                                        decoder,
                                        encoderHidden,
                                        encoderMask,
                                        input,
                                        first,
                                        previous,
                                        run);
                        if (previous != null && previous != first) previous.close();
                        previous = next;
                        if (first == null) first = next;
                        OnnxTensor tensor = (OnnxTensor) next.get("logits").get();
                        long[] shape = tensor.getInfo().getShape();
                        int count = (int) shape[shape.length - 1];
                        FloatBuffer logits = tensor.getFloatBuffer();
                        int offset = logits.remaining() - count;
                        int chosen = targetId;
                        if (step > 0) {
                            float best = -Float.MAX_VALUE;
                            chosen = 2;
                            for (int id = 0; id < count; id++) {
                                if (id == 0 || id == 1 || id == 3 || id >= 256001) continue;
                                float value = logits.get(offset + id);
                                if (value > best) {
                                    best = value;
                                    chosen = id;
                                }
                            }
                        }
                        if (chosen == 2) {
                            ended = true;
                            break;
                        }
                        if (chosen == last) repeat++;
                        else repeat = 0;
                        if (repeat > 16) throw new IOException("翻译重复，请缩短文字");
                        last = chosen;
                        if (step > 0) output.add(chosen);
                        input = new long[] {chosen};
                        if (step % 16 == 0) progress.update("离线翻译 · " + step + " 个词元");
                    }
                } finally {
                    if (previous != null && previous != first) previous.close();
                    if (first != null) first.close();
                }
                if (!ended) throw new IOException("翻译达到长度限制，请拆分文字");
                String result = tokenizer.decode(output);
                if (result.isEmpty()) throw new IOException("模型没有生成翻译");
                return result;
            }
        } finally {
            running = null;
        }
    }

    OrtSession.Result step(
            OrtEnvironment env,
            OrtSession decoder,
            OnnxTensor hidden,
            OnnxTensor attention,
            long[] input,
            OrtSession.Result first,
            OrtSession.Result previous,
            OrtSession.RunOptions run)
            throws Exception {
        List<OnnxTensor> owned = new ArrayList<>();
        Map<String, OnnxTensor> feed = new HashMap<>();
        try {
            OnnxTensor ids =
                    OnnxTensor.createTensor(
                            env, LongBuffer.wrap(input), new long[] {1, input.length});
            OnnxTensor branch =
                    OnnxTensor.createTensor(
                            env,
                            ByteBuffer.wrap(new byte[] {(byte) (first == null ? 0 : 1)}),
                            new long[] {1},
                            OnnxJavaType.BOOL);
            owned.add(ids);
            owned.add(branch);
            feed.put("input_ids", ids);
            feed.put("encoder_hidden_states", hidden);
            feed.put("encoder_attention_mask", attention);
            feed.put("use_cache_branch", branch);
            for (String name : decoder.getInputNames())
                if (name.startsWith("past_key_values")) {
                    OnnxTensor cache;
                    if (first == null) {
                        cache =
                                OnnxTensor.createTensor(
                                        env, FloatBuffer.allocate(0), new long[] {1, 16, 0, 64});
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
}
