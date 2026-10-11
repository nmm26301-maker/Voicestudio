package com.voice.studio;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.speech.tts.*;
import android.text.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    static final int SUCCESS = CosmicUi.SUCCESS;
    static final int SPACE_BG=CosmicUi.BG, SPACE_CARD=CosmicUi.SURFACE, SPACE_INK=CosmicUi.INK, SPACE_MUTED=CosmicUi.MUTED, CYAN=CosmicUi.CYAN;
    static final int BG = CosmicUi.BG,
            INK = CosmicUi.INK,
            MUTED = CosmicUi.MUTED,
            PURPLE = CosmicUi.PURPLE,
            PALE = CosmicUi.SECONDARY,
            LINE = CosmicUi.LINE;
    final Handler handler = new Handler(Looper.getMainLooper());
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final OmniEngine omni = new OmniEngine();
    JSONObject omniManifest;
    JSONArray omniLanguages = new JSONArray();
    String omniLang = "zh";
    boolean importingModel;
    TextView modelStatus;
    final ExecutorService fileExecutor = Executors.newSingleThreadExecutor();
    final List<PlaybackUI> playbackViews = new ArrayList<>();
    LinearLayout worksList, latestContainer;
    TextView archiveResults;
    TextView archiveCount, archiveDuration, archiveFavorites;
    int archiveSort;
    final ExecutorService waveformExecutor=Executors.newSingleThreadExecutor();
    final Map<String,float[]> waveformCache=new LinkedHashMap<String,float[]>(64,.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<String,float[]> e) { return size()>80; }
    };
    EditText keyEditor;
    Spinner modelPicker;
    ScrollView pageScroll;
    String previewId = "",
            renderedPage = "",
            keyDraft = null,
            modelDraft = null,
            search = "",
            jobStage = "",
            lastMessage = "";
    boolean favoritesOnly, prepared, foreground, verifying;
    LinearLayout generationPanel;
    DreamProgressView dreamProgress;
    SignalOrbView headerOrb;
    TextView generationTitle, generationStage, generationTime, generationDetail, generationAction, generationCopy;
    String trackedJobId = "", localGenerationState = "idle";
    JSONObject trackedJob, localCompletedJob;
    boolean jobReloadPending, refreshTracked = true, cancelRequested;
    int visibleWorks = 20;
    final Runnable searchUpdate =
            () -> {
                if ("作品".equals(this.page)) populateWorks();
            };
    long jobStarted;
    long seenHistoryRevision = -1;
    boolean historyReloadPending;
    String undoDraft = "";

    static class PlaybackUI {
        String file;
        TextView play, time;
        SeekBar seek;
        ArchiveWaveView wave;

        PlaybackUI(String f, TextView p, TextView t, SeekBar s) {
            file = f;
            play = p;
            time = t;
            seek = s;
        }
    }

    SharedPreferences prefs;
    TextToSpeech tts;
    boolean ttsReady, destroyed, busy, noisyRegistered;
    final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (player != null && isPlaying()) try { player.pause(); } catch (RuntimeException ignored) { }
            stopPreview(); abandonFocus(); updatePlayback();
        }
    };
    String page = "创作",
            engine = "omni",
            voiceId = "",
            voiceName = "默认音色",
            localVoice = "",
            localLang = "zh-CN",
            draft = "",
            model = "s2.1-pro-free";
    float speed = 1;
    LinearLayout root, content, nav;
    EditText editor;
    TextView status, generate, playButton, timeLabel;
    SeekBar progress;
    JSONArray voices = new JSONArray(), history = new JSONArray();
    JSONObject latest, activeJob;
    File jobFile, exportFile;
    AtomicBoolean cancelled = new AtomicBoolean(false);
    MediaPlayer player;
    AudioManager audioManager;
    AudioFocusRequest focusRequest;
    int duration;
    String playingFile = "";
    private final Runnable ticker =
            new Runnable() {
                public void run() {
                    updatePlayback();
                    pollTrackedJob();
                    updateJobUI();
                    refreshHistory();
                    if (!destroyed) handler.postDelayed(this, 400);
                }
            };

    void updatePlayback() {
        int position = 0;
        boolean running = isPlaying();
        if (prepared && player != null)
            try {
                position = player.getCurrentPosition();
            } catch (Exception ignored) {
            }
        for (PlaybackUI ui : playbackViews) {
            boolean active = ui.file.equals(playingFile) && player != null;
            ui.play.setText(active && !prepared ? "加载中…" : active && running ? "暂停" : "播放");
            ui.play.setEnabled(!active || prepared);
            ui.play.setAlpha(active && !prepared ? .55f : 1f);
            ui.seek.setEnabled(active && prepared);
            if (active && prepared) {
                ui.seek.setMax(Math.max(1, duration));
                if (!ui.seek.isPressed()) ui.seek.setProgress(position);
                ui.time.setText(clock(position) + " / " + clock(duration));
            } else {
                ui.seek.setProgress(0);
                ui.time.setText("点击播放，可拖动定位");
            }
            if(ui.wave!=null) ui.wave.progress(active && prepared ? (float)position/Math.max(1,duration) : 0,active&&running);
        }
    }

    void refreshHistory() {
        if (!foreground || historyReloadPending || (!page.equals("作品") && !page.equals("创作"))
                || seenHistoryRevision == StudioFiles.historyRevision) return;
        historyReloadPending = true;
        fileExecutor.execute(() -> {
            try {
                final JSONArray snapshot;
                final long revision;
                synchronized (StudioFiles.class) { snapshot = readHistory(); revision = StudioFiles.historyRevision; }
                handler.post(() -> {
                    historyReloadPending = false;
                    if (destroyed || revision != StudioFiles.historyRevision) return;
                    seenHistoryRevision = revision;
                    if (snapshot.toString().equals(history.toString())) return;
                    history = snapshot; latest = history.optJSONObject(0);
                    if (page.equals("作品")) populateWorks(); else if (page.equals("创作")) showLatest();
                });
            } catch (Exception e) { handler.post(() -> { historyReloadPending = false; }); }
        });
    }

    void updateJobUI() {
        boolean running = isGenerating();
        if(headerOrb!=null) headerOrb.busy(running || !StudioService.active.isEmpty());
        if (generate != null) {
            generate.setText(cancelRequested ? "正在取消…" : running ? "取消生成" : "生成声音");
            generate.setEnabled(!cancelRequested);
        }
        if (status != null)
            status.setText(
                    running
                            ? currentStage()
                                    + " · 已等待 "
                                    + elapsedSeconds()
                                    + " 秒"
                            : !lastMessage.isEmpty() ? lastMessage : readyMessage());
        updateGenerationPanel();
    }

    boolean isGenerating() {
        return busy || (trackedJob != null && SynthesisProgress.active(trackedJob.optString("state")));
    }

    String currentStage() {
        return trackedJob != null ? trackedJob.optString("progress", "等待执行") : jobStage;
    }

    long elapsedSeconds() {
        long start = trackedJob != null ? trackedJob.optLong("started", trackedJob.optLong("created", jobStarted)) : jobStarted;
        long end = trackedJob != null && !SynthesisProgress.active(trackedJob.optString("state")) ? trackedJob.optLong("finished", System.currentTimeMillis()) : System.currentTimeMillis();
        return Math.max(0, (end - start) / 1000);
    }

    void generationPanel() {
        generationPanel = card();
        generationPanel.setBackground(CosmicUi.panel(this,PURPLE));
        if("作品".equals(page)) generationPanel.setBackground(border(SPACE_CARD,0xff42426D,20));
        generationTitle = text("正在酝酿你的声音", 17, PURPLE, true);
        if("作品".equals(page)) generationTitle.setTextColor(CYAN);
        add(generationPanel, generationTitle);
        dreamProgress = new DreamProgressView(this);
        generationPanel.addView(dreamProgress, new LinearLayout.LayoutParams(-1, dp(67)));
        generationStage = text("", 13, INK, false);
        if("作品".equals(page)) generationStage.setTextColor(SPACE_INK);
        add(generationPanel, generationStage);
        gap(generationPanel, 7);
        generationTime = text("", 11, MUTED, false);
        if("作品".equals(page)) generationTime.setTextColor(SPACE_MUTED);
        add(generationPanel, generationTime);
        gap(generationPanel, 9);
        generationDetail = text("", 12, MUTED, false);
        if("作品".equals(page)) generationDetail.setTextColor(SPACE_MUTED);
        generationDetail.setTextIsSelectable(true);
        add(generationPanel, generationDetail);
        generationAction = button("取消生成", false, () -> {
            if (isGenerating()) cancelJob();
            else if (trackedJob != null && ("failed".equals(trackedJob.optString("state")) || "paused".equals(trackedJob.optString("state")))) resumeTrackedJob();
            else if (trackedJob != null && "done".equals(trackedJob.optString("state"))) play(trackedJob);
            else if ("done".equals(localGenerationState) && localCompletedJob != null) play(localCompletedJob);
            else if ("创作".equals(page)) generate();
        });
        add(generationPanel, generationAction);
        generationCopy = button("复制错误详情", false, () -> {
            String detail = trackedJob != null ? trackedJob.optString("failureStage") + "\n" + trackedJob.optString("failureDetail", trackedJob.optString("progress")) : lastMessage;
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("生成错误", detail));
            toast("错误详情已复制");
        });
        add(generationPanel, generationCopy);
        add(content, generationPanel);
        updateGenerationPanel();
    }

    void updateGenerationPanel() {
        if (generationPanel == null) return;
        String state = trackedJob != null ? trackedJob.optString("state") : busy ? "running" : localGenerationState;
        generationPanel.setVisibility("idle".equals(state) ? View.GONE : View.VISIBLE);
        if ("idle".equals(state)) { dreamProgress.setVisibility(View.GONE); return; }
        boolean active = SynthesisProgress.active(state), done = "done".equals(state), failed = "failed".equals(state);
        generationTitle.setText(done ? "你的声音，已生成" : failed ? "生成未完成" : "paused".equals(state) ? "生成已暂停" : "queued".equals(state) ? "声音正在等候" : "正在酝酿你的声音");
        String stage = active ? cancelRequested ? "正在取消，请稍候…" : currentStage()
                : done ? "音频已检查并保存，可在这里播放" : trackedJob != null ? trackedJob.optString("progress") : lastMessage;
        if (stage.length() > 240) stage = stage.substring(0, 240) + "…";
        generationStage.setText(stage);
        float fraction = active ? SynthesisProgress.fraction(currentStage()) : done ? 1 : -1;
        dreamProgress.configure(active, fraction);
        dreamProgress.setVisibility(active || done ? View.VISIBLE : View.GONE);
        String label = fraction >= 0 && active ? "本段推理 " + Math.round(fraction * 100) + "% · " : "";
        generationTime.setText(active ? label + "已等待 " + elapsedSeconds() + " 秒 · 可留在这里等待"
                : done ? "已保存到作品" : "草稿已保留，可调整配置后重新生成");
        String detail = failed && trackedJob != null ? "失败阶段：" + trackedJob.optString("failureStage", "生成声音") + "\n" + trackedJob.optString("failureDetail", "暂无详细错误") : "";
        if (detail.length() > 600) detail = detail.substring(0, 600) + "…\n可复制完整错误详情";
        generationDetail.setText(detail);
        generationDetail.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);
        generationCopy.setVisibility(failed ? View.VISIBLE : View.GONE);
        generationAction.setText(active ? cancelRequested ? "正在取消…" : "取消生成" : done ? "播放这段声音" : trackedJob != null ? "继续此任务" : "重新生成");
        generationAction.setEnabled(!cancelRequested);
        generationAction.setVisibility(active || done || trackedJob != null || "创作".equals(page) ? View.VISIBLE : View.GONE);
    }

    void trackJob(String id, JSONObject request) throws Exception {
        trackedJobId = id;
        trackedJob = new JSONObject(request.toString()).put("id", id).put("state", "queued").put("progress", "等待执行");
        jobStarted = System.currentTimeMillis();
        localGenerationState = "idle"; lastMessage = ""; cancelRequested = false; refreshTracked = true;
        prefs.edit().putString("main-generation-job", id).apply();
        releasePlayer();
        updateJobUI(); pollTrackedJob();
    }

    void pollTrackedJob() {
        if (!foreground || destroyed || trackedJobId.isEmpty() || jobReloadPending
                || (!refreshTracked && trackedJob != null && !SynthesisProgress.active(trackedJob.optString("state")))) return;
        final String id = trackedJobId;
        jobReloadPending = true;
        fileExecutor.execute(() -> {
            JSONObject snapshot = null;
            boolean missing = false;
            try { snapshot = StudioFiles.read(StudioFiles.job(this, id)); }
            catch (FileNotFoundException e) { missing = true; }
            catch (Exception ignored) { }
            final JSONObject result = snapshot;
            final boolean removed = missing;
            handler.post(() -> {
                jobReloadPending = false;
                if (destroyed || !id.equals(trackedJobId)) return;
                if (result != null) {
                    trackedJob = result; refreshTracked = false;
                    if (!SynthesisProgress.active(result.optString("state"))) {
                        cancelRequested = false;
                        lastMessage = "done".equals(result.optString("state")) ? "声音已保存，可直接播放" : result.optString("progress");
                    }
                } else if (removed) {
                    trackedJobId = ""; trackedJob = null; cancelRequested = false;
                    prefs.edit().remove("main-generation-job").apply();
                    lastMessage = "此任务已移除，草稿仍保留";
                }
                updateJobUI();
            });
        });
    }

    void resumeTrackedJob() {
        if (isGenerating() || trackedJobId.isEmpty()) return;
        try {
            StudioService.resume(this, trackedJobId);
            trackedJob.put("state", "queued").put("progress", "等待继续");
            refreshTracked = true; lastMessage = ""; cancelRequested = false;
            updateJobUI(); pollTrackedJob();
        } catch (Exception e) { toast(e.getMessage()); }
    }

    void generationError(String message) {
        trackedJobId = ""; trackedJob = null; cancelRequested = false;
        prefs.edit().remove("main-generation-job").apply();
        localGenerationState = "failed"; lastMessage = message == null ? "生成失败，请重试" : message;
        updateJobUI(); toast(lastMessage);
    }

    String readyMessage() {
        if ("fish".equals(engine)) {
            if (getSharedPreferences("secure", 0).getString("token", "").isEmpty()) return "Fish：先在设置保存 API Key";
            if (voiceId.isEmpty()) return "Fish：先在音色页添加并选择 Voice ID";
            return "Fish 配置已填写 · 生成时联网";
        }
        if ("omni".equals(engine))
            return omniReady() ? "OmniVoice 已导入 · 本机离线合成" : "先在设置导入 OmniVoice 模型包";
        return "使用手机已安装的离线语音包";
    }

    void jobProgress(String id, String message) {
        handler.post(
                () -> {
                    if (!destroyed && activeJob != null && id.equals(activeJob.optString("id"))) {
                        jobStage = message;
                        updateJobUI();
                    }
                });
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("studio", 0);
        archiveSort=prefs.getInt("archive-sort",0);
        trackedJobId = prefs.getString("main-generation-job", "");
        jobStarted = System.currentTimeMillis();
        if (!trackedJobId.isEmpty()) {
            trackedJob = new JSONObject();
            try { trackedJob.put("state", "queued").put("progress", "正在恢复任务状态…"); } catch (JSONException ignored) { }
        }
        if (!prefs.getBoolean("fast-default-140", false))
            prefs.edit().putInt("voice-steps", 16).putBoolean("fast-default-140", true).apply();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        IntentFilter noisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(noisyReceiver, noisyFilter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(noisyReceiver, noisyFilter);
        noisyRegistered = true;
        engine = prefs.getString("engine", "omni");
        if (!Arrays.asList("fish", "omni", "local").contains(engine)) engine = "omni";
        omniLang = prefs.getString("omniLang", "zh");
        try {
            android.system.Os.setenv("ORT_DISABLE_TELEMETRY", "1", true);
            omniManifest = new JSONObject(readAsset("omni-model-manifest.json"));
            omniLanguages = new JSONArray(readAsset("omni-languages.json"));
            boolean valid = false;
            for (int i = 0; i < omniLanguages.length(); i++)
                if (omniLanguages.getJSONObject(i).getString("id").equals(omniLang)) valid = true;
            if (!valid) omniLang = "zh";
        } catch (Exception e) {
            toast("离线配置无法加载");
        }
        voiceId = prefs.getString("voiceId", "");
        voiceName = prefs.getString("voiceName", "默认音色");
        localVoice = prefs.getString("localVoice", "");
        localLang = prefs.getString("localLang", "zh-CN");
        draft = prefs.getString("draft", "");
        speed = prefs.getFloat("speed", 1);
        model = prefs.getString("model", "s2.1-pro-free");
        try {
            voices = new JSONArray(prefs.getString("voices", "[]"));
            history = readHistory();
        } catch (Exception ignored) {
        }
        if (history.length() > 0) latest = history.optJSONObject(0);
        if (saved != null) {
            page = saved.getString("page", "创作");
            String exported = saved.getString("exportFile", "");
            exporting = saved.getBoolean("exporting", false);
            if (exported.matches("[a-f0-9-]+\\.(mp3|wav)"))
                exportFile = new File(new File(getFilesDir(), "audio"), exported);
        }
        if (saved == null) acceptText(getIntent());
        tts =
                new TextToSpeech(
                        this,
                        result -> {
                            ttsReady = result == TextToSpeech.SUCCESS;
                            if (ttsReady) {
                                tts.setOnUtteranceProgressListener(
                                        new UtteranceProgressListener() {
                                            public void onStart(String id) {}

                                            public void onDone(String id) {
                                                handler.post(
                                                        () -> {
                                                            if (id.startsWith("preview-")) {
                                                                if (!id.equals(previewId)) return;
                                                                previewId = "";
                                                                if (player == null) abandonFocus();
                                                                return;
                                                            }
                                                            if (activeJob != null
                                                                    && id.equals(
                                                                            activeJob.optString(
                                                                                    "id"))
                                                                    && !cancelled.get())
                                                                finishJob();
                                                        });
                                            }

                                            public void onError(String id) {
                                                handler.post(
                                                        () -> {
                                                            if (id.startsWith("preview-")) {
                                                                if (!id.equals(previewId)) return;
                                                                previewId = "";
                                                                if (player == null) abandonFocus();
                                                                toast("试听失败，请检查语音包");
                                                                return;
                                                            }
                                                            if (activeJob != null
                                                                    && id.equals(
                                                                            activeJob.optString(
                                                                                    "id")))
                                                                failJob("手机语音引擎无法生成，请检查已安装的语音数据");
                                                        });
                                            }
                                        });
                            }
                            if (!destroyed && "音色".equals(page)) render();
                        });
        CosmicUi.install(this);
        render();
        handler.post(ticker);
    }

    private void acceptText(Intent i) {
        if ("com.voice.studio.SELECT_ENGINE".equals(i.getAction())) {
            String selected = i.getStringExtra("engine");
            if (Arrays.asList("fish", "omni", "local").contains(selected)) { engine = selected; saveSettings(); }
            String text = i.getStringExtra("retryText");
            if (text != null && !text.trim().isEmpty() && text.length() <= 12000) {
                if (!draft.equals(text)) undoDraft = draft;
                draft = text; editor = null; prefs.edit().putString("draft", draft).apply();
            }
        }
        if (Intent.ACTION_SEND.equals(i.getAction())) {
            String s = i.getStringExtra(Intent.EXTRA_TEXT);
            if (s != null) {
                draft = s;
                editor = null;
                prefs.edit().putString("draft", draft).apply();
            }
        } else if (Intent.ACTION_PROCESS_TEXT.equals(i.getAction())) {
            CharSequence s = i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            if (s != null) {
                draft = s.toString();
                editor = null;
                prefs.edit().putString("draft", draft).apply();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        acceptText(i);
        page = "创作";
        render();
    }

    int dp(float x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    GradientDrawable border(int color, int stroke, int radius) {
        GradientDrawable d = shape(color, radius);
        d.setStroke(dp(1), stroke);
        return d;
    }

    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    void gap(LinearLayout l, int n) {
        View v = new View(this);
        l.addView(v, new LinearLayout.LayoutParams(1, dp(n)));
    }

    void add(LinearLayout l, View v) {
        l.addView(v, new LinearLayout.LayoutParams(-1, -2));
    }

    void flexible(LinearLayout l, View v) {
        l.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
    }

    LinearLayout card() {
        LinearLayout l = col();
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(CosmicUi.panel(this,PURPLE));
        return l;
    }

    TextView button(String title, boolean primary, Runnable action) {
        TextView t = text(title, 15, primary ? Color.WHITE : PURPLE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(15), dp(14), dp(15));
        t.setBackground(shape(primary ? PURPLE : PALE, 15));
        t.setMinHeight(dp(48));
        t.setClickable(true);
        t.setFocusable(true);
        t.setContentDescription(title);
        t.setBackground(CosmicUi.button(this,primary));
        CosmicUi.press(t);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    void sideGap(LinearLayout row, int n) {
        View v = new View(this);
        row.addView(v, new LinearLayout.LayoutParams(dp(n), 1));
    }

    EditText field(String hint, String value, boolean secret) {
        EditText e = new EditText(this);
        e.setTextSize(15);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setText(value);
        e.setPadding(dp(14), dp(13), dp(14), dp(13));
        CosmicUi.field(e);
        e.setSingleLine(true);
        e.setInputType(
                secret
                        ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                        : InputType.TYPE_CLASS_TEXT);
        return e;
    }

    void info(LinearLayout l, String title, String detail) {
        add(l, text(title, 16, INK, true));
        gap(l, 7);
        TextView t = text(detail, 13, MUTED, false);
        t.setLineSpacing(dp(3), 1);
        add(l, t);
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void saveSettings() {
        prefs.edit()
                .putString("engine", engine)
                .putString("omniLang", omniLang)
                .putString("voiceId", voiceId)
                .putString("voiceName", voiceName)
                .putString("localVoice", localVoice)
                .putString("localLang", localLang)
                .putFloat("speed", speed)
                .putString("model", model)
                .putString("voices", voices.toString())
                .apply();
    }

    private void saveDraft() {
        if (editor != null) draft = editor.getText().toString();
        prefs.edit().putString("draft", draft).apply();
    }

    private void change(String name) {
        saveDraft();
        hideKeyboard();
        stopPreview();
        lastMessage = "";
        if ("工具".equals(name)) { openWorkspace("转写"); return; }
        page = name;
        render();
    }

    void openWorkspace(String section) {
        startActivity(new Intent(this, WorkspaceActivity.class).putExtra("section", section));
    }

    private void hideKeyboard() {
        View f = getCurrentFocus();
        if (f != null)
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(f.getWindowToken(), 0);
    }

    void render() {
        if (destroyed) return;
        boolean same = page.equals(renderedPage);
        int y = same && pageScroll != null ? pageScroll.getScrollY() : 0;
        int cursor = same && editor != null ? editor.getSelectionStart() : -1;
        saveDraft();
        if (keyEditor != null) keyDraft = keyEditor.getText().toString();
        if (modelPicker != null) modelDraft = modelPicker.getSelectedItem().toString();
        editor = null;
        progress = null;
        timeLabel = null;
        playButton = null;
        generate = null;
        status = null;
        generationPanel = null;
        dreamProgress = null;
        keyEditor = null;
        modelPicker = null;
        latestContainer = null;
        worksList = null;
        archiveResults = null;
        archiveCount = null; archiveDuration = null; archiveFavorites = null;
        playbackViews.clear();
        renderedPage = page;
        root = col();
        boolean cosmic=true;
        root.setBackgroundColor(cosmic ? SPACE_BG : BG);
        CosmicUi.install(this);
        setContentView(root);
        LinearLayout header = row();
        header.setPadding(dp(22), dp(18), dp(22), dp(17));
        LinearLayout word = col();
        add(word, text("VoiceStudio", 23, cosmic ? SPACE_INK : INK, true));
        gap(word, 5);
        add(word, text(cosmic ? "声音，值得被收藏" : "让文字，拥有声音", 12, cosmic ? SPACE_MUTED : MUTED, false));
        TextView badge=text("设置".equals(page)?"控制中心":"音色".equals(page)?"音色实验室":"作品".equals(page)?"声音星库":"创作舱",10,CYAN,true); badge.setLetterSpacing(.12f); gap(word,7); add(word,badge);
        flexible(header, word);
        headerOrb=new SignalOrbView(this,"设置".equals(page)?CosmicUi.SUCCESS:"音色".equals(page)?PURPLE:CYAN,true);
        header.addView(headerOrb,new LinearLayout.LayoutParams(dp(72),dp(72)));
        add(root, header);
        ScrollView scroll = new ScrollView(this);
        pageScroll = scroll;
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(20), dp(8), dp(20), dp(20));
        content = col();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if ("创作".equals(page)) creation();
        else if ("音色".equals(page)) voicePage();
        else if ("作品".equals(page)) works();
        else settings();
        if (!"创作".equals(page)) generationPanel();
        if("创作".equals(page) && generate!=null) {
            LinearLayout dock=col(); dock.setPadding(dp(20),dp(10),dp(20),dp(8)); dock.setBackground(border(BG,LINE,0));
            if(dreamProgress.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup)dreamProgress.getParent()).removeView(dreamProgress);
            dock.addView(dreamProgress,new LinearLayout.LayoutParams(-1,dp(48))); add(dock,generate); gap(dock,7);
            status.setMaxLines(2); status.setEllipsize(android.text.TextUtils.TruncateAt.END);add(dock,status);add(root,dock);
        }
        nav = row();
        nav.setBackgroundColor(cosmic ? SPACE_BG : Color.WHITE);
        nav.setPadding(dp(8), dp(8), dp(8), dp(10));
        String[] names = {"创作", "音色", "工具", "作品", "设置"}, icons = {"edit", "wave", "tools", "folder", "settings"};
        for (int i = 0; i < names.length; i++) {
            String n = names[i];
            boolean chosen = n.equals(page);
            LinearLayout item = col();
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(5), 0, dp(4));
            if(chosen)item.setBackground(border(0xff1B3049,CYAN,17));
            CosmicUi.press(item);
            IconView iv = new IconView(this, icons[i], chosen ? cosmic ? CYAN : PURPLE : cosmic ? SPACE_MUTED : MUTED);
            item.addView(iv, new LinearLayout.LayoutParams(dp(23), dp(23)));
            gap(item, 5);
            TextView t = text(n, 11, chosen ? cosmic ? CYAN : PURPLE : cosmic ? SPACE_MUTED : MUTED, chosen);
            item.addView(t);
            item.setContentDescription(n);
            item.setOnClickListener(v -> change(n));
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(61), 1));
        }
        add(root, nav);
        if (cursor >= 0 && editor != null) editor.setSelection(Math.min(cursor, editor.length()));
        scroll.post(() -> scroll.scrollTo(0, y));
        updatePlayback();
        updateJobUI();
        if(!same)CosmicUi.enter(content,1);
    }

    void heading(String name, String subtitle) {
        add(content, text(name, 28, INK, true));
        gap(content, 8);
        add(content, text(subtitle, 13, MUTED, false));
        gap(content, 22);
    }

    void creation() {
        add(
                content,
                button(
                        "打开工具与任务",
                        false,
                        () -> openWorkspace("任务")));
        gap(content, 16);
        heading("今天，想说些什么？", "写下文字，制作属于你的第一段声音。");
        LinearLayout draftCard = card();
        LinearLayout label = row();
        flexible(label, text("配音文本", 15, INK, true));
        TextView paste = text("粘贴", 13, PURPLE, true);
        paste.setPadding(dp(12), dp(8), 0, dp(8));
        paste.setOnClickListener(
                v -> {
                    android.content.ClipboardManager c =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (c.hasPrimaryClip()) {
                        CharSequence clip = c.getPrimaryClip().getItemAt(0).coerceToText(this);
                        editor.setText(clip);
                        if (clip.length() > 12000) toast("全文已保留，请缩短到 12000 字以内再生成");
                    } else toast("剪贴板没有文字");
                });
        label.addView(paste);
        add(draftCard, label);
        gap(draftCard, 10);
        editor = new EditText(this);
        editor.setTextSize(17);
        editor.setTextColor(INK);
        editor.setHintTextColor(MUTED);
        editor.setGravity(Gravity.TOP);
        editor.setBackgroundColor(Color.TRANSPARENT);
        editor.setPadding(0, dp(8), 0, dp(8));
        editor.setHint("例如：愿你今天遇见的，都是温柔的小事。\n\n也可以粘贴文章、旁白或想练习的句子。");
        editor.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setMinHeight(dp(150));
        editor.setText(draft);
        add(draftCard, editor);
        LinearLayout foot = row();
        flexible(foot, text("草稿自动保存", 11, MUTED, false));
        TextView count = text(draft.length() + " / 12000", 11, MUTED, false);
        foot.addView(count);
        add(draftCard, foot);
        editor.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

                    public void onTextChanged(CharSequence s, int st, int before, int c) {
                        draft = s.toString();
                        count.setText(
                                s.length() + " / 12000" + (s.length() > 12000 ? " · 超出上限" : ""));
                        count.setTextColor(s.length() > 12000 ? 0xffFF8FAB : MUTED);
                        prefs.edit().putString("draft", draft).apply();
                    }

                    public void afterTextChanged(Editable e) {}
                });
        gap(draftCard, 12);
        LinearLayout draftActions = row();
        flexible(draftActions, button("示例文本", false, () -> sampleText()));
        sideGap(draftActions, 8);
        flexible(
                draftActions,
                button(
                        "清空 / 撤销",
                        false,
                        () -> {
                            if (editor.getText().length() == 0 && !undoDraft.isEmpty()) {
                                editor.setText(undoDraft);
                                undoDraft = "";
                                return;
                            }
                            if (editor.getText().length() == 0) return;
                            new CosmicDialogBuilder(this)
                                    .setTitle("清空草稿？")
                                    .setMessage("清空后可点击同一按钮撤销一次。")
                                    .setNegativeButton("取消", null)
                                    .setPositiveButton(
                                            "清空",
                                            (d, w) -> {
                                                undoDraft = editor.getText().toString();
                                                editor.setText("");
                                            })
                                    .show();
                        }));
        add(draftCard, draftActions);
        add(content, draftCard);
        gap(content, 14);
        LinearLayout voice = card();
        LinearLayout vr = row();
        IconView vi = new IconView(this, "wave", PURPLE);
        vi.setBackground(shape(PALE, 12));
        vr.addView(vi, new LinearLayout.LayoutParams(dp(46), dp(46)));
        sideGap(vr, 13);
        LinearLayout desc = col();
        add(desc, text(selectedVoiceName(), 16, INK, true));
        gap(desc, 6);
        add(
                desc,
                text(
                        "fish".equals(engine) ? "云端合成 · 按文本识别语言" : "omni".equals(engine) ? "原生 646 种语言 · 本机推理" : "使用手机已安装的离线语音包",
                        12,
                        MUTED,
                        false));
        flexible(vr, desc);
        vr.addView(text("›", 25, MUTED, false));
        add(voice, vr);
        voice.setOnClickListener(v -> change("音色"));
        gap(voice, 12);
        add(
                voice,
                button(
                        "设计与克隆音色",
                        false,
                        () -> openWorkspace("音色")));
        add(content, voice);
        gap(content, 14);
        if("omni".equals(engine)) {
            LinearLayout mode=card();
            info(mode,"生成速度",prefs.getInt("voice-steps",16)==16 ? "快速 · 16 步推理，减少计算等待" : "质量优先 · 32 步推理，需要更多等待");
            gap(mode,10);
            TextView toggle=button(prefs.getInt("voice-steps",16)==16 ? "切换质量优先" : "切换快速生成",false,()->{
                prefs.edit().putInt("voice-steps",prefs.getInt("voice-steps",16)==16 ? 32 : 16).apply();
                render();
            });
            add(mode,toggle); gap(mode,7);
            add(mode,text("快速模式的音质可能略有变化。语速只影响说话节奏；已提交的任务沿用原配置。",11,MUTED,false));
            add(content,mode); gap(content,14);
        }
        LinearLayout speedRow = row();
        flexible(speedRow, text("语速", 14, INK, true));
        TextView speedText = text(String.format(Locale.US, "%.2f×", speed), 14, PURPLE, true);
        speedRow.addView(speedText);
        add(content, speedRow);
        SeekBar slider = new SeekBar(this);
        slider.setMax(100);
        slider.setProgress(Math.round((speed - 0.5f) * 100));
        slider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar s, int p, boolean user) {
                        if (user) {
                            speed = .5f + p / 100f;
                            speedText.setText(String.format(Locale.US, "%.2f×", speed));
                            saveSettings();
                        }
                    }

                    public void onStartTrackingTouch(SeekBar s) {}

                    public void onStopTrackingTouch(SeekBar s) {}
                });
        add(content, slider);
        gap(content, 12);
        generate =
                button(
                        isGenerating() ? "取消生成" : "生成声音",
                        true,
                        () -> {
                            if (isGenerating()) cancelJob();
                            else generate();
                        });

        gap(content, 10);
        status = text(readyMessage(), 12, MUTED, false);
        status.setGravity(Gravity.CENTER);

        gap(content, 12);
        generationPanel();
        latestContainer = col();
        add(content, latestContainer);
        showLatest();
    }

    void showLatest() {
        if (latestContainer == null) return;
        playbackViews.clear();
        latestContainer.removeAllViews();
        if (latest != null) {
            gap(latestContainer, 20);
            add(latestContainer, text("最近的声音", 14, INK, true));
            gap(latestContainer, 12);
            add(latestContainer, playerCard(latest));
        }
        updatePlayback();
    }

    void sampleText() {
        String[] names = {"温柔问候", "视频旁白", "英语练习"};
        String[] samples = {
            "愿你今天遇见的，都是温柔的小事。放慢脚步，也别忘了听听自己的声音。",
            "每一次出发，都藏着新的可能。用一段声音，记录今天的故事。",
            "Good morning. Take a deep breath, and enjoy the little things today."
        };
        new CosmicDialogBuilder(this)
                .setTitle("选择示例文本")
                .setItems(
                        names,
                        (d, w) -> {
                            if (editor.length() > 0)
                                new CosmicDialogBuilder(this)
                                        .setTitle("替换当前草稿？")
                                        .setNegativeButton("取消", null)
                                        .setPositiveButton(
                                                "替换",
                                                (x, y) -> {
                                                    undoDraft = editor.getText().toString();
                                                    editor.setText(samples[w]);
                                                })
                                        .show();
                            else editor.setText(samples[w]);
                        })
                .show();
    }

    void voicePage() {
        heading("找到你的声音", "选择 Fish Audio、OmniVoice 或手机语音。");
        LinearLayout toggle = row();
        flexible(toggle, button("Fish Audio", "fish".equals(engine), () -> {
            engine = "fish"; saveSettings(); render();
        }));
        sideGap(toggle, 6);
        flexible(
                toggle,
                button(
                        "OmniVoice",
                        "omni".equals(engine),
                        () -> {
                            engine = "omni";
                            saveSettings();
                            render();
                        }));
        sideGap(toggle, 10);
        flexible(
                toggle,
                button(
                        "手机语音",
                        "local".equals(engine),
                        () -> {
                            engine = "local";
                            saveSettings();
                            render();
                        }));
        add(content, toggle);
        gap(content, 20);
        if ("fish".equals(engine)) {
            fishVoicePage(); return;
        }
        if ("omni".equals(engine)) {
            omniVoicePage();
            return;
        }
        {
            LinearLayout c = card();
            info(c, "朗读语言", "选择语言后，将使用已安装的手机语音包。");
            gap(c, 13);
            String[] langs = {
                "zh-CN", "en-US", "ja-JP", "ko-KR", "zh-HK", "yue-HK", "fr-FR", "de-DE", "es-ES"
            };
            String[] labels = new String[langs.length];
            for (int i = 0; i < langs.length; i++) labels[i] = languageName(langs[i]);
            Spinner spinner = new Spinner(this);
            spinner.setAdapter(
                    new ArrayAdapter<String>(
                            this, android.R.layout.simple_spinner_dropdown_item, labels));
            for (int i = 0; i < langs.length; i++)
                if (langs[i].equals(localLang)) spinner.setSelection(i);
            spinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        public void onNothingSelected(AdapterView<?> p) {}

                        public void onItemSelected(AdapterView<?> p, View v, int i, long id) {
                            if (!langs[i].equals(localLang)) {
                                localLang = langs[i];
                                localVoice = "";
                                saveSettings();
                                render();
                            }
                        }
                    });
            add(c, spinner);
            gap(c, 8);
            add(c, text("语言标签用于选择手机语音包，不会翻译文字。", 12, MUTED, false));
            add(content, c);
            gap(content, 16);
            if (!ttsReady) {
                add(content, text("语音引擎尚未就绪，请检查手机的文字转语音设置。", 14, MUTED, false));
                return;
            }
            Set<Voice> available = tts.getVoices();
            int n = 0;
            if (available != null) {
                List<Voice> sorted = new ArrayList<Voice>(available);
                Collections.sort(sorted, (a, b) -> a.getName().compareTo(b.getName()));
                for (Voice v : sorted) {
                    Locale language = Locale.forLanguageTag(localLang);
                    if (!AudioChecks.localeMatches(v.getLocale(), language)
                            || v.isNetworkConnectionRequired()) continue;
                    n++;
                    LinearLayout vc = card();
                    boolean chosen = v.getName().equals(localVoice);
                    vc.setBackground(border(CosmicUi.SURFACE, chosen ? PURPLE : LINE, 20));
                    info(
                            vc,
                            languageName(localLang) + " · 音色 " + n,
                            (v.isNetworkConnectionRequired() ? "需要联网" : "可离线使用")
                                    + " · "
                                    + v.getName());
                    gap(vc, 10);
                    LinearLayout options = row();
                    flexible(
                            options,
                            button(
                                    chosen ? "已选择" : "选择",
                                    chosen,
                                    () -> {
                                        localVoice = v.getName();
                                        saveSettings();
                                        render();
                                    }));
                    sideGap(options, 8);
                    flexible(options, button("试听", false, () -> previewVoice(v)));
                    add(vc, options);
                    add(content, vc);
                    gap(content, 12);
                }
            }
            if (n == 0) add(content, text("没有找到这个语言的音色，请安装对应语音包或更换手机 TTS 引擎。", 14, MUTED, false));
            gap(content, 12);
            add(
                    content,
                    button(
                            "手机语音设置",
                            false,
                            () -> {
                                try {
                                    startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
                                } catch (Exception e) {
                                    toast("请到系统设置搜索：文字转语音");
                                }
                            }));
        }
    }

    void stopPreview() {
        previewId = "";
        if (ttsReady && !busy) tts.stop();
    }

    void previewVoice(Voice voice) {
        if (busy) {
            toast("请先完成或取消生成");
            return;
        }
        releasePlayer();
        if (tts.setVoice(voice) != TextToSpeech.SUCCESS) {
            toast("音色暂时不可用");
            return;
        }
        if (!requestFocus()) return;
        tts.setSpeechRate(speed);
        String lang = voice.getLocale().getLanguage();
        String sample =
                (lang.equals("zh") || lang.equals("yue"))
                        ? "你好，欢迎来到你的声音工作室。"
                        : lang.equals("en")
                                ? "Hello. Welcome to your voice studio."
                                : lang.equals("ja")
                                        ? "こんにちは。今日も素敵な一日を。"
                                        : lang.equals("ko")
                                                ? "안녕하세요. 좋은 하루 보내세요."
                                                : lang.equals("fr")
                                                        ? "Bonjour, bienvenue dans votre studio."
                                                        : lang.equals("de")
                                                                ? "Hallo, willkommen in deinem"
                                                                        + " Studio."
                                                                : "Hola, bienvenido a tu estudio.";
        previewId = "preview-" + UUID.randomUUID().toString();
        if (tts.speak(sample, TextToSpeech.QUEUE_FLUSH, new Bundle(), previewId)
                != TextToSpeech.SUCCESS) {
            abandonFocus();
            toast("无法试听，请检查语音包");
        }
    }

    void voiceCard(String name, String id, boolean builtin) {
        boolean selected = id.equals(voiceId);
        LinearLayout c = card();
        c.setBackground(border(CosmicUi.SURFACE, selected ? PURPLE : LINE, 20));
        LinearLayout r = row();
        LinearLayout d = col();
        add(d, text(name, 16, INK, true));
        gap(d, 6);
        add(
                d,
                text(
                        builtin
                                ? "由服务选择默认声音"
                                : "已收藏 · " + id.substring(0, Math.min(8, id.length())) + "…",
                        12,
                        MUTED,
                        false));
        flexible(r, d);
        r.addView(text(selected ? "✓" : "›", 22, selected ? PURPLE : MUTED, true));
        add(c, r);
        c.setOnClickListener(
                v -> {
                    voiceId = id;
                    voiceName = name;
                    saveSettings();
                    render();
                    toast("已选择 " + name);
                });
        if (!builtin) {
            gap(c, 12);
            add(c, button("管理音色", false, () -> manageVoice(name, id)));
            c.setOnLongClickListener(
                    v -> {
                        manageVoice(name, id);
                        return true;
                    });
        }
        add(content, c);
        gap(content, 12);
    }

    void manageVoice(String name, String id) {
        int index = -1;
        for (int i = 0; i < voices.length(); i++)
            if (id.equals(voices.optJSONObject(i).optString("id"))) index = i;
        if (index < 0) return;
        final int at = index;
        new CosmicDialogBuilder(this)
                .setTitle(name)
                .setItems(
                        new String[] {"修改音色", "删除音色"},
                        (d, w) -> {
                            if (w == 0) voiceDialog(at);
                            else
                                new CosmicDialogBuilder(this)
                                        .setTitle("删除收藏音色？")
                                        .setMessage("已经生成的作品会保留。")
                                        .setNegativeButton("取消", null)
                                        .setPositiveButton(
                                                "删除",
                                                (x, y) -> {
                                                    voices.remove(at);
                                                    if (voiceId.equals(id)) {
                                                        voiceId = "";
                                                        voiceName = "默认音色";
                                                    }
                                                    saveSettings();
                                                    render();
                                                })
                                        .show();
                        })
                .show();
    }

    void voiceDialog(int index) {
        JSONObject old = index >= 0 ? voices.optJSONObject(index) : null;
        LinearLayout l = col();
        l.setPadding(dp(20), dp(16), dp(20), 0);
        EditText name = field("音色名称", old == null ? "" : old.optString("name"), false),
                id = field("Voice ID（32位字符）", old == null ? "" : old.optString("id"), false);
        add(l, name);
        gap(l, 12);
        add(l, id);
        AlertDialog dialog =
                new CosmicDialogBuilder(this)
                        .setTitle(index >= 0 ? "修改音色" : "添加音色")
                        .setView(l)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("保存", null)
                        .create();
        dialog.setOnShowListener(
                x ->
                        dialog.getButton(-1)
                                .setOnClickListener(
                                        v -> {
                                            String nm = name.getText().toString().trim(),
                                                    ref = id.getText().toString().trim();
                                            if (nm.isEmpty()) {
                                                name.setError("请输入名称");
                                                return;
                                            }
                                            if (!ref.matches("[a-fA-F0-9]{32}")) {
                                                id.setError("请粘贴有效的 32 位 Voice ID");
                                                return;
                                            }
                                            for (int i = 0; i < voices.length(); i++)
                                                if (i != index
                                                        && ref.equalsIgnoreCase(
                                                                voices.optJSONObject(i)
                                                                        .optString("id"))) {
                                                    id.setError("这个音色已收藏");
                                                    return;
                                                }
                                            try {
                                                JSONObject item =
                                                        new JSONObject()
                                                                .put("name", nm)
                                                                .put(
                                                                        "id",
                                                                        ref.toLowerCase(
                                                                                Locale.ROOT));
                                                if (index >= 0) voices.put(index, item);
                                                else voices.put(item);
                                                if (old != null
                                                        && voiceId.equals(old.optString("id"))) {
                                                    voiceId = ref.toLowerCase(Locale.ROOT);
                                                    voiceName = nm;
                                                } else if (voiceId.isEmpty()) {
                                                    voiceId = ref.toLowerCase(Locale.ROOT);
                                                    voiceName = nm;
                                                }
                                                saveSettings();
                                                dialog.dismiss();
                                                render();
                                            } catch (Exception e) {
                                                toast("保存失败");
                                            }
                                        }));
        dialog.show();
    }

    void works() {
        LinearLayout hero=card();
        hero.setBackground(new ArchivePanelDrawable(getResources().getDisplayMetrics().density));
        TextView eyebrow=text("VOICE / ARCHIVE",10,CYAN,true); eyebrow.setLetterSpacing(.18f); eyebrow.setTypeface(Typeface.MONOSPACE); add(hero,eyebrow);
        gap(hero,10); add(hero,text("声音星库",28,SPACE_INK,true));
        gap(hero,14); LinearLayout stats=row();
        int favorites=0; for(int i=0;i<history.length();i++)if(history.optJSONObject(i).optBoolean("favorite"))favorites++;
        archiveCount=archiveStat(stats,Integer.toString(history.length()),"段声音");
        archiveDuration=archiveStat(stats,WorksCatalog.duration(WorksCatalog.totalDuration(history)),"累计时长");
        archiveFavorites=archiveStat(stats,Integer.toString(favorites),"已收藏"); add(hero,stats); gap(hero,16);
        TextView local=text("●  本机保存  /  随时播放与分享",10,0xffA3C6EB,false); add(hero,local);
        add(content,hero); gap(content,20);
        EditText query=field("搜索标题、文字或音色",search,false);
        query.setBackground(border(0xff111B30,0xff2B3954,16)); query.setTextColor(SPACE_INK); query.setHintTextColor(SPACE_MUTED);
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH); add(content,query); gap(content,12);
        LinearLayout filter=row();
        flexible(filter,archiveButton("全部",!favoritesOnly,()->{ favoritesOnly=false; render(); })); sideGap(filter,8);
        flexible(filter,archiveButton("收藏",favoritesOnly,()->{ favoritesOnly=true; render(); })); sideGap(filter,8);
        String[] sorts={"最近生成","最早生成","最长音频"};
        TextView order=archiveButton(sorts[Math.max(0,Math.min(2,archiveSort))]+" ↓",false,()->{
            new CosmicDialogBuilder(this).setTitle("声音排列").setSingleChoiceItems(sorts,archiveSort,(d,n)->{
                archiveSort=n; prefs.edit().putInt("archive-sort",n).apply(); d.dismiss(); render();
            }).setNegativeButton("关闭",null).show();
        });
        filter.addView(order,new LinearLayout.LayoutParams(dp(108),dp(44))); add(content,filter); gap(content,22);
        archiveResults=text("声音档案",13,SPACE_MUTED,true); add(content,archiveResults); gap(content,12);
        worksList=col(); add(content,worksList); populateWorks();
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int d) {}
            public void onTextChanged(CharSequence s,int a,int b,int c) {
                search=s.toString(); visibleWorks=20; handler.removeCallbacks(searchUpdate); handler.postDelayed(searchUpdate,250);
            }
            public void afterTextChanged(Editable e) {}
        });
    }

    TextView archiveStat(LinearLayout row,String value,String label) {
        LinearLayout stat=col(); TextView number=text(value,21,SPACE_INK,true); add(stat,number); gap(stat,4); add(stat,text(label,10,SPACE_MUTED,false)); flexible(row,stat); return number;
    }

    TextView archiveButton(String label,boolean primary,Runnable action) {
        TextView view=button(label,false,action); view.setTextSize(12);
        view.setTextColor(primary ? SPACE_BG : SPACE_INK);
        if(primary) {
            GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xff67E8F9,0xffA5B4FC});
            gradient.setCornerRadius(dp(13)); view.setBackground(gradient);
        } else view.setBackground(border(0xff1B263F,0xff35415E,13));
        view.setForeground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x3367E8F9),null,shape(Color.WHITE,13)));
        return view;
    }

    LinearLayout archiveCard(JSONObject item) {
        LinearLayout card=card(); card.setBackground(border(SPACE_CARD,0xff2E3A59,21));
        String file=item.optString("file"), format=file.endsWith("wav") ? "WAV" : "MP3";
        String source="fish".equals(item.optString("engine")) ? "FISH AUDIO" : "local".equals(item.optString("engine")) ? "ANDROID TTS" : "OMNIVOICE";
        LinearLayout top=row(); TextView badge=text(source+" / "+format,10,CYAN,true); badge.setLetterSpacing(.07f); flexible(top,badge);
        TextView star=archiveButton(item.optBoolean("favorite") ? "★" : "☆",false,()->{
            if(updateWork(item,"favorite",!item.optBoolean("favorite")))render();
        }); star.setTextSize(19); star.setContentDescription((item.optBoolean("favorite")?"取消收藏 ":"收藏 ")+titleOf(item));
        top.addView(star,new LinearLayout.LayoutParams(dp(48),dp(48))); sideGap(top,6);
        TextView menu=archiveButton("···",false,()->workMenu(item)); menu.setTextSize(20); menu.setContentDescription("管理作品 "+titleOf(item));
        top.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48))); add(card,top); gap(card,9);
        TextView title=text(titleOf(item),17,SPACE_INK,true); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END); add(card,title);
        gap(card,7); TextView voice=text(item.optString("voice","自动音色")+" · "+new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(item.optLong("created"))),11,SPACE_MUTED,false);
        voice.setMaxLines(1); voice.setEllipsize(TextUtils.TruncateAt.END); add(card,voice); gap(card,12);
        ArchiveWaveView wave=new ArchiveWaveView(this); card.addView(wave,new LinearLayout.LayoutParams(-1,dp(55)));
        if(file.endsWith("wav"))loadArchiveWave(wave,audioFile(item));
        LinearLayout legend=row(); flexible(legend,text(file.endsWith("wav")?"WAVE / 采样波形概览":"TRACK / 音频轨道",9,SPACE_MUTED,false));
        legend.addView(text(WorksCatalog.duration(item.optLong("duration")),12,CYAN,true)); add(card,legend); gap(card,14);
        LinearLayout actions=row(); TextView play=archiveButton("播放",true,()->play(item)); actions.addView(play,new LinearLayout.LayoutParams(0,dp(48),1.4f)); sideGap(actions,8);
        actions.addView(archiveButton("导出",false,()->export(item)),new LinearLayout.LayoutParams(0,dp(48),1)); sideGap(actions,8);
        actions.addView(archiveButton("分享",false,()->share(item)),new LinearLayout.LayoutParams(0,dp(48),1)); add(card,actions); gap(card,7);
        SeekBar seek=new SeekBar(this); seek.setMax(Math.max(1,item.optInt("duration"))); seek.setContentDescription("音频播放进度");
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN)); seek.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff35405A));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b,int position,boolean user) {
                if(user && prepared && player!=null && playingFile.equals(file))try { player.seekTo(position); } catch(Exception ignored) {}
            }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        }); card.addView(seek,new LinearLayout.LayoutParams(-1,dp(32)));
        TextView time=text("点击播放，可拖动定位",10,SPACE_MUTED,false); add(card,time);
        PlaybackUI playback=new PlaybackUI(file,play,time,seek); playback.wave=wave; playbackViews.add(playback);
        gap(card,10); String detail=item.optLong("generationMillis")>0 ? "生成用时 "+WorksCatalog.duration(item.optLong("generationMillis"))+" · " : "";
        if(item.has("steps") && !"fish".equals(item.optString("engine")))detail+=item.optInt("steps")+" 步 · ";
        detail+=String.format(Locale.US,"%.1f KB",audioFile(item).length()/1024f)+" · 本机音频";
        add(card,text(detail,10,SPACE_MUTED,false));
        JSONObject timing=item.optJSONObject("synthesisTiming");
        if(timing!=null) {
            gap(card,5);
            add(card,text(String.format(Locale.CHINA,"加载 %.1f秒 · 推理 %.1f秒 · 解码 %.1f秒",
                    timing.optLong("loadMillis")/1000.0, timing.optLong("inferenceMillis")/1000.0,
                    timing.optLong("decodeMillis")/1000.0),10,SPACE_MUTED,false));
        }
        card.setOnLongClickListener(v->{ workMenu(item); return true; }); return card;
    }

    void loadArchiveWave(ArchiveWaveView view,File file) {
        String key=file.getAbsolutePath()+":"+file.length()+":"+file.lastModified();
        synchronized(waveformCache) { if(waveformCache.containsKey(key)) { view.samples(waveformCache.get(key)); return; } }
        view.setTag(key);
        waveformExecutor.execute(()->{
            float[] samples=null; try { samples=WaveformEnvelope.read(file); } catch(Exception ignored) {}
            final float[] result=samples;
            synchronized(waveformCache) { waveformCache.put(key,result); }
            handler.post(()->{ if(!destroyed && key.equals(view.getTag()))view.samples(result); });
        });
    }

    String titleOf(JSONObject item) {
        String title = item.optString("title").trim();
        return title.isEmpty() ? AudioChecks.shortTitle(item.optString("text")) : title;
    }

    void populateWorks() {
        if(worksList==null)return;
        worksList.removeAllViews(); playbackViews.clear();
        if(archiveCount!=null)archiveCount.setText(Integer.toString(history.length()));
        if(archiveDuration!=null)archiveDuration.setText(WorksCatalog.duration(WorksCatalog.totalDuration(history)));
        int favorites=0; for(int i=0;i<history.length();i++)if(history.optJSONObject(i).optBoolean("favorite"))favorites++;
        if(archiveFavorites!=null)archiveFavorites.setText(Integer.toString(favorites));
        List<JSONObject> selected=WorksCatalog.select(history,search,favoritesOnly,archiveSort);
        if(archiveResults!=null)archiveResults.setText((favoritesOnly?"收藏轨道":"声音档案")+" · "+selected.size()+" 段");
        for(int i=0;i<Math.min(selected.size(),visibleWorks);i++) { add(worksList,playerCard(selected.get(i))); gap(worksList,14); }
        if(selected.isEmpty()) {
            LinearLayout empty=card(); empty.setBackground(border(SPACE_CARD,0xff2E3A59,21));
            add(empty,text(history.length()==0?"你的第一段声音，正在等待诞生":favoritesOnly?"收藏轨道还是空的":"没有找到匹配的声音",17,SPACE_INK,true)); gap(empty,10);
            add(empty,text(history.length()==0?"生成后会自动进入星库。播放、收藏、导出和分享，都在这里。":"尝试其他关键词，或切换全部作品。",12,SPACE_MUTED,false)); gap(empty,15);
            if(history.length()==0)add(empty,archiveButton("开始创作",true,()->change("创作"))); add(worksList,empty);
        }
        if(selected.size()>visibleWorks)add(worksList,archiveButton("展开更多 · 还有 "+(selected.size()-visibleWorks)+" 段",false,()->{ visibleWorks+=20; populateWorks(); }));
        updatePlayback();
    }

    LinearLayout playerCard(JSONObject item) {
        if("作品".equals(page)) return archiveCard(item);
        LinearLayout c = card();
        LinearLayout heading = row();
        TextView title =
                text((item.optBoolean("favorite") ? "★ " : "") + titleOf(item), 16, INK, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        flexible(heading, title);
        TextView menu = button("···", false, () -> workMenu(item));
        menu.setContentDescription("管理作品 " + titleOf(item));
        heading.addView(menu, new LinearLayout.LayoutParams(dp(52), dp(48)));
        add(c, heading);
        gap(c, 8);
        add(
                c,
                text(
                        item.optString("voice")
                                + " · "
                                + new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                                        .format(new Date(item.optLong("created"))),
                        12,
                        MUTED,
                        false));
        gap(c, 7);
        File f = audioFile(item);
        String detail = f.getName().endsWith("wav") ? "WAV" : "MP3";
        detail += " · " + String.format(Locale.US, "%.1f KB", f.length() / 1024f);
        if (item.optInt("duration") > 0) detail += " · " + clock(item.optInt("duration"));
        add(c, text(detail, 11, MUTED, false));
        gap(c, 14);
        LinearLayout actions = row();
        TextView play = button("播放", true, () -> play(item));
        flexible(actions, play);
        sideGap(actions, 8);
        flexible(actions, button("导出", false, () -> export(item)));
        sideGap(actions, 8);
        flexible(actions, button("分享", false, () -> share(item)));
        add(c, actions);
        gap(c, 8);
        SeekBar seek = new SeekBar(this);
        seek.setMax(Math.max(1, item.optInt("duration")));
        seek.setContentDescription("音频播放进度");
        seek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar bar, int p, boolean user) {
                        if (user && prepared && player != null && playingFile.equals(f.getName()))
                            try {
                                player.seekTo(p);
                            } catch (Exception ignored) {
                            }
                    }

                    public void onStartTrackingTouch(SeekBar bar) {}

                    public void onStopTrackingTouch(SeekBar bar) {}
                });
        add(c, seek);
        TextView time = text("点击播放，可拖动定位", 11, MUTED, false);
        add(c, time);
        playbackViews.add(new PlaybackUI(f.getName(), play, time, seek));
        c.setOnLongClickListener(
                v -> {
                    workMenu(item);
                    return true;
                });
        return c;
    }

    void workMenu(JSONObject item) {
        new CosmicDialogBuilder(this)
                .setTitle(titleOf(item))
                .setItems(
                        new String[] {
                            "重命名",
                            item.optBoolean("favorite") ? "取消收藏" : "收藏作品",
                            "查看 / 复制文本",
                            "用这段文字再创作",
                            "移到回收站"
                        },
                        (d, w) -> {
                            if (w == 0) renameWork(item);
                            else if (w == 1)
                                updateWork(item, "favorite", !item.optBoolean("favorite"));
                            else if (w == 2) {
                                TextView body = text(item.optString("text"), 16, INK, false);
                                body.setTextIsSelectable(true);
                                body.setPadding(dp(20), dp(16), dp(20), dp(16));
                                ScrollView scroll = new ScrollView(this);
                                scroll.addView(body);
                                new CosmicDialogBuilder(this)
                                        .setTitle("作品文本")
                                        .setView(scroll)
                                        .setNegativeButton("关闭", null)
                                        .setPositiveButton(
                                                "复制",
                                                (x, y) -> {
                                                    ((android.content.ClipboardManager)
                                                                    getSystemService(
                                                                            CLIPBOARD_SERVICE))
                                                            .setPrimaryClip(
                                                                    ClipData.newPlainText(
                                                                            "作品文本",
                                                                            item.optString(
                                                                                    "text")));
                                                    toast("已复制文本");
                                                })
                                        .show();
                            } else if (w == 3) {
                                Runnable reuse =
                                        () -> {
                                            undoDraft = draft;
                                            draft = item.optString("text");
                                            editor = null;
                                            prefs.edit().putString("draft", draft).apply();
                                            page = "创作";
                                            render();
                                        };
                                saveDraft();
                                if (!draft.trim().isEmpty()
                                        && !draft.equals(item.optString("text")))
                                    new CosmicDialogBuilder(this)
                                            .setTitle("替换当前草稿？")
                                            .setMessage("替换后可使用清空 / 撤销按钮找回一次。")
                                            .setNegativeButton("取消", null)
                                            .setPositiveButton("替换", (x, y) -> reuse.run())
                                            .show();
                                else reuse.run();
                            } else confirmDelete(item);
                        })
                .show();
    }

    void renameWork(JSONObject item) {
        EditText name = field("作品名称", titleOf(item), false);
        name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(60)});
        LinearLayout box = col();
        box.setPadding(dp(20), dp(12), dp(20), 0);
        add(box, name);
        AlertDialog dialog =
                new CosmicDialogBuilder(this)
                        .setTitle("重命名作品")
                        .setView(box)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("保存", null)
                        .create();
        dialog.setOnShowListener(
                v ->
                        dialog.getButton(-1)
                                .setOnClickListener(
                                        x -> {
                                            String value = name.getText().toString().trim();
                                            if (value.isEmpty()) {
                                                name.setError("请输入作品名称");
                                                return;
                                            }
                                            if (updateWork(item, "title", value)) dialog.dismiss();
                                        }));
        dialog.show();
    }

    boolean updateWork(JSONObject item, String property, Object value) {
        try {
            StudioFiles.editHistory(this, item.getString("id"), property, value);
            history = readHistory();
            latest = history.optJSONObject(0);
            if ("作品".equals(this.page)) populateWorks();
            else showLatest();
            return true;
        } catch (Exception e) {
            toast(e.getMessage() == null ? "保存失败，请检查手机空间" : e.getMessage());
            return false;
        }
    }

    void confirmDelete(JSONObject item) {
        new CosmicDialogBuilder(this)
                .setTitle("移到回收站？")
                .setMessage("可以在设置的作品回收站里恢复。音频仍保留在手机里。")
                .setNegativeButton("保留", null)
                .setPositiveButton("移到回收站", (d, w) -> delete(item))
                .show();
    }

    void trashWorks() {
        try {
            JSONArray all = StudioFiles.historyList(this);
            List<JSONObject> trash = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int n = 0; n < all.length(); n++) {
                JSONObject item = all.getJSONObject(n);
                if (item.optLong("deletedAt") > 0) {
                    trash.add(item); labels.add(titleOf(item));
                }
            }
            if (trash.isEmpty()) { toast("回收站是空的"); return; }
            new CosmicDialogBuilder(this).setTitle("作品回收站 · " + trash.size() + " 项")
                    .setItems(labels.toArray(new String[0]), (d, n) -> {
                        JSONObject item = trash.get(n);
                        new CosmicDialogBuilder(this).setTitle(titleOf(item))
                                .setMessage("恢复会返回作品列表；彻底删除会移除内部音频及对应任务，已导出的文件保留。")
                                .setPositiveButton("恢复", (a, b) -> {
                                    if (updateWork(item, "deletedAt", 0)) toast("作品已恢复");
                                })
                                .setNeutralButton("彻底删除", (a, b) -> {
                                    new CosmicDialogBuilder(this).setTitle("彻底删除此作品？")
                                            .setMessage("此操作无法恢复。")
                                            .setNegativeButton("保留", null)
                                            .setPositiveButton("彻底删除", (x, y) -> fileExecutor.execute(() -> {
                                                try {
                                                    StudioFiles.deleteJob(this, item.getString("id"));
                                                    handler.post(() -> { if (!destroyed) {
                                                        try { history = readHistory(); latest = history.optJSONObject(0); render(); toast("已彻底删除"); }
                                                        catch (Exception e) { toast("请重新打开作品页"); }
                                                    } });
                                                } catch (Exception e) { handler.post(() -> { if (!destroyed) toast(e.getMessage()); }); }
                                            })).show();
                                }).setNegativeButton("关闭", null).show();
                    }).setNegativeButton("关闭", null).show();
        } catch (Exception e) { toast("读取回收站失败：" + e.getMessage()); }
    }

    void fishVoicePage() {
        if (voiceId.isEmpty()) {
            LinearLayout missing = card();
            info(missing, "先选择 Fish 音色", "需要一个有效的 Voice ID。添加自己的音色或官方音色库中的音色后，再生成或试听。");
            add(content, missing); gap(content, 12);
        }
        for (int i = 0; i < voices.length(); i++) {
            JSONObject voice = voices.optJSONObject(i);
            if (voice != null) voiceCard(voice.optString("name"), voice.optString("id"), false);
        }
        add(content, button("＋ 添加 Fish 音色", false, () -> voiceDialog(-1)));
        gap(content, 16);
        LinearLayout c = card();
        info(c, "Fish Audio 云端音色", "原有收藏音色会继续保留。复制官方 Voice ID 添加音色；点击试听会联网生成一句示例，并保存到作品中。");
        gap(c, 12);
        add(c, button("生成一句试听", true, () -> {
            if (voiceId.isEmpty()) { toast("请先添加并选择 Fish 音色"); return; }
            try { enqueueFishText("你好，欢迎来到你的声音工作室。"); }
            catch (Exception e) { toast(e.getMessage() == null ? "Fish 密钥无法读取，请在设置重新保存" : e.getMessage()); }
        }));
        gap(c, 12);
        add(c, button("打开官方音色库", false, () -> openUrl("https://fish.audio/app/discovery/")));
        add(content, c);
    }

    void fishSettings() {
        LinearLayout c = card();
        info(c, "Fish Audio 已恢复", "使用自己的 API Key。点击 Fish 生成会将文字与音色 ID 发给 Fish Audio，密钥加密保存在手机中。");
        String token = keyDraft;
        if (token == null) try { token = new SecretStore(this).read(); }
        catch (Exception e) { token = ""; toast("旧密钥无法读取，请重新填写"); }
        gap(c, 12);
        keyEditor = field("Fish Audio API Key", token == null ? "" : token, true);
        keyEditor.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        add(c, keyEditor);
        gap(c, 12);
        add(c, text("Fish 语音模型（按 Fish 账户额度与服务条款使用）", 12, MUTED, false));
        modelPicker = new Spinner(this);
        modelPicker.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, FishClient.MODELS));
        String chosen = modelDraft == null ? model : modelDraft;
        for (int i = 0; i < FishClient.MODELS.length; i++) if (FishClient.MODELS[i].equals(chosen)) modelPicker.setSelection(i);
        add(c, modelPicker);
        gap(c, 12);
        add(c, button("保存 Fish 配置", true, () -> {
            try {
                String key = keyEditor.getText().toString().trim();
                if (!key.isEmpty() && (key.length() > 4096 || !key.matches("[!-~]+"))) {
                    keyEditor.setError("密钥含空格或无效字符，请重新粘贴"); return;
                }
                new SecretStore(this).save(key);
                model = modelPicker.getSelectedItem().toString();
                keyDraft = key; modelDraft = model; saveSettings(); hideKeyboard(); toast("Fish 配置已保存");
            } catch (Exception e) { toast("Fish 配置保存失败，请重试"); }
        }));
        gap(c, 10);
        add(c, button("使用 Fish Audio", false, () -> { engine = "fish"; saveSettings(); change("音色"); }));
        gap(c, 10);
        add(c, button("申请 / 管理 API Key", false, () -> openUrl("https://fish.audio/app/api-keys")));
        add(content, c);
    }

    void motionSettings() {
        LinearLayout c=card(); info(c,"界面与动效","深空配色、轨道光环与柔和转场。合成与省电模式下暂停装饰动效。");gap(c,12);
        boolean enabled=prefs.getBoolean("ui-motion",true);
        add(c,button(enabled?"动效已开启 · 点击关闭":"动效已关闭 · 点击开启",false,()->{prefs.edit().putBoolean("ui-motion",!enabled).apply();render();}));add(content,c);
    }

    void settings() {
        offlineSettings();
    }

    String readAsset(String name) throws IOException {
        try (InputStream in = getAssets().open(name);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return new String(out.toByteArray(), "UTF-8");
        }
    }

    File omniModelDir() {
        return new File(getFilesDir(), "omni-model");
    }

    boolean omniReady() {
        return omniManifest != null && OmniModels.ready(omniModelDir(), omniManifest);
    }

    String omniLanguageName() {
        for (int i = 0; i < omniLanguages.length(); i++) {
            JSONObject l = omniLanguages.optJSONObject(i);
            if (l != null && l.optString("id").equals(omniLang)) return l.optString("label");
        }
        return omniLang;
    }

    String selectedVoiceName() {
        if ("fish".equals(engine)) return "Fish Audio · " + voiceName;
        if ("omni".equals(engine)) {
            String profile = prefs.getString("selected-profile", "");
            if (!profile.isEmpty())
                try {
                    return StudioFiles.read(
                                            new File(
                                                    StudioFiles.dir(this, "profiles"),
                                                    profile + ".json"))
                                    .optString("name", "参考音色")
                            + " · "
                            + omniLanguageName();
                } catch (Exception ignored) {
                }
        }
        return "omni".equals(engine)
                ? "OmniVoice · " + omniLanguageName()
                : "手机语音 · " + languageName(localLang);
    }

    void omniVoicePage() {
        LinearLayout c = card();
        info(c, "646 种语言", "完整保留原模型语言目录。选择输入文字所属的语言，不会自动翻译。");
        gap(c, 16);
        add(c, button(omniLanguageName() + "  ·  选择语言", false, () -> pickOmniLanguage()));
        add(content, c);
        gap(content, 16);
        LinearLayout voice = card();
        info(voice, "参考音色与声音设计", "当前：" + selectedVoiceName());
        gap(voice, 12);
        add(voice, button("选择、创建或管理参考音色", false, () -> openWorkspace("音色")));
        add(content, voice);
        gap(content, 16);
        LinearLayout state = card();
        info(
                state,
                omniReady() ? "模型已导入" : "模型尚未导入",
                omniReady() ? "音频完全由手机本机计算，不发送文本。" : "在设置导入完整模型包，一次导入后无需联网。");
        gap(state, 12);
        add(state, button("管理离线模型", false, () -> change("设置")));
        add(content, state);
        gap(content, 16);
        add(content, text("各语言效果取决于原模型的训练覆盖。646 种语言尚未逐一实测，Android 真机性能也待验证。", 12, MUTED, false));
    }

    void pickOmniLanguage() {
        LinearLayout box = col();
        box.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText query = field("搜索中文名、英文名或语言代码", "", false);
        add(box, query);
        ListView list = new ListView(this);
        box.addView(list, new LinearLayout.LayoutParams(-1, dp(340)));
        List<JSONObject> matches = new ArrayList<>();
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_single_choice,
                        new ArrayList<String>());
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        Runnable update =
                () -> {
                    String q = query.getText().toString().trim().toLowerCase(Locale.ROOT);
                    matches.clear();
                    adapter.clear();
                    int chosen = -1;
                    for (int i = 0; i < omniLanguages.length(); i++) {
                        JSONObject l = omniLanguages.optJSONObject(i);
                        if (l == null) continue;
                        String label = l.optString("label"),
                                name = l.optString("name"),
                                id = l.optString("id");
                        if ((label + " " + name + " " + id).toLowerCase(Locale.ROOT).contains(q)) {
                            if (id.equals(omniLang)) chosen = matches.size();
                            matches.add(l);
                            adapter.add(label + " · " + id);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (chosen >= 0) list.setItemChecked(chosen, true);
                };
        query.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

                    public void onTextChanged(CharSequence s, int a, int b, int c) {
                        update.run();
                    }

                    public void afterTextChanged(Editable e) {}
                });
        update.run();
        AlertDialog dialog =
                new CosmicDialogBuilder(this)
                        .setTitle("选择语言 · 646 种")
                        .setView(box)
                        .setNegativeButton("取消", null)
                        .create();
        list.setOnItemClickListener(
                (parent, v, pos, id) -> {
                    omniLang = matches.get(pos).optString("id");
                    saveSettings();
                    dialog.dismiss();
                    render();
                });
        dialog.show();
    }

    void offlineSettings() {
        heading("设置", "云端与本机合成，按需选择。");
        motionSettings(); gap(content,16);
        fishSettings();
        gap(content, 16);
        add(content, button("作品回收站", false, this::trashWorks));
        gap(content, 12);
        add(content, button("六类模型安装与设备检查", false,
                () -> startActivity(new Intent(this, WorkspaceActivity.class).putExtra("openModels", true))));
        gap(content, 12);
        add(content, button("下载与安装离线模型", true,
                () -> startActivity(new Intent(this, ModelDownloadsActivity.class))));
        gap(content, 12);
        LinearLayout c = card();
        info(
                c,
                "OmniVoice 离线模型",
                "INT8 原生引擎 · 646 种语言\n模型约 715 MB。安装页会检查分包、剩余空间与文件内容，并显示可取消的安装进度。");
        gap(c, 14);
        modelStatus =
                text(
                        importingModel
                                ? "正在导入模型…"
                                : omniReady() ? "模型已导入 · SHA-256 校验通过" : "尚未导入模型包",
                        13,
                        omniReady() ? SUCCESS : MUTED,
                        true);
        add(c, modelStatus);
        gap(c, 12);
        TextView importButton = button(omniReady() ? "查看基础模型安装状态" : "安装基础模型", !omniReady(),
                () -> startActivity(new Intent(this, ModelInstallActivity.class).putExtra("module", "omni")));
        importButton.setEnabled(!busy);
        add(c, importButton);
        gap(c, 12);
        add(c, text("长文本自动按句分段，后台生成。低内存手机可能加载失败；尚未完成 Android 真机测试。", 12, MUTED, false));
        if (omniModelDir().exists()) {
            gap(c, 12);
            TextView remove =
                    button(
                            "移除模型，释放空间",
                            false,
                            () -> {
                                if (busy || importingModel || StudioFiles.pending(this)) {
                                    toast("请先结束当前操作");
                                    return;
                                }
                                new CosmicDialogBuilder(this)
                                        .setTitle("移除离线模型？")
                                        .setMessage("作品会保留，下次使用 OmniVoice 需要重新导入模型包。")
                                        .setNegativeButton("取消", null)
                                        .setPositiveButton(
                                                "移除",
                                                (d, w) -> {
                                                    if (!StudioFiles.beginModelWork(this)) {
                                                        toast("请暂停并等待当前队列停止"); return;
                                                    }
                                                    fileExecutor.execute(() -> {
                                                        try { OmniModels.remove(omniModelDir()); }
                                                        finally { StudioFiles.endModelWork(); }
                                                        handler.post(() -> { if (!destroyed) {
                                                            render(); toast(omniModelDir().exists() ? "移除失败，请重试" : "模型已移除");
                                                        } });
                                                    });
                                                })
                                        .show();
                            });
            remove.setEnabled(!busy && !importingModel);
            add(c, remove);
        }
        add(content, c);
        gap(content, 16);
        LinearLayout privacy = card();
        info(privacy, "本机处理", "OmniVoice 与手机离线音色在本机处理。只有选择 Fish Audio 并点击生成，才会向 Fish Audio 发送输入文字与音色 ID。API Key 加密保存在手机中；运行库遥测关闭。");
        add(content, privacy);
        gap(content, 16);
        LinearLayout about = card();
        info(
                about,
                "VoiceStudio 1.5.3 · 云端与离线合成",
                "恢复原版 OmniVoice 引擎。646 是原模型的语言覆盖数，不代表每种语言都已通过实测。\n\n"
                        + "模型来源：k2-fsa/OmniVoice；ONNX 转换：gluschenko 与 onnx-community。预训练权重遵循原模型"
                        + " CC-BY-NC 条款。应用与第三方代码许可见源码 LICENSE 和 NOTICE。");
        add(content, about);
    }

    String selectedFileName(Uri uri) throws IOException {
        try (android.database.Cursor cursor =
                getContentResolver()
                        .query(
                                uri,
                                new String[] {android.provider.OpenableColumns.DISPLAY_NAME},
                                null,
                                null,
                                null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        }
        return "";
    }

    InputStream openModelParts(List<Uri> uris) throws Exception {
        List<ModelParts.Source> sources = new ArrayList<>();
        for (Uri uri : uris) sources.add(new ModelParts.Source() {
            public String name() throws Exception { return selectedFileName(uri); }
            public InputStream open() throws Exception { return getContentResolver().openInputStream(uri); }
        });
        return ModelParts.open(sources);
    }

    void importModel(List<Uri> uris) {
        if (busy || StudioFiles.pending(this)) { toast("请先结束当前任务或模型操作"); return; }
        startActivity(new Intent(this, ModelInstallActivity.class).putExtra("module", "omni")
                .putParcelableArrayListExtra("modelUris", new ArrayList<>(uris)));
    }

    void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("未找到浏览器");
        }
    }

    String languageName(String lang) {
        switch (lang) {
            case "zh-CN":
                return "普通话";
            case "zh-HK":
                return "中文（香港）";
            case "yue-HK":
                return "粤语（香港）";
            case "en-US":
                return "英语";
            case "ja-JP":
                return "日语";
            case "ko-KR":
                return "韩语";
            case "fr-FR":
                return "法语";
            case "de-DE":
                return "德语";
            case "es-ES":
                return "西班牙语";
            default:
                return lang;
        }
    }

    void enqueueFishText(String text) throws Exception {
        if (isGenerating()) { toast("当前声音正在生成，可先取消或等待完成"); return; }
        String token = new SecretStore(this).read();
        FishClient.request(token, model, text, voiceId, speed);
        saveSettings();
        JSONObject job = new JSONObject().put("type", "tts").put("deduplicate", true).put("engine", "fish")
                .put("text", text).put("speed", speed).put("model", model)
                .put("reference", voiceId).put("voice", voiceName)
                .put("title", AudioChecks.shortTitle(text));
        trackJob(StudioService.enqueue(this, job), job);
    }

    void generate() {
        if (isGenerating()) return;
        saveDraft();
        String s = draft.trim();
        if (s.isEmpty()) {
            editor.setError("先写一些文字吧");
            return;
        }
        if (s.length() > 12000) {
            toast("请将文本分成 12000 字以内");
            return;
        }
        hideKeyboard();
        if ("fish".equals(engine)) {
            try {
                enqueueFishText(s);
            } catch (Exception e) {
                generationError(e.getMessage() == null ? "Fish 密钥无法读取，请在设置重新保存" : e.getMessage());
            }
            return;
        }
        if ("omni".equals(engine)) {
            if (importingModel) {
                toast("请等待模型导入完成");
                return;
            }
            if (!omniReady()) {
                generationError("请先在设置导入离线模型包");
                return;
            }
            try {
                String profile = prefs.getString("selected-profile", "");
                if (!profile.isEmpty() && !new File(StudioFiles.dir(this, "profiles"), profile + ".vpc").isFile()) {
                    prefs.edit().putString("selected-profile", "").apply();
                    toast("原参考音色已不存在，已切回自动音色");
                }
                saveSettings();
                JSONObject job =
                        new JSONObject()
                                .put("type", "tts").put("deduplicate", true)
                                .put("engine", "omni")
                                .put("text", s)
                                .put("language", omniLang)
                                .put("speed", speed)
                                .put("profile", prefs.getString("selected-profile", ""))
                                .put("style", prefs.getString("voice-style", "None"))
                                .put("steps", prefs.getInt("voice-steps", 16))
                                .put("seed", prefs.getLong("voice-seed", 42))
                                .put("voice", selectedVoiceName());
                trackJob(StudioService.enqueue(this, job), job);
            } catch (Exception e) {
                generationError(e.getMessage());
            }
            return;
        } else {
            if (!ttsReady) {
                toast("手机语音引擎尚未就绪");
                return;
            }
            if (localVoice.isEmpty()) {
                generationError("请在音色页面选择一个离线音色");
                return;
            }
            int result = tts.setLanguage(Locale.forLanguageTag(localLang));
            if (result < 0) {
                toast("没有安装这个语言的语音包，请更换语言");
                return;
            }
            if (!localVoice.isEmpty()) {
                boolean found = false;
                Set<Voice> av = tts.getVoices();
                if (av != null)
                    for (Voice v : av)
                        if (v.getName().equals(localVoice) && !v.isNetworkConnectionRequired()) {
                            tts.setVoice(v);
                            found = true;
                        }
                if (!found) {
                    toast("所选音色已不可用，请重新选择");
                    return;
                }
            }
            if (s.length() > TextToSpeech.getMaxSpeechInputLength()) {
                toast("文本超过手机语音引擎上限，请分段");
                return;
            }
        }
        releasePlayer();
        previewId = "";
        if (tts != null) tts.stop();
        try {
            String id = UUID.randomUUID().toString();
            String ext = "wav";
            File dir = new File(getFilesDir(), "audio");
            dir.mkdirs();
            jobFile = new File(dir, id + "." + ext + ".pending");
            activeJob =
                    new JSONObject()
                            .put("id", id)
                            .put("file", id + "." + ext)
                            .put("title", AudioChecks.shortTitle(s))
                            .put("speed", speed)
                            .put("model", "omni".equals(engine) ? "OmniVoice INT8" : "Android TTS")
                            .put("language", "omni".equals(engine) ? omniLang : localLang)
                            .put("reference", "")
                            .put("text", s)
                            .put("voice", selectedVoiceName())
                            .put("engine", engine)
                            .put("created", System.currentTimeMillis());
            cancelled = new AtomicBoolean(false);
            busy = true;
            trackedJobId = ""; trackedJob = null;
            prefs.edit().remove("main-generation-job").apply();
            localGenerationState = "running";
            localCompletedJob = null;
            jobStarted = System.currentTimeMillis();
            jobStage = "omni".equals(engine) ? "准备本机推理…" : "手机语音引擎正在合成…";
            lastMessage = "";
            updateJobUI();
            if ("omni".equals(engine)) {
                final String chosenText = s, language = omniLang;
                final float rate = speed;
                final File output = jobFile;
                final AtomicBoolean cancellation = cancelled;
                executor.execute(
                        () -> {
                            try {
                                omni.generate(
                                        omniModelDir(),
                                        chosenText,
                                        language,
                                        rate,
                                        output,
                                        cancellation,
                                        message -> jobProgress(id, message));
                                handler.post(
                                        () -> {
                                            if (!destroyed
                                                    && activeJob != null
                                                    && id.equals(activeJob.optString("id"))
                                                    && !cancellation.get()) finishJob();
                                            else output.delete();
                                        });
                            } catch (Exception e) {
                                output.delete();
                                handler.post(
                                        () -> {
                                            if (!destroyed
                                                    && activeJob != null
                                                    && id.equals(activeJob.optString("id")))
                                                failJob(
                                                        cancellation.get()
                                                                ? "已取消生成"
                                                                : "本机合成失败："
                                                                        + (e.getMessage() == null
                                                                                ? "请检查内存及模型"
                                                                                : e.getMessage()));
                                        });
                            }
                        });
            } else if ("local".equals(engine)) {
                tts.setSpeechRate(speed);
                int r = tts.synthesizeToFile(s, new Bundle(), jobFile, id);
                if (r == TextToSpeech.ERROR) failJob("手机语音引擎拒绝生成，请检查语音包");
                else
                    handler.postDelayed(
                            () -> {
                                if (activeJob != null
                                        && !verifying
                                        && activeJob.optString("id").equals(id))
                                    failJob("手机语音生成超时，请检查语音引擎");
                            },
                            120000);
            }
        } catch (Exception e) {
            failJob("无法创建作品，请检查手机可用空间");
        }
    }

    String networkMessage(Exception e) {
        if (cancelled.get()) return "已取消生成";
        if (e instanceof java.net.SocketTimeoutException) return "请求超时，请检查网络后重试";
        if (e instanceof java.net.UnknownHostException || e instanceof java.net.ConnectException)
            return "无法连接 Fish Audio，请检查网络";
        if (e instanceof IOException
                && e.getMessage() != null
                && e.getMessage().matches(".*[\\u4e00-\\u9fa5].*")) return e.getMessage();
        return "生成失败，请检查网络与音色配置后重试";
    }

    // release() works on API 26; close()/AutoCloseable support was added later.
    @SuppressWarnings("resource")
    void finishJob() {
        if (destroyed || activeJob == null || cancelled.get() || verifying) return;
        verifying = true;
        final JSONObject job = activeJob;
        final File pending = jobFile;
        final AtomicBoolean cancellation = cancelled;
        jobStage = "正在检查并保存音频…";
        updateJobUI();
        fileExecutor.execute(
                () -> {
                    int millis = 0;
                    Exception problem = null;
                    MediaMetadataRetriever metadata = new MediaMetadataRetriever();
                    try {
                        String format = job.optString("file").endsWith("wav") ? "wav" : "mp3";
                        AudioChecks.validate(pending, format);
                        metadata.setDataSource(pending.getAbsolutePath());
                        millis =
                                Integer.parseInt(
                                        metadata.extractMetadata(
                                                MediaMetadataRetriever.METADATA_KEY_DURATION));
                        if (millis <= 0) throw new IOException("生成的音频无法解码，请更换音色后重试");
                    } catch (Exception e) {
                        problem = e;
                    } finally {
                        try {
                            metadata.release();
                        } catch (Exception ignored) {
                        }
                    }
                    final int length = millis;
                    final Exception error = problem;
                    handler.post(
                            () -> {
                                if (destroyed || activeJob != job || cancellation.get()) {
                                    pending.delete();
                                    return;
                                }
                                if (error != null) {
                                    failJob(
                                            error.getMessage() != null
                                                            && error.getMessage()
                                                                    .matches(
                                                                            ".*[\\u4e00-\\u9fa5].*")
                                                    ? error.getMessage()
                                                    : "音频检查失败，请重新生成");
                                    return;
                                }
                                File completed = audioFile(job);
                                if (!pending.renameTo(completed)) {
                                    failJob("保存音频失败，请检查手机空间");
                                    return;
                                }
                                boolean retained = false;
                                try {
                                    job.put("duration", length);
                                    StudioFiles.history(this, job);
                                    retained = true;
                                    history = readHistory();
                                    latest = job;
                                    activeJob = null;
                                    jobFile = null;
                                    busy = false;
                                    localGenerationState = "done";
                                    localCompletedJob = job;
                                    verifying = false;
                                    lastMessage = "已保存到作品，可播放、导出或分享";
                                    updateJobUI();
                                    if ("创作".equals(page)) showLatest();
                                    else if ("作品".equals(page)) render();
                                    toast("声音已保存到作品");
                                } catch (Exception e) {
                                    if (!retained) completed.delete();
                                    failJob(retained ? "音频已保存，请重新打开作品页" : "保存作品失败，请检查手机空间");
                                }
                            });
                });
    }

    void failJob(String msg) {
        localGenerationState = cancelled.get() ? "paused" : "failed";
        if (activeJob != null) {
            cancelled.set(true);
            omni.cancel();
            if (jobFile != null) jobFile.delete();
        }
        activeJob = null;
        jobFile = null;
        busy = false;
        verifying = false;
        lastMessage = msg;
        if (!destroyed) {
            updateJobUI();
            toast(msg);
        }
    }

    void cancelJob() {
        if (trackedJob != null && SynthesisProgress.active(trackedJob.optString("state"))) {
            if (cancelRequested) return;
            try {
                startService(new Intent(this, StudioService.class).setAction("cancelTask").putExtra("id", trackedJobId));
                cancelRequested = true; updateJobUI();
            } catch (Exception e) { toast("取消请求未发送，请重试"); }
            return;
        }
        if (!busy) return;
        cancelled.set(true);
        omni.cancel();
        previewId = "";
        if (tts != null) tts.stop();
        failJob("已取消生成，草稿已保留");
    }

    File audioFile(JSONObject item) {
        return new File(new File(getFilesDir(), "audio"), item.optString("file"));
    }

    JSONArray readHistory() throws Exception {
        JSONArray data = StudioFiles.historyList(this), valid = new JSONArray();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item != null && item.optLong("deletedAt") <= 0
                    && item.optString("file").matches("[a-f0-9-]+\\.(mp3|wav)")
                    && audioFile(item).isFile()) valid.put(item);
        }
        return valid;
    }

    void delete(JSONObject item) {
        if (playingFile.equals(item.optString("file"))) releasePlayer();
        if (updateWork(item, "deletedAt", System.currentTimeMillis())) {
            render();
            toast("已移到回收站，可在设置中恢复");
        }
    }

    boolean isPlaying() {
        try {
            return prepared && player != null && player.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    void play(JSONObject item) {
        File f = audioFile(item);
        if (!f.isFile()) {
            toast("音频文件不存在");
            return;
        }
        stopPreview();
        if (player != null && f.getName().equals(playingFile)) {
            if (!prepared) return;
            try {
                if (player.isPlaying()) {
                    player.pause();
                    abandonFocus();
                } else {
                    if (!foreground || !requestFocus()) return;
                    player.start();
                }
                updatePlayback();
                return;
            } catch (Exception e) {
                releasePlayer();
            }
        }
        releasePlayer();
        if (!foreground || !requestFocus()) return;
        playingFile = f.getName();
        MediaPlayer p = new MediaPlayer();
        player = p;
        prepared = false;
        updatePlayback();
        try {
            p.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
            p.setDataSource(f.getAbsolutePath());
            p.setOnPreparedListener(
                    mp -> {
                        if (player != mp) return;
                        prepared = true;
                        duration = mp.getDuration();
                        if (foreground && focusRequest != null) mp.start();
                        updatePlayback();
                    });
            p.setOnCompletionListener(
                    mp -> {
                        if (player == mp) {
                            abandonFocus();
                            updatePlayback();
                        }
                    });
            p.setOnErrorListener(
                    (mp, w, e) -> {
                        if (player == mp) {
                            releasePlayer();
                            toast("音频无法播放，请重新生成");
                        }
                        return true;
                    });
            p.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            toast("播放失败，请重试");
        }
    }

    boolean requestFocus() {
        abandonFocus();
        final AudioFocusRequest[] requested = new AudioFocusRequest[1];
        focusRequest =
                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build())
                        .setOnAudioFocusChangeListener(
                                change -> {
                                    if (change < 0 && focusRequest == requested[0]) {
                                        if (player != null && prepared)
                                            try {
                                                player.pause();
                                            } catch (Exception ignored) {
                                            }
                                        stopPreview();
                                        abandonFocus();
                                        updatePlayback();
                                    }
                                })
                        .build();
        requested[0] = focusRequest;
        if (audioManager.requestAudioFocus(focusRequest)
                != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusRequest = null;
            toast("暂时无法播放，请结束通话或其他声音任务");
            return false;
        }
        return true;
    }

    void abandonFocus() {
        if (focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
        focusRequest = null;
    }

    void releasePlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignored) {
            }
        }
        player = null;
        prepared = false;
        duration = 0;
        playingFile = "";
        abandonFocus();
        updatePlayback();
    }

    String clock(int millis) {
        int s = Math.max(0, millis) / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    void share(JSONObject item) {
        File f = audioFile(item);
        if (!f.isFile()) {
            toast("文件不存在");
            return;
        }
        Uri u = AudioProvider.uri(this, f);
        Intent i =
                new Intent(Intent.ACTION_SEND)
                        .setType(f.getName().endsWith("wav") ? "audio/wav" : "audio/mpeg")
                        .putExtra(Intent.EXTRA_STREAM, u)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.setClipData(ClipData.newRawUri("VoiceStudio 音频", u));
        try {
            startActivity(Intent.createChooser(i, "分享声音"));
        } catch (Exception e) {
            toast("没有可用的分享应用");
        }
    }

    boolean exporting;

    void export(JSONObject item) {
        if (exporting) {
            toast("请先完成当前导出");
            return;
        }
        exportFile = audioFile(item);
        if (!exportFile.isFile()) {
            toast("文件不存在");
            return;
        }
        Intent i =
                new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(exportFile.getName().endsWith("wav") ? "audio/wav" : "audio/mpeg")
                        .putExtra(
                                Intent.EXTRA_TITLE,
                                AudioChecks.exportName(titleOf(item))
                                        + "-"
                                        + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                                                .format(new Date(item.optLong("created")))
                                        + (exportFile.getName().endsWith("wav") ? ".wav" : ".mp3"));
        try {
            exporting = true;
            startActivityForResult(i, 81);
        } catch (Exception e) {
            exporting = false;
            toast("手机没有可用的文件保存器");
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 83) {
            if (result == RESULT_OK && data != null) {
                List<Uri> files = new ArrayList<>();
                ClipData clips = data.getClipData();
                if (clips != null) {
                    for (int i = 0; i < clips.getItemCount(); i++)
                        files.add(clips.getItemAt(i).getUri());
                } else if (data.getData() != null) files.add(data.getData());
                if (!files.isEmpty()) importModel(files);
            }
            return;
        }

        if (request == 81) exporting = false;
        if (request == 81
                && result == RESULT_OK
                && data != null
                && data.getData() != null
                && exportFile != null) {
            final Uri uri = data.getData();
            final File f = exportFile;
            fileExecutor.execute(
                    () -> {
                        try (InputStream in = new FileInputStream(f);
                                OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                            if (os == null) throw new IOException();
                            byte[] b = new byte[16384];
                            int n;
                            while ((n = in.read(b)) != -1) os.write(b, 0, n);
                            handler.post(
                                    () -> {
                                        if (!destroyed) toast("已导出音频");
                                    });
                        } catch (Exception e) {
                            handler.post(
                                    () -> {
                                        if (!destroyed) toast("导出失败，请重试或更换保存位置");
                                    });
                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
        saveDraft();
        if (player != null && isPlaying()) player.pause();
        stopPreview();
        abandonFocus();
        updatePlayback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        refreshTracked = true;
        pollTrackedJob();
        try {
            history = readHistory();
            latest = history.optJSONObject(0);
            if (root != null) render();
        } catch (Exception ignored) {
        }
        updatePlayback();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (noisyRegistered) { unregisterReceiver(noisyReceiver); noisyRegistered = false; }
        cancelled.set(true);
        omni.cancel();
        if (jobFile != null) jobFile.delete();
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        executor.shutdownNow();
        fileExecutor.shutdown();
        waveformExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        saveDraft();
        state.putString("page", page);
        state.putBoolean("exporting", exporting);
        if (exportFile != null) state.putString("exportFile", exportFile.getName());
        super.onSaveInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if (!"创作".equals(page)) change("创作");
        else if (busy) {
            new CosmicDialogBuilder(this)
                    .setTitle("正在生成声音")
                    .setMessage("离开应用会取消生成，草稿会保留。")
                    .setNegativeButton("继续等待", null)
                    .setPositiveButton(
                            "取消并退出",
                            (d, w) -> {
                                cancelJob();
                                finish();
                            })
                    .show();
        } else super.onBackPressed();
    }

    static class IconView extends View {
        final Paint paint = new Paint(3);
        final String icon;
        final int color;

        IconView(Context c, String s, int co) {
            super(c);
            icon = s;
            color = co;
            setContentDescription(s);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            float size = Math.min(getWidth(), getHeight());
            canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
            canvas.scale(size / 32, size / 32);
            paint.setColor(color);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            if (icon.equals("wave")) {
                for (int i = 0; i < 5; i++) {
                    float h = new float[] {6, 15, 23, 15, 6}[i];
                    canvas.drawLine(6 + i * 5, 16 - h / 2, 6 + i * 5, 16 + h / 2, paint);
                }
            } else if (icon.equals("tools")) {
                for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++)
                    canvas.drawRoundRect(5 + x * 13, 5 + y * 13, 14 + x * 13, 14 + y * 13, 2, 2, paint);
            } else if (icon.equals("folder")) {
                Path p = new Path();
                p.moveTo(4, 10);
                p.lineTo(4, 7);
                p.lineTo(13, 7);
                p.lineTo(16, 10);
                p.lineTo(28, 10);
                p.lineTo(28, 26);
                p.lineTo(4, 26);
                p.close();
                canvas.drawPath(p, paint);
                canvas.drawLine(4, 13, 28, 13, paint);
            } else if (icon.equals("edit")) {
                canvas.drawRoundRect(5, 5, 26, 27, 4, 4, paint);
                canvas.drawLine(11, 20, 23, 8, paint);
                canvas.drawLine(11, 20, 10, 24, paint);
                canvas.drawLine(10, 24, 14, 23, paint);
            } else {
                canvas.drawCircle(16, 16, 9, paint);
                canvas.drawCircle(16, 16, 3, paint);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    canvas.drawLine(
                            16 + (float) Math.cos(a) * 10,
                            16 + (float) Math.sin(a) * 10,
                            16 + (float) Math.cos(a) * 13,
                            16 + (float) Math.sin(a) * 13,
                            paint);
                }
            }
            canvas.restore();
        }
    }
}
