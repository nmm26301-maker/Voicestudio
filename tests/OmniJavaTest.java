package com.voice.studio;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
public class OmniJavaTest {
 public static void main(String[] a)throws Exception{
 File dir=new File(a[0]);String text=a.length>1?a[1]:"你好，欢迎使用语音工作室。";
 if(a.length>2&&a[2].equals("tokens")){System.out.println(Arrays.toString(new OmniTokenizer(new File(dir,"tokenizer.json")).encode(text)));return;}
 long start=System.currentTimeMillis();new OmniEngine().generate(dir,text,a.length>2?a[2]:"zh",1,new File("experiments/omni-java.wav"),new AtomicBoolean(false),System.out::println);
 System.out.println("DONE ms="+(System.currentTimeMillis()-start));
 }
}
