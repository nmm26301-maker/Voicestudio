package com.voice.studio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** A native, low-allocation constellation and flowing violet progress ribbon. */
final class DreamProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final Path star = new Path();
    private boolean active, attached, foreground=true;
    void foreground(boolean value){foreground=value;if(value)invalidate();}
    private float fraction = -1, shown = -1;
    private long previous;
    private final float density;
    private Shader ribbon, halo;
    DreamProgressView(Context context) {
        super(context); density = getResources().getDisplayMetrics().density;
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void configure(boolean running, float value) {
        if (fraction != value) { if (value < 0 || shown < 0 || value < shown) shown = value; fraction = value; }
        active = running; invalidate();
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ribbon = new LinearGradient(0, 0, Math.max(1,w), 0,
                new int[]{0xff8279F5, 0xffC091EE, 0xff83C9FF, 0xffA79BFE}, null, Shader.TileMode.CLAMP);
        halo = new RadialGradient(w*.5f, h*.48f, Math.max(1,w*.52f),
                new int[]{0x60C9BAFF, 0x159ED8FF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(), h=getHeight(), d=density;
        long now=SystemClock.uptimeMillis();
        boolean animate=active && foreground && CosmicUi.isForeground(this) && CosmicUi.motionAllowed(getContext());
        float t=animate ? (now%100000L)/1000f : 0;
        if (fraction >= 0 && shown < fraction) {
            float elapsed=previous==0 ? .04f : Math.min(.1f,(now-previous)/1000f);
            shown=animate ? Math.min(fraction, shown+elapsed*1.8f) : fraction;
        }
        previous=now;
        paint.setShader(halo); paint.setAlpha(255); canvas.drawRect(0,0,w,h,paint);
        paint.setShader(null);
        for (int i=0;i<15;i++) {
            float x=w*((i*.618034f)%1), y=d*(7+(i*13)%32);
            y+=animate ? (float)Math.sin(t*.8+i)*d*3 : 0;
            paint.setColor(i%2==0 ? 0xffA599E7 : 0xff9BBDDF);
            paint.setAlpha((int)(95+65*(.5+.5*Math.sin(t*1.4+i))));
            float r=d*(i%4==0 ? 2.4f : 1.1f);
            if(i%4==0) {
                star.reset(); star.moveTo(x,y-r*2); star.lineTo(x+r*.45f,y-r*.45f);
                star.lineTo(x+r*2,y); star.lineTo(x+r*.45f,y+r*.45f);
                star.lineTo(x,y+r*2); star.lineTo(x-r*.45f,y+r*.45f);
                star.lineTo(x-r*2,y); star.lineTo(x-r*.45f,y-r*.45f); star.close();
                canvas.drawPath(star,paint);
            } else canvas.drawCircle(x,y,r,paint);
        }
        float y=h-d*16, left=d*3, right=w-d*3, width=Math.max(0,right-left);
        paint.setAlpha(255); paint.setColor(0xff27334F); track.set(left,y-d*4,right,y+d*4);
        canvas.drawRoundRect(track,d*4,d*4,paint);
        int saved=canvas.save(); canvas.clipRect(left,y-d*9,right,y+d*9);
        float a=left, b;
        if(fraction >= 0) b=left+width*Math.max(0,shown);
        else {
            float phase=animate ? (t*.32f)%1 : .5f;
            a=left+width*(phase*1.3f-.3f); b=a+width*.32f;
        }
        paint.setShader(ribbon); paint.setAlpha(70); track.set(a,y-d*7,b,y+d*7);
        canvas.drawRoundRect(track,d*8,d*8,paint);
        paint.setAlpha(255); track.set(a,y-d*4,b,y+d*4); canvas.drawRoundRect(track,d*4,d*4,paint);
        paint.setShader(null);
        if(animate && b>a) {
            float x=a+(b-a)*((t*.48f)%1);
            paint.setColor(0xB3FFFFFF); canvas.drawCircle(x,y,d*2.5f,paint);
        }
        canvas.restore();
        if(animate && attached && getWindowVisibility()==VISIBLE && isShown()) postInvalidateDelayed(50);
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); attached=true; previous=0; invalidate(); }
    @Override protected void onDetachedFromWindow() { attached=false; super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); if(visibility==VISIBLE) invalidate(); }
}
