package com.voice.studio;
import java.io.*;
/** Minimal MessagePack writer for the official Fish inline-reference request. */
final class FishInlineMessage {
    static byte[] encode(String text,byte[] audio,String transcript,float speed)throws IOException{
        if(audio==null||audio.length<44||audio.length>5*1024*1024||transcript==null||transcript.trim().isEmpty()||transcript.length()>2000)
            throw new IOException("Fish 录音克隆需要有效的参考 WAV 和准确的录音原文");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeByte(0x84);
        string(out,"text");string(out,text);string(out,"format");string(out,"mp3");
        string(out,"prosody");out.writeByte(0x81);string(out,"speed");out.writeByte(0xca);out.writeFloat(speed);
        string(out,"references");out.writeByte(0x91);out.writeByte(0x82);
        string(out,"audio");out.writeByte(0xc6);out.writeInt(audio.length);out.write(audio);
        string(out,"text");string(out,transcript);out.flush();return bytes.toByteArray();
    }
    private static void string(DataOutputStream out,String value)throws IOException{
        byte[] data=value.getBytes("UTF-8");
        if(data.length<32)out.writeByte(0xa0|data.length);
        else if(data.length<256){out.writeByte(0xd9);out.writeByte(data.length);}
        else if(data.length<65536){out.writeByte(0xda);out.writeShort(data.length);}
        else{out.writeByte(0xdb);out.writeInt(data.length);}
        out.write(data);
    }
}
