package com.voice.studio;

import java.io.IOException;
import java.util.*;
import org.json.*;

final class SubtitleDocument {
    static JSONArray normalize(JSONArray source) throws Exception {
        List<JSONObject> sorted = new ArrayList<>();
        for (int n = 0; n < source.length(); n++) {
            JSONObject segment = new JSONObject(source.getJSONObject(n).toString());
            double start = segment.getDouble("start"), end = segment.getDouble("end");
            if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start)
                throw new IOException("第 " + (n + 1) + " 条字幕时间无效");
            segment.getString("text");
            if (segment.has("speaker") && segment.getInt("speaker") < 0) throw new IOException("说话人编号不能为负数");
            sorted.add(segment);
        }
        sorted.sort((a, b) -> Double.compare(a.optDouble("start"), b.optDouble("start")));
        JSONArray result = new JSONArray(); double end = 0;
        for (JSONObject segment : sorted) {
            if (segment.getDouble("start") < end) throw new IOException("字幕时间重叠，请先校对");
            result.put(segment); end = segment.getDouble("end");
        }
        return result;
    }
    static String plain(JSONArray segments) throws Exception {
        StringBuilder text = new StringBuilder();
        for (int n = 0; n < segments.length(); n++) text.append(segments.getJSONObject(n).getString("text")).append('\n');
        return text.toString();
    }
    static String srt(JSONArray segments) throws Exception {
        StringBuilder text = new StringBuilder();
        for (int n = 0; n < segments.length(); n++) {
            JSONObject segment = segments.getJSONObject(n);
            text.append(n + 1).append('\n').append(stamp(segment.getDouble("start")))
                    .append(" --> ").append(stamp(segment.getDouble("end"))).append('\n')
                    .append(segment.getString("text")).append("\n\n");
        }
        return text.toString();
    }
    static String stamp(double seconds) {
        long millis = Math.max(0, Math.round(seconds * 1000));
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", millis / 3600000,
                millis / 60000 % 60, millis / 1000 % 60, millis % 1000);
    }
}
