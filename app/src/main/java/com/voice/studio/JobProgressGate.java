package com.voice.studio;

import java.util.regex.*;

/** Successful progress persistence only; lifecycle transitions are never rate limited. */
final class JobProgressGate {
    static final long INTERVAL_NANOS=250000000L;
    private static final Pattern STEPS=Pattern.compile("^(.*本机合成 · )([0-9]+) / ([0-9]+) 步$");
    private String id="",state="",message="";
    private long writtenAt;
    synchronized boolean shouldWrite(String nextId,String nextState,String nextMessage,long now) {
        if(!"running".equals(nextState)||!nextId.equals(id)||!nextState.equals(state))return true;
        if(nextMessage.equals(message))return false;
        Matcher current=STEPS.matcher(nextMessage),previous=STEPS.matcher(message);
        if(!current.matches()||!previous.matches()||!current.group(1).equals(previous.group(1)))return true;
        try {
            int done=Integer.parseInt(current.group(2)),total=Integer.parseInt(current.group(3));
            if(done==0||total<=0||done>=total)return true;
        }catch(NumberFormatException error){return true;}
        return now<writtenAt||now-writtenAt>=INTERVAL_NANOS;
    }
    synchronized void written(String nextId,String nextState,String nextMessage,long now) {
        id=nextId;state=nextState;message=nextMessage;writtenAt=now;
    }
}
