package com.voice.studio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class TokenizerProgressTest {
    static int checks;
    static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    static List<String> chunks(Pattern p, String s) {
        List<String> out = new ArrayList<>(); Matcher m=p.matcher(s); int end=0;
        while(m.find()) { check(m.start()==end,"no text skipped"); out.add(m.group()); end=m.end(); }
        check(end==s.length(),"all text retained"); return out;
    }
    public static void main(String[] args) throws Exception {
        Pattern android = OmniWordPattern.compile();
        if (args.length > 0) Files.write(new File(args[0]).toPath(), android.pattern().getBytes(StandardCharsets.UTF_8));
        Pattern reference = Pattern.compile("(?i:'s|'t|'re|'ve|'m|'ll|'d)|[^\\r\\n\\p{L}\\p{N}]?\\p{L}+|\\p{N}| ?[^\\s\\p{L}\\p{N}]+[\\r\\n]*|\\s*[\\r\\n]+|\\s+(?!\\S)|\\s+", Pattern.UNICODE_CHARACTER_CLASS);
        check((android.flags() & 256)==0,"Android flag 256 absent");
        String[] cases={"你好，世界！123", "We're ready. I'M here, she'll go.", "مرحبا بالعالم ١٢٣", "カタカナ 日本語", "Café e\u0301 straße", "🌙✨ 𠀀 𝟙𝟚", "\r\n \t\nhello\n", "a\u00A0b\u0085c\u2003d\u2028e\u3000", "'ſ 'İ 'ı", "", "\u0000\u001C\uFEFF"};
        List<String> nativeTexts = new ArrayList<>(Arrays.asList(cases));
        for(String s:cases) check(chunks(android,s).equals(chunks(reference,s)),"reference split: "+s);
        int[] spaces={9,10,11,12,13,32,0x85,0xA0,0x1680,0x2000,0x2001,0x2002,0x2003,0x2004,0x2005,0x2006,0x2007,0x2008,0x2009,0x200A,0x2028,0x2029,0x202F,0x205F,0x3000};
        for(int cp:spaces) {
            String space=new String(Character.toChars(cp));
            for(String s:new String[]{space+space,"a"+space+"!",space+"\n ","你好"+space+"world"}) {
                nativeTexts.add(s);
                check(chunks(android,s).equals(chunks(reference,s)),"Unicode whitespace "+cp);
            }
        }
        Random random=new Random(139);
        String[] pieces={"你","界","Hello","!","'s","١","🌙","𠀀","\r","\n","\u00A0","\u0085","\u2009","\u2029","é","Ж","𝟙"};
        for(int n=0;n<100;n++) {
            StringBuilder s=new StringBuilder(); for(int i=0;i<80;i++)s.append(pieces[random.nextInt(pieces.length)]);
            check(chunks(android,s.toString()).equals(chunks(reference,s.toString())),"random multilingual split");
            nativeTexts.add(s.toString());
        }
        if (args.length > 1) {
            JSONArray fixtures = new JSONArray();
            for(String s:nativeTexts) fixtures.put(new JSONObject().put("text",s).put("expected",new JSONArray(chunks(reference,s))));
            Files.write(new File(args[1]).toPath(),fixtures.toString().getBytes(StandardCharsets.UTF_8));
        }
        JSONObject vocab=new JSONObject(); int extra=256;
        for(int i=0;i<256;i++) { int cp=((i>=33&&i<=126)||(i>=161&&i<=172)||i>=174)?i:extra++; vocab.put(new String(Character.toChars(cp)),i); }
        vocab.put("Hi",256);
        JSONObject fixture=new JSONObject().put("model",new JSONObject().put("vocab",vocab).put("merges",new JSONArray().put(new JSONArray().put("H").put("i"))))
                .put("added_tokens",new JSONArray().put(new JSONObject().put("content","<|text_start|>").put("id",1000)).put(new JSONObject().put("content","<|text_end|>").put("id",1001)));
        File file=File.createTempFile("tokenizer139",".json");
        try {
            Files.write(file.toPath(),fixture.toString().getBytes(StandardCharsets.UTF_8));
            OmniTokenizer tokenizer=new OmniTokenizer(file);
            OmniTokenizerCache cache=new OmniTokenizerCache();
            OmniTokenizer first=cache.get(file);
            check(first==cache.get(file),"warm tokenizer reused");
            cache.clear(); check(first!=cache.get(file),"low-memory cache released");
            first=cache.get(file);
            Files.write(file.toPath(),(fixture.toString()+" ").getBytes(StandardCharsets.UTF_8));
            check(first!=cache.get(file),"changed tokenizer file reloaded");
            check(Arrays.equals(tokenizer.encode("<|text_start|>Hi<|text_end|>"),new long[]{1000,256,1001}),"BPE and model special tokens");
            for(String text:new String[]{"你好🌙\u00A0مرحبا","e\u0301\u202F世界","H i\n123"}) {
                byte[] raw=java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFC).getBytes(StandardCharsets.UTF_8);
                long[] expected=new long[raw.length]; for(int i=0;i<raw.length;i++)expected[i]=raw[i]&255;
                check(Arrays.equals(tokenizer.encode(text),expected),"full tokenizer Unicode byte preservation");
            }
        } finally { file.delete(); }
        check(SynthesisProgress.fraction("第 2 / 4 段 · 本机合成 · 8 / 32 步")==.25f,"real step fraction");
        check(SynthesisProgress.fraction("本机合成 · 16 / 16 步")==1,"final inference step");
        for(String stage:new String[]{"加载本机 OmniVoice 模型…","第 2 / 4 段 · 本机解码音频…","Fish 正在生成并接收音频…","本机合成 · 33 / 32 步","本机合成 · 1 / 0 步","本机合成 · 999999999999 / 32 步"})
            check(SynthesisProgress.fraction(stage)==-1,"indeterminate or invalid "+stage);
        check(SynthesisProgress.fraction("已保存 1 / 4 段 · 第 2 段 · 排队中")==.25f,"durable remote fraction");
        check(SynthesisProgress.fraction("已保存 0 / 4 段 · 下载重试")==0,"queued chunk does not count as saved");
        check(SynthesisProgress.fraction("已保存 4 / 4 段 · 正在合并完整音频")==1,"all chunks saved, final merge still pending");
        check(SynthesisProgress.label("已保存 1 / 4 段").equals("分段已保存 "),"remote label is not inference percent");
        for(String s:new String[]{"已保存 9 / 4 段","已保存 1 / 0 段","已保存 99999999999 / 4 段"})check(SynthesisProgress.fraction(s)==-1,"invalid remote counter");
        check(SynthesisProgress.active("queued")&&SynthesisProgress.active("running"),"queued/running busy");
        for(String state:new String[]{"done","failed","paused","idle"})check(!SynthesisProgress.active(state),"terminal releases UI "+state);
        System.out.println("Tokenizer / actual progress: "+checks+" checks passed (host JVM, not device inference)");
    }
}
