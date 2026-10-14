package com.voice.studio;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

import org.json.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StudioService extends Service {
    static volatile String active = "";
    final Object gate = new Object();
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final AtomicBoolean cancel = new AtomicBoolean();
    final JobProgressGate progressGate=new JobProgressGate();
    final OmniEngine engine = new OmniEngine();
    final FishClient fish = new FishClient();
    final AcceleratedSpeechClient accelerated = new AcceleratedSpeechClient();
    final GradioSpeechClient gradio = new GradioSpeechClient();
    volatile WhisperEngine whisper;
    volatile NllbEngine translator;
    volatile SpeakerEngine speaker;
    volatile SeparationEngine separator;
    static volatile boolean microphone;
    static volatile String recording = "";
    final AtomicBoolean stopMic = new AtomicBoolean();
    int pending, lastStart;
    boolean paused;

    public IBinder onBind(Intent i) {
        return null;
    }

    static synchronized String enqueue(Context c, JSONObject job) throws Exception {
        if (StudioFiles.modelWork()) throw new IOException("请等待模型导入或检查完成");
        if (job.optString("type", "tts").equals("tts") && job.optBoolean("deduplicate")) {
            JSONArray queued = StudioFiles.list(c);
            for (int at = 0; at < queued.length(); at++) {
                JSONObject existing = queued.getJSONObject(at);
                if (SynthesisRequests.same(existing, job)) return existing.getString("id");
            }
        }
        String id = UUID.randomUUID().toString();
        job.put("id", id)
                .put("file", id + ("fish".equals(job.optString("engine")) ? ".mp3" : ".wav"))
                .put("state", "queued")
                .put("created", System.currentTimeMillis())
                .put("progress", "等待执行");
        requireMicrophonePermission(c, job);
        StudioFiles.queue(c, job);
        try {
            c.startForegroundService(new Intent(c, StudioService.class).putExtra("id", id));
        } catch (RuntimeException failure) {
            job.put("state", "failed").put("progress", "任务未能启动，请回到应用点击继续")
                    .put("failureStage", "请求启动后台服务")
                    .put("failureDetail", failure.getClass().getSimpleName() + ": " + failure.getMessage());
            StudioFiles.save(StudioFiles.job(c, id), job);
            throw new IOException("任务未能启动，请回到应用重试", failure);
        }
        return id;
    }

    static File remoteFolder(Context context,String id) throws Exception {return new File(new File(StudioFiles.dir(context,"jobs"),StudioFiles.job(context,id).getName().replace(".json","")),"gpu");}
    static void resume(Context context, String id) throws Exception {resume(context,id,false);}
    static void resume(Context context, String id,boolean allowResubmit) throws Exception {
        if (StudioFiles.modelWork()) throw new IOException("请等待模型导入或检查完成");
        JSONObject job = StudioFiles.read(StudioFiles.job(context, id));
        requireMicrophonePermission(context, job);
        if(job.optString("engine").equals("accelerated")&&RemoteSpeechSession.needsResubmit(remoteFolder(context,id))) {
            if(!allowResubmit)throw new RemoteSpeechPipeline.ResubmitRequired();
            if(!active.isEmpty())throw new IOException("请等待当前任务停止");
            RemoteSpeechSession.allowResubmit(remoteFolder(context,id));
        }
        job = StudioFiles.requeue(context, id);
        try {
            context.startForegroundService(new Intent(context, StudioService.class).putExtra("id", id));
        } catch (RuntimeException failure) {
            job.put("state", "paused").put("progress", "继续失败，请回到应用重试");
            StudioFiles.save(StudioFiles.job(context, id), job);
            throw new IOException("任务未能启动，请重试", failure);
        }
    }

    static int resumePaused(Context context) throws Exception {
        if (StudioFiles.modelWork()) throw new IOException("请等待模型导入或检查完成");
        if (!active.isEmpty()) throw new IOException("请等待当前任务停止后继续队列");
        JSONArray jobs = StudioFiles.list(context);
        List<JSONObject> selected = new ArrayList<>();
        try {
            for (int n = 0; n < jobs.length(); n++) {
                JSONObject job = jobs.getJSONObject(n);
                // Reuse saved recordings, but never start a new microphone capture in a bulk retry.
                if (job.optString("state").equals("paused") && !needsMicrophone(context, job)
                        && !(job.optString("engine").equals("accelerated")&&RemoteSpeechSession.needsResubmit(remoteFolder(context,job.getString("id"))))) {
                    selected.add(StudioFiles.requeue(context, job.getString("id")));
                }
            }
            if (selected.isEmpty()) return 0;
            context.startForegroundService(new Intent(context, StudioService.class));
        } catch (Exception failure) {
            for (JSONObject job : selected) {
                job.put("state", "paused").put("progress", "继续失败，请回到应用重试");
                StudioFiles.save(StudioFiles.job(context, job.getString("id")), job);
            }
            throw new IOException("队列未能启动，请重试", failure);
        }
        return selected.size();
    }

    public int onStartCommand(Intent i, int flags, int start) {
        synchronized (gate) {
            lastStart = start;
        }
        try {
            android.system.Os.setenv("ORT_DISABLE_TELEMETRY", "1", true);
        } catch (Exception ignored) {
        }
        if (i != null && "cancelTask".equals(i.getAction())) {
            String id = i.getStringExtra("id");
            synchronized (gate) {
                if (id != null && id.equals(active)) {
                    cancel.set(true); stopMic.set(true);
                    cancelEngines();
                } else if (id != null) {
                    try {
                        JSONObject task = StudioFiles.read(StudioFiles.job(this, id));
                        if (task.optString("state").equals("queued")) state(task, "paused", "此任务已暂停，可继续");
                    } catch (Exception ignored) { }
                }
                if (active.isEmpty() && pending == 0) stopSelfResult(lastStart);
            }
            return START_NOT_STICKY;
        }
        if (i != null && "stopMic".equals(i.getAction())) {
            stopMic.set(true);
            synchronized (gate) {
                if (active.isEmpty() && pending == 0) stopSelfResult(lastStart);
            }
            return START_NOT_STICKY;
        }
        if (i != null && "cancel".equals(i.getAction())) {
            synchronized (gate) {
                paused = true;
                cancel.set(true);
                stopMic.set(true);
                cancelEngines();
                JSONArray all = StudioFiles.list(this);
                for (int at = 0; at < all.length(); at++) {
                    JSONObject task = all.optJSONObject(at);
                    if (task.optString("state").equals("queued")) state(task, "paused", "队列已暂停");
                }
                if (active.isEmpty() && pending == 0) stopSelfResult(lastStart);
            }
            return START_NOT_STICKY;
        }
        try {
            if (i != null && i.hasExtra("id")) {
                JSONObject requested = StudioFiles.read(StudioFiles.job(this, i.getStringExtra("id")));
                if (needsMicrophone(this, requested)) microphone = true;
            }
        } catch (Exception ignored) { }
        try {
            foreground("准备音频任务…");
        } catch (RuntimeException failure) {
            String requested = i == null ? "" : i.getStringExtra("id");
            JSONArray jobs = StudioFiles.list(this);
            for (int n = 0; n < jobs.length(); n++) {
                JSONObject task = jobs.optJSONObject(n);
                if (task.optString("state").equals("queued") && (requested == null || requested.isEmpty() || task.optString("id").equals(requested)))
                    recordFailure(task, failure, "启动后台服务");
            }
            synchronized (gate) { if (pending == 0) { microphone = false; stopSelfResult(lastStart); } }
            return START_NOT_STICKY;
        }
        synchronized (gate) {
            paused = false;
            pending++;
            worker.execute(this::drain);
        }
        return START_NOT_STICKY;
    }

    static void requireMicrophonePermission(Context context, JSONObject job) throws IOException {
        if (needsMicrophone(context, job)
                && context.checkSelfPermission("android.permission.RECORD_AUDIO")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new IOException("请先授权麦克风，再开始录音");
    }

    static boolean needsMicrophone(Context context, JSONObject job) {
        return job.optString("type").equals("dictation")
                && !new File(new File(StudioFiles.dir(context, "jobs"), job.optString("id")), "input.wav").isFile();
    }

    void foreground(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(
                new NotificationChannel("studio", "音频生成与处理", NotificationManager.IMPORTANCE_LOW));
        PendingIntent
                open =
                        PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, WorkspaceActivity.class).putExtra("section", "任务"),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE),
                stop =
                        PendingIntent.getService(
                                this,
                                1,
                                new Intent(this, StudioService.class)
                                        .setAction(microphone ? "stopMic" : "cancel"),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n =
                new Notification.Builder(this, "studio")
                        .setContentTitle("VoiceStudio · 音频任务")
                        .setContentText(text)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setOngoing(true)
                        .setContentIntent(open)
                        .addAction(
                                new Notification.Action.Builder(
                                                null, microphone ? "结束录音" : "暂停队列", stop)
                                        .build())
                        .build();
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(
                    47,
                    n,
                    microphone
                            ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                            : ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else if (Build.VERSION.SDK_INT >= 29)
            startForeground(47, n, microphone ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0);
        else startForeground(47, n);
    }

    void state(JSONObject job, String state, String message) {
        try {
            String id=job.getString("id");long now=System.nanoTime();
            String previous=job.optString("state");
            boolean persist=!state.equals(previous)||progressGate.shouldWrite(id,state,message,now);
            job.put("state", state).put("progress", message);
            if (state.equals("running") && (!job.has("started") || previous.equals("queued"))) {
                job.put("started", System.currentTimeMillis());
                job.remove("finished"); job.remove("failureStage"); job.remove("failureDetail"); job.remove("previewReady");
            }
            if (state.equals("done") || state.equals("failed") || state.equals("paused")) job.put("finished", System.currentTimeMillis());
            // Always update live state for cancellation/failure detail, even when UI persistence is coalesced.
            if(persist) {
                StudioFiles.save(StudioFiles.job(this,id),job);
                progressGate.written(id,state,message,now);
                sendBroadcast(new Intent("com.voice.studio.JOB").setPackage(getPackageName()));
            }
        } catch (Exception ignored) {
        }
    }

    void drain() {
        PowerManager.WakeLock wake =
                ((PowerManager) getSystemService(POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceStudio:offline");
        wake.acquire(4 * 60 * 60 * 1000L);
        try {
            while (true) {
                JSONObject job;
                synchronized (gate) {
                    if (paused) break;
                    JSONArray all = StudioFiles.list(this);
                    job = null;
                    for (int i = all.length() - 1; i >= 0; i--)
                        if (all.optJSONObject(i).optString("state").equals("queued")) {
                            job = all.optJSONObject(i);
                            break;
                        }
                    if (job == null) break;
                    active = job.optString("id");
                    cancel.set(false);
                    stopMic.set(false);
                    state(job, "running", "开始处理…");
                }
                microphone = needsMicrophone(this, job);
                try {
                    foreground("正在处理 · " + job.optString("title", "离线任务"));
                    execute(job);
                    OmniEngine.check(cancel);
                    state(job, "done", "已完成，可导出");
                } catch (Throwable e) {
                    if (cancel.get()) state(job, "paused", "已暂停，可继续");
                    else recordFailure(job, e, job.optString("progress", "开始处理"));
                } finally {
                    active = "";
                    microphone = false;
                    whisper = null;
                    translator = null;
                    speaker = null;
                    separator = null;
                }
            }
        } finally {
            if (wake.isHeld()) wake.release();
            synchronized (gate) {
                if (--pending == 0) {
                    stopForeground(true);
                    stopSelfResult(lastStart);
                }
            }
        }
    }

    void recordFailure(JSONObject job, Throwable failure, String stage) {
        try {
            String detail = "fish".equals(job.optString("engine")) ? FishClient.failure(failure)
                    : failure.getClass().getSimpleName() + ": " + (failure.getMessage() == null ? "无详细消息" : failure.getMessage());
            if (detail.length() > 4096) detail = detail.substring(0, 4096);
            job.put("failureStage", stage).put("failureDetail", detail);
            String hint = failure instanceof OutOfMemoryError ? "手机内存不足，请关闭其他应用、缩短文本，或切换 Fish Audio"
                    : failure instanceof LinkageError ? "本机推理库加载失败，请复制错误详情并检查手机架构"
                    : "fish".equals(job.optString("engine")) ? detail : "失败：" + detail;
            state(job, "failed", hint);
        } catch (Exception ignored) { state(job, "failed", "处理失败，请重新打开任务详情"); }
    }

    static long audioDuration(File file, String format) throws Exception {
        if ("wav".equals(format)) {
            try (PcmAudio.Reader reader = new PcmAudio.Reader(file)) { return Math.round(reader.seconds() * 1000); }
        }
        android.media.MediaMetadataRetriever metadata = new android.media.MediaMetadataRetriever();
        try {
            metadata.setDataSource(file.getAbsolutePath());
            String value = metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            return value == null ? 0 : Long.parseLong(value);
        } finally { metadata.release(); }
    }

    void fishSpeech(JSONObject job, File out) throws Exception {
        File pendingFile = new File(out.getPath() + ".pending");
        try {
            fish.generate(new SecretStore(this).read(), job.optString("model", "s2.1-pro-free"),
                    job.getString("text"), job.optString("reference"), (float)job.optDouble("speed", 1),
                    pendingFile, cancel, m -> state(job, "running", m));
            state(job, "running", "检查 Fish 音频并保存…");
            long millis = audioDuration(pendingFile, "mp3");
            if (millis <= 0) throw new IOException("Fish 返回音频无法解码，请重新生成");
            job.put("duration", millis);
            OmniEngine.check(cancel);
            if (!pendingFile.renameTo(out)) throw new IOException("Fish 音频保存失败，请检查手机空间");
        } finally { pendingFile.delete(); }
    }

    void acceleratedSpeech(JSONObject job, File out) throws Exception {
        String endpoint = job.getString("endpoint");
        String current = getSharedPreferences("studio",0).getString("accelerated-url", "");
        String provider=job.optString("provider","custom");
        if(!provider.equals(getSharedPreferences("studio",0).getString("accelerated-provider","custom")))throw new IOException("GPU 平台已改变，请按新配置重新创建任务");
        if (!endpoint.equals(current)) throw new IOException("加速接口地址已改变，请按新配置重新创建任务");
        File pending = new File(out.getPath()+".pending"),reference=new File(out.getPath()+".reference.pending.wav");
        try {
            long start = System.nanoTime();
            boolean useGradio=job.optString("protocol","rest").equals("gradio");
            if(!job.optString("protocol","rest").equals(getSharedPreferences("studio",0).getString("accelerated-protocol","rest")))throw new IOException("GPU 调用方式已改变，请重新创建任务");
            if(useGradio&&!provider.equals("hf"))throw new IOException("Space 平台配置无效");
            String referenceText=null;File ref=null;
            if(useGradio || !job.optString("profile").isEmpty()) {
                String profile=job.optString("profile");
                if(!profile.equals(getSharedPreferences("studio",0).getString(useGradio?"gpu-hf-profile":"cloud-profile","")))throw new IOException("参考录音上传设置已改变，请重新创建任务");
                if(!profile.isEmpty()) {
                    if(!profile.matches("[a-f0-9-]{36}"))throw new IOException("参考人物音色编号无效");
                    File folder=StudioFiles.dir(this,"profiles");JSONObject meta=StudioFiles.read(new File(folder,profile+".json"));
                    double referenceStart=meta.optDouble("referenceStart",0),seconds=meta.optDouble("referenceSeconds",6);
                    if(!Double.isFinite(referenceStart)||referenceStart<0||!Double.isFinite(seconds)||seconds<3||seconds>10)throw new IOException("远程参考录音需为 3—10 秒，请重新选择片段");
                    try(PcmAudio.Reader reader=new PcmAudio.Reader(new File(folder,profile+".reference.wav"));PcmAudio.Writer writer=new PcmAudio.Writer(reference,24000)) {
                        if(referenceStart+seconds>reader.seconds()+.02)throw new IOException("参考录音片段不完整，请重新创建音色");
                        writer.write(PcmAudio.resample(reader.read(referenceStart,seconds),reader.rate,24000));
                    }
                    ref=reference;referenceText=meta.optString("referenceText","");
                }
            } else if(!provider.equals("custom")&&!provider.equals("modal"))throw new IOException("GPU 平台配置无效");
            String scope=useGradio?"gpu-hf":provider.equals("modal")?"gpu-modal":"accelerated";
            long millis=RemoteSpeechPipeline.generate(job,new SecretStore(this,scope).read(),ref,referenceText,remoteFolder(this,job.getString("id")),
                pending,cancel,m->{
                    if(m.contains("音频已校验")||m.contains("复用已完成音频"))try{if(SynthesisPreview.remote(new File(new File(StudioFiles.dir(this,"jobs"),job.getString("id")),"gpu"))!=null)job.put("previewReady",true);}catch(Exception ignored){}
                    state(job,"running",m);
                },gradio,accelerated);
            OmniEngine.check(cancel);
            job.put("duration",millis).put("generationMillis",(System.nanoTime()-start)/1000000);
            if(!pending.renameTo(out))throw new IOException("接口音频保存失败");
        } finally {pending.delete();reference.delete();}
    }

    VoicePrompt voiceForSpeech(JSONObject job) throws Exception {
        String id = job.optString("profile");
        VoicePrompt original = StudioFiles.voice(this, id);
        if (original == null) return null;
        File profiles = StudioFiles.dir(this, "profiles"), source = new File(profiles, id + ".reference.wav");
        if (!source.isFile() || !StudioFiles.ready(this, "whisper")) return original;
        VoicePrompt resolved = original;
        File folder = new File(StudioFiles.dir(this, "jobs"), job.getString("id"));folder.mkdirs();
        File cut = new File(folder, "reference-transcript.wav");
        File pending = new File(profiles, id + ".vpc.pending");
        try {
            JSONObject meta = StudioFiles.read(new File(profiles, id + ".json"));
            double start = meta.optDouble("referenceStart", 0), seconds = meta.optDouble("referenceSeconds", original.frames / 25.0);
            if (!Double.isFinite(start) || start < 0 || !Double.isFinite(seconds) || seconds < 3 || seconds > 20) return original;
            if (ReferenceTranscript.checked(meta, original, source, start, seconds)) return original;
            state(job, "running", "正在自动识别原参考录音，保留克隆音色…");
            try (PcmAudio.Reader reader = new PcmAudio.Reader(source);PcmAudio.Writer writer = new PcmAudio.Writer(cut, 24000)) {
                if (start + seconds > reader.seconds() + .02) return original;
                writer.write(PcmAudio.resample(reader.read(start, seconds), reader.rate, 24000));
            }
            whisper = new WhisperEngine(new File(getFilesDir(), "whisper-model"), getAssets().open("whisper-mel80.bin"));
            StringBuilder content = new StringBuilder();
            for (WhisperEngine.Segment segment : whisper.transcribe(cut, "auto", false, cancel, m -> state(job, "running", m)))
                content.append(segment.text).append(' ');
            String transcript = content.toString().trim();
            transcript = ReferenceTranscript.select(original.text, transcript, original.frames);
            resolved = new VoicePrompt(transcript, original.codes, original.frames, original.rms);
            File current = new File(profiles, id + ".vpc");
            boolean changed = !transcript.equals(original.text);
            if (!current.isFile() || !VoicePrompt.load(current).identity().equals(original.identity())) return original;
            OmniEngine.check(cancel);
            if (changed) {
                resolved.save(pending);OmniEngine.check(cancel);
                if (!pending.renameTo(current)) throw new IOException("保存参考文字修复失败");
                if (!meta.has("providedReferenceText")) meta.put("providedReferenceText", original.text);
                meta.put("referenceText", transcript).put("referenceTextAutoRepaired", true);
            }
            meta.put("referenceTranscriptCheckedFor", ReferenceTranscript.identity(resolved, source, start, seconds));
            StudioFiles.save(new File(profiles, id + ".json"), meta);
            job.put("referenceTranscriptChecked", true).put("referenceTextAutoRepaired", changed);
            state(job, "running", "已核对参考录音内容，正在用克隆音色朗读新文字…");
        } catch (Exception repairFailure) {
            OmniEngine.check(cancel);
            state(job, "running", "沿用已保存的克隆音色，新文字独立估算时长…");
        } finally { whisper = null;cut.delete();pending.delete(); }
        return resolved;
    }

    void speech(JSONObject job, String text, File out) throws Exception {
        if (!StudioFiles.ready(this, "omni")) throw new IOException("请先导入基础模型");
        VoicePrompt ref = voiceForSpeech(job);
        File anchor = null;
        boolean bookAuto = job.optString("type").equals("book") && ref == null;
        boolean anchored = false;
        if (bookAuto) {
            anchor =
                    new File(
                            new File(StudioFiles.dir(this, "jobs"), job.getString("id")),
                            "book.voice.vpc");
            if (anchor.isFile()) {
                ref = VoicePrompt.load(anchor);
                anchored = true;
            }
        }
        File tmp = new File(out.getPath() + ".pending");
        try {
            engine.generate(
                    new File(getFilesDir(), "omni-model"),
                    text,
                    job.optString("language", "zh"),
                    anchored ? 1 : (float) job.optDouble("speed", 1),
                    tmp,
                    cancel,
                    m -> {if(m.equals(SynthesisPreview.READY))try{job.put("previewReady",true);}catch(Exception ignored){}state(job,"running",m);},
                    ref,
                    job.optString("style", "None"),
                    job.optInt("steps", 32),
                    job.optLong("seed", 42),
                    new File(remoteFolder(this,job.getString("id")).getParentFile(),"local-"+out.getName()),
                    "tts".equals(job.optString("type","tts"))?new File(remoteFolder(this,job.getString("id")).getParentFile(),"preview.wav"):null);
            OmniEngine.check(cancel);
            if("tts".equals(job.optString("type", "tts"))) {
                job.put("generationMillis", engine.lastGenerationMillis);
                job.put("synthesisTiming", new JSONObject()
                        .put("tokenizerMillis", engine.tokenizerNanos / 1000000)
                        .put("loadMillis", engine.loadNanos / 1000000)
                        .put("inferenceMillis", engine.inferenceNanos / 1000000)
                        .put("modelRunMillis", engine.modelRunNanos / 1000000)
                        .put("samplingMillis", engine.samplingNanos / 1000000)
                        .put("modelRuns", engine.backboneRuns)
                        .put("samplingWorkers", engine.samplingWorkers)
                        .put("parallelSamplingRounds", engine.parallelSamplingRounds)
                        .put("prePinnedChunks", engine.prePinnedChunks)
                        .put("outputCopyBytes", engine.outputCopyBytes)
                        .put("outputShapeFallbacks", engine.outputShapeFallbacks)
                        .put("decodedPinnedChunks", engine.decodedPinnedChunks)
                        .put("decoderOutputCopyBytes", engine.decoderOutputCopyBytes)
                        .put("pcmWriteMillis", engine.pcmWriteNanos / 1000000)
                        .put("bufferedPcmChunks", engine.bufferedPcmChunks)
                        .put("pcmBufferBytes", OmniPcmWriter.BUFFER_BYTES)
                        .put("reusedEncodingChunks", engine.reusedEncodingChunks)
                        .put("reusedDecodedChunks", engine.reusedDecodedChunks)
                        .put("decoderRuns", engine.decoderRuns)
                        .put("decodeMillis", engine.decodeNanos / 1000000)
                        .put("chunks", engine.chunkCount)
                        .put("backboneLoads", engine.backboneLoads)
                        .put("decoderLoads", engine.decoderLoads));
            }
            if (!tmp.renameTo(out)) throw new IOException("无法保存音频");
            if (bookAuto && !anchored && engine.automaticPrompt != null) {
                File pendingVoice = new File(anchor.getPath() + ".pending");
                try {
                    engine.automaticPrompt.save(pendingVoice);
                    if (!pendingVoice.renameTo(anchor)) throw new IOException("保存有声书音色失败");
                } finally {
                    pendingVoice.delete();
                }
            }
        } finally {
            tmp.delete();
        }
    }

    void execute(JSONObject job) throws Exception {
        String id = job.getString("id");
        String type = job.optString("type", "tts");
        if (type.equals("tts")) {
            if(!"fish".equals(job.optString("engine"))&&!"accelerated".equals(job.optString("engine")))
                throw new IOException("本地合成已移除。请改用 Fish Audio 或七引擎云端服务。");
            boolean cloud = "fish".equals(job.optString("engine"));
            boolean api = "accelerated".equals(job.optString("engine"));
            File audio = StudioFiles.output(this, id, cloud ? "mp3" : "wav");
            long existing = SynthesisOutput.existing(audio, cloud ? "mp3" : "wav", StudioService::audioDuration);
            if (existing > 0 && !cloud && !api) {
                try {
                    OmniDuration.checkOutput(job.getString("text"), (float)job.optDouble("speed", 1), existing / 1000.0,
                            job.optString("language","zh"),!job.optString("profile").isEmpty(),job.optInt("steps",32));
                    try (PcmAudio.Reader reader = new PcmAudio.Reader(audio)) { reader.validateSpeech(cancel); }
                }
                catch (IOException incomplete) {
                    OmniEngine.check(cancel);
                    // Keep the original file until a valid replacement is ready.
                    existing = 0;
                    state(job, "running", "旧结果截短或静音，正在重新生成完整音频…");
                }
            }
            if (existing > 0) {
                state(job, "running", "复用已生成音频，恢复保存记录…");
                job.put("duration", existing);
            } else if (cloud) fishSpeech(job, audio);
            else if (api) acceleratedSpeech(job, audio);
            else {
                speech(job, job.getString("text"), audio);
                job.put("duration", audioDuration(audio, "wav"));
            }
            OmniEngine.check(cancel);
            job.put("output", audio.getName()).put("file", audio.getName())
                    .put("engine", cloud ? "fish" : api ? "accelerated" : "omni")
                    .put("model", cloud ? job.optString("model", "s2.1-pro-free") : api ? job.optString("model", "OmniVoice 加速接口") : "OmniVoice INT8")
                    .put("voice", job.optString("voice", "OmniVoice"))
                    .put("title", AudioChecks.shortTitle(job.getString("text")));
            job.put("audioOutput", audio.getName());
            StudioFiles.history(this, job);
        } else if (type.equals("clone")) {
            if (!StudioFiles.ready(this, "clone")) throw new IOException("请先导入克隆编码模型");
            File folder = StudioFiles.dir(this, "profiles"),
                    source = new File(folder, id + ".reference.wav");
            if (!source.isFile()) {
                File pending = new File(source.getPath() + ".pending");
                try {
                    String recorded = job.optString("referenceAudio");
                    if (!recorded.isEmpty()) {
                        if (!recorded.matches("[a-f0-9-]+\\.wav")) throw new IOException("参考录音路径无效");
                        try (InputStream in = new FileInputStream(new File(StudioFiles.dir(this, "audio"), recorded));
                                OutputStream out = new FileOutputStream(pending)) {
                            MediaFiles.copy(in, out, cancel, 500000000L);
                        }
                    } else MediaFiles.decode(
                            this,
                            android.net.Uri.parse(job.getString("uri")),
                            pending,
                            cancel,
                            m -> state(job, "running", m));
                    OmniEngine.check(cancel);
                    if (!pending.renameTo(source)) throw new IOException("保存参考音频失败");
                } finally {
                    pending.delete();
                }
            }

            String supplied = job.getString("text"), transcript = supplied;
            boolean transcriptChecked = false;
            if (transcript.trim().isEmpty() || StudioFiles.ready(this, "whisper")) {
                if (!StudioFiles.ready(this, "whisper")) throw new IOException("自动参考文字需要识别模型");
                state(job, "running", "正在核对录音内容；此音色可朗读其他新文字…");
                File cut = new File(folder, id + ".cut.wav");
                try {
                    try (PcmAudio.Reader reader = new PcmAudio.Reader(source);
                            PcmAudio.Writer writer = new PcmAudio.Writer(cut, 24000)) {
                        writer.write(
                                PcmAudio.resample(
                                        reader.read(
                                                job.optDouble("start", 0),
                                                job.optDouble("seconds", 6)),
                                        reader.rate,
                                        24000));
                    }
                    whisper =
                            new WhisperEngine(
                                    new File(getFilesDir(), "whisper-model"),
                                    getAssets().open("whisper-mel80.bin"));
                    StringBuilder text = new StringBuilder();
                    for (WhisperEngine.Segment segment :
                            whisper.transcribe(
                                    cut, "auto", false, cancel, m -> state(job, "running", m)))
                        text.append(segment.text).append(' ');
                    transcript = ReferenceTranscript.select(supplied, text.toString().trim(),
                            (int)(job.optDouble("seconds", 6) * 25));
                    transcriptChecked = true;
                    job.put("referenceText", transcript);
                } catch (Exception recognitionFailure) {
                    OmniEngine.check(cancel);
                    if (supplied.trim().isEmpty()) throw recognitionFailure;
                    transcript = supplied;
                    state(job, "running", "自动核对暂不可用，沿用填写的录音文字…");
                } finally {
                    whisper = null;cut.delete();
                }
            }
            VoicePrompt voice =
                    new OmniCodec()
                            .encode(
                                    new File(getFilesDir(), "clone-model"),
                                    source,
                                    transcript,
                                    job.optDouble("start", 0),
                                    job.optDouble("seconds", 6),
                                    cancel,
                                    m -> state(job, "running", m));
            File target = new File(folder, id + ".vpc"),
                    tmp = new File(target.getPath() + ".pending");
            voice.save(tmp);
            OmniEngine.check(cancel);
            if (!tmp.renameTo(target)) throw new IOException("保存音色失败");
            JSONObject info =
                    new JSONObject()
                            .put("name", job.optString("name", "我的音色"))
                            .put("id", id)
                            .put("referenceText", transcript)
                            .put("referenceStart", job.optDouble("start", 0))
                            .put("referenceSeconds", job.optDouble("seconds", 6))
                            .put("created", System.currentTimeMillis());
            if (!supplied.equals(transcript)) info.put("providedReferenceText", supplied).put("referenceTextAutoRepaired", true);
            if (transcriptChecked) info.put("referenceTranscriptCheckedFor", ReferenceTranscript.identity(voice, source,
                    job.optDouble("start", 0), job.optDouble("seconds", 6)));
            StudioFiles.save(new File(folder, id + ".json"), info);
            job.put("profileResult", id);
            // Preserve the original reference recording for later listening.
        } else if (type.equals("extractVoices")) {
            extractVoices(job);
        } else if (type.equals("transcribe") || type.equals("dictation")) {
            if (!StudioFiles.ready(this, "whisper")) throw new IOException("请先导入 Whisper 识别模型");
            File folder = new File(StudioFiles.dir(this, "jobs"), id);
            folder.mkdirs();
            File audio = new File(folder, "input.wav");
            if (!audio.isFile()) {
                File tmp = new File(folder, "input.pending.wav");
                try {
                    if (type.equals("dictation")) {
                        try { record(job, tmp); }
                        catch (Exception recordingFailure) {
                            boolean preserved = false;
                            try { RecordedInput.preserve(tmp, audio); preserved = true; }
                            catch (Exception preserveFailure) { recordingFailure.addSuppressed(preserveFailure); }
                            if (preserved) {
                                job.put("partialRecording", true);
                                throw new IOException((recordingFailure.getMessage() == null ? "录音中断" : recordingFailure.getMessage())
                                        + "；已保留录音，可试听或继续识别", recordingFailure);
                            }
                            throw recordingFailure;
                        }
                    } else
                        MediaFiles.decode(
                                this,
                                android.net.Uri.parse(job.getString("uri")),
                                tmp,
                                cancel,
                                m -> state(job, "running", m));
                    if (!type.equals("dictation")) OmniEngine.check(cancel);
                    if (type.equals("dictation")) RecordedInput.preserve(tmp, audio);
                    else if (!tmp.renameTo(audio)) throw new IOException("无法保存录音");
                } finally {
                    tmp.delete();
                }
            }
            OmniEngine.check(cancel);
            try (PcmAudio.Reader input = new PcmAudio.Reader(audio)) { job.put("audioSeconds", input.seconds()); }
            if (type.equals("dictation")) foreground("录音结束 · 正在离线识别");
            whisper =
                    new WhisperEngine(
                            new File(getFilesDir(), "whisper-model"),
                            getAssets().open("whisper-mel80.bin"));
            List<WhisperEngine.Segment> segments =
                    whisper.transcribe(
                            audio,
                            job.optString("recognitionLanguage", "auto"),
                            false,
                            cancel,
                            m -> state(job, "running", m));
            if (segments.isEmpty()) throw new IOException("没有识别到语音，请检查录音");
            JSONArray list = new JSONArray();
            StringBuilder text = new StringBuilder();
            for (WhisperEngine.Segment segment : segments) {
                list.put(
                        new JSONObject()
                                .put("start", segment.start)
                                .put("end", segment.end)
                                .put("text", segment.text));
                text.append(segment.text).append('\n');
            }
            StudioFiles.writeText(StudioFiles.output(this, id, "txt"), text.toString());
            StudioFiles.writeText(StudioFiles.output(this, id, "srt"), WhisperEngine.srt(segments));
            StudioFiles.writeText(StudioFiles.output(this, id, "json"), list.toString());
            job.put("segments", list).put("output", id + ".txt");
        } else if (type.equals("translate")) {
            if (!StudioFiles.ready(this, "translation")) throw new IOException("请先导入翻译模型");
            translator = new NllbEngine(new File(getFilesDir(), "translation-model"));
            String result =
                    translator.translate(
                            job.getString("text"),
                            job.getString("sourceLanguage"),
                            job.getString("targetLanguage"),
                            cancel,
                            m -> state(job, "running", m));
            StudioFiles.writeText(StudioFiles.output(this, id, "txt"), result);
            job.put("translation", result).put("output", id + ".txt");
        } else if (type.equals("dub")) {
            dub(job);
        } else if (type.equals("speaker")) {
            String sourceId = job.getString("sourceJob");
            JSONObject source = StudioFiles.read(StudioFiles.job(this, sourceId));
            File input = new File(new File(StudioFiles.dir(this, "jobs"), source.optString("sourceJob", sourceId)), "input.wav");
            if (!StudioFiles.ready(this, "speaker")) throw new IOException("请先导入说话人模型");
            try (SpeakerEngine runtime =
                    new SpeakerEngine(new File(getFilesDir(), "speaker-model"))) {
                speaker = runtime;
                JSONArray grouped =
                        runtime.group(
                                input,
                                job.getJSONArray("segments"),
                                job.optDouble("threshold", .55),
                                job.optInt("speakerCount", 0),
                                cancel,
                                m -> state(job, "running", m));
                OmniEngine.check(cancel);
                StudioFiles.replaceSegments(this, sourceId, job.optLong("sourceRevision"), grouped);
                job.put("groupedSource", sourceId).put("output", sourceId + ".json");
            }
        } else if (type.equals("separate")) {
            if (!StudioFiles.ready(this, "separation")) throw new IOException("请导入人声分离模型");
            File folder = new File(StudioFiles.dir(this, "jobs"), id);
            folder.mkdirs();
            File input = new File(folder, "stereo.wav");
            if (!input.isFile()) {
                File pending = new File(folder, "input.pending.wav");
                try {
                    MediaFiles.decode(
                            this,
                            android.net.Uri.parse(job.getString("uri")),
                            pending,
                            cancel,
                            m -> state(job, "running", m),
                            true);
                    OmniEngine.check(cancel);
                    if (!pending.renameTo(input)) throw new IOException("保存输入音轨失败");
                } finally {
                    pending.delete();
                }
            }
            File vocals = StudioFiles.output(this, id, "wav"),
                    instrumental = StudioFiles.output(this, id + "-a", "wav"),
                    vtmp = new File(folder, "vocals.pending.wav"),
                    mtmp = new File(folder, "instrumental.pending.wav");
            separator = new SeparationEngine();
            try {
                separator.separate(
                        new File(getFilesDir(), "separation-model"),
                        input,
                        vtmp,
                        mtmp,
                        cancel,
                        m -> state(job, "running", m));
                OmniEngine.check(cancel);
                if (!vtmp.renameTo(vocals) || !mtmp.renameTo(instrumental))
                    throw new IOException("保存分离结果失败");
                job.put("output", vocals.getName())
                        .put("instrumentalOutput", instrumental.getName());
            } finally {
                vtmp.delete();
                mtmp.delete();
            }
        } else if (type.equals("book")) {
            File folder = new File(StudioFiles.dir(this, "jobs"), id);
            folder.mkdirs();
            File source = new File(folder, "source");
            if (!source.isFile()) {
                File pending = new File(folder, "source.pending");
                try {
                    try (InputStream in =
                                    getContentResolver()
                                            .openInputStream(
                                                    android.net.Uri.parse(job.getString("uri")));
                            OutputStream out = new FileOutputStream(pending)) {
                        MediaFiles.copy(in, out, cancel, 20000000L);
                    }
                    if (!pending.renameTo(source)) throw new IOException("导入书籍失败");
                } finally {
                    pending.delete();
                }
            }
            List<String> chapters = BookReader.chapters(source);
            JSONArray index = new JSONArray();
            File result = StudioFiles.output(this, id, "zip"),
                    pending = new File(folder, "book.pending");
            try (java.util.zip.ZipOutputStream zip =
                    new java.util.zip.ZipOutputStream(
                            new BufferedOutputStream(new FileOutputStream(pending)))) {
                int number = 0;
                for (String text : chapters) {
                    OmniEngine.check(cancel);
                    String name = String.format(Locale.US, "chapter-%04d.wav", ++number);
                    File chapter = new File(folder, name);
                    if (!chapter.isFile()) speech(job, text, chapter);
                    double seconds;
                    try (PcmAudio.Reader reader = new PcmAudio.Reader(chapter)) {
                        if (reader.frames < 1) throw new IOException("章节不完整");
                        seconds = reader.seconds();
                    }
                    index.put(
                            new JSONObject()
                                    .put("file", name)
                                    .put("title", AudioChecks.shortTitle(text))
                                    .put("seconds", seconds));
                    zip.putNextEntry(new java.util.zip.ZipEntry(name));
                    try (InputStream in = new FileInputStream(chapter)) {
                        MediaFiles.copy(in, zip, cancel, 2000000000L);
                    }
                    zip.closeEntry();
                    state(job, "running", "有声书 · " + number + " / " + chapters.size() + " 章");
                }
                zip.putNextEntry(new java.util.zip.ZipEntry("chapters.json"));
                zip.write(index.toString().getBytes("UTF-8"));
                zip.closeEntry();
                zip.putNextEntry(new java.util.zip.ZipEntry("NOTICE.txt"));
                zip.write("VoiceStudio AI 合成语音".getBytes("UTF-8"));
                zip.closeEntry();
            }
            OmniEngine.check(cancel);
            if (!pending.renameTo(result)) throw new IOException("保存有声书失败");
            job.put("output", result.getName()).put("chapters", index);
        } else throw new IOException("任务类型不受支持");
    }

    void dub(JSONObject job) throws Exception {
        String id = job.getString("id"), sourceId = job.getString("sourceJob");
        JSONObject source = StudioFiles.read(StudioFiles.job(this, sourceId));
        JSONArray segments = job.getJSONArray("segments");
        if (segments.length() == 0) throw new IOException("没有可配音字幕");
        File original = new File(new File(StudioFiles.dir(this, "jobs"), sourceId), "input.wav"),
                folder = new File(StudioFiles.dir(this, "jobs"), id);
        folder.mkdirs();
        double duration;
        try (PcmAudio.Reader r = new PcmAudio.Reader(original)) {
            duration = r.seconds();
        }
        File result = StudioFiles.output(this, id, "wav"),
                pending = new File(folder, "dub.pending.wav");
        long cursor = 0;
        List<WhisperEngine.Segment> finalSegments = new ArrayList<>();
        boolean translating =
                !job.optString("sourceLanguage").isEmpty()
                        && !job.optString("sourceLanguage").equals(job.optString("targetLanguage"));
        if (translating) {
            if (!StudioFiles.ready(this, "translation")) throw new IOException("请导入翻译模型");
            translator = new NllbEngine(new File(getFilesDir(), "translation-model"));
        }
        try (PcmAudio.Writer writer = new PcmAudio.Writer(pending, 24000)) {
            for (int n = 0; n < segments.length(); n++) {
                OmniEngine.check(cancel);
                JSONObject segment = segments.getJSONObject(n);
                double start = segment.getDouble("start"), end = segment.getDouble("end");
                if (!Double.isFinite(start)
                        || !Double.isFinite(end)
                        || start < 0
                        || end <= start
                        || end > duration + .1
                        || end - start > 60) throw new IOException("字幕时间超出音轨范围或单条超过 60 秒");
                long begin = Math.round(start * 24000), finish = Math.round(end * 24000);
                if (begin < cursor) throw new IOException("字幕时间重叠，请先校对");
                writer.silence((double) (begin - cursor) / 24000);
                String text = segment.getString("text").trim();
                if (translating)
                    text =
                            translator.translate(
                                    text,
                                    job.getString("sourceLanguage"),
                                    job.getString("targetLanguage"),
                                    cancel,
                                    m -> state(job, "running", m));
                if (text.isEmpty()) writer.silence((double) (finish - begin) / 24000);
                else {
                    File clip = new File(folder, String.format(Locale.US, "segment-%04d.wav", n));
                    JSONObject params = new JSONObject(job.toString());
                    params.put("profile", segment.optString("profile", job.optString("profile")));
                    speech(params, text, clip);
                    try (PcmAudio.Reader audio = new PcmAudio.Reader(clip)) {
                        writer.write(
                                AudioTiming.fit(
                                        AudioTiming.readCompleteClip(audio),
                                        (int) (finish - begin)));
                    }
                    clip.delete();
                }
                cursor = finish;
                finalSegments.add(new WhisperEngine.Segment(start, end, text));
                state(job, "running", "配音 · " + (n + 1) + " / " + segments.length() + " 条");
            }
            writer.silence(Math.max(0, duration - (double) cursor / 24000));
        }
        OmniEngine.check(cancel);
        PcmAudio.markSynthetic(pending);
        if (!pending.renameTo(result)) throw new IOException("配音音轨保存失败");
        StudioFiles.writeText(
                StudioFiles.output(this, id, "srt"), WhisperEngine.srt(finalSegments));
        job.put("output", id + ".wav").put("audioOutput", id + ".wav");
        if (job.optBoolean("video")) {
            if (!source.has("uri")) throw new IOException("该任务没有视频源，可导出 WAV");
            File mp4 = StudioFiles.output(this, id, "mp4"),
                    tmp = new File(folder, "video.pending.mp4");
            try {
                MediaFiles.dub(
                        this,
                        android.net.Uri.parse(source.getString("uri")),
                        result,
                        tmp,
                        cancel,
                        m -> state(job, "running", m));
                OmniEngine.check(cancel);
                if (!tmp.renameTo(mp4)) throw new IOException("视频保存失败");
                job.put("output", id + ".mp4");
            } finally {
                tmp.delete();
            }
        }
    }

    void extractVoices(JSONObject job) throws Exception {
        String id = job.getString("id"), sourceId = job.optString("sourceJob");
        File folder = new File(StudioFiles.dir(this, "jobs"), sourceId.isEmpty() ? id : sourceId);
        folder.mkdirs();
        File input = new File(folder, "input.wav");
        if (!input.isFile()) {
            if (!sourceId.isEmpty()) throw new IOException("原任务音轨已丢失，请重新导入");
            File pending = new File(folder, "input.pending.wav");
            try {
                MediaFiles.decode(this, android.net.Uri.parse(job.getString("uri")), pending, cancel, m -> state(job, "running", m));
                OmniEngine.check(cancel);
                if (!pending.renameTo(input)) throw new IOException("保存视频音轨失败");
            } finally { pending.delete(); }
        }
        if (!job.has("segments") || job.optBoolean("regroup")) {
            if (!StudioFiles.ready(this, "speaker")) throw new IOException("请先安装说话人模型");
            JSONArray segments = new JSONArray(), shortSegments = new JSONArray();
            if (job.has("segments")) {
                JSONArray previous = job.getJSONArray("segments");
                for (int n=0;n<previous.length();n++) {
                    JSONObject item=new JSONObject(previous.getJSONObject(n).toString());
                    if (item.getDouble("end")-item.getDouble("start")>=.5) segments.put(item);
                    else shortSegments.put(item.put("speaker",0));
                }
            } else {
                if (!StudioFiles.ready(this,"whisper")) throw new IOException("请先安装识别模型");
                whisper = new WhisperEngine(new File(getFilesDir(), "whisper-model"), getAssets().open("whisper-mel80.bin"));
                for (WhisperEngine.Segment segment : whisper.transcribe(input, "auto", false, cancel, m -> state(job, "running", m))) {
                    JSONObject item = new JSONObject().put("start", segment.start).put("end", segment.end).put("text", segment.text);
                    if (segment.end - segment.start >= .5) segments.put(item); else shortSegments.put(item.put("speaker", 0));
                }
                whisper=null;
            }
            if (segments.length() == 0) throw new IOException("没有足够长的单人对白；请转写后人工校对人物片段");
            try (SpeakerEngine runtime = new SpeakerEngine(new File(getFilesDir(), "speaker-model"))) {
                speaker = runtime;
                JSONArray grouped = runtime.group(input, segments, job.optDouble("threshold", .55), job.optInt("speakerCount", 0), cancel, m -> state(job, "running", m));
                for (int n = 0; n < shortSegments.length(); n++) grouped.put(shortSegments.getJSONObject(n));
                job.put("segments", grouped).put("shortSegments", shortSegments.length()).put("regroup", false).put("speakerGroupingVersion", 1);
            } finally { speaker=null; }
            state(job, "running", "分组完成 · 正在提取人物声音");
        }
        JSONArray results = new JSONArray();
        try (PcmAudio.Reader reader = new PcmAudio.Reader(input)) {
            job.put("audioSeconds", reader.seconds());
            Map<Integer, JSONArray> groups = SpeakerClips.plan(job.getJSONArray("segments"), reader.seconds());
            for (Map.Entry<Integer, JSONArray> entry : groups.entrySet()) {
                OmniEngine.check(cancel);
                int number = entry.getKey(); JSONArray ranges = entry.getValue();
                String stem = id + "-" + Integer.toHexString(number);
                File full = new File(StudioFiles.dir(this, "audio"), stem + ".wav");
                File reference = new File(StudioFiles.dir(this, "audio"), stem + "-a.wav");
                long[] counts = SpeakerClips.export(reader, ranges, full, reference, cancel);
                long totalFrames = counts[0], referenceFrames = counts[1];
                results.put(new JSONObject().put("speaker", number).put("name", "人物 " + number)
                        .put("file", full.getName()).put("referenceFile", reference.getName())
                        .put("seconds", totalFrames / (double)reader.rate).put("referenceSeconds", referenceFrames / (double)reader.rate)
                        .put("canClone", referenceFrames >= reader.rate * 3L));
                state(job, "running", "已提取人物 " + number + " · " + results.length() + " / " + groups.size());
            }
        }
        OmniEngine.check(cancel);
        job.put("speakerResults", results).put("output", id + ".json");
        StudioFiles.writeText(StudioFiles.output(this, id, "json"), results.toString());
        if (sourceId.isEmpty()) {
            JSONArray segments = job.getJSONArray("segments");
            StudioFiles.writeText(StudioFiles.output(this, id, "txt"), SubtitleDocument.plain(segments));
            StudioFiles.writeText(StudioFiles.output(this, id, "srt"), SubtitleDocument.srt(segments));
        }
    }

    void record(JSONObject job, File output) throws Exception {
        int rate = 16000,
                size =
                        android.media.AudioRecord.getMinBufferSize(
                                rate,
                                android.media.AudioFormat.CHANNEL_IN_MONO,
                                android.media.AudioFormat.ENCODING_PCM_16BIT);
        if (size < 1) throw new IOException("手机不支持该录音格式");
        boolean bluetooth = getSharedPreferences("studio", 0).getBoolean("record-bluetooth", false);
        RecordRoute route = new RecordRoute(this, bluetooth);
        android.media.AudioRecord recorder = null;
        try (PcmAudio.Writer writer = new PcmAudio.Writer(output, rate)) {
            recorder =
                    new android.media.AudioRecord(
                            bluetooth
                                    ? android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION
                                    : android.media.MediaRecorder.AudioSource.MIC,
                            rate,
                            android.media.AudioFormat.CHANNEL_IN_MONO,
                            android.media.AudioFormat.ENCODING_PCM_16BIT,
                            Math.max(32000, size * 2));
            if (recorder.getState() != android.media.AudioRecord.STATE_INITIALIZED)
                throw new IOException("无法打开麦克风");
            foreground("正在录音 · 点击通知结束");
            recorder.startRecording();
            if (recorder.getRecordingState() != android.media.AudioRecord.RECORDSTATE_RECORDING)
                throw new IOException("麦克风没有开始录音，请检查系统权限或其他应用是否正在录音");
            recording = job.getString("id");
            state(job, "running", "正在录音 · 点击通知或悬浮按钮结束");
            short[] buffer = new short[1600];
            RecordingHealth health = new RecordingHealth(rate, SystemClock.elapsedRealtime());
            long last = -1;
            while (!stopMic.get() && !cancel.get()) {
                int n = recorder.read(buffer, 0, buffer.length, android.media.AudioRecord.READ_NON_BLOCKING);
                if (n < 0) throw new IOException("麦克风读取失败");
                long now = SystemClock.elapsedRealtime();
                if (n == 0) { health.checkInput(now); Thread.sleep(20); continue; }
                health.accept(buffer, n, now);
                float[] values = new float[n];
                for (int i = 0; i < n; i++) values[i] = buffer[i] / 32768f;
                writer.write(values);
                if (health.seconds() > last) {
                    last = health.seconds();
                    state(job, "running", "正在录音 · " + last + " 秒 · " + health.hint(now) + " · 点击结束录音");
                }
                if (health.limit(now)) break;
            }
            if (!health.enough()) throw new IOException("录音少于 1 秒，请重新录音");
        } finally {
            recording = "";
            microphone = false;
            if (recorder != null) {
                try {
                    recorder.stop();
                } catch (Exception ignored) {
                }
                recorder.release();
            }
            try {
                route.close();
            } catch (Exception ignored) {
            }
        }
    }

    void cancelEngines() {
        engine.cancel();
        fish.cancel();
        accelerated.cancel();
        gradio.cancel();
        if (whisper != null) whisper.cancel();
        if (translator != null) translator.cancel();
        if (speaker != null) speaker.cancel();
        if (separator != null) separator.cancel();
    }

    public void onDestroy() {
        cancel.set(true);
        stopMic.set(true);
        engine.cancel();
        fish.cancel();
        accelerated.cancel();
        gradio.cancel();
        if (whisper != null) whisper.cancel();
        if (translator != null) translator.cancel();
        if (speaker != null) speaker.cancel();
        if (separator != null) separator.cancel();
        worker.shutdown();
        super.onDestroy();
    }
}
