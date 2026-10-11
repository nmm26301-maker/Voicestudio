package com.voice.studio;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Browser downloads and module-specific installation, with explicit installed state. */
public class ModelDownloadsActivity extends Activity {
    ModelViews views; JSONArray modules; LinearLayout list; TextView summary;
    int filter;
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        try {
            modules = ModelImports.catalog(this).getJSONArray("modules");
            filter = saved == null ? 0 : saved.getInt("filter", 0);
            String focus = getIntent().getStringExtra("module");
            if (saved == null && focus != null)
                for (int i = 0; i < modules.length(); i++) if (focus.equals(modules.getJSONObject(i).getString("code"))) filter = i + 2;
            if (filter < 0 || filter >= modules.length() + 2) filter = 0;
            views = new ModelViews(this);
            views.action(views.root, "‹ 返回 VoiceStudio", false, this::finish);
            views.root.addView(CosmicUi.hero(this,"模型下载与安装","按需下载，安装后即可离线使用。","模型空间",CosmicUi.PURPLE));
            summary = views.label(views.root, "", 14, ModelViews.MUTED, false);
            List<String> names = new ArrayList<>(Arrays.asList("全部模型", "尚未安装"));
            for (int i = 0; i < modules.length(); i++) names.add(modules.getJSONObject(i).getString("name"));
            Spinner spinner = new Spinner(this);
            spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
            views.root.addView(spinner); spinner.setSelection(filter);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { filter = position; refresh(); }
                public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            views.action(views.root, "下载与安装说明", false, this::instructions);
            list = new LinearLayout(this); list.setOrientation(1); views.root.addView(list);
            refresh();
        } catch (Exception e) { Toast.makeText(this, "无法读取模型下载目录，请重新安装完整更新包", Toast.LENGTH_LONG).show(); finish(); }
    }
    void refresh() {
        if (list == null) return;
        int position = views.scroll.getScrollY(); list.removeAllViews();
        int installed = 0, shown = 0; long remaining = 0;
        for (int i = 0; i < modules.length(); i++) {
            try {
                JSONObject module = modules.getJSONObject(i);
                boolean ready = StudioFiles.ready(this, module.getString("code"));
                if (ready) installed++;
                else {
                    JSONArray files = module.getJSONArray("files");
                    for (int j = 0; j < files.length(); j++) remaining += files.getJSONObject(j).getLong("bytes");
                }
                if (filter == 1 && ready || filter >= 2 && filter - 2 != i) continue;
                card(module, ready); shown++;
            } catch (Exception e) { views.label(list, "模块配置不完整，请重新安装更新包", 14, ModelViews.INK, false); }
        }
        summary.setText("已安装 " + installed + " / " + modules.length() + " 类 · 未安装模块的下载包共 " + ModelViews.mib(remaining) + " MiB");
        if (shown == 0) views.label(list, "全部模型已安装，可回到创作或工作台使用。", 16, ModelViews.INK, true);
        views.scroll.post(() -> views.scroll.scrollTo(0, position));
    }
    void card(JSONObject module, boolean ready) throws Exception {
        LinearLayout box = views.card(); views.root.removeView(box); list.addView(box);
        String code = module.getString("code");
        views.label(box, module.getString("name"), 21, ModelViews.INK, true);
        views.label(box, (ready ? "已安装 · " : "待安装 · ") + ModelViews.mib(module.getLong("installedBytes")) + " MiB", 14,
                ready ? CosmicUi.SUCCESS : ModelViews.MUTED, true);
        views.label(box, module.getString("use"), 14, ModelViews.MUTED, false);
        JSONArray files = module.getJSONArray("files");
        views.label(box, files.length() == 1 ? "下载完整 ZIP 后直接选择安装" : "需要 " + files.length() + " 份分包，全部下载后一起选择安装", 13, ModelViews.MUTED, true);
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String name = file.getString("name"), url = file.getString("url");
            views.label(box, name, 12, ModelViews.MUTED, false);
            views.action(box, "下载 " + (files.length() == 1 ? "完整 ZIP" : "分包 " + (i + 1) + " / " + files.length())
                    + " · " + ModelViews.mib(file.getLong("bytes")) + " MiB", !ready, () -> download(url));
        }
        views.action(box, ready ? "查看安装与完整性" : "选择已下载文件并安装", true,
                () -> startActivity(new Intent(this, ModelInstallActivity.class).putExtra("module", code)));
        views.action(box, "复制本模块下载地址", false, () -> {
            try {
                StringBuilder links = new StringBuilder(module.getString("name")).append('\n');
                for (int i = 0; i < files.length(); i++) links.append(files.getJSONObject(i).getString("name")).append('\n')
                        .append(files.getJSONObject(i).getString("url")).append('\n');
                copy(links.toString());
            } catch (Exception e) { toast("无法读取下载地址"); }
        });
    }
    void instructions() {
        new CosmicDialogBuilder(this).setTitle("下载后安装")
                .setMessage("1. 点击每个下载文件，浏览器显示下载进度。\n2. 保留文件名，无需解压或拼接。\n3. 打开安装页，一次选中同一模块的全部分包。\n\n下载页可能要求浏览器登录保存模型的 ChatGPT 账号。若无权限或链接失效，也可以从对话中的模型附件下载。\n\n完整模型约 2.25 GiB；考虑下载包和临时安装文件，建议预留 6 GB。模型内容安装时会核对大小与 SHA-256。")
                .setPositiveButton("知道了", null).show();
    }
    void download(String url) {
        Uri uri = Uri.parse(url);
        if (!"https".equals(uri.getScheme()) || !"chatgpt.com".equals(uri.getHost())) { toast("下载地址不完整，请更新应用"); return; }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception e) {
            new CosmicDialogBuilder(this).setTitle("无法打开浏览器下载页")
                    .setMessage("可以复制地址在浏览器打开，也可从对话中的模型附件下载。")
                    .setNegativeButton("关闭", null).setPositiveButton("复制地址", (d, n) -> copy(url)).show();
        }
    }
    void copy(String text) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("VoiceStudio 模型下载", text)); toast("下载地址已复制");
    }
    void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    protected void onResume() { super.onResume(); if (list != null) refresh(); }
    protected void onSaveInstanceState(Bundle saved) { saved.putInt("filter", filter); super.onSaveInstanceState(saved); }
}
