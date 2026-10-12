package com.voice.studio;

import org.json.*;

/** A genuine GPU batch task; Kaggle does not supply a permanent speech HTTP endpoint. */
final class KaggleTask {
    static final String REV="08be0b4ccbac3e13e374e86fbfead4b4cac343e2";
    static JSONObject notebook(JSONObject request) throws Exception {
        AcceleratedSpeechClient.request(request.getString("input"),"","auto",request.optString("language"),
                (float)request.optDouble("speed",1),request.optInt("num_step",32),request.optLong("seed",42));
        JSONObject task=new JSONObject().put("input",request.getString("input")).put("language",request.optString("language","zh"))
                .put("speed",request.optDouble("speed",1)).put("num_step",request.optInt("num_step",32)).put("seed",request.optLong("seed",42));
        JSONArray cells=new JSONArray();
        cells.put(new JSONObject().put("cell_type","markdown").put("metadata",new JSONObject()).put("source",new JSONArray().put(
                "# VoiceStudio 免费 GPU 合成\n在 Kaggle 设置中开启 GPU 和 Internet，然后 Run All。下载 /kaggle/working/VoiceStudio-Kaggle.wav 并在 VoiceStudio 导入。额度以账号页面为准；本文件含完整待合成文字，不含密钥或录音。\n")));
        cells.put(code("%pip install -q git+https://github.com/k2-fsa/OmniVoice.git@"+REV+" soundfile\n"));
        String source="import json, torch, soundfile as sf\nfrom omnivoice import OmniVoice, OmniVoiceGenerationConfig\n"+
                "task = json.loads("+JSONObject.quote(task.toString())+")\n"+
                "if not torch.cuda.is_available():\n    raise RuntimeError('请在 Kaggle 设置开启 GPU，检查账号验证与剩余额度')\n"+
                "torch.manual_seed(task['seed'])\nmodel = OmniVoice.from_pretrained('k2-fsa/OmniVoice', device_map='cuda', dtype=torch.float16)\n"+
                "audio = model.generate(text=task['input'], language=task['language'] or None, speed=task['speed'], generation_config=OmniVoiceGenerationConfig(num_step=task['num_step']))\n"+
                "sf.write('/kaggle/working/VoiceStudio-Kaggle.wav', audio[0], model.sampling_rate, subtype='PCM_16')\n"+
                "print('完整任务已生成：VoiceStudio-Kaggle.wav', len(audio[0]) / model.sampling_rate, '秒')\n";
        cells.put(code(source));
        JSONObject metadata=new JSONObject().put("kernelspec",new JSONObject().put("display_name","Python 3").put("language","python").put("name","python3"))
                .put("language_info",new JSONObject().put("name","python"))
                .put("kaggle",new JSONObject().put("isGpuEnabled",true).put("isInternetEnabled",true));
        return new JSONObject().put("nbformat",4).put("nbformat_minor",5).put("metadata",metadata).put("cells",cells);
    }
    private static JSONObject code(String source) throws Exception {
        return new JSONObject().put("cell_type","code").put("execution_count",JSONObject.NULL).put("metadata",new JSONObject())
                .put("outputs",new JSONArray()).put("source",new JSONArray().put(source));
    }
}
