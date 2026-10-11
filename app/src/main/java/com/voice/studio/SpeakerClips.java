package com.voice.studio;

import org.json.*;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Validated, non-overlapping source ranges for per-speaker audio export. */
final class SpeakerClips {
    static Map<Integer, JSONArray> plan(JSONArray segments, double duration) throws Exception {
        if (!Double.isFinite(duration) || duration <= 0) throw new IOException("原始音轨时长无效");
        List<JSONObject> sorted = new ArrayList<>();
        for (int n = 0; n < segments.length(); n++) sorted.add(segments.getJSONObject(n));
        sorted.sort(Comparator.comparingDouble(s -> s.optDouble("start", -1)));
        Map<Integer, JSONArray> groups = new TreeMap<>();
        double previousEnd = 0;
        for (JSONObject segment : sorted) {
            double start = segment.getDouble("start"), end = segment.getDouble("end");
            int speaker = segment.optInt("speaker", 0);
            if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start
                    || end > duration + .05 || speaker < 0 || speaker > 32)
                throw new IOException("字幕时间或人物编号无效，请校对后重试");
            if (start < previousEnd - .01) throw new IOException("字幕片段重叠，请先校对；暂不支持重叠对白的人物分离");
            previousEnd = Math.max(previousEnd, end);
            if (speaker == 0) continue;
            JSONArray ranges = groups.get(speaker);
            if (ranges == null) { ranges = new JSONArray(); groups.put(speaker, ranges); }
            ranges.put(new JSONObject(segment.toString()).put("end", Math.min(end, duration)));
        }
        if (groups.isEmpty()) throw new IOException("没有可提取的人物，请先分组或手动填写人物编号");
        return groups;
    }
    static double seconds(JSONArray ranges) throws Exception {
        double sum = 0;
        for (int n = 0; n < ranges.length(); n++) sum += ranges.getJSONObject(n).getDouble("end") - ranges.getJSONObject(n).getDouble("start");
        return sum;
    }
    static long[] export(PcmAudio.Reader reader, JSONArray ranges, File full, File reference, AtomicBoolean cancel) throws Exception {
        long referenceFrames = 0, totalFrames = 0;
        // Reader has a 60-second per-read bound. Export arbitrarily long speakers
        // with bounded blocks, preserving complete dialogue after the first minute.
        try (PcmAudio.Writer all = new PcmAudio.Writer(full, reader.rate);
                PcmAudio.Writer ref = new PcmAudio.Writer(reference, reader.rate)) {
            for (int n = 0; n < ranges.length(); n++) {
                JSONObject range = ranges.getJSONObject(n);
                double start = range.getDouble("start"), end = range.getDouble("end");
                for (double at = start; at < end; at += 30) {
                    if (cancel.get()) throw new IOException("人物声音提取已暂停");
                    float[] samples = reader.read(at, Math.min(30, end - at));
                    all.write(samples); totalFrames += samples.length;
                    int count = (int)Math.min(samples.length, reader.rate * 10L - referenceFrames);
                    if (count > 0) { ref.write(Arrays.copyOf(samples, count)); referenceFrames += count; }
                }
            }
        }
        return new long[]{totalFrames, referenceFrames};
    }
}
