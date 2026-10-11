package com.voice.studio;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.WindowManager;
import android.widget.*;
import java.util.*;
import org.json.*;

public class ModelInstallActivity extends Activity {
    String module, title;
    String startError = "";
    long requiredSpace;
    ModelViews views;
    TextView message, amount, space;
    ProgressBar progress;
    Button choose, cancel, use, verify;
    final Handler ui = new Handler(Looper.getMainLooper());
    boolean visible, gone;
    final Runnable poll = new Runnable() {
        public void run() { if (visible && !gone) { update(); ui.postDelayed(this, 400); } }
    };
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        module = getIntent().getStringExtra("module");
        try {
            JSONObject info = ModelPackageCheck.module(ModelImports.catalog(this), module);
            title = info.getString("name");
            JSONArray payloads = StudioFiles.manifest(this, module).getJSONArray("files");
            requiredSpace = 67108864;
            for (int i = 0; i < payloads.length(); i++) requiredSpace += payloads.getJSONObject(i).getLong("size");
            views = new ModelViews(this);
            views.action(views.root, "‹ 返回", false, this::onBackPressed);
            views.root.addView(CosmicUi.hero(this,"安装"+title,"核对文件与空间，安全保存模型。","模型安装舱",CosmicUi.CYAN));
            views.label(views.root, "选择完整 ZIP，或同时选择全部分包。", 14, ModelViews.MUTED, false);
            LinearLayout status = views.card();
            message = views.label(status, "准备安装", 16, ModelViews.INK, true);
            progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100); status.addView(progress, new LinearLayout.LayoutParams(-1, views.dp(20)));
            amount = views.label(status, "", 13, ModelViews.MUTED, false);
            space = views.label(status, "", 13, ModelViews.MUTED, false);
            choose = views.action(status, "选择模型文件并安装", true, this::pick);
            cancel = views.action(status, "取消安装", false, () -> {
                ModelImports.cancel(module); update();
            });
            use = views.action(status, "使用这个模型", true, this::useModel);
            views.action(status, "下载对应模型", false, () -> startActivity(new Intent(this, ModelDownloadsActivity.class).putExtra("module", module)));
            verify = views.action(status, "检查已安装模型", false, () -> startActivity(new Intent(this, WorkspaceActivity.class).putExtra("inspectModule", module)));
            LinearLayout files = views.card();
            views.label(files, "需要选择的下载文件", 19, ModelViews.INK, true);
            JSONArray list = info.getJSONArray("files");
            for (int i = 0; i < list.length(); i++) {
                JSONObject f = list.getJSONObject(i);
                views.label(files, f.getString("name") + "\n" + ModelViews.mib(f.getLong("bytes")) + " MiB", 13, ModelViews.MUTED, false);
            }
            views.label(files, "文件选择后先检查分包数量、文件名和已知大小，再校验模型内容。无需解压或拼接。", 13, ModelViews.MUTED, false);
            views.label(files, "安装时页面会保持亮屏。可取消并重选文件；系统结束进程后需重新选择文件安装。", 13, ModelViews.MUTED, false);
            ArrayList<Uri> selected = getIntent().getParcelableArrayListExtra("modelUris");
            getIntent().removeExtra("modelUris");
            if (saved == null && selected != null && !selected.isEmpty()) start(selected);
            update();
        } catch (Exception e) { toast("无法打开安装页：" + e.getMessage()); finish(); }
    }
    void pick() {
        if (ModelImports.snapshot().running()) { toast("请等待当前安装完成或取消"); return; }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try { startActivityForResult(intent, 91); }
        catch (ActivityNotFoundException e) { toast("没有可用的文件选择器，请安装或启用手机文件管理器"); }
    }
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 91 || result != RESULT_OK || data == null) return;
        ArrayList<Uri> selected = new ArrayList<>();
        if (data.getClipData() != null)
            for (int i = 0; i < data.getClipData().getItemCount(); i++) selected.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) selected.add(data.getData());
        for (Uri uri : selected) try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        start(selected);
    }
    void start(List<Uri> selected) {
        startError = "";
        try { ModelImports.start(this, module, selected); }
        catch (Exception e) { startError = e.getMessage() == null ? "无法启动安装，请重试" : e.getMessage(); toast(startError); }
        update();
    }
    void update() {
        if (views == null || message == null) return;
        ModelImports.Snapshot s = ModelImports.snapshot();
        boolean mine = module.equals(s.module), active = mine && s.running();
        boolean ready = !s.running() && StudioFiles.ready(this, module);
        if (!startError.isEmpty()) message.setText(startError);
        else if (s.running() && !mine) message.setText("正在安装其他模块，请等待当前安装完成");
        else if (mine && !s.phase.equals("idle")) message.setText(ModelImports.cancelling() ? "正在取消，等待当前文件读取结束…" : s.message);
        else if (ready) message.setText("模型已安装，可以使用");
        else if (message.getText().length() == 0 || message.getText().toString().equals("准备安装")) message.setText("请选择下载到手机的模型文件");
        progress.setIndeterminate(active && s.total == 0);
        int percent = active && s.total > 0 ? (int)Math.min(99, s.done * 100 / s.total) : ready ? 100 : 0;
        progress.setProgress(percent);
        amount.setText(active && s.total > 0 ? percent + "% · " + ModelViews.mib(s.done) + " / " + ModelViews.mib(s.total) + " MiB" : ready ? "模型已启用" : "");
        space.setText("当前可用 " + ModelViews.mib(getFilesDir().getUsableSpace()) + " MiB" + (ready ? "" : " · 安装前需至少 " + ModelViews.mib(requiredSpace) + " MiB"));
        choose.setEnabled(!s.running() && !ready); choose.setText(mine && !s.running() && !ready ? "重新选择模型文件" : "选择模型文件并安装");
        cancel.setVisibility(active ? android.view.View.VISIBLE : android.view.View.GONE);
        cancel.setEnabled(!ModelImports.cancelling()); use.setVisibility(ready ? android.view.View.VISIBLE : android.view.View.GONE);
        verify.setEnabled(ready);
        use.setText(module.equals("clone") && !StudioFiles.ready(this, "omni") ? "继续安装基础合成模型"
                : module.equals("clone") ? "去创建参考音色" : module.equals("omni") ? "去创作" : "打开对应工具");
        if (active) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    void useModel() {
        if (module.equals("clone") && !StudioFiles.ready(this, "omni"))
            startActivity(new Intent(this, ModelInstallActivity.class).putExtra("module", "omni"));
        else if (module.equals("omni"))
            startActivity(new Intent(this, MainActivity.class).setAction("com.voice.studio.SELECT_ENGINE")
                    .putExtra("engine", "omni").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        else startActivity(new Intent(this, WorkspaceActivity.class).putExtra("section",
                module.equals("clone") ? "音色" : module.equals("translation") ? "翻译"
                        : module.equals("separation") ? "音频" : module.equals("speaker") ? "任务" : "转写"));
        finish();
    }
    public void onBackPressed() {
        ModelImports.Snapshot s = ModelImports.snapshot();
        if (s.running() && module.equals(s.module)) {
            new CosmicDialogBuilder(this).setTitle("安装正在进行")
                    .setMessage("退出会取消本次安装，已经下载的模型文件会保留。")
                    .setNegativeButton("继续安装", null)
                    .setPositiveButton("取消并返回", (d, n) -> { ModelImports.cancel(module); finish(); }).show();
        } else super.onBackPressed();
    }
    protected void onResume() { super.onResume(); visible = true; ui.removeCallbacks(poll); ui.post(poll); }
    protected void onPause() { visible = false; ui.removeCallbacks(poll); super.onPause(); }
    protected void onDestroy() { gone = true; ui.removeCallbacksAndMessages(null); super.onDestroy(); }
    void toast(String s) { Toast.makeText(this, s == null ? "请重试" : s, Toast.LENGTH_LONG).show(); }
}
