package com.voice.studio;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Mock transport only: no account key, network request or paid generation. */
public class FishClientTest {
    static int checks;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    interface Attempt { void run() throws Exception; }
    static String fail(Attempt attempt) throws Exception {
        try { attempt.run(); throw new AssertionError("expected rejection"); }
        catch (IOException expected) { checks++; return expected.getMessage(); }
    }
    static class Fake extends HttpURLConnection {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        byte[] audio = new byte[128];
        int status = 200; String type = "audio/mpeg"; boolean disconnected;
        AtomicBoolean cancelDuringRead;
        Fake() throws Exception { super(new URL("https://example.invalid/v1/tts")); audio[0]='I'; audio[1]='D'; audio[2]='3'; }
        public void connect() {} public boolean usingProxy() { return false; }
        public void disconnect() { disconnected = true; }
        public OutputStream getOutputStream() { return sent; }
        public int getResponseCode() { return status; }
        public String getContentType() { return type; }
        public InputStream getInputStream() {
            return new ByteArrayInputStream(audio) {
                public int read(byte[] b, int off, int length) {
                    int n = super.read(b, off, length);
                    if (cancelDuringRead != null) cancelDuringRead.set(true);
                    return n;
                }
            };
        }
    }
    public static void main(String[] args) throws Exception {
        String key="test-key", model="s2.1-pro-free", ref="ABCDEF1234567890ABCDEF1234567890";
        JSONObject body=FishClient.request(key,model,"你好",ref,1.2f);
        check(body.getString("reference_id").equals(ref.toLowerCase()));
        check(body.getString("format").equals("mp3"));
        check(Math.abs(body.getJSONObject("prosody").getDouble("speed")-1.2)<.0001);
        check(!body.has("token") && !body.has("api_key"));
        fail(() -> FishClient.request(key,model,"hello","",1));
        fail(() -> FishClient.request("",model,"hello","",1));
        fail(() -> FishClient.request("x\r\nheader",model,"hello","",1));
        fail(() -> FishClient.request("x y",model,"hello","",1));
        fail(() -> FishClient.request(key,"unknown-paid-fallback","hello","",1));
        fail(() -> FishClient.request(key,model,"","",1));
        fail(() -> FishClient.request(key,model,new String(new char[12001]),"",1));
        fail(() -> FishClient.request(key,model,"hello","bad-voice",1));
        fail(() -> FishClient.request(key,model,"hello","",Float.NaN));
        fail(() -> FishClient.request(key,model,"hello","",.4f));
        fail(() -> FishClient.request(key,model,"hello","",2.1f));
        Path dir=Files.createTempDirectory("fish-mock-"); File file=dir.resolve("test.mp3.pending").toFile();
        Fake success=new Fake(); FishClient client=new FishClient(() -> success);
        client.generate(key,model,"你好",ref,1,file,new AtomicBoolean(),m -> {});
        check(file.length()==128); check(success.disconnected && client.active==null);
        check(success.getRequestProperty("Authorization").equals("Bearer "+key));
        check(success.getRequestProperty("model").equals(model));
        check(success.getRequestMethod().equals("POST"));
        check(!success.getInstanceFollowRedirects());
        check(new JSONObject(success.sent.toString("UTF-8")).getString("text").equals("你好"));
        file.delete();
        for (int status : new int[]{400,401,402,403,429,503,302}) {
            Fake error=new Fake(); error.status=status;
            String message=fail(() -> new FishClient(() -> error).generate(key,model,"hello",ref,1,file,new AtomicBoolean(),m -> {}));
            check(message.contains("HTTP "+status)); check(error.disconnected && !file.exists());
        }
        Fake json=new Fake(); json.type="APPLICATION/JSON";
        fail(() -> new FishClient(() -> json).generate(key,model,"hello",ref,1,file,new AtomicBoolean(),m -> {}));
        check(!file.exists() && json.disconnected);
        Fake empty=new Fake(); empty.audio=new byte[0];
        fail(() -> new FishClient(() -> empty).generate(key,model,"hello",ref,1,file,new AtomicBoolean(),m -> {}));
        check(!file.exists());
        Fake corrupt=new Fake(); corrupt.audio=new byte[128];
        fail(() -> new FishClient(() -> corrupt).generate(key,model,"hello",ref,1,file,new AtomicBoolean(),m -> {}));
        check(!file.exists());
        AtomicBoolean cancel=new AtomicBoolean(true); final int[] opened={0};
        fail(() -> new FishClient(() -> { opened[0]++; return success; }).generate(key,model,"hello",ref,1,file,cancel,m -> {}));
        check(opened[0]==0 && !file.exists());
        cancel.set(false); Fake interrupted=new Fake(); interrupted.cancelDuringRead=cancel;
        fail(() -> new FishClient(() -> interrupted).generate(key,model,"hello",ref,1,file,cancel,m -> {}));
        check(!file.exists() && interrupted.disconnected);
        check(FishClient.failure(new SocketTimeoutException()).contains("超时"));
        check(FishClient.failure(new UnknownHostException()).contains("网络"));
        Files.delete(dir);
        System.out.println("Fish mock transport: " + checks + " assertions passed; no live API calls.");
    }
}
