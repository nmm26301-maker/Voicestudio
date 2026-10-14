package com.voice.studio;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Host regression: freeware web clips merge without any model weights or network. */
public final class CloudWavMergeTest {
    static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    static File tone(File directory,String name,int rate,double seconds,boolean silent)throws Exception {
        File f=new File(directory,name);
        try(PcmAudio.Writer out=new PcmAudio.Writer(f,rate)) {
            for(int start=0;start<(int)Math.round(seconds*rate);start+=rate) {
                int n=Math.min(rate,(int)Math.round(seconds*rate)-start);
                float[] samples=new float[n];
                if(!silent)for(int i=0;i<n;i++)
                    samples[i]=.14f*(float)Math.sin(2*Math.PI*220*(start+i)/rate);
                out.write(samples);
            }
        }
        return f;
    }
    public static void main(String[] args)throws Exception {
        File dir=Files.createTempDirectory("vs-merge-test-").toFile();
        File a=tone(dir,"01.wav",16000,2,false), b=tone(dir,"02.wav",24000,3,false);
        File out=new File(dir,"joined.wav");
        List<String> events=new ArrayList<>();
        long millis=CloudWavMerge.merge(Arrays.asList(a,b),out,new AtomicBoolean(),events::add);
        check(out.isFile(),"joined WAV exists");
        check(events.size()==2,"real progress emitted for each clip");
        check(Math.abs(millis-5080)<200,"all five seconds preserved plus pause: "+millis);
        try(PcmAudio.Reader reader=new PcmAudio.Reader(out)){
            check(reader.rate==24000,"normalized output rate");
            check(reader.seconds()>5,"no dropped last clip");
            reader.validateSpeech(new AtomicBoolean());
        }
        File silent=tone(dir,"03.wav",16000,1,true);
        File reject=new File(dir,"reject.wav"); boolean failed=false;
        try{CloudWavMerge.merge(Arrays.asList(a,silent),reject,new AtomicBoolean(),s->{});}
        catch(IOException expected){failed=true;}
        check(failed && !reject.exists(),"reject silent segments without publishing output");
        File stopped=new File(dir,"cancelled.wav");failed=false;
        try{CloudWavMerge.merge(Arrays.asList(a,b),stopped,new AtomicBoolean(true),s->{});}
        catch(InterruptedIOException expected){failed=true;}
        check(failed && !stopped.exists(),"cancellation leaves no artifact");
        failed=false;
        try{CloudWavMerge.merge(Collections.singletonList(a),new File(dir,"one.wav"),new AtomicBoolean(),s->{});}
        catch(IOException expected){failed=true;}
        check(failed,"a single file is not a merge");
        List<String> names=new ArrayList<>(Arrays.asList("10.wav","2.wav","1.wav"));
        names.sort(CloudWavMerge::compareNames);
        check(names.equals(Arrays.asList("1.wav","2.wav","10.wav")),"natural numeric sequence");
        check(CloudWavMerge.compareNames("clip999999999999999999999999.wav","clip1000000000000000000000000.wav")<0,"no numeric overflow");
        check(CloudWavMerge.compareNames("01.wav","2.WAV")<0,"zero padding and case");
        check(CloudWavMerge.compareNames(null,"1.wav")<0,"missing name");
        File midCancel=new File(dir,"mid-cancel.wav");failed=false;
        AtomicBoolean stop=new AtomicBoolean();
        try{CloudWavMerge.merge(Arrays.asList(a,b),midCancel,stop,message->stop.set(true));}
        catch(InterruptedIOException expected){failed=true;}
        check(failed && !midCancel.exists(),"cancel during processing cleans temporary WAV");
        System.out.println("Cloud WAV merge: 10 host checks passed");
    }
}
