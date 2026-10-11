package com.voice.studio;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One application-owned import; activity recreation does not restart the work. */
final class ModelImports {
    static final class Snapshot {
        final String module, phase, message;
        final long done, total;
        Snapshot(String module, String phase, String message, long done, long total) {
            this.module = module; this.phase = phase; this.message = message; this.done = done; this.total = total;
        }
        boolean running() { return phase.equals("running"); }
    }
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static volatile Snapshot state = new Snapshot("", "idle", "请选择下载到手机的模型文件", 0, 0);
    private static AtomicBoolean cancellation = new AtomicBoolean();
    static Snapshot snapshot() { return state; }

    static JSONObject catalog(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("model-downloads.json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    static synchronized void start(Context owner, String module, List<Uri> selected) throws Exception {
        Context context = owner.getApplicationContext();
        ModelPackageCheck.module(catalog(context), module);
        if (selected.isEmpty()) throw new IOException("请选择模型文件");
        if (state.running()) throw new IOException("正在安装其他模型，请等待或取消当前安装");
        if (StudioFiles.ready(context, module)) throw new IOException("模型已经安装；如需修复，请先检查完整性");
        if (!StudioFiles.beginModelWork(context)) throw new IOException("请先暂停队列，并等待任务或其他模型操作完成");
        AtomicBoolean cancel = new AtomicBoolean(); cancellation = cancel;
        final ArrayList<Uri> uris = new ArrayList<>(selected);
        state = new Snapshot(module, "running", "检查分包与剩余空间…", 0, 0);
        try {
            journal(context, module, "running", state.message);
            worker.execute(() -> run(context, module, uris, cancel));
        } catch (Exception e) {
            StudioFiles.endModelWork();
            state = new Snapshot(module, "failed", "无法启动安装，请重试", 0, 0);
            throw e;
        }
    }
    private static void run(Context context, String module, List<Uri> uris, AtomicBoolean cancel) {
        String phase = "failed", result = "安装未完成";
        try {
            JSONObject catalog = catalog(context), manifest = StudioFiles.manifest(context, module);
            List<ModelPackageCheck.Selected> metadata = new ArrayList<>();
            List<ModelParts.Source> parts = new ArrayList<>();
            for (Uri uri : uris) {
                OmniModels.checkCancel(cancel::get);
                String filename = null; long bytes = -1;
                try (Cursor cursor = context.getContentResolver().query(uri,
                        new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (nameIndex >= 0 && !cursor.isNull(nameIndex)) filename = cursor.getString(nameIndex);
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) bytes = cursor.getLong(sizeIndex);
                    }
                }
                metadata.add(new ModelPackageCheck.Selected(filename, bytes));
                final String partName = filename;
                parts.add(new ModelParts.Source() {
                    public String name() { return partName; }
                    public InputStream open() throws Exception {
                        OmniModels.checkCancel(cancel::get);
                        InputStream in = context.getContentResolver().openInputStream(uri);
                        if (in == null) throw new IOException("无法读取模型文件，请重新选择手机中的文件");
                        return in;
                    }
                });
            }
            ModelPackageCheck.validate(catalog, module, metadata);
            JSONArray entries = manifest.getJSONArray("files"); long total = 0;
            for (int i = 0; i < entries.length(); i++) total += entries.getJSONObject(i).getLong("size");
            long required = total + 64 * 1024 * 1024L;
            if (context.getFilesDir().getUsableSpace() < required)
                throw new IOException("手机空间不足，安装还需至少 " + ((required + 1048575) / 1048576) + " MiB 空间。清理后重新选择文件");
            OmniModels.checkCancel(cancel::get);
            try (InputStream input = ModelParts.open(parts)) {
                OmniModels.install(input, new File(context.getFilesDir(), module + "-model"), manifest,
                        (done, size, message) -> state = new Snapshot(module, "running", message, done, size), cancel::get);
            }
            phase = "done"; result = "安装完成 · 文件大小与 SHA-256 校验通过";
        } catch (Exception e) {
            if (StudioFiles.ready(context, module)) { phase = "done"; result = "模型已安装，可以使用"; }
            else if (cancel.get()) { phase = "cancelled"; result = "安装已取消，临时文件已清理。下载文件和已有模型保留"; }
            else result = e.getMessage() == null ? "安装失败，请重新选择手机中的完整模型文件" : e.getMessage();
        } finally {
            synchronized (ModelImports.class) {
                try { journal(context, module, phase, result); } catch (Exception ignored) { }
                StudioFiles.endModelWork();
                Snapshot old = state;
                state = new Snapshot(module, phase, result, phase.equals("done") ? old.total : old.done, old.total);
            }
        }
    }
    static synchronized void cancel(String module) {
        if (state.running() && state.module.equals(module)) cancellation.set(true);
    }
    static boolean cancelling() { return state.running() && cancellation.get(); }
    private static void journal(Context context, String module, String phase, String message) throws Exception {
        StudioFiles.save(new File(context.getFilesDir(), "model-install-status.json"),
                new JSONObject().put("module", module).put("phase", phase).put("message", message));
    }
    static void recover(Context context) {
        File file = new File(context.getFilesDir(), "model-install-status.json");
        if (!file.exists()) return;
        try {
            JSONObject stored = StudioFiles.read(file); String module = stored.getString("module");
            ModelPackageCheck.module(catalog(context), module);
            String phase = stored.getString("phase"), message = stored.getString("message");
            if (phase.equals("running")) {
                boolean ready = StudioFiles.ready(context, module);
                phase = ready ? "done" : "interrupted";
                message = ready ? "模型已安装，可以使用" : "上次安装被中断，临时文件已清理。请重新选择完整模型文件";
                journal(context, module, phase, message);
            }
            state = new Snapshot(module, phase, message, 0, 0);
        } catch (Exception ignored) { }
    }
}
