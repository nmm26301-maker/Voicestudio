package com.voice.studio;

import org.json.JSONObject;

/** Identical waiting/running requests share a job; completed/failed requests remain explicitly retryable. */
final class SynthesisRequests {
    static boolean same(JSONObject existing, JSONObject request) {
        if (!existing.optBoolean("deduplicate") || !request.optBoolean("deduplicate")) return false;
        if (!existing.optString("type", "tts").equals("tts") || !request.optString("type", "tts").equals("tts")) return false;
        String state = existing.optString("state");
        if (!state.equals("queued") && !state.equals("running")) return false;
        String engine = request.optString("engine", "omni");
        if (!engine.equals(existing.optString("engine", "omni"))
                || !request.optString("text").equals(existing.optString("text"))
                || Double.compare(request.optDouble("speed", 1), existing.optDouble("speed", 1)) != 0) return false;
        if (engine.equals("accelerated")) return request.optString("endpoint").equals(existing.optString("endpoint"))
                && request.optString("model").equals(existing.optString("model"))
                && request.optString("apiVoice").equals(existing.optString("apiVoice"))
                && request.optString("language","zh").equals(existing.optString("language","zh"))
                && request.optInt("steps",32)==existing.optInt("steps",32)
                && request.optLong("seed",42)==existing.optLong("seed",42);
        if (engine.equals("fish")) return request.optString("model", "s2.1-pro-free").equals(existing.optString("model", "s2.1-pro-free"))
                && request.optString("reference").equals(existing.optString("reference"));
        return request.optString("language", "zh").equals(existing.optString("language", "zh"))
                && request.optString("profile").equals(existing.optString("profile"))
                && request.optString("style", "None").equals(existing.optString("style", "None"))
                && request.optInt("steps", 32) == existing.optInt("steps", 32)
                && request.optLong("seed", 42) == existing.optLong("seed", 42);
    }
}
