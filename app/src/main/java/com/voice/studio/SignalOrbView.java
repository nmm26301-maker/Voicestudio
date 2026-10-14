package com.voice.studio;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** Decorative cloud companion, never a microphone meter or synthesis progress. */
final class SignalOrbView extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF rect=new RectF();final Path star=new Path();final int accent;final boolean ambient;
    boolean attached,foreground=true,busy;Shader glow;
    final Runnable tick=()->invalidate();
    SignalOrbView(Context c,int accent,boolean ambient){super(c);this.accent=accent;this.ambient=ambient;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void foreground(boolean v){foreground=v;removeCallbacks(tick);if(v)invalidate();}
    void busy(boolean v){if(busy!=v){busy=v;removeCallbacks(tick);invalidate();}}
    protected void onSizeChanged(int w,int h,int ow,int oh){glow=new RadialGradient(w*.5f,h*.5f,Math.max(1,Math.min(w,h)*.5f),new int[]{0x505A69AC,0x18424A84,0x002A3656},null,Shader.TileMode.CLAMP);}
    void oval(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);rect.set(l,t,r,b);c.drawOval(rect,p);}
    void round(Canvas c,float l,float t,float r,float b,float radius,int color){p.setColor(color);rect.set(l,t,r,b);c.drawRoundRect(rect,radius,radius,p);}
    protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),u=Math.min(w,h),x=w*.46f,y=h*.51f;
        boolean moving=ambient && attached && foreground && !busy && CosmicUi.isForeground(this) && getWindowVisibility()==VISIBLE && isShown() && CosmicUi.ambientAllowed(getContext());
        float phase=moving?(SystemClock.uptimeMillis()%8000L)/8000f:0;
        p.setStyle(Paint.Style.FILL);p.setShader(glow);c.drawRect(0,0,w,h,p);p.setShader(null);
        oval(c,x-u*.29f,h*.83f,x+u*.31f,h*.9f,0x444A71A6);
        int saved=c.save();c.translate(0,moving?(float)Math.sin(phase*Math.PI*2)*u*.018f:0);
        // A cloud wearing headphones; decorative only, never a microphone meter.
        c.translate(w*.5f,h*.5f);c.scale(u/100f,u/100f);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);p.setColor(0xff8CA7FF);
        rect.set(-32,-36,32,34);c.drawArc(rect,185,170,false,p);
        Path cloud=new Path();cloud.moveTo(-31,25);
        cloud.cubicTo(-46,25,-47,3,-34,0);cloud.cubicTo(-41,-17,-19,-27,-8,-17);
        cloud.cubicTo(-3,-43,33,-35,29,-12);cloud.cubicTo(48,-12,50,12,39,16);
        cloud.cubicTo(40,27,29,30,17,27);cloud.lineTo(-31,25);cloud.close();
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(-25,-25,29,29,
            new int[]{0xffA4F1FF,0xffB6C8FF,0xffEBC2FF},null,Shader.TileMode.CLAMP));
        c.drawPath(cloud,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(0xffE1F4FF);c.drawPath(cloud,p);
        p.setStyle(Paint.Style.FILL);
        round(c,-38,-6,-28,18,5,0xff799EFF);round(c,31,-7,42,18,5,0xffD0A0F8);
        round(c,-35,-3,-31,15,2,0xffB3D9FF);round(c,35,-4,39,15,2,0xffEDD8FF);
        oval(c,-22,8,-13,12,0x66FF8CD0);oval(c,12,8,21,12,0x66FF8CD0);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff344C83);
        rect.set(-20,1,-11,9);c.drawArc(rect,5,170,false,p);
        rect.set(10,1,19,9);c.drawArc(rect,5,170,false,p);
        rect.set(-4,8,5,15);c.drawArc(rect,5,170,false,p);
        p.setColor(accent);p.setStrokeWidth(1.5f);
        c.drawLine(-43,-24,-37,-24,p);c.drawLine(-40,-27,-40,-21,p);
        c.drawLine(34,-27,42,-27,p);c.drawLine(38,-31,38,-23,p);
        c.restoreToCount(saved);
        removeCallbacks(tick);if(moving)postDelayed(tick,80);
    }
    protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;invalidate();}
    protected void onDetachedFromWindow(){attached=false;removeCallbacks(tick);super.onDetachedFromWindow();}
    protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);removeCallbacks(tick);if(v==VISIBLE)invalidate();}
}
