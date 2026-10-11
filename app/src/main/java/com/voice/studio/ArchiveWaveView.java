package com.voice.studio;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Actual sampled WAV envelope, or a plain progress track for compressed audio. */
final class ArchiveWaveView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r=new RectF();
    private float[] samples;
    private float progress;
    private boolean playing;
    ArchiveWaveView(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    void samples(float[] value) { samples=value; invalidate(); }
    void progress(float value, boolean active) {
        if(progress!=value || playing!=active) { progress=Math.max(0,Math.min(1,value)); playing=active; invalidate(); }
    }
    @Override protected void onDraw(Canvas canvas) {
        float w=getWidth(),h=getHeight(), d=getResources().getDisplayMetrics().density, center=h*.5f;
        p.setColor(0xff24304A); p.setStrokeWidth(d);
        canvas.drawLine(0,center,w,center,p);
        if(samples!=null) {
            float step=w/samples.length;
            for(int i=0;i<samples.length;i++) {
                float height=Math.max(d*2,(float)Math.sqrt(samples[i])*h*.7f);
                p.setColor((i+.5f)/samples.length<=progress ? 0xff67E8F9 : 0xff595E9B);
                r.set(step*i+step*.24f,center-height*.5f,step*(i+1)-step*.24f,center+height*.5f);
                canvas.drawRoundRect(r,2*d,2*d,p);
            }
        } else {
            p.setColor(0xff59678A); p.setStrokeWidth(d*3); canvas.drawLine(0,center,w,center,p);
            p.setColor(0xff67E8F9); canvas.drawLine(0,center,w*progress,center,p);
            for(int i=0;i<=8;i++) { p.setColor(0xff8091B5); canvas.drawCircle(w*i/8,center,d*2,p); }
        }
        if(progress>0) {
            float x=progress*w; p.setColor(playing?0x3067E8F9:0x2067E8F9); canvas.drawCircle(x,center,d*12,p);
            p.setColor(0xffD8FFFF); canvas.drawCircle(x,center,d*3,p);
        }
    }
}
