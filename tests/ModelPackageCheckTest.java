package com.voice.studio;

import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ModelPackageCheckTest {
    static int assertions;
    static void check(boolean ok) { assertions++; if (!ok) throw new AssertionError(); }
    static JSONObject catalog() throws Exception {
        return new JSONObject().put("modules", new JSONArray()
                .put(new JSONObject().put("code", "omni").put("name", "基础语音合成").put("files", new JSONArray()
                    .put(new JSONObject().put("name", "VoiceStudio-OmniVoice-Models.part01").put("bytes", 120))
                    .put(new JSONObject().put("name", "VoiceStudio-OmniVoice-Models.part02").put("bytes", 121))))
                .put(new JSONObject().put("code", "whisper").put("name", "语音识别").put("files", new JSONArray()
                    .put(new JSONObject().put("name", "VoiceStudio-whisper-Models.zip").put("bytes", 140)))));
    }
    static ModelPackageCheck.Selected s(String name, long size) { return new ModelPackageCheck.Selected(name, size); }
    static void reject(String module, String contains, ModelPackageCheck.Selected... selected) throws Exception {
        boolean failed = false;
        try { ModelPackageCheck.validate(catalog(), module, Arrays.asList(selected)); }
        catch (IOException e) { failed = true; check(e.getMessage().contains(contains)); }
        check(failed);
    }
    public static void main(String[] args) throws Exception {
        ModelPackageCheck.validate(catalog(), "omni", Arrays.asList(s("VoiceStudio-OmniVoice-Models.part02", 121), s("VoiceStudio-OmniVoice-Models.part01", 120))); check(true);
        ModelPackageCheck.validate(catalog(), "omni", Arrays.asList(s("VoiceStudio-OmniVoice-Models (1).part01", 120), s("VoiceStudio-OmniVoice-Models.part02 (2)", 121))); check(true);
        ModelPackageCheck.validate(catalog(), "omni", Arrays.asList(s("voicestudio-omnivoice-models.part01", -1), s("VoiceStudio-OmniVoice-Models.part02", -1))); check(true);
        reject("omni", "part02", s("VoiceStudio-OmniVoice-Models.part01", 120));
        reject("omni", "语音识别", s("VoiceStudio-whisper-Models.zip", 140));
        reject("omni", "重复", s("VoiceStudio-OmniVoice-Models.part01", 120), s("VoiceStudio-OmniVoice-Models.part01 (1)", 120));
        reject("omni", "120", s("VoiceStudio-OmniVoice-Models.part01", 119), s("VoiceStudio-OmniVoice-Models.part02", 121));
        reject("omni", "文件名", s("another.part01", 120), s("another.part02", 121));
        reject("omni", "文件名", s("VoiceStudio-OmniVoice-Models.part01", 120), s("VoiceStudio-OmniVoice-Models.part02", 121), s("VoiceStudio-OmniVoice-Models.part03", 1));
        reject("omni", "无法读取", s(null, -1));
        reject("omni", "请选择");
        reject("whisper", "140", s("VoiceStudio-whisper-Models.zip", 139));
        reject("whisper", "基础语音合成", s("VoiceStudio-OmniVoice-Models.part01", 120));
        reject("omni", "ZIP 为空", s("custom.zip", 0));
        ModelPackageCheck.validate(catalog(), "omni", Collections.singletonList(s("user-created.zip", -1))); check(true);
        ModelPackageCheck.validate(catalog(), "whisper", Collections.singletonList(s("VoiceStudio-whisper-Models (1).zip", 140))); check(true);
        reject("unknown", "不受支持", s("a.zip", 1));
        Path assets = Paths.get(args[0]);
        JSONObject live = new JSONObject(new String(Files.readAllBytes(assets.resolve("model-downloads.json")), "UTF-8"));
        JSONArray modules = live.getJSONArray("modules"); check(modules.length() == 6);
        int volumeCount = 0;
        for (int i = 0; i < modules.length(); i++) {
            JSONObject module = modules.getJSONObject(i); String code = module.getString("code");
            JSONArray parts = module.getJSONArray("files"); volumeCount += parts.length();
            List<ModelPackageCheck.Selected> metadata = new ArrayList<>();
            for (int j = 0; j < parts.length(); j++) metadata.add(s(parts.getJSONObject(j).getString("name"), parts.getJSONObject(j).getLong("bytes")));
            Collections.reverse(metadata); ModelPackageCheck.validate(live, code, metadata); check(true);
            JSONObject manifest = new JSONObject(new String(Files.readAllBytes(assets.resolve(code + "-model-manifest.json")), "UTF-8"));
            JSONArray files = manifest.getJSONArray("files"); long total = 0;
            for (int j = 0; j < files.length(); j++) total += files.getJSONObject(j).getLong("size");
            check(total == module.getLong("installedBytes"));
        }
        check(volumeCount == 10);
        System.out.println(assertions + " preflight assertions passed: missing/mixed/duplicate/incomplete parts, ZIP compatibility and six-module catalog.");
    }
}
