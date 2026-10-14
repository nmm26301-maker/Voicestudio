package com.voice.studio;

final class CloudEngineCatalog {
    static final String[] IDS={"auto","indextts25","cosyvoice3","qwen3tts","chatterboxv3","elevenlabs","minimax","fish"};
    static final String[] LABELS={"自动选择","IndexTTS 2.5","CosyVoice 3","Qwen3-TTS","Chatterbox V3","ElevenLabs","MiniMax","Fish Audio"};
    static String label(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return LABELS[i];return "云端服务";}
    static boolean known(String id){for(String value:IDS)if(value.equals(id))return !id.equals("auto");return false;}
}
