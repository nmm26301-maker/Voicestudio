package com.voice.studio;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class WorkspaceActivity extends Activity {
    LinearLayout root, tasks, shell, bottomNav;
    final Map<String, View> sections = new LinkedHashMap<>();
    final Map<String, TextView> sectionButtons = new LinkedHashMap<>();
    final Map<TextView, String[]> requirements = new LinkedHashMap<>();
    String section = "音色", pendingResume = "";
    TextView recordingStatus;
    VoiceCapturePanel voiceCapture;
    final Runnable recordingPoll = new Runnable() {
        public void run() {
            if (gone || !visible) return;
            if (recordingStatus != null) recordingStatus.setText(StudioService.recording.isEmpty()
                    ? "未在录音 · 开始后点击“结束录音并转写”" : "正在录音 · 完成后点击下方结束按钮");
            ui.postDelayed(this, 500);
        }
    };
    ScrollView scroll;
    TextView status;
    final Handler ui = new Handler();
    final ExecutorService files = Executors.newSingleThreadExecutor();
    String last = "";
    File export;
    AudioPreview preview;
    boolean gone, importing, visible, loadingTasks;
    int taskLimit = 50, taskFilter, taskHeaderCount;
    JSONArray taskSnapshot = new JSONArray();
    TextView taskSummary;
    final Runnable poll = new Runnable() {
        public void run() {
            if (gone || !visible || loadingTasks) return;
            loadingTasks = true;
            files.execute(() -> {
                JSONArray snapshot = StudioFiles.list(WorkspaceActivity.this);
                String now = snapshot.toString();
                ui.post(() -> {
                    loadingTasks = false;
                    if (gone || !visible) return;
                    if (!now.equals(last)) {
                        last = now; taskSnapshot = snapshot; showTasks();
                    }
                    ui.removeCallbacks(poll); ui.postDelayed(poll, 1000);
                });
            });
        }
    };

    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        section = saved == null ? getIntent().getStringExtra("section") : saved.getString("section", "音色");
        if (section == null) section = "音色";
        if (saved != null) {
            pendingResume = saved.getString("pendingResume", "");
            taskLimit = saved.getInt("taskLimit", 50);
            taskFilter = Math.max(0, Math.min(4, saved.getInt("taskFilter", 0)));
            // Persist the exact chosen source while Android's document picker is open.
            // Resolve only two trusted internal directories; never restore an arbitrary path.
            String name=saved.getString("exportFile","");
            String directory=saved.getString("exportDirectory","audio");
            if (name.matches("[a-f0-9-]+\\.(mp3|wav|mp4|zip|txt|srt|json)")
                    && (directory.equals("audio") || directory.equals("profiles"))) {
                File file=new File(StudioFiles.dir(this,directory),name);
                if(file.isFile())export=file;
            }
        }
        CosmicUi.install(this);
        scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        scroll.addView(root);
        shell = new LinearLayout(this); shell.setOrientation(1); shell.setBackground(new DreamBackdropDrawable(getResources().getDisplayMetrics().density));
        shell.setPadding(dp(12), dp(8), dp(12), 0);
        LinearLayout toolbar = new LinearLayout(this); toolbar.setOrientation(0);
        TextView back = CosmicUi.label(this, "‹ 返回", 14, CosmicUi.CYAN, true);
        back.setPadding(dp(12), dp(12), dp(16), dp(12)); CosmicUi.press(back);
        back.setOnClickListener(v -> finish()); toolbar.addView(back);
        TextView heading = CosmicUi.label(this, "声音实验室", 22, CosmicUi.INK, true);
        heading.setGravity(Gravity.CENTER_VERTICAL); toolbar.addView(heading, new LinearLayout.LayoutParams(0, dp(48), 1));
        shell.addView(toolbar); quickSections();
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        bottomNav=CosmicUi.navigation(this,"任务",this::navigate);shell.addView(bottomNav);setContentView(shell);
        LinearLayout voice=sectionCard("音色","把你的声音，装进星星里","清晰录制 3—10 秒，保存后可用云端引擎朗读新的文字。");
        voice.addView(CosmicUi.hero(this,"声音小宇宙","录音保存在手机，生成时才上传。","VOICE LAB",CosmicUi.PINK));
        voiceCapture=new VoiceCapturePanel(this,voice);
        button(voice,"选择 / 删除已保存音色",this::profiles);
        button(voice,"✦ 免电脑免费克隆 · 官方 Qwen / IndexTTS",this::openFreeDemo);
        button(voice,"配置云端合成引擎",()->startActivity(new Intent(this,MainActivity.class).setAction("com.voice.studio.SELECT_ENGINE").putExtra("engine","accelerated")));
        button(voice,"♫ 进入文字合成工作台",()->jumpSection("合成"));
        LinearLayout compose=sectionCard("合成","文字合成工作台","录音 / 音色 → 选择引擎 → 输入文字 → 合成 → 保存作品。");
        compose.addView(CosmicUi.workflow(this,2));
        button(compose,"♫ 打开合成工作台",()->openMainPage("合成"));
        label(compose,"草稿、引擎和任务共用同一个工作台，切换页面会保存文字。",12,false);
        LinearLayout bulk=sectionCard("批量","云端批量朗读","每行一个任务；自动分段保存完整声音。批量可能消耗第三方 API 额度，请先确认配置和费用。");
        button(bulk,"输入批量文字并提交",this::batch);
        tasks=sectionCard("任务","生成任务与结果","查看任务状态、失败详情与旧版作品。后台不会加载已停用的本地语音模型。");
        button(tasks,"暂停整个队列",()->startService(new Intent(this,StudioService.class).setAction("cancel")));
        button(tasks,"清理旧离线模型（保留所有录音和作品）",this::clearLegacyModels);
        label(tasks,"清理只涉及模型目录。升级不会自动删除历史作品、参考音色或录音。",12,false);

        taskSummary = label(tasks, "正在读取任务…", 13, false);
        taskHeaderCount = tasks.getChildCount();
        if (getIntent().getBooleanExtra("openModels", false)) {
            section = "任务"; getIntent().removeExtra("openModels");
        }
        String inspectModule = getIntent().getStringExtra("inspectModule");
        String importModule = getIntent().getStringExtra("importModule");
        getIntent().removeExtra("inspectModule"); getIntent().removeExtra("importModule");
        if (inspectModule != null || importModule != null) section = "任务";

        jumpSection(sections.containsKey(section) ? section : "音色");
        if (saved == null && getIntent().getBooleanExtra("chooseReferenceForFree",false)) {
            getIntent().removeExtra("chooseReferenceForFree");
            ui.post(this::profiles);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 73);
    }

    JSONObject speechJob(String text) throws Exception {
        android.content.SharedPreferences p=getSharedPreferences("studio",0);
        String engine=p.getString("engine","fish");
        if(engine.equals("fish")){
            String voice=p.getString("voiceId","");
            if(voice.isEmpty())throw new IOException("请先在 Fish Audio 设置 Voice ID");
            return new JSONObject().put("type","tts").put("engine","fish").put("text",text)
                .put("reference",voice).put("model",p.getString("model","s2.1-pro-free"))
                .put("speed",p.getFloat("speed",1)).put("voice",p.getString("voiceName","Fish 音色"));
        }
        if(!engine.equals("accelerated"))throw new IOException("请先在首页选择并配置云端引擎");
        String url=p.getString("accelerated-url","");
        if(url.isEmpty())throw new IOException("请先填写云端网关地址");
        boolean gradio="gradio".equals(p.getString("accelerated-protocol","rest"));
        String protocol=p.getString("accelerated-protocol","rest");
        if(protocol.equals("official"))CloudConnectionPolicy.validate(p.getString("accelerated-model","auto"),url);else if(gradio)GradioSpeechClient.base(url);else CloudConnectionPolicy.gateway(url);
        String model=p.getString("accelerated-model","auto");
        String voice=p.getString("accelerated-voice","auto");
        AcceleratedSpeechClient.request(text,model,voice,p.getString("omniLang","zh"),
            p.getFloat("speed",1),p.getInt("accelerated-steps",32),p.getLong("voice-seed",42));
        return new JSONObject().put("type","tts").put("engine","accelerated")
            .put("text",text).put("endpoint",url)
            .put("provider",p.getString("accelerated-provider","custom"))
            .put("protocol",protocol)
            .put("profile",gradio?p.getString("gpu-hf-profile",""):p.getString("cloud-profile",""))
            .put("language",p.getString("omniLang","zh"))
            .put("languageName","Auto")
            .put("model",model).put("apiVoice",voice)
            .put("speed",p.getFloat("speed",1)).put("steps",p.getInt("accelerated-steps",32))
            .put("seed",p.getLong("voice-seed",42)).put("voice",voice);
    }

    void batch() {
        EditText text = new EditText(this);
        text.setMinLines(5);
        text.setHint("每行一个任务");
        new CosmicDialogBuilder(this).submit("加入队列", () -> {
            String[] lines = text.getText().toString().split("\\r?\\n");
            if (lines.length > 100) throw new IOException("一次最多 100 条");
            List<JSONObject> jobs = new ArrayList<>();
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                if (line.length() > 12000)
                    throw new IOException("某行超过 12000 字");
                jobs.add(speechJob(line.trim()));
            }
            if (jobs.isEmpty()) throw new IOException("请先输入文字");
            for (int at = 0; at < jobs.size(); at++) {
                StudioService.enqueue(this, jobs.get(at));
                StringBuilder remaining = new StringBuilder();
                for (int next = at + 1; next < jobs.size(); next++) remaining.append(jobs.get(next).getString("text")).append('\n');
                text.setText(remaining.toString());
            }
            toast("已加入 " + jobs.size() + " 个任务");
                })
                .setTitle("批量生成")
                .setView(text)
                .setNegativeButton("取消", null)

                .show();
    }

    void pick(int code, String mime, boolean multi) {
        for (int n = 0; n < MODULE_REQUESTS.length; n++) if (code == MODULE_REQUESTS[n]) {
            startActivity(new Intent(this, ModelInstallActivity.class).putExtra("module", MODULE_CODES[n]));
            return;
        }
        Intent i =
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(mime)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multi);
        try { startActivityForResult(i, code); }
        catch (ActivityNotFoundException e) { toast("没有可用的文件选择器，请启用手机文件管理器"); }
    }

    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 82) {
            if (android.provider.Settings.canDrawOverlays(this)) floatingDictation();
            return;
        }
        if (result != RESULT_OK || data == null) return;
        if (request == 74 && export != null) {
            File source = export;
            export = null;
            Uri target = data.getData();
            if (target == null) { toast("没有选择保存位置"); return; }
            files.execute(
                    () -> {
                        try (InputStream in = new FileInputStream(source);
                                OutputStream out = getContentResolver().openOutputStream(target, "wt")) {
                            if (out == null) throw new IOException("无法打开保存位置");
                            copy(in, out, Long.MAX_VALUE);
                            ui.post(() -> toast("已保存"));
                        } catch (Exception e) {
                            ui.post(() -> toast("保存失败：" + e.getMessage()));
                        }
                    });
            return;
        }
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null)
            for (int i = 0; i < data.getClipData().getItemCount(); i++)
                uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        for (Uri uri : uris)
            try {
                getContentResolver()
                        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
        if (request == 71 && !uris.isEmpty()) cloneDialog(uris.get(0));
        if (request == 90 && !uris.isEmpty()) extractVideoDialog(uris.get(0));
        if (request == 72) importModels(uris, "clone");
        if (request == 76) importModels(uris, "whisper");
        if (request == 78) importModels(uris, "translation");
        if (request == 80) importModels(uris, "separation");
        if (request == 81) importModels(uris, "speaker");
        if (request == 87) importModels(uris, "omni");
        if (request == 79 && !uris.isEmpty()) {
            try {
                if (!StudioFiles.ready(this, "separation")) throw new IOException("请先导入分离模型");
                StudioService.enqueue(
                        this,
                        new JSONObject()
                                .put("type", "separate")
                                .put("uri", uris.get(0).toString())
                                .put("title", "人声分离 · " + name(uris.get(0))));
                toast("人声分离已加入队列");
            } catch (Exception e) {
                toast(e.getMessage());
            }
        }
        if (request == 77 && !uris.isEmpty()) transcriptionDialog(uris.get(0));
        if (request == 75 && !uris.isEmpty()) {
            try {
                if (!StudioFiles.ready(this, "omni")) throw new IOException("请先导入基础模型");
                JSONObject book =
                        speechJob("")
                                .put("type", "book")
                                .put("title", "有声书 · " + name(uris.get(0)))
                                .put("uri", uris.get(0).toString());
                StudioService.enqueue(this, book);
                toast("有声书已加入队列");
            } catch (Exception e) {
                toast(e.getMessage());
            }
        }
    }

    void cloneDialog(Uri uri) {
        cloneDialog(uri, null);
    }

    void cloneDialog(Uri uri, JSONObject previous) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        box.setPadding(dp(20), dp(10), dp(20), 0);
        EditText name = field(box, "音色名称", previous == null ? "我的声音" : previous.optString("name", "我的声音") + " · 修正版"),
                start = field(box, "开始秒数", previous == null ? "0" : Double.toString(previous.optDouble("referenceStart", 0))),
                seconds = field(box, "参考秒数（3—10）", previous == null ? "6" : Double.toString(previous.optDouble("referenceSeconds", 6))),
                text = field(box, "对应文字（留空可自动离线转写）", previous == null ? "" : previous.optString("referenceText"));
        label(box, "这里是参考录音的内容，可留空自动识别；保存音色后，首页可输入任意新文字进行创作。", 12, false);
        text.setMinLines(3);
        new CosmicDialogBuilder(this).submit("创建", () -> {
            if (!StudioFiles.ready(this, "clone"))
                throw new IOException("请先导入克隆模型");
            double a = Double.parseDouble(start.getText().toString()),
                    b = Double.parseDouble(seconds.getText().toString());
            if (!Double.isFinite(a)
                    || !Double.isFinite(b)
                    || a < 0
                    || b < 3
                    || b > 10) throw new IOException("参考时间无效");
            String transcript = text.getText().toString().trim();
            if (transcript.length() > 2000)
                throw new IOException("对应文字最多 2000 字");
            if (transcript.isEmpty() && !StudioFiles.ready(this, "whisper"))
                throw new IOException("对应文字留空时需要导入识别模型");
            StudioService.enqueue(
                    this,
                    new JSONObject()
                            .put("type", "clone")
                            .put("uri", uri.toString())
                            .put("start", a)
                            .put("seconds", b)
                            .put("text", transcript)
                            .put("name", name.getText().toString())
                            .put("title", "创建音色 · " + name.getText()));
            toast("音色任务已加入队列");
                })
                .setTitle("创建参考音色")
                .setView(box)
                .setNegativeButton("取消", null)

                .show();
    }

    void queueRecordedVoice(File audio,String title,String text) throws Exception {
        String name=title.trim(),transcript=text.trim();
        if(name.isEmpty()||name.length()>80)throw new IOException("请填写 1—80 字的人物名称");
        if(transcript.length()>2000)throw new IOException("参考录音文字最多 2000 字");
        double seconds;try(PcmAudio.Reader reader=new PcmAudio.Reader(audio)){seconds=reader.seconds();}
        if(seconds<3||seconds>10.01)throw new IOException("请录制 3—10 秒的清晰单人声音");
        String id=UUID.randomUUID().toString();File folder=StudioFiles.dir(this,"profiles");
        File target=new File(folder,id+".reference.wav"),pending=new File(folder,id+".pending.wav");
        try{
            try(InputStream in=new FileInputStream(audio);OutputStream out=new FileOutputStream(pending)){copy(in,out,5000000L);}
            if(!pending.renameTo(target))throw new IOException("保存参考录音失败");
            StudioFiles.save(new File(folder,id+".json"),new JSONObject().put("id",id).put("name",name)
                .put("referenceStart",0).put("referenceSeconds",seconds).put("referenceText",transcript).put("cloudReference",true));
            getSharedPreferences("studio",0).edit().putString("cloud-profile",id).putString("selected-profile",id).apply();
            toast("人物录音已保存。选择支持参考音频的云端引擎即可克隆音色，朗读新文字");
        }finally{pending.delete();}
    }

    void extractVideoDialog(Uri video) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(dp(20), dp(12), dp(20), dp(12));
        label(box, "提取音轨 → 识别对白 → 按声音分组 → 分人物试听与创建音色", 14, true);
        label(box, "需要识别与说话人模型。分组编号不是画面人物身份，请试听命名；背景音乐、多人同时讲话可能影响结果，可在字幕中手动校对。", 12, false);
        EditText count = field(box, "人物数量（已知填人数，未知填 0 自动）", "3");
        count.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new CosmicDialogBuilder(this).submit("提取人物声音", () -> {
            if (!StudioFiles.ready(this, "whisper") || !StudioFiles.ready(this, "speaker"))
                throw new IOException("请先安装识别与说话人分组模型");
            int value = Integer.parseInt(count.getText().toString());
            if (value < 0 || value > 32) throw new IOException("人物数量需为 0—32，0 为自动");
            StudioService.enqueue(this, new JSONObject().put("type", "extractVoices").put("uri", video.toString())
                    .put("speakerCount", value).put("threshold", .55).put("title", "人物声音 · " + name(video)));
            toast("视频人物提取已加入任务；完成后点击打开人物提取结果");
        }).setTitle("从视频提取人物声音").setView(box).setNegativeButton("取消", null).show();
    }

    void extractFromTranscript(JSONObject source) throws Exception {
        if (!source.has("segments")) throw new IOException("请先完成转写并校对字幕");
        String rawId = source.optString("sourceJob", source.getString("id"));
        File input = new File(new File(StudioFiles.dir(this, "jobs"), rawId), "input.wav");
        try (PcmAudio.Reader reader = new PcmAudio.Reader(input)) { SpeakerClips.plan(source.getJSONArray("segments"), reader.seconds()); }
        StudioService.enqueue(this, new JSONObject().put("type", "extractVoices").put("sourceJob", rawId)
                .put("segments", new JSONArray(source.getJSONArray("segments").toString()))
                .put("title", "校对后人物声音 · " + source.optString("title")));
        toast("已按校对后的人物编号提取；请在人物提取结果中查看");
    }

    void voiceExtractions() {
        files.execute(() -> {
            JSONArray list = StudioFiles.list(this); List<JSONObject> jobs = new ArrayList<>();
            for (int n = 0; n < list.length(); n++) {
                JSONObject j = list.optJSONObject(n);
                if (j != null && j.has("speakerResults") && j.optString("state").equals("done")) jobs.add(j);
            }
            ui.post(() -> {
                if (gone) return;
                if (jobs.isEmpty()) { toast("还没有完成的人物提取结果，请查看任务进度"); return; }
                String[] names = new String[jobs.size()];
                for (int n = 0; n < names.length; n++) names[n] = jobs.get(n).optString("title");
                new CosmicDialogBuilder(this).setTitle("人物提取结果").setItems(names, (d, n) -> speakerResults(jobs.get(n))).show();
            });
        });
    }

    void speakerResults(JSONObject job) {
        JSONArray results = job.optJSONArray("speakerResults");
        if (results == null || results.length() == 0) { toast("没有可用人物声音，请校对字幕与编号"); return; }
        LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(dp(16), dp(8), dp(16), dp(16));
        label(box, job.optInt("speakerCount") > 0 ? "按 " + job.optInt("speakerCount") + " 人统一分组 · 当前 " + results.length() + " 组" : "自动分组 · 当前 " + results.length() + " 组", 13, false);
        label(box, "同一人物的多段对白已合并，可试听命名。重叠对白需单独处理。", 12, false);
        if (job.optInt("shortSegments") > 0) label(box, "有 " + job.optInt("shortSegments") + " 段过短对白未分组，请在字幕中校对人物编号后重新提取。", 12, false);
        button(box, "按人物数量重新分组", () -> regroupVoices(job));
        button(box, "试听原始音轨", () -> {
            try {
                String sourceId = job.optString("sourceJob", job.optString("id"));
                previewInput(StudioFiles.read(StudioFiles.job(this, sourceId)));
            } catch (Exception e) { toast(e.getMessage()); }
        });
        for (int n = 0; n < results.length(); n++) {
            JSONObject person = results.optJSONObject(n); if (person == null) continue;
            String title = person.optString("name", "人物 " + person.optInt("speaker"));
            LinearLayout item = detachedCard(title, String.format(Locale.CHINA, "对白 %.1f 秒 · 参考片段 %.1f 秒", person.optDouble("seconds"), person.optDouble("referenceSeconds")));
            File audio = new File(StudioFiles.dir(this, "audio"), person.optString("file"));
            File ref = new File(StudioFiles.dir(this, "audio"), person.optString("referenceFile"));
            button(item, "试听人物对白", () -> preview(audio, title));
            button(item, "导出人物声音 WAV", () -> saveOutput(audio));
            if (person.optBoolean("canClone")) {
                button(item, "试听参考片段", () -> preview(ref, title + " · 参考"));
                button(item, "命名并创建人物音色", () -> speakerCloneDialog(job, person));
            } else label(item, "可导出对白；不足 3 秒，请补录此人物声音后创建音色。", 12, false);
            box.addView(item);
        }
        button(box, "校对字幕 / 修改人物编号", () -> {
            try {
                JSONObject source = StudioFiles.read(StudioFiles.job(this, job.getString("id")));
                editSegments(source);
            } catch (Exception e) { toast(e.getMessage()); }
        });
        button(box, "按最新校对结果重新提取", () -> {
            try {
                JSONObject source = StudioFiles.read(StudioFiles.job(this, job.getString("id")));
                extractFromTranscript(source);
            } catch (Exception e) { toast(e.getMessage()); }
        });
        ScrollView scroller = new ScrollView(this); scroller.addView(box);
        new CosmicDialogBuilder(this).setTitle("人物声音实验室").setView(scroller).setNegativeButton("关闭", null).show();
    }

    void speakerCloneDialog(JSONObject extraction, JSONObject person) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(dp(20), dp(10), dp(20), dp(12));
        EditText name = field(box, "人物名称", person.optString("name", "人物 " + person.optInt("speaker")));
        EditText text = field(box, "参考片段说出的文字（留空自动识别）", ""); text.setMinLines(3);
        label(box, "先试听参考片段，确认属于同一人物。创建后在保存音色中选择使用。", 12, false);
        new CosmicDialogBuilder(this).submit("创建人物音色", () -> {
            String nm = name.getText().toString().trim(), transcript = text.getText().toString().trim();
            if (nm.isEmpty() || nm.length() > 80) throw new IOException("人物名称应为 1—80 字");
            if (transcript.length() > 2000) throw new IOException("对应文字最多 2000 字");
            if (!StudioFiles.ready(this, "clone")) throw new IOException("请先安装声音克隆模型");
            if (transcript.isEmpty() && !StudioFiles.ready(this, "whisper")) throw new IOException("请填写对应文字，或安装识别模型");
            StudioService.enqueue(this, new JSONObject().put("type", "clone").put("sourceJob", extraction.getString("id"))
                    .put("referenceAudio", person.getString("referenceFile")).put("start", 0)
                    .put("seconds", person.getDouble("referenceSeconds")).put("text", transcript).put("name", nm)
                    .put("title", "创建人物音色 · " + nm));
            toast("人物音色已加入任务，完成后在保存音色列表使用");
        }).setTitle("创建人物音色").setView(box).setNegativeButton("取消", null).show();
    }

    void importModels(List<Uri> uris, String module) {
        if (uris.isEmpty()) return;
        startActivity(new Intent(this, ModelInstallActivity.class).putExtra("module", module)
                .putParcelableArrayListExtra("modelUris", new ArrayList<>(uris)));
    }

    String name(Uri u) {
        try (android.database.Cursor c =
                getContentResolver()
                        .query(u, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {
        }
        return u.toString();
    }

    void openFreeDemo() {
        new CosmicDialogBuilder(this).setTitle("✦ 免费云端声音星球")
            .setMessage("不需要电脑！先在本页录音并点“导出 WAV”，然后在官方 ZeroGPU 网页上传声音、输入新文字；完成后下载音频。免费额度有限，浏览器演示不等于已连接后台 API。")
            .setNegativeButton("取消",null)
            .setNeutralButton("Qwen3-TTS",(d,n)->{
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/spaces/Qwen/Qwen3-TTS")));
            })
            .setPositiveButton("IndexTTS 2.5",(d,n)->{
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/spaces/IndexTeam/IndexTTS-2.5-Demo")));
            }).show();
    }

    void profiles() {
        File[] fs = StudioFiles.dir(this, "profiles").listFiles((f, n) -> n.endsWith(".json"));
        List<JSONObject> all = new ArrayList<>();
        if (fs != null)
            for (File f : fs)
                try {
                    JSONObject j = StudioFiles.read(f);
                    if (new File(f.getParentFile(), j.getString("id") + ".reference.wav").isFile())
                        all.add(j);
                } catch (Exception ignored) {
                }
        if (all.isEmpty()) {
            toast("还没有参考音色");
            return;
        }
        String[] names = new String[all.size()];
        for (int i = 0; i < names.length; i++) names[i] = all.get(i).optString("name");
        new CosmicDialogBuilder(this)
                .setTitle("选择参考音色")
                .setItems(
                        names,
                        (d, n) -> {
                            JSONObject j = all.get(n);
                            String id = j.optString("id");
                            LinearLayout details = new LinearLayout(this); details.setOrientation(1);
                            details.setPadding(dp(20), dp(12), dp(20), dp(12));
                            label(details, String.format(Locale.CHINA, "参考片段 %.1f 秒", j.optDouble("referenceSeconds", 6)), 13, true);
                            String referenceText = j.optString("referenceText", "旧音色没有记录对应文字");
                            int characters = referenceText.codePointCount(0, referenceText.length());
                            String excerpt = characters > 90 ? referenceText.substring(0, referenceText.offsetByCodePoints(0, 90)) + "…" : referenceText;
                            label(details, "参考文字（" + characters + " 字）：" + excerpt, 12, false);
                            label(details,"要修正参考文字，可以重新录音并保存新音色。",12,false);
                            button(details,"① 导出参考 WAV 到下载文件夹",()->{
                                File original=new File(StudioFiles.dir(this,"profiles"),id+".reference.wav");
                                if(original.isFile())saveOutput(original);
                                else toast("参考录音不存在，请重新录制");
                            });
                            button(details,"② 复制参考录音原文 · 网页克隆使用",()->{
                                String transcript=j.optString("referenceText","").trim();
                                if(transcript.isEmpty()){
                                    toast("没有填写参考录音原文，请先听原声并自行核对，不能用生成文本冒充录音原文");
                                    return;
                                }
                                ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                                     ClipData.newPlainText("VoiceStudio 参考音频原文",transcript));
                                toast("录音原文已复制；在网页克隆的参考文本栏粘贴");
                            });
                            new CosmicDialogBuilder(this)
                                    .setTitle(j.optString("name"))
                                    .setView(details)
                                    .setPositiveButton(
                                            "使用",
                                            (x, y) -> {
                                                getSharedPreferences("studio", 0)
                                                        .edit()
                                                        .putString("selected-profile", id).putString("cloud-profile", id)
                                                        .apply();
                                                toast("已选参考录音，请在云端引擎中选择支持录音克隆的模型");
                                            })
                                    .setNeutralButton(
                                            "删除",
                                            (x, y) ->
                                                    new CosmicDialogBuilder(this)
                                                            .setMessage("删除这个保存的参考音色？")
                                                            .setNegativeButton("取消", null)
                                                            .setPositiveButton(
                                                                    "删除",
                                                                    (a, b) -> {
                                                                        new File(
                                                                                        StudioFiles
                                                                                                .dir(
                                                                                                        this,
                                                                                                        "profiles"),
                                                                                        id + ".vpc")
                                                                                .delete();
                                                                        new File(
                                                                                        StudioFiles
                                                                                                .dir(
                                                                                                        this,
                                                                                                        "profiles"),
                                                                                        id
                                                                                                + ".json")
                                                                                .delete();
                                                                        if (id.equals(
                                                                                getSharedPreferences(
                                                                                                "studio",
                                                                                                0)
                                                                                        .getString(
                                                                                                "selected-profile",
                                                                                                "")))
                                                                            getSharedPreferences(
                                                                                            "studio",
                                                                                            0)
                                                                                    .edit()
                                                                                    .remove("selected-profile").remove("cloud-profile")
                                                                                    .apply();
                                                                        new File(StudioFiles.dir(this, "profiles"), id + ".reference.wav").delete();
                                                                        toast("音色已删除");
                                                                    })
                                                            .show())
                                    .setNegativeButton("试听原声", (x, y) -> {
                                        File audio = new File(StudioFiles.dir(this, "profiles"), id + ".reference.wav");
                                        if (audio.isFile()) preview(audio, j.optString("name")); else toast("此旧音色没有保留原声录音");
                                    })
                                    .show();
                        })
                .show();
    }

    void settings() {
        android.content.SharedPreferences p = getSharedPreferences("studio", 0);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        box.setPadding(dp(20), dp(10), dp(20), 0);
        EditText style = field(box, "音色描述（原模型英文指令）", p.getString("voice-style", "None"));
        label(box, "例如 female, young adult, whispering。选择参考音色后，以参考声音为准。", 12, false);
        Spinner steps = new Spinner(this);
        steps.setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[] {"32 步 · 质量优先", "16 步 · 快速（默认）", "8 步 · 极速（实验）"}));
        int savedSteps = p.getInt("voice-steps", 16);
        steps.setSelection(savedSteps == 8 ? 2 : savedSteps == 32 ? 0 : 1);
        box.addView(steps);
        label(box, "16 步更快；32 步更慢，也可能错读，需试听核对全文。8 步可能漏字。参考录音只用于克隆音色，创作时可输入任意新文字；已排队任务沿用原配置。", 12, false);
        EditText seed = field(box, "随机种子", Long.toString(p.getLong("voice-seed", 42)));
        new CosmicDialogBuilder(this).submit("保存", () -> {
            long value = Long.parseLong(seed.getText().toString());
            String instruction = style.getText().toString().trim();
            if (instruction.length() > 300) throw new IOException("音色描述过长");
            p.edit()
                    .putString(
                            "voice-style",
                            instruction.isEmpty() ? "None" : instruction)
                    .putInt(
                            "voice-steps",
                            new int[]{32, 16, 8}[steps.getSelectedItemPosition()])
                    .putLong("voice-seed", value)
                    .apply();
            toast("设置已保存");
                })
                .setTitle("音色设计与采样")
                .setView(box)

                .setNegativeButton("取消", null)
                .show();
    }

    void showTasks() {
        if (taskSummary == null) return;
        while (tasks.getChildCount() > taskHeaderCount) tasks.removeViewAt(taskHeaderCount);
        JSONArray all = new JSONArray();
        int working = 0, paused = 0, failed = 0;
        for (int n = 0; n < taskSnapshot.length(); n++) {
            JSONObject candidate = taskSnapshot.optJSONObject(n);
            String state = candidate.optString("state");
            boolean busy = state.equals("queued") || state.equals("running");
            if (busy) working++;
            if (state.equals("paused")) paused++;
            if (state.equals("failed")) failed++;
            if (taskFilter == 0 || taskFilter == 1 && busy || taskFilter == 2 && state.equals("paused")
                    || taskFilter == 3 && state.equals("done") || taskFilter == 4 && state.equals("failed")) all.put(candidate);
        }
        taskSummary.setText("共 " + taskSnapshot.length() + " 项 · 等待/运行 " + working + " · 暂停 " + paused + " · 失败 " + failed);
        for (int i = 0; i < Math.min(taskLimit, all.length()); i++) {
            JSONObject j = all.optJSONObject(i);
            String name = j.optString("title", AudioChecks.shortTitle(j.optString("text")));
            LinearLayout taskRow = new LinearLayout(this);
            taskRow.setOrientation(0);
            tasks.addView(taskRow);
            TextView row =
                    label(
                            taskRow,
                            name
                                    + "\n"
                                    + stateName(j.optString("state"))
                                    + " · "
                                    + j.optString("progress"),
                            14,
                            false);
            row.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            row.setPadding(0, dp(12), 0, dp(12));
            TextView menu = label(taskRow, "···", 20, true);
            menu.setContentDescription("任务操作");
            menu.setPadding(dp(14), dp(16), dp(14), dp(16));
            menu.setOnClickListener(v -> taskMenu(j));
            row.setOnClickListener(v -> task(j));
            row.setOnLongClickListener(
                    v -> {
                        deleteTask(j);
                        return true;
                    });
        }
        if (all.length() > taskLimit)
            button(
                    tasks,
                    "显示更多任务",
                    () -> {
                        taskLimit += 50;
                        showTasks();
                    });
        if (all.length() == 0) label(tasks, taskSnapshot.length() == 0 ? "还没有任务" : "此分类没有任务", 14, false);
    }

    void taskMenu(JSONObject snapshot) {
        try {
            JSONObject current = StudioFiles.read(StudioFiles.job(this, snapshot.getString("id")));
            String state = current.optString("state");
            List<String> options = new ArrayList<>();
            options.add("打开结果或状态");
            if (state.equals("queued") || state.equals("running")) options.add("暂停此任务");
            if (state.equals("paused") || state.equals("failed")) options.add("继续此任务");
            if (new File(new File(StudioFiles.dir(this, "jobs"), current.optString("id")), "input.wav").isFile())
                options.add("试听原始音轨");
            options.add("删除任务与结果");
            new CosmicDialogBuilder(this).setTitle(current.optString("title", "任务操作"))
                    .setItems(options.toArray(new String[0]), (d, n) -> {
                        String action = options.get(n);
                        try {
                            if (action.equals("打开结果或状态") || action.equals("继续此任务")) task(current);
                            else if (action.equals("试听原始音轨")) previewInput(current);
                            else if (action.equals("暂停此任务")) {
                                startService(new Intent(this, StudioService.class).setAction("cancelTask").putExtra("id", current.getString("id")));
                                toast("已请求暂停此任务，其他等待任务会继续");
                            } else deleteTask(current);
                        } catch (Exception e) { toast(e.getMessage()); }
                    }).setNegativeButton("关闭", null).show();
        } catch (Exception e) { toast("任务已变化，请重新打开"); }
    }

    void deleteTask(JSONObject job) {
        new CosmicDialogBuilder(this)
                .setTitle("删除此任务及结果？")
                .setMessage("导出的外部文件和已保存音色保留。此任务的内部音频、字幕和章节会删除。")
                .setNegativeButton("保留", null)
                .setPositiveButton(
                        "删除",
                        (d, n) ->
                                files.execute(
                                        () -> {
                                            try {
                                                StudioFiles.deleteJob(this, job.getString("id"));
                                                ui.post(
                                                        () -> {
                                                            if (!gone) {
                                                                last = "";
                                                                ui.removeCallbacks(poll); ui.post(poll);
                                                                toast("任务已删除");
                                                            }
                                                        });
                                            } catch (Exception e) {
                                                ui.post(() -> toast(e.getMessage()));
                                            }
                                        }))
                .show();
    }

    void showFailure(JSONObject job) {
        String detail = "阶段：" + job.optString("failureStage", "旧版未记录") + "\n"
                + job.optString("failureDetail", job.optString("progress", "无详细错误"));
        new CosmicDialogBuilder(this).setTitle("合成错误详情").setMessage(detail)
                .setPositiveButton("复制详情", (d, n) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("VoiceStudio 合成错误", detail)); toast("已复制错误详情");
                }).setNeutralButton("带原文换引擎重做", (d, n) -> {
                    new CosmicDialogBuilder(this).setTitle("选择合成引擎")
                            .setItems(new String[]{"Fish Audio（联网）", "OmniVoice（本机）", "手机语音（离线音色）"}, (x, at) -> {
                                String selected = new String[]{"fish", "omni", "local"}[at];
                                startActivity(new Intent(this, MainActivity.class).setAction("com.voice.studio.SELECT_ENGINE").putExtra("engine", selected).putExtra("retryText", job.optString("type").equals("tts") ? job.optString("text") : ""));
                            }).setNegativeButton("取消", null).show();
                }).setNegativeButton("关闭", null).show();
    }

    void task(JSONObject snapshot) {
        JSONObject j;
        try { j = StudioFiles.ensureSubtitles(this, snapshot.getString("id")); }
        catch (Exception e) { toast("任务已变化，请刷新后重试"); return; }
        String state = j.optString("state"), id = j.optString("id");
        if (state.equals("failed") || state.equals("paused")) {
            new CosmicDialogBuilder(this).setTitle(state.equals("failed") ? "任务失败" : "任务已暂停")
                    .setNeutralButton(
                            j.has("audioOutput") ? "保存已生成音轨" : "错误详情",
                            (d, w) -> { if (j.has("audioOutput")) saveOutput(new File(StudioFiles.dir(this, "audio"), j.optString("audioOutput", id + ".wav")));
                                else showFailure(j); })
                    .setMessage(j.optString("progress") + "\n" +
                            (j.optString("type").equals("dictation") && !StudioService.needsMicrophone(WorkspaceActivity.this, j)
                                    ? "继续时识别已保存录音，不会重新开启麦克风。"
                                    : j.optString("type").equals("book") ? "继续时复用已完成章节。"
                                    : j.optString("type").equals("tts") ? "继续时先检查已生成音频；可用时仅恢复保存，否则重新合成。" : "继续时会重新执行此任务。"))
                    .setPositiveButton(
                            "继续",
                            (d, w) -> {
                                try {
                                    resumeTask(id);
                                } catch (Exception e) {
                                    toast(e.getMessage());
                                }
                            })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        if (!state.equals("done")) {
            toast(j.optString("progress"));
            return;
        }
        if (j.has("instrumentalOutput")) {
            separationResult(j);
            return;
        }
        if (j.has("speakerResults")) { speakerResults(j); return; }
        if (j.has("groupedSource")) {
            try {
                transcriptResult(
                        StudioFiles.read(StudioFiles.job(this, j.optString("groupedSource"))));
            } catch (Exception e) {
                toast(e.getMessage());
            }
            return;
        }
        if (j.optString("type").equals("dub")) {
            dubResult(j);
            return;
        }
        if (j.has("segments")) {
            transcriptResult(j);
            return;
        }
        if (j.has("translation")) {
            translationResult(j);
            return;
        }
        if (j.has("profileResult")) {
            getSharedPreferences("studio", 0)
                    .edit()
                    .putString("selected-profile", j.optString("profileResult"))
                    .apply();
            toast("已使用新音色，返回首页生成");
            return;
        }
        File f = new File(StudioFiles.dir(this, "audio"), j.optString("output", id + ".wav"));
        String mime = mime(f);
        Uri uri = AudioProvider.uri(this, f);
        new CosmicDialogBuilder(this)
                .setTitle("任务结果")
                .setItems(
                        new String[] {"播放", "保存到文件", "分享"},
                        (d, n) -> {
                            try {
                                if (n == 1) {
                                    saveOutput(f);
                                } else if (n == 0 && mime.startsWith("audio/")) {
                                    preview(f, "音频试听");
                                } else {
                                    Intent intent =
                                            new Intent(
                                                            n == 0
                                                                    ? Intent.ACTION_VIEW
                                                                    : Intent.ACTION_SEND)
                                                    .setDataAndType(uri, mime)
                                                    .addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (n == 2) intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    intent.setClipData(
                                            ClipData.newUri(
                                                    getContentResolver(), "VoiceStudio", uri));
                                    startActivity(
                                            Intent.createChooser(intent, n == 0 ? "播放音频" : "分享音频"));
                                }
                            } catch (Exception e) {
                                toast("没有可处理此音频的应用");
                            }
                        })
                .show();
    }

    String stateName(String s) {
        return s.equals("queued")
                ? "等待执行"
                : s.equals("running")
                        ? "处理中"
                        : s.equals("done") ? "已完成" : s.equals("paused") ? "已暂停" : "失败";
    }

    String mime(File f) {
        String n = f.getName();
        return n.endsWith(".mp4")
                ? "video/mp4"
                : n.endsWith(".zip")
                        ? "application/zip"
                        : n.endsWith(".json")
                                ? "application/json"
                                : n.endsWith(".txt") || n.endsWith(".srt")
                                        ? "text/plain"
                                        : n.endsWith(".mp3") ? "audio/mpeg" : "audio/wav";
    }

    void startDictation() {
        if (voiceCapture != null && voiceCapture.capturing) { toast("请先结束人物音色录音"); return; }
        if (checkSelfPermission("android.permission.RECORD_AUDIO")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.RECORD_AUDIO"}, 80);
            return;
        }
        try {
            if (!StudioFiles.ready(this, "whisper")) throw new IOException("请先导入 Whisper 模型");
            StudioService.enqueue(
                    this,
                    new JSONObject()
                            .put("type", "dictation")
                            .put("title", "麦克风听写")
                            .put("recognitionLanguage", "auto"));
            toast("开始录音，完成后点击结束录音");
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 89) {
            if (results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED && voiceCapture != null) voiceCapture.start();
            else toast("未授权麦克风，请授权后点击开始录音");
            return;
        }
        if (request == 88) {
            String retry = pendingResume; pendingResume = "";
            if (results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED && !retry.isEmpty()) resumeTask(retry);
            else toast("未授权麦克风，任务已保留，可授权后继续");
            return;
        }
        if ((request == 80 || request == 85) && (results.length == 0
                || results[0] != android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            toast("未授权麦克风，可在系统设置中授权后重试"); return;
        }
        if (request == 86) {
            boolean granted =
                    results.length > 0
                            && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            getSharedPreferences("studio", 0)
                    .edit()
                    .putBoolean("record-bluetooth", granted)
                    .apply();
            toast(granted ? "已选择蓝牙通话耳机" : "已使用手机麦克风");
        }
        if (request == 85
                && results.length > 0
                && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
            floatingDictation();
        if (request == 80
                && results.length > 0
                && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
            startDictation();
    }

    void transcriptionDialog(Uri uri) {
        try {
            if (!StudioFiles.ready(this, "whisper")) throw new IOException("请先导入 Whisper 识别模型");
            JSONObject langs =
                    StudioFiles.read(
                                    new File(getFilesDir(), "whisper-model/generation_config.json"))
                            .getJSONObject("lang_to_id");
            List<String> codes = new ArrayList<>();
            codes.add("auto");
            for (Iterator<String> it = langs.keys(); it.hasNext(); ) {
                String k = it.next();
                codes.add(k.substring(2, k.length() - 2));
            }
            Collections.sort(codes.subList(1, codes.size()));
            String[] labels = new String[codes.size()];
            for (int i = 0; i < labels.length; i++)
                labels[i] =
                        i == 0
                                ? "自动检测"
                                : Locale.forLanguageTag(codes.get(i))
                                                .getDisplayName(Locale.SIMPLIFIED_CHINESE)
                                        + " · "
                                        + codes.get(i);
            new CosmicDialogBuilder(this)
                    .setTitle("选择识别语言")
                    .setItems(
                            labels,
                            (d, n) -> {
                                try {
                                    StudioService.enqueue(
                                            this,
                                            new JSONObject()
                                                    .put("type", "transcribe")
                                                    .put("title", "转写 · " + name(uri))
                                                    .put("uri", uri.toString())
                                                    .put("recognitionLanguage", codes.get(n)));
                                    toast("转写已加入队列");
                                } catch (Exception e) {
                                    toast(e.getMessage());
                                }
                            })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    void preview(File file, String title) {
        if (!file.isFile()) { toast("音频文件不存在"); return; }
        if (preview != null) preview.close();
        preview = new AudioPreview(this, file, title);
    }

    void previewInput(JSONObject job) {
        File folder = new File(StudioFiles.dir(this, "jobs"), job.optString("sourceJob", job.optString("id")));
        preview(new File(folder, "input.wav"), "原始音轨试听");
    }

    void saveOutput(File file) {
        if (!file.isFile()) { toast("结果文件不存在，请检查任务状态"); return; }
        export = file;
        try { startActivityForResult(
                new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(mime(file))
                        .putExtra(Intent.EXTRA_TITLE, file.getName()),
                74); }
        catch (ActivityNotFoundException e) { export = null; toast("没有可用的文件保存器，请启用手机文件管理器"); }
    }

    void transcriptResult(JSONObject job) {
        String id = job.optString("id");
        new CosmicDialogBuilder(this)
                .setTitle("转写与字幕")
                .setItems(
                        new String[] {
                            "查看 / 复制文字",
                            "编辑字幕时间与文字",
                            "导出 TXT",
                            "导出 SRT 字幕",
                            "导出 JSON",
                            "用校对字幕生成配音",
                            "按片段识别说话人",
                            "试听原始音轨",
                            "提取每位人物的声音"
                        },
                        (d, n) -> {
                            try {
                                if (n == 0) {
                                    File f = StudioFiles.output(this, id, "txt");
                                    EditText text = new EditText(this);
                                    text.setMinLines(5);
                                    text.setText(StudioFiles.text(f));
                                    new CosmicDialogBuilder(this)
                                            .setTitle("转写文字")
                                            .setView(text)
                                            .setNeutralButton(
                                                    "复制",
                                                    (a, b) ->
                                                            ((android.content.ClipboardManager)
                                                                            getSystemService(
                                                                                    CLIPBOARD_SERVICE))
                                                                    .setPrimaryClip(
                                                                            ClipData.newPlainText(
                                                                                    "VoiceStudio",
                                                                                    text
                                                                                            .getText())))
                                            .setPositiveButton(
                                                    "保存 TXT 修改",
                                                    (a, b) -> {
                                                        try {
                                                            StudioFiles.writeText(
                                                                    f, text.getText().toString());
                                                            toast("TXT 已保存；字幕请逐条校对");
                                                        } catch (Exception e) {
                                                            toast(e.getMessage());
                                                        }
                                                    })
                                            .setNegativeButton("关闭", null)
                                            .show();
                                } else if (n == 1) editSegments(job);
                                else if (n == 5) dubDialog(job);
                                else if (n == 6) speakerDialog(job);
                                else if (n == 7) previewInput(job);
                                else if (n == 8) extractFromTranscript(job);
                                else
                                    saveOutput(
                                            StudioFiles.output(
                                                    this,
                                                    id,
                                                    n == 2 ? "txt" : n == 3 ? "srt" : "json"));
                            } catch (Exception e) {
                                toast(e.getMessage());
                            }
                        })
                .show();
    }

    void persistSegments(JSONObject job) throws Exception {
        String id = job.getString("id");
        try {
            JSONObject saved = StudioFiles.replaceSegments(this, id, job.optLong("revision"), job.getJSONArray("segments"));
            job.put("segments", saved.getJSONArray("segments")).put("revision", saved.optLong("revision"));
        } catch (Exception failure) {
            JSONObject latest = StudioFiles.read(StudioFiles.job(this, id));
            job.put("segments", latest.getJSONArray("segments")).put("revision", latest.optLong("revision"));
            throw failure;
        }
    }

    void editSegments(JSONObject job) throws Exception {
        JSONArray segments = job.getJSONArray("segments");
        String[] labels = new String[segments.length()];
        for (int i = 0; i < labels.length; i++) {
            JSONObject s = segments.getJSONObject(i);
            labels[i] =
                    WhisperEngine.stamp(s.getDouble("start"))
                            + " · 说话人 "
                            + s.optInt("speaker", 0)
                            + " · "
                            + s.getString("text");
        }
        new CosmicDialogBuilder(this)
                .setTitle("字幕校对")
                .setItems(
                        labels,
                        (d, n) -> {
                            try {
                                JSONObject s = segments.getJSONObject(n);
                                LinearLayout box = new LinearLayout(this);
                                box.setOrientation(1);
                                EditText start = field(box, "开始秒数", s.optString("start")),
                                        end = field(box, "结束秒数", s.optString("end")),
                                        text = field(box, "字幕文字", s.getString("text"));
                                new CosmicDialogBuilder(this)
                                        .setTitle("字幕 " + (n + 1))
                                        .setView(box)
                                        .setPositiveButton(
                                                "保存",
                                                (x, y) -> {
                                                    try {
                                                        double
                                                                a =
                                                                        Double.parseDouble(
                                                                                start.getText()
                                                                                        .toString()),
                                                                b =
                                                                        Double.parseDouble(
                                                                                end.getText()
                                                                                        .toString());
                                                        if (!Double.isFinite(a)
                                                                || !Double.isFinite(b)
                                                                || a < 0
                                                                || b <= a)
                                                            throw new IOException("时间无效");
                                                        s.put("start", a)
                                                                .put("end", b)
                                                                .put(
                                                                        "text",
                                                                        text.getText().toString());
                                                        persistSegments(job);
                                                        toast("字幕已保存");
                                                    } catch (Exception e) {
                                                        toast(e.getMessage());
                                                    }
                                                })
                                        .setNeutralButton("拆分 / 删除", (x, y) -> segmentTools(job, n))
                                        .setNegativeButton("取消", null)
                                        .show();
                            } catch (Exception e) {
                                toast(e.getMessage());
                            }
                        })
                .setNeutralButton(
                        "添加",
                        (d, w) -> {
                            try {
                                double start =
                                        segments.length() == 0
                                                ? 0
                                                : segments.getJSONObject(segments.length() - 1)
                                                        .getDouble("end");
                                segments.put(
                                        new JSONObject()
                                                .put("start", start)
                                                .put("end", start + 2)
                                                .put("text", "新字幕"));
                                persistSegments(job);
                                editSegments(job);
                            } catch (Exception e) {
                                toast(e.getMessage());
                            }
                        })
                .setNegativeButton("关闭", null)
                .show();
    }

    void segmentTools(JSONObject job, int index) {
        new CosmicDialogBuilder(this)
                .setItems(
                        new String[] {"从文字中间拆成两条", "删除此条", "为这个说话人指定音色", "更改说话人编号"},
                        (d, n) -> {
                            try {
                                if (n == 2) {
                                    assignSpeakerVoice(job, index);
                                    return;
                                }
                                if (n == 3) {
                                    EditText value = new EditText(this);
                                    value.setInputType(2);
                                    value.setText(
                                            String.valueOf(
                                                    job.getJSONArray("segments")
                                                            .getJSONObject(index)
                                                            .optInt("speaker", 0)));
                                    new CosmicDialogBuilder(this)
                                            .setTitle("说话人编号（0 未指定）")
                                            .setView(value)
                                            .setPositiveButton(
                                                    "保存",
                                                    (a, b) -> {
                                                        try {
                                                            int speaker =
                                                                    Integer.parseInt(
                                                                            value.getText()
                                                                                    .toString());
                                                            if (speaker < 0 || speaker > 32)
                                                                throw new IOException("编号需为 0—32");
                                                            job.getJSONArray("segments")
                                                                    .getJSONObject(index)
                                                                    .put("speaker", speaker);
                                                            persistSegments(job);
                                                        } catch (Exception e) {
                                                            toast(e.getMessage());
                                                        }
                                                    })
                                            .setNegativeButton("取消", null)
                                            .show();
                                    return;
                                }
                                JSONArray segments = job.getJSONArray("segments");
                                JSONObject s = segments.getJSONObject(index);
                                if (n == 1) segments.remove(index);
                                else {
                                    String text = s.getString("text");
                                    int count = text.codePointCount(0, text.length());
                                    double start = s.getDouble("start"), end = s.getDouble("end");
                                    if (count < 2 || end - start < .4)
                                        throw new IOException("这条字幕太短，不能拆分");
                                    int mid = text.offsetByCodePoints(0, count / 2);
                                    double split =
                                            start + (end - start) * ((double) (count / 2) / count);
                                    JSONObject next = new JSONObject(s.toString());
                                    s.put("end", split).put("text", text.substring(0, mid).trim());
                                    next.put("start", split)
                                            .put("text", text.substring(mid).trim());
                                    segments.put(next);
                                }
                                persistSegments(job);
                                editSegments(job);
                            } catch (Exception e) {
                                toast(e.getMessage());
                            }
                        })
                .setNegativeButton("取消", null)
                .show();
    }

    void separationResult(JSONObject job) {
        File vocals = new File(StudioFiles.dir(this, "audio"), job.optString("output")),
                music =
                        new File(
                                StudioFiles.dir(this, "audio"),
                                job.optString("instrumentalOutput"));
        new CosmicDialogBuilder(this)
                .setTitle("人声 / 伴奏")
                .setItems(
                        new String[] {"播放人声", "导出人声 WAV", "播放伴奏", "导出伴奏 WAV"},
                        (d, n) -> {
                            File file = n < 2 ? vocals : music;
                            if (n % 2 == 1) saveOutput(file);
                            else preview(file, n < 2 ? "人声试听" : "伴奏试听");
                        })
                .setNegativeButton("关闭", null)
                .show();
    }

    void regroupVoices(JSONObject source) {
        LinearLayout box = new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(12),dp(20),dp(12));
        EditText count = field(box, "人物数量（未知填 0 自动）", "3");count.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new CosmicDialogBuilder(this).submit("重新分组", () -> {
            int people = Integer.parseInt(count.getText().toString());
            if (people < 0 || people > 32) throw new IOException("人物数量需为 0—32");
            if (!StudioFiles.ready(this,"speaker")) throw new IOException("请先安装说话人模型");
            String rawId = source.optString("sourceJob",source.getString("id"));
            StudioService.enqueue(this,new JSONObject().put("type","extractVoices").put("sourceJob",rawId)
                .put("segments",new JSONArray(source.getJSONArray("segments").toString())).put("regroup",true)
                .put("speakerCount",people).put("threshold",.55).put("title","重新分组 · "+source.optString("title")));
            toast("已使用原音轨重新分组，原结果保留");
        }).setTitle("重新合并人物声音").setView(box).setNegativeButton("取消",null).show();
    }

    void speakerDialog(JSONObject source) {
        LinearLayout box = new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(12),dp(20),dp(12));
        EditText count = field(box,"人物数量（未知填 0 自动）","3");count.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        label(box,"将全部片段统一分组，同一人物的多段对白合并。混有多人说话的字幕需先拆分。",12,false);
        new CosmicDialogBuilder(this).submit("分组", () -> {
            int people = Integer.parseInt(count.getText().toString());
            if (people < 0 || people > 32) throw new IOException("人物数量需为 0—32");
            if (!StudioFiles.ready(this,"speaker")) throw new IOException("请先安装说话人模型");
            StudioService.enqueue(this,new JSONObject().put("type","speaker").put("title","人物分组 · "+source.optString("title"))
                .put("sourceJob",source.getString("id")).put("sourceRevision",source.optLong("revision"))
                .put("segments",new JSONArray(source.getJSONArray("segments").toString())).put("speakerCount",people).put("threshold",.55));
            toast("分组已加入队列");
        }).setTitle("按人物数量分组").setView(box).setNegativeButton("取消",null).show();
    }

    void assignSpeakerVoice(JSONObject job, int index) {
        try {
            JSONArray segments = job.getJSONArray("segments");
            int speaker = segments.getJSONObject(index).optInt("speaker", 0);
            List<String> ids = new ArrayList<>(), names = new ArrayList<>();
            ids.add("");
            names.add("首页当前音色");
            File[] profiles =
                    StudioFiles.dir(this, "profiles")
                            .listFiles((dir, name) -> name.endsWith(".json"));
            if (profiles != null)
                for (File file : profiles) {
                    JSONObject profile = StudioFiles.read(file);
                    String id = profile.getString("id");
                    if (new File(file.getParentFile(), id + ".vpc").isFile()) {
                        ids.add(id);
                        names.add(profile.optString("name", "参考音色"));
                    }
                }
            new CosmicDialogBuilder(this)
                    .setTitle(speaker == 0 ? "指定此条配音音色" : "为说话人 " + speaker + " 指定音色")
                    .setItems(
                            names.toArray(new String[0]),
                            (d, n) -> {
                                try {
                                    for (int at = 0; at < segments.length(); at++) {
                                        JSONObject segment = segments.getJSONObject(at);
                                        if (at == index
                                                || (speaker > 0
                                                        && segment.optInt("speaker", 0)
                                                                == speaker)) {
                                            if (ids.get(n).isEmpty()) segment.remove("profile");
                                            else segment.put("profile", ids.get(n));
                                        }
                                    }
                                    persistSegments(job);
                                    toast("配音音色已分配");
                                } catch (Exception e) {
                                    toast(e.getMessage());
                                }
                            })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    void recordInput() {
        new CosmicDialogBuilder(this)
                .setTitle("录音输入")
                .setSingleChoiceItems(
                        new String[] {"手机麦克风", "蓝牙通话耳机（先连接耳机）"},
                        getSharedPreferences("studio", 0).getBoolean("record-bluetooth", false)
                                ? 1
                                : 0,
                        (d, n) -> {
                            d.dismiss();
                            if (n == 1
                                    && Build.VERSION.SDK_INT >= 31
                                    && checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                                            != android.content.pm.PackageManager
                                                    .PERMISSION_GRANTED) {
                                requestPermissions(
                                        new String[] {"android.permission.BLUETOOTH_CONNECT"}, 86);
                                return;
                            }
                            getSharedPreferences("studio", 0)
                                    .edit()
                                    .putBoolean("record-bluetooth", n == 1)
                                    .apply();
                            toast(n == 1 ? "已选择蓝牙通话耳机" : "已选择手机麦克风");
                        })
                .setNegativeButton("取消", null)
                .show();
    }

    void floatingDictation() {
        if (!StudioFiles.ready(this, "whisper")) {
            toast("请先导入识别模型");
            return;
        }
        if (!android.provider.Settings.canDrawOverlays(this)) {
            startActivityForResult(
                    new Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())),
                    82);
            return;
        }
        if (checkSelfPermission("android.permission.RECORD_AUDIO")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.RECORD_AUDIO"}, 85);
            return;
        }
        startForegroundService(new Intent(this, DictationOverlay.class));
        toast("悬浮按钮已打开；轻点录音，再点结束，完成后可复制文字");
    }

    void dubResult(JSONObject job) {
        String id = job.optString("id");
        File result = new File(StudioFiles.dir(this, "audio"), job.optString("output"));
        new CosmicDialogBuilder(this)
                .setTitle("配音结果")
                .setItems(
                        new String[] {"播放结果", "保存结果", "单独导出配音 WAV", "导出配音 SRT 字幕", "分享结果"},
                        (d, n) -> {
                            try {
                                if (n == 1) saveOutput(result);
                                else if (n == 2) saveOutput(StudioFiles.output(this, id, "wav"));
                                else if (n == 3) saveOutput(StudioFiles.output(this, id, "srt"));
                                else if (n == 0 && mime(result).startsWith("audio/")) preview(result, "配音试听");
                                else {
                                    Uri uri = AudioProvider.uri(this, result);
                                    Intent intent =
                                            new Intent(
                                                            n == 0
                                                                    ? Intent.ACTION_VIEW
                                                                    : Intent.ACTION_SEND)
                                                    .setDataAndType(uri, mime(result))
                                                    .addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (n == 4) intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    intent.setClipData(
                                            ClipData.newUri(
                                                    getContentResolver(), "VoiceStudio", uri));
                                    startActivity(Intent.createChooser(intent, "配音结果"));
                                }
                            } catch (Exception e) {
                                toast("无法打开结果：" + e.getMessage());
                            }
                        })
                .setNegativeButton("关闭", null)
                .show();
    }

    static final String[] MODULE_CODES = {"omni", "clone", "whisper", "translation", "separation", "speaker"};
    static final String[] MODULE_NAMES = {"基础语音合成", "声音克隆", "语音识别", "离线翻译", "人声分离", "说话人分组"};
    static final String[] MODULE_USES = {"配音、长文、批量、有声书；646 项合成目录", "保存参考音色；另需基础模型进行合成", "转写、录音、悬浮听写；99 项识别目录", "文字和字幕翻译；202 个语言与文字体系选项", "输出人声与伴奏 WAV", "按已校对字幕分组；另需识别结果"};
    static final int[] MODULE_REQUESTS = {87, 72, 76, 78, 80, 81};

    void modelModules() {
        try {
            String[] labels = new String[MODULE_CODES.length];
            for (int n = 0; n < labels.length; n++) {
                long bytes = 0;
                JSONArray entries = StudioFiles.manifest(this, MODULE_CODES[n]).getJSONArray("files");
                for (int i = 0; i < entries.length(); i++) bytes += entries.getJSONObject(i).getLong("size");
                labels[n] = MODULE_NAMES[n] + (StudioFiles.ready(this, MODULE_CODES[n]) ? " · 已安装" : " · 待安装")
                        + " · " + (bytes / 1024 / 1024) + " MiB";
            }
            new CosmicDialogBuilder(this)
                    .setTitle("模型安装与检查 · 可用 " + (getFilesDir().getUsableSpace() / 1024 / 1024) + " MiB")
                    .setItems(labels, (d, n) -> modelDetails(n))
                    .setNeutralButton("下载模型", (d, n) -> startActivity(new Intent(this, ModelDownloadsActivity.class)))
                    .setNegativeButton("关闭", null).show();
        } catch (Exception e) { toast("无法读取模型配置：" + e.getMessage()); }
    }

    void modelDetails(int index) {
        String module = MODULE_CODES[index];
        File dir = new File(getFilesDir(), module + "-model");
        boolean ready = StudioFiles.ready(this, module);
        AlertDialog.Builder dialog = new CosmicDialogBuilder(this)
                .setTitle(MODULE_NAMES[index])
                .setMessage(MODULE_USES[index] + "\n\n" + (ready ? "已安装。可以检查文件完整性。" : "未安装或文件不完整。请选择一个完整 ZIP，或该模块的全部分包。"))
                .setNegativeButton("关闭", null);
        dialog.setPositiveButton(ready ? "检查完整性" : "导入模型", (d, n) -> {
            if (ready) verifyModule(module, MODULE_NAMES[index]);
            else pick(MODULE_REQUESTS[index], "*/*", true);
        });
        if (!dir.exists()) dialog.setNeutralButton("下载模型", (d, n) ->
                startActivity(new Intent(this, ModelDownloadsActivity.class).putExtra("module", module)));
        if (dir.exists()) dialog.setNeutralButton("移除", (d, n) -> {
            new CosmicDialogBuilder(this).setTitle("移除 " + MODULE_NAMES[index] + "？")
                    .setMessage("作品与音色保留；使用此模块前需重新导入模型。")
                    .setNegativeButton("保留", null)
                    .setPositiveButton("移除", (a, b) -> {
                        if (!StudioFiles.beginModelWork(this)) { toast("请等待任务或模型操作结束"); return; }
                        files.execute(() -> {
                            try { OmniModels.remove(dir); }
                            finally { StudioFiles.endModelWork(); }
                            ui.post(() -> { if (!gone) { status.setText(dir.exists() ? "模型移除失败，请重试" : "模型已移除"); modelModules(); } });
                        });
                    }).show();
        });
        dialog.show();
    }

    void verifyModule(String module, String title) {
        if (!StudioFiles.beginModelWork(this)) { toast("请等待当前任务或模型操作完成"); return; }
        status.setText("正在检查 " + title + "，大模型需要一些时间…");
        files.execute(() -> {
            String message;
            try {
                OmniModels.verify(new File(getFilesDir(), module + "-model"), StudioFiles.manifest(this, module),
                        m -> ui.post(() -> { if (!gone) status.setText(title + " · " + m); }));
                message = title + " · 全部文件校验通过";
            } catch (Exception e) { message = title + " · " + e.getMessage(); }
            finally { StudioFiles.endModelWork(); }
            final String result = message;
            ui.post(() -> { if (!gone) { status.setText(result); toast(result); } });
        });
    }

    void deviceChecks() {
        if (!StudioFiles.beginModelWork(this)) { toast("请先暂停队列，并等待模型操作结束后自检"); return; }
        importing = true; status.setText("基础自检准备中…");
        files.execute(() -> {
            try {
                JSONObject report = DeviceChecks.run(this, message -> ui.post(() -> { if (!gone) status.setText(message); }));
                File output = StudioFiles.output(this, UUID.randomUUID().toString(), "json");
                StudioFiles.writeText(output, report.toString(2));
                StringBuilder text = new StringBuilder(); JSONArray results = report.getJSONArray("checks");
                for (int n = 0; n < results.length(); n++) {
                    JSONObject result = results.getJSONObject(n);
                    text.append(result.getString("name")).append(" · ")
                            .append(result.getString("status").equals("passed") ? "通过" : result.getString("status").equals("failed") ? "失败" : "跳过")
                            .append("\n").append(result.optString("detail")).append("\n\n");
                }
                text.append(report.getString("note")); String summary = text.toString();
                ui.post(() -> {
                    if (gone) return;
                    status.setText("基础自检已结束");
                    new CosmicDialogBuilder(this).setTitle("设备基础自检结果").setMessage(summary)
                            .setPositiveButton("导出报告", (d,n) -> saveOutput(output)).setNegativeButton("关闭",null).show();
                });
            } catch (Throwable e) { ui.post(() -> { if (!gone) { status.setText("基础自检失败"); toast(e.getMessage()); } }); }
            finally { importing = false; StudioFiles.endModelWork(); }
        });
    }

    void diagnostics() {
        files.execute(() -> {
            try {
                android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
                ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memory);
                JSONObject report = new JSONObject().put("version", info.versionName)
                        .put("device", Build.MANUFACTURER + " " + Build.MODEL).put("androidSdk", Build.VERSION.SDK_INT)
                        .put("supportedAbis", new JSONArray(Arrays.asList(Build.SUPPORTED_ABIS)))
                        .put("availableStorageBytes", getFilesDir().getUsableSpace())
                        .put("availableMemoryBytes", memory.availMem).put("totalMemoryBytes", memory.totalMem)
                        .put("lowMemory", memory.lowMemory).put("created", System.currentTimeMillis());
                JSONObject modules = new JSONObject(), states = new JSONObject();
                for (String code : MODULE_CODES) modules.put(code, StudioFiles.ready(this, code));
                JSONArray jobs = StudioFiles.list(this);
                for (int n = 0; n < jobs.length(); n++) {
                    String state = jobs.getJSONObject(n).optString("state");
                    states.put(state, states.optInt(state) + 1);
                }
                report.put("modelsReady", modules).put("jobStateCounts", states)
                        .put("note", "不包含配音文字、录音、书籍、字幕或音色内容；此报告不代表功能验收。");
                File output = StudioFiles.output(this, UUID.randomUUID().toString(), "json");
                StudioFiles.writeText(output, report.toString(2));
                ui.post(() -> { if (!gone) saveOutput(output); });
            } catch (Exception e) { ui.post(() -> toast("诊断导出失败：" + e.getMessage())); }
        });
    }

    JSONArray translationLanguages() throws Exception {
        try (InputStream in = getAssets().open("translation-languages.json");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            copy(in, bytes, 100000);
            return new JSONArray(bytes.toString("UTF-8"));
        }
    }

    String languageName(String code) {
        String iso = code.substring(0, 3), label = iso;
        for (Locale locale : Locale.getAvailableLocales())
            try {
                if (locale.getISO3Language().equals(iso)) {
                    label = locale.getDisplayLanguage(Locale.SIMPLIFIED_CHINESE);
                    break;
                }
            } catch (Exception ignored) {
            }
        return label + " · " + code;
    }

    Spinner languageSpinner(LinearLayout box, JSONArray codes, String selected) throws Exception {
        String[] labels = new String[codes.length()];
        int choice = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = languageName(codes.getString(i));
            if (codes.getString(i).equals(selected)) choice = i;
        }
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(
                new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_dropdown_item, labels));
        spinner.setSelection(choice);
        box.addView(spinner);
        return spinner;
    }

    void translationDialog() {
        try {
            if (!StudioFiles.ready(this, "translation")) throw new IOException("请先导入翻译模型");
            JSONArray codes = translationLanguages();
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(1);
            box.setPadding(dp(20), 0, dp(20), 0);
            label(box, "原文语言", 13, true);
            Spinner source = languageSpinner(box, codes, "eng_Latn");
            label(box, "目标语言", 13, true);
            Spinner target = languageSpinner(box, codes, "zho_Hans");
            EditText text = field(box, "输入较短段落", "");
            text.setMinLines(5);
            new CosmicDialogBuilder(this).submit("加入队列", () -> {
            String content = text.getText().toString().trim();
            if (content.isEmpty() || content.length() > 2000)
                throw new IOException("请输入 1—2000 字；长段落请分开翻译");
            StudioService.enqueue(
                    this,
                    new JSONObject()
                            .put("type", "translate")
                            .put("text", content)
                            .put(
                                    "title",
                                    "翻译 · "
                                            + AudioChecks.shortTitle(
                                                    content))
                            .put(
                                    "sourceLanguage",
                                    codes.getString(
                                            source
                                                    .getSelectedItemPosition()))
                            .put(
                                    "targetLanguage",
                                    codes.getString(
                                            target
                                                    .getSelectedItemPosition())));
            toast("翻译已加入队列");
                })
                    .setTitle("离线翻译")
                    .setView(box)

                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    void translationResult(JSONObject job) {
        EditText text = new EditText(this);
        text.setMinLines(5);
        text.setText(job.optString("translation"));
        new CosmicDialogBuilder(this)
                .setTitle("翻译结果")
                .setView(text)
                .setNeutralButton(
                        "导出 TXT",
                        (d, n) -> saveOutput(StudioFiles.output(this, job.optString("id"), "txt")))
                .setPositiveButton(
                        "复制",
                        (d, n) ->
                                ((android.content.ClipboardManager)
                                                getSystemService(CLIPBOARD_SERVICE))
                                        .setPrimaryClip(
                                                ClipData.newPlainText(
                                                        "VoiceStudio", text.getText())))
                .setNegativeButton("关闭", null)
                .show();
    }

    String synthesisLanguage(String translationCode) throws Exception {
        JSONObject mapping;
        try (InputStream in = getAssets().open("translation-tts-map.json");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            copy(in, bytes, 100000);
            mapping = new JSONObject(bytes.toString("UTF-8"));
        }
        String code = mapping.optString(translationCode);
        if (code.isEmpty()) throw new IOException("该翻译目标还没有可匹配的合成语言，请先导出翻译文字");
        return code;
    }

    void dubDialog(JSONObject source) {
        try {
            if (!StudioFiles.ready(this, "omni")) throw new IOException("请先导入基础合成模型");
            JSONArray codes = translationLanguages();
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(1);
            box.setPadding(dp(20), 0, dp(20), 0);
            label(box, "使用校对后的文字和时间轴；原始背景音会被替换。", 13, false);
            CheckBox translate = new CheckBox(this);
            translate.setText("先翻译成目标语言");
            box.addView(translate);
            label(box, "字幕原文语言", 13, true);
            Spinner from = languageSpinner(box, codes, "eng_Latn");
            label(box, "翻译目标语言", 13, true);
            Spinner to = languageSpinner(box, codes, "zho_Hans");
            CheckBox video = new CheckBox(this);
            video.setText("输出 MP4 视频（保留原画面）");
            video.setEnabled(source.has("uri"));
            box.addView(video);
            label(box, "直接配音使用首页当前语言和音色。翻译配音自动匹配目标合成语言。", 12, false);
            new CosmicDialogBuilder(this)
                    .setTitle("字幕配音")
                    .setView(box)
                    .setPositiveButton(
                            "生成",
                            (d, n) -> {
                                try {
                                    JSONObject job =
                                            speechJob("")
                                                    .put("type", "dub")
                                                    .put(
                                                            "title",
                                                            "配音 · " + source.optString("title"))
                                                    .put("sourceJob", source.getString("id"))
                                                    .put(
                                                            "segments",
                                                            new JSONArray(
                                                                    source.getJSONArray("segments")
                                                                            .toString()))
                                                    .put("video", video.isChecked());
                                    if (translate.isChecked()) {
                                        if (!StudioFiles.ready(this, "translation"))
                                            throw new IOException("请先导入翻译模型");
                                        String target =
                                                codes.getString(to.getSelectedItemPosition());
                                        job.put(
                                                        "sourceLanguage",
                                                        codes.getString(
                                                                from.getSelectedItemPosition()))
                                                .put("targetLanguage", target)
                                                .put("language", synthesisLanguage(target));
                                    }
                                    StudioService.enqueue(this, job);
                                    toast("配音已加入队列");
                                } catch (Exception e) {
                                    toast(e.getMessage());
                                }
                            })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    static void copy(InputStream in, OutputStream out, long limit) throws IOException {
        if (in == null || out == null) throw new IOException("无法读取文件");
        byte[] b = new byte[65536];
        long total = 0;
        int n;
        while ((n = in.read(b)) >= 0) {
            total += n;
            if (total > limit) throw new IOException("文件过大");
            out.write(b, 0, n);
        }
    }

    LinearLayout card(String title, String sub) {
        LinearLayout c = detachedCard(title, sub);
        root.addView(c);
        return c;
    }

    LinearLayout detachedCard(String title, String sub) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(1);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        c.setBackground(CosmicUi.panel(this,CosmicUi.CYAN));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        c.setLayoutParams(lp);
        label(c, title, 18, true);
        label(c, sub, 12, false);
        return c;
    }

    TextView label(LinearLayout p, String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(bold?CosmicUi.INK:CosmicUi.MUTED);t.setLineSpacing(dp(3),1);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        p.addView(t);
        return t;
    }

    void button(LinearLayout p, String title, Runnable click) {
        TextView t = label(p, title, 14, true);
        t.setTextColor(CosmicUi.CYAN);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(14), dp(10), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xffEEEBFF);
        bg.setCornerRadius(dp(14));
        t.setBackground(CosmicUi.button(this,false));CosmicUi.press(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> click.run());
    }

    EditText field(LinearLayout p, String hint, String value) {
        EditText t = new EditText(this);
        t.setHint(hint);
        t.setText(value);
        t.setSingleLine(false);CosmicUi.field(t);
        p.addView(t);
        return t;
    }

    LinearLayout sectionCard(String key, String title, String subtitle) {
        LinearLayout card = card(title, subtitle); sections.put(key, card); return card;
    }

    void clearLegacyModels() {
        new CosmicDialogBuilder(this).setTitle("清理所有旧离线模型？")
          .setMessage("清理 OmniVoice、克隆编码器、Whisper、NLLB、分离及说话人识别模型目录；不会删除录音、人物音色、作品或历史任务。请先暂停全部任务。")
          .setNegativeButton("取消",null)
          .setPositiveButton("确认清理",(d,n)->{
              if(!StudioFiles.beginModelWork(this)){toast("请先暂停所有任务");return;}
              files.execute(()->{
                  int removed=0;String failure="";
                  try{
                      for(String module:MODULE_CODES){
                          File dir=new File(getFilesDir(),module+"-model");
                          if(dir.exists()){OmniModels.remove(dir);if(dir.exists())throw new IOException("无法清理 "+module);removed++;}
                      }
                  }catch(Exception error){failure=error.getMessage();}
                  finally{StudioFiles.endModelWork();}
                  final int count=removed;final String err=failure;
                  ui.post(()->{if(!gone)toast(err.isEmpty()?"已清理 "+count+" 个旧模型目录；录音和作品保留":"清理 "+count+" 个目录后出错："+err);});
              });
          }).show();
    }

    void openMainPage(String page) {
        if(voiceCapture!=null)voiceCapture.persist();
        startActivity(new Intent(this,MainActivity.class).setAction("com.voice.studio.OPEN_PAGE").putExtra("page",page));
    }
    void navigate(String page) {
        if(page.equals("任务"))jumpSection("任务");
        else if(page.equals("音色"))jumpSection("音色");
        else openMainPage(page);
    }
    void quickSections() {
        HorizontalScrollView strip = new HorizontalScrollView(this); strip.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this); row.setOrientation(0);
        for (String name : new String[]{"音色", "合成", "批量", "任务"}) {
            TextView chip = CosmicUi.label(this, name, 14, CosmicUi.CYAN, true);
            chip.setGravity(Gravity.CENTER); chip.setPadding(dp(16), dp(10), dp(16), dp(10));
            CosmicUi.press(chip); chip.setContentDescription(name);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48)); lp.rightMargin = dp(8);
            row.addView(chip, lp); sectionButtons.put(name, chip);
            chip.setOnClickListener(v -> {if(name.equals("合成"))openMainPage("合成");else jumpSection(name);});
        }
        strip.addView(row); shell.addView(strip);
    }

    void jumpSection(String name) {
        if (!sections.containsKey(name)) return;
        if (!name.equals("音色") && voiceCapture != null) voiceCapture.pause();
        section = name;
        if(bottomNav!=null){shell.removeView(bottomNav);bottomNav=CosmicUi.navigation(this,name.equals("音色")?"音色":name.equals("合成")?"合成":"任务",this::navigate);shell.addView(bottomNav);}
        for (Map.Entry<String, View> entry : sections.entrySet())
            entry.getValue().setVisibility(name.equals(entry.getKey()) ? View.VISIBLE : View.GONE);
        for (Map.Entry<String, TextView> entry : sectionButtons.entrySet()) {
            boolean chosen = name.equals(entry.getKey()); TextView chip = entry.getValue();
            chip.setSelected(chosen); chip.setTextColor(chosen ? Color.WHITE : CosmicUi.CYAN);
            chip.setBackground(CosmicUi.button(this, chosen));
            if (chosen) chip.post(() -> chip.requestRectangleOnScreen(new android.graphics.Rect(0, 0, chip.getWidth(), chip.getHeight()), true));
        }
        scroll.post(() -> scroll.scrollTo(0, 0));
    }

    void requireModels(LinearLayout parent, String... modules) {
        TextView hint = label(parent, "", 12, false); requirements.put(hint, modules);
        button(parent, "查看所需模型", () -> jumpSection("模型"));
    }

    void refreshModels() {
        if (status == null || StudioFiles.modelWork()) return;
        Map<String, Boolean> ready = new HashMap<>();
        int installed = 0;
        for (String code : MODULE_CODES) {
            boolean available = StudioFiles.ready(this, code); ready.put(code, available);
            if (available) installed++;
        }
        for (Map.Entry<TextView, String[]> entry : requirements.entrySet()) {
            StringBuilder label = new StringBuilder("所需模型：");
            for (String code : entry.getValue()) {
                int index = Arrays.asList(MODULE_CODES).indexOf(code);
                if (label.length() > 5) label.append(" · ");
                label.append(MODULE_NAMES[index]).append(Boolean.TRUE.equals(ready.get(code)) ? "已安装" : "待安装");
            }
            entry.getKey().setText(label.toString());
        }
        status.setText("已安装 " + installed + " / " + MODULE_CODES.length + " 个模块 · 可用空间 "
                + (getFilesDir().getUsableSpace() / 1024 / 1024) + " MiB");
    }

    void resumeTask(String id) {resumeTask(id,false);}
    void resumeTask(String id,boolean allowResubmit) {
        try {
            JSONObject job = StudioFiles.read(StudioFiles.job(this, id));
            if (StudioService.needsMicrophone(this, job)
                    && checkSelfPermission("android.permission.RECORD_AUDIO") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pendingResume = id;
                requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 88);
                return;
            }
            StudioService.resume(this, id,allowResubmit); pendingResume = "";
            toast(StudioService.needsMicrophone(this, job) ? "正在开始录音" : "任务已加入队列");
        } catch(RemoteSpeechPipeline.ResubmitRequired error) {
            new CosmicDialogBuilder(this).setTitle("重新提交未完成段？").setMessage(error.getMessage())
                .setNegativeButton("保留进度",null).setPositiveButton("仅重做未完成段",(d,n)->resumeTask(id,true)).show();
        } catch (Exception error) { toast(error.getMessage()); }
    }

    int dp(int x) {
        return (int) (x * getResources().getDisplayMetrics().density + .5f);
    }

    void toast(String s) {
        Toast.makeText(this, s == null ? "操作失败" : s, Toast.LENGTH_LONG).show();
    }

    protected void onSaveInstanceState(Bundle state) {
        if (voiceCapture != null) voiceCapture.persist();
        if (export != null) {
            state.putString("exportFile",export.getName());
            File parent=export.getParentFile();
            state.putString("exportDirectory",
                parent != null && parent.equals(StudioFiles.dir(this,"profiles")) ? "profiles" : "audio");
        }
        state.putString("section", section); state.putString("pendingResume", pendingResume);
        state.putInt("taskLimit", taskLimit);
        state.putInt("taskFilter", taskFilter);
        super.onSaveInstanceState(state);
    }

    protected void onResume() {
        super.onResume(); visible = true;
        if (voiceCapture != null) voiceCapture.refresh();
        refreshModels();
        ui.removeCallbacks(recordingPoll); ui.post(recordingPoll);
        ui.removeCallbacks(poll); ui.post(poll);
    }

    protected void onPause() {
        if (voiceCapture != null) voiceCapture.pause();
        visible = false; ui.removeCallbacks(poll); ui.removeCallbacks(recordingPoll);
        if (preview != null) { preview.close(); preview = null; }
        super.onPause();
    }

    protected void onDestroy() {
        gone = true;
        if (voiceCapture != null) voiceCapture.pause();
        if (preview != null) { preview.close(); preview = null; }
        ui.removeCallbacksAndMessages(null);
        files.shutdown();
        super.onDestroy();
    }
}
