package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class AudioChecksTest {
    static int assertions;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static File fixture(Path root,String name,byte[] prefix,int size)throws Exception{byte[] b=new byte[size];System.arraycopy(prefix,0,b,0,Math.min(prefix.length,size));File file=root.resolve(name).toFile();Files.write(file.toPath(),b);return file;}
    static void rejects(File file,String format)throws Exception{boolean rejected=false;try{AudioChecks.validate(file,format);}catch(IOException expected){rejected=true;}check(rejected,"Should reject "+file.getName());}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("voicestudio-checks-");
        File wav=fixture(root,"valid.wav",new byte[]{'R','I','F','F',36,0,0,0,'W','A','V','E'},44);
        AudioChecks.validate(wav,"wav");assertions++;
        File mp3=fixture(root,"valid.mp3",new byte[]{(byte)255,(byte)251,(byte)144,0},64);
        AudioChecks.validate(mp3,"mp3");assertions++;
        AudioChecks.validate(fixture(root,"tagged.mp3",new byte[]{'I','D','3',4,0,0},64),"mp3");assertions++;
        rejects(fixture(root,"error.mp3","<html>service error</html>".getBytes("UTF-8"),80),"mp3");
        rejects(fixture(root,"json.mp3","{\"error\":\"invalid token\"}".getBytes("UTF-8"),80),"mp3");
        rejects(fixture(root,"short.wav",new byte[]{'R','I','F','F'},8),"wav");
        rejects(fixture(root,"invalid-frame.mp3",new byte[]{(byte)255,(byte)251,(byte)240,0},64),"mp3");
        rejects(mp3,"wav");rejects(wav,"mp3");
        rejects(root.resolve("missing.mp3").toFile(),"mp3");
        String emojis=String.join("",Collections.nCopies(40,"🎙"));String title=AudioChecks.shortTitle(emojis);
        check(title.codePointCount(0,title.length())==27,"Title cuts by code points");
        check(!Character.isHighSurrogate(title.charAt(title.length()-2)),"No split surrogate");
        check(AudioChecks.shortTitle("  hello\n  world  ").equals("hello world"),"Whitespace normalized");
        check(!AudioChecks.exportName("a/b\\c:d*e?f\"g<h>i|j\n").matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*"),"Safe export name");
        check(AudioChecks.exportName("  ").equals("VoiceStudio"),"Empty name fallback");
        check(AudioChecks.exportName(emojis+emojis).codePointCount(0,AudioChecks.exportName(emojis+emojis).length())==60,"Export length bounded by code points");
        check(AudioChecks.localeMatches(Locale.forLanguageTag("zh-CN"),Locale.forLanguageTag("zh-CN")),"Exact locale");
        check(!AudioChecks.localeMatches(Locale.forLanguageTag("zh-HK"),Locale.forLanguageTag("zh-CN")),"Avoid mismatched regions");
        check(!AudioChecks.localeMatches(Locale.ENGLISH,Locale.CHINESE),"Avoid mismatched languages");
        check(AudioChecks.localeMatches(Locale.US,Locale.ENGLISH),"Language-only selection");
        System.out.println("AudioChecks: "+assertions+" assertions passed; decoder/device checks separate.");
    }
}
