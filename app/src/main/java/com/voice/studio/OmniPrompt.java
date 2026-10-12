package com.voice.studio;

/** Upstream _combine_text and _prepare_inference_inputs framing (k2-fsa OmniVoice). */
final class OmniPrompt {
    static String combine(String text, String reference) {
        String value = (reference == null || reference.trim().isEmpty() ? "" : reference.trim()+" ")+text.trim();
        value=value.replaceAll("[\\r\\n]+", "").replace('\uff08','(').replace('\uff09',')').replaceAll("[ \\t]+", " ");
        return value.replaceAll("(?<=[\\u4e00-\\u9fff])\\s+|\\s+(?=[\\u4e00-\\u9fff])", "");
    }
    static String build(String text,String language,String instruct,VoicePrompt ref) {
        String lang=language==null || language.trim().isEmpty()?"None":language;
        String style=instruct==null || instruct.trim().isEmpty()?"None":instruct;
        return (ref==null?"":"<|denoise|>")+"<|lang_start|>"+lang+"<|lang_end|><|instruct_start|>"+style+
                "<|instruct_end|><|text_start|>"+combine(text,ref==null?null:ref.text)+"<|text_end|>";
    }
}
