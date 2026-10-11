package com.voice.studio;

import ai.onnxruntime.*;
import java.nio.*;
import java.util.*;

public class OmniTensorRunTest {
    static int checks;
    static void check(boolean ok,String label) { checks++;if(!ok)throw new AssertionError(label); }
    public static void main(String[] args)throws Exception {
        OrtEnvironment env=OrtEnvironment.getEnvironment();env.setTelemetry(false);
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions();OrtSession session=env.createSession(args[0],options)) {
            for(int s:new int[]{2,7,32}) {
                long[] ids=new long[16*s],att=new long[2*s],pos=new long[2*s];byte[] mask=new byte[2*s];
                Arrays.fill(att,1);Arrays.fill(mask,(byte)1);for(int i=0;i<2*s;i++)pos[i]=i%s;
                OmniTensorRun tensors=new OmniTensorRun(env,s,ids,mask,att,pos);
                try(OrtSession.RunOptions run=new OrtSession.RunOptions()) {
                    for(int step=0;step<40;step++) {
                        for(int i=0;i<ids.length;i++)ids[i]=(step%2==0?1:-1)*(step*1000+i);
                        FloatBuffer output=tensors.run(session,run,ids);
                        check(output.isReadOnly(),"read-only scoring view");check(output.remaining()==ids.length,"learned actual output shape");
                        for(int i=0;i<ids.length;i++)check(output.get(i)==ids[i],"updated direct inputs / pinned output exact");
                    }
                    run.setTerminate(true);boolean stopped=false;try { tensors.run(session,run,ids); }catch(OrtException e) { stopped=true; }
                    check(stopped,"cancellation forwarded to native runtime");run.setTerminate(false);
                    check(tensors.run(session,run,ids).get(0)==ids[0],"buffers usable after cancelled run");
                } finally { tensors.close();tensors.close(); }
                try(OrtSession.RunOptions run=new OrtSession.RunOptions()) {
                    boolean closed=false;try { tensors.run(session,run,ids); }catch(IllegalStateException e) { closed=true; }
                    check(closed,"closed buffers reject run");
                }
            }
        }
        System.out.println("Native ONNX Runtime 1.30 buffer reuse: "+checks+" checks passed. Authored graph; full OmniVoice and phone tests separate.");
    }
}
