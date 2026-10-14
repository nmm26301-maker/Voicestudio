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
        round(c,x-u*.15f,y+u*.12f,x+u*.15f,y+u*.33f,u*.08f,0xffDDE8FF);
        oval(c,x-u*.25f,y+u*.14f,x-u*.13f,y+u*.29f,0xffB4CDF0);
        oval(c,x+u*.13f,y+u*.14f,x+u*.25f,y+u*.29f,0xffB4CDF0);
        round(c,x-u*.22f,y-u*.2f,x+u*.22f,y+u*.16f,u*.13f,0xffEDF3FF);
        round(c,x-u*.17f,y-u*.13f,x+u*.17f,y+u*.07f,u*.07f,0xff183656);
        oval(c,x-u*.115f,y-u*.068f,x-u*.055f,y-u*.005f,accent);
        oval(c,x+u*.055f,y-u*.068f,x+u*.115f,y-u*.005f,accent);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(u*.016f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(accent);
        rect.set(x-u*.045f,y-u*.012f,x+u*.045f,y+u*.032f);c.drawArc(rect,15,150,false,p);
        c.drawLine(x,y-u*.2f,x,y-u*.27f,p);p.setStyle(Paint.Style.FILL);p.setColor(CosmicUi.PINK);c.drawCircle(x,y-u*.28f,u*.025f,p);
        round(c,x-u*.065f,y+u*.195f,x+u*.065f,y+u*.23f,u*.015f,CosmicUi.PURPLE);
        float sx=x+u*.3f,sy=y+u*.025f;
        star.reset();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=u*(i%2==0?.13f:.065f),px=sx+(float)Math.cos(a)*r,py=sy+(float)Math.sin(a)*r;if(i==0)star.moveTo(px,py);else star.lineTo(px,py);}star.close();p.setColor(0xffFFE3A0);c.drawPath(star,p);
        oval(c,sx-u*.035f,sy-u*.017f,sx-u*.02f,sy,0xff72547E);oval(c,sx+u*.02f,sy-u*.017f,sx+u*.035f,sy,0xff72547E);
        c.restoreToCount(saved);
        removeCallbacks(tick);if(moving)postDelayed(tick,80);
    }
    protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;invalidate();}
    protected void onDetachedFromWindow(){attached=false;removeCallbacks(tick);super.onDetachedFromWindow();}
    protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);removeCallbacks(tick);if(v==VISIBLE)invalidate();}
}
