package com.voice.studio;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Fish Audio is invoked only for an explicitly selected Fish job. No automatic cloud fallback. */
final class FishClient {
    static final String[] MODELS = {"s2.1-pro-free", "s2.1-pro", "s2-pro", "s1"};
    interface Connections { HttpURLConnection open() throws IOException; }
    interface Progress { void update(String message); }
    private final Connections connections;
    volatile HttpURLConnection active;
    FishClient() { this(() -> (HttpURLConnection)new URL("https://api.fish.audio/v1/tts").openConnection()); }
    FishClient(Connections connections) { this.connections = connections; }
    void cancel() { HttpURLConnection connection = active; if (connection != null) connection.disconnect(); }

    static JSONObject request(String token, String model, String text, String reference, float speed) throws Exception {
        if (token == null || token.trim().isEmpty()) throw new IOException("请在设置填写 Fish Audio API Key");
        if (token.length() > 4096 || !token.matches("[!-~]+")) throw new IOException("API Key 含空格或无效字符，请重新粘贴");
        // An unknown model silently falls back to a paid model at the provider. Reject locally.
        if (!Arrays.asList(MODELS).contains(model)) throw new IOException("Fish 模型配置无效，请在设置重新选择模型");
        if (text == null || text.trim().isEmpty() || text.length() > 12000) throw new IOException("文本需为 1—12000 字");
        if (reference == null || !reference.matches("[a-fA-F0-9]{32}"))
            throw new IOException("请在音色页添加并选择有效的 Fish Voice ID");
        if (!Float.isFinite(speed) || speed < .5f || speed > 2f) throw new IOException("语速需为 0.5—2.0");
        JSONObject body = new JSONObject().put("text", text).put("format", "mp3").put("normalize", true)
                .put("mp3_bitrate", 128).put("prosody", new JSONObject().put("speed", speed).put("volume", 0));
        if (!reference.isEmpty()) body.put("reference_id", reference.toLowerCase(Locale.ROOT));
        return body;
    }
    static String httpError(int status) {
        String detail = status == 401 ? "API Key 无效，请在设置更新"
                : status == 402 ? "账号额度不足或当前模型不可用，请检查 Fish 账户"
                : status == 403 ? "账号无访问权限，请检查 Fish 账户与音色授权"
                : status == 429 ? "请求过于频繁，请稍后重试"
                : status >= 500 ? "Fish 服务暂时不可用，请稍后重试"
                : "请检查 Fish 模型、音色 ID 与账号权限；默认音色失败时请选择具体音色";
        return "Fish Audio（HTTP " + status + "）：" + detail;
    }
    static String failure(Throwable error) {
        if (error instanceof SocketTimeoutException) return "Fish 请求超时，请检查网络后重试";
        if (error instanceof UnknownHostException || error instanceof ConnectException)
            return "无法连接 Fish Audio，请检查手机网络";
        if (error instanceof javax.net.ssl.SSLException) return "Fish 安全连接失败，请检查手机时间与网络";
        return error.getMessage() != null && error.getMessage().matches(".*[\\u4e00-\\u9fa5].*")
                ? error.getMessage() : "Fish Audio 请求失败（" + error.getClass().getSimpleName() + "），请重试";
    }
    void generate(String token, String model, String text, String reference, float speed,
            File output, AtomicBoolean cancelled, Progress progress) throws Exception {
        byte[] body = request(token, model, text, reference, speed).toString().getBytes("UTF-8");
        check(cancelled);
        HttpURLConnection connection = connections.open(); active = connection;
        boolean complete = false;
        try {
            check(cancelled);
            connection.setConnectTimeout(15000); connection.setReadTimeout(120000);
            connection.setInstanceFollowRedirects(false); connection.setRequestMethod("POST");
            connection.setDoOutput(true); connection.setFixedLengthStreamingMode(body.length);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("model", model);
            progress.update("正在连接 Fish Audio…");
            try (OutputStream stream = connection.getOutputStream()) { check(cancelled); stream.write(body); }
            int status = connection.getResponseCode(); check(cancelled);
            if (status != 200) throw new IOException(httpError(status));
            String type = connection.getContentType();
            if (type != null && (type.toLowerCase(Locale.ROOT).contains("json") || type.toLowerCase(Locale.ROOT).contains("text")))
                throw new IOException("Fish 返回了非音频内容，请检查模型与音色配置");
            progress.update("Fish 正在生成并接收音频…");
            try (InputStream input = connection.getInputStream(); OutputStream stream = new FileOutputStream(output)) {
                byte[] buffer = new byte[16384]; int n; long total = 0;
                while ((n = input.read(buffer)) != -1) {
                    check(cancelled); total += n;
                    if (total > 50L * 1024 * 1024) throw new IOException("Fish 音频超过 50 MiB，请分段生成");
                    stream.write(buffer, 0, n);
                }
            }
            check(cancelled); AudioChecks.validate(output, "mp3"); complete = true;
        } finally {
            active = null; connection.disconnect();
            if (!complete) output.delete();
        }
    }
    private static void check(AtomicBoolean cancelled) throws InterruptedIOException {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("已暂停生成");
    }
}
