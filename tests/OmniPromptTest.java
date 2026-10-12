package com.voice.studio;
public class OmniPromptTest {
 static int checks;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception {
  VoicePrompt ref=new VoicePrompt("你好，欢迎来到声音工作室。",new long[8*100],100,.1);
  String plain=OmniPrompt.build(" 你好 \n世界（测试） ","zh","None",null);
  check(plain.equals("<|lang_start|>zh<|lang_end|><|instruct_start|>None<|instruct_end|><|text_start|>你好世界(测试)<|text_end|>"),"upstream normalization and framing");
  check(!plain.startsWith("<|denoise|>"),"no reference means no denoise token");
  check(OmniPrompt.build("今天测试。","zh",null,ref).startsWith("<|denoise|><|lang_start|>zh"),"reference prompt uses upstream denoise marker");
  check(OmniPrompt.combine(" world\r\n!"," Hello   ").equals("Hello world!"),"space and newline rules");
  for(double estimate:new double[]{.001,.2,1,5,25,49,50,100,250}) {
   double expected=estimate<50?50*Math.pow(estimate/50,1.0/3):estimate;
   check(OmniDuration.boost(estimate)==expected,"independent upstream boost formula");
  }
  for(String text:new String[]{"你","你好。","今天测试短句。","这是一个比较长的句子，用于验证参考音色的时长估计。"})for(float speed:new float[]{.5f,1,2}) {
   double raw=ref.frames*OmniDuration.total(text)/OmniDuration.total(ref.text);
   double expected=(raw<50?50*Math.pow(raw/50,1.0/3):raw)/speed;
   check(OmniSegments.duration(OmniDuration.total(text),speed,ref)==expected,"reference-backed target duration matches upstream");
   check(Math.abs(OmniSegments.estimate(OmniDuration.total(text),speed,ref.frames/OmniDuration.total(ref.text))-expected)<1e-10,"splitter and target duration agree");
  }
  double old=ref.frames*OmniDuration.total("你")/OmniDuration.total(ref.text);
  check(OmniSegments.duration(OmniDuration.total("你"),1,ref)>old*2,"short reference-backed utterance no longer uses raw clipped duration");
  System.out.println("Omni upstream prompt/duration: "+checks+" checks passed; semantic speech quality requires real weights/device.");
 }
}
