package com.voice.studio;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** Decorative orbit: never represented as a live microphone or audio meter. */
final class SignalOrbView extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF rect=new RectF();final int accent;final boolean ambient;
    boolean attached,foreground=true,busy;Shader glow;
    final Runnable tick=()->invalidate();
    SignalOrbView(Context c,int accent,boolean ambient){super(c);this.accent=accent;this.ambient=ambient;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void foreground(boolean v){foreground=v;removeCallbacks(tick);if(v)invalidate();}
    void busy(boolean v){if(busy!=v){busy=v;removeCallbacks(tick);invalidate();}}
    protected void onSizeChanged(int w,int h,int ow,int oh){glow=new RadialGradient(w*.5f,h*.5f,Math.max(1,Math.min(w,h)*.48f),new int[]{0x604264A1,0x26344A7B,0x002A3656},null,Shader.TileMode.CLAMP);}
    protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),x=w*.5f,y=h*.5f,r=Math.min(w,h)*.36f;
        boolean moving=ambient && attached && foreground && !busy && CosmicUi.isForeground(this) && getWindowVisibility()==VISIBLE && isShown() && CosmicUi.ambientAllowed(getContext());
        float phase=moving?(SystemClock.uptimeMillis()%120000L)/120000f:0;
        p.setStyle(Paint.Style.FILL);p.setShader(glow);c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.012f));p.setColor(0x55445777);c.drawCircle(x,y,r,p);
        rect.set(x-r,y-r,x+r,y+r);p.setColor(accent);c.drawArc(rect,phase*360-70,82,false,p);
        int save=c.save();c.rotate(-28,x,y);rect.set(x-r*1.13f,y-r*.52f,x+r*1.13f,y+r*.52f);p.setColor(0x806576B7);c.drawOval(rect,p);c.restoreToCount(save);
        p.setStyle(Paint.Style.FILL);p.setColor(accent);double angle=Math.toRadians(phase*360-70);c.drawCircle(x+(float)Math.cos(angle)*r,y+(float)Math.sin(angle)*r,w*.025f,p);
        for(int i=0;i<7;i++){float bx=x+(i-3)*w*.075f,bh=h*(.1f+.18f*(.5f+.5f*(float)Math.sin(i*.9f+phase*40)));p.setColor(i%2==0?accent:CosmicUi.PURPLE);rect.set(bx-w*.013f,y-bh*.5f,bx+w*.013f,y+bh*.5f);c.drawRoundRect(rect,w*.013f,w*.013f,p);}
        removeCallbacks(tick);if(moving)postDelayed(tick,80);
    }
    protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;invalidate();}
    protected void onDetachedFromWindow(){attached=false;removeCallbacks(tick);super.onDetachedFromWindow();}
    protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);removeCallbacks(tick);if(v==VISIBLE)invalidate();}
}
