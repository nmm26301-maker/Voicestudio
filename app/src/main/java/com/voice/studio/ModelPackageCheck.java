package com.voice.studio;

import java.io.IOException;
import java.util.*;
import org.json.*;

/** Metadata preflight; payload hashes remain authoritative at installation. */
final class ModelPackageCheck {
    static final class Selected {
        final String name; final long size;
        Selected(String name, long size) { this.name = name; this.size = size; }
    }
    static String name(String value) {
        return value == null ? "" : value.replaceAll("\\s*\\(\\d+\\)", "").trim().toLowerCase(Locale.ROOT);
    }
    static JSONObject module(JSONObject catalog, String code) throws Exception {
        JSONArray modules = catalog.getJSONArray("modules");
        for (int i = 0; i < modules.length(); i++)
            if (code.equals(modules.getJSONObject(i).getString("code"))) return modules.getJSONObject(i);
        throw new IOException("模型模块不受支持");
    }
    static void validate(JSONObject catalog, String code, List<Selected> selected) throws Exception {
        JSONObject module = module(catalog, code);
        if (selected.isEmpty()) throw new IOException("请选择模型文件");
        JSONArray files = module.getJSONArray("files");
        Map<String, JSONObject> expected = new LinkedHashMap<>();
        for (int i = 0; i < files.length(); i++) {
            JSONObject f = files.getJSONObject(i); expected.put(name(f.getString("name")), f);
        }
        Map<String, Selected> actual = new LinkedHashMap<>();
        for (Selected s : selected) {
            String key = name(s.name);
            if (key.isEmpty()) throw new IOException("无法读取文件名，请先将模型下载到手机");
            if (actual.put(key, s) != null) throw new IOException("重复选择文件：" + s.name + "，每份只选一次");
            JSONArray modules = catalog.getJSONArray("modules");
            for (int i = 0; i < modules.length(); i++) {
                JSONObject other = modules.getJSONObject(i);
                if (code.equals(other.getString("code"))) continue;
                JSONArray others = other.getJSONArray("files");
                for (int j = 0; j < others.length(); j++)
                    if (key.equals(name(others.getJSONObject(j).getString("name"))))
                        throw new IOException("选中了“" + other.getString("name") + "”文件，请为“" + module.getString("name") + "”选择对应模型");
            }
        }
        // A user-created complete ZIP is still supported; verify its payload during extraction.
        if (selected.size() == 1 && name(selected.get(0).name).endsWith(".zip")) {
            JSONObject known = expected.get(name(selected.get(0).name));
            if (known != null) size(selected.get(0), known);
            if (selected.get(0).size == 0) throw new IOException("模型 ZIP 为空，请重新下载");
            return;
        }
        for (String key : actual.keySet()) if (!expected.containsKey(key))
            throw new IOException("文件名不匹配：" + actual.get(key).name + "。请保留下载时的原文件名，或选择完整 ZIP");
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, JSONObject> f : expected.entrySet())
            if (!actual.containsKey(f.getKey())) missing.add(f.getValue().getString("name"));
        if (!missing.isEmpty()) throw new IOException("缺少 " + missing.size() + " 个文件：\n" + join(missing) + "\n请一次选中这个模块的全部文件");
        for (Map.Entry<String, JSONObject> f : expected.entrySet()) size(actual.get(f.getKey()), f.getValue());
    }
    static void size(Selected selected, JSONObject expected) throws Exception {
        if (selected.size >= 0 && selected.size != expected.getLong("bytes"))
            throw new IOException("文件未下载完整或版本不符：" + selected.name + "\n需要 " + expected.getLong("bytes") + " 字节，当前 " + selected.size + " 字节。请重新下载该文件");
    }
    static String join(List<String> names) {
        StringBuilder result = new StringBuilder();
        for (String s : names) { if (result.length() > 0) result.append('\n'); result.append(s); }
        return result.toString();
    }
}
