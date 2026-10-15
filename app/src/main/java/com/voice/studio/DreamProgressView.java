package com.voice.studio;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** A small progress track. Only real progress is shown as a percentage. */
final class DreamProgressView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track=new RectF();
    private boolean active,attached,foreground=true;
    private float fraction=-1,shown=-1;
    private long previous;
    private final float density;
    DreamProgressView(Context c){super(c);density=getResources().getDisplayMetrics().density;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void foreground(boolean value){foreground=value;previous=0;invalidate();}
    void configure(boolean running,float value){
        float bounded=value<0?-1:Math.max(0,Math.min(1,value));
        if(fraction!=bounded){if(bounded<0||shown<0||bounded<shown)shown=bounded;fraction=bounded;}
        if(active!=running)previous=0;active=running;invalidate();
    }
    protected void onDraw(Canvas canvas){
        float left=0,right=getWidth(),width=right-left,y=getHeight()*.5f,r=2*density;
        boolean animate=active && attached && foreground && CosmicUi.isForeground(this) && CosmicUi.motionAllowed(getContext())
            && isShown() && getWindowVisibility()==VISIBLE;
        long now=SystemClock.uptimeMillis();
        if(fraction>=0&&shown<fraction){
            float dt=previous==0?.04f:Math.min(.1f,(now-previous)/1000f);
            shown=animate?Math.min(fraction,shown+dt*1.8f):fraction;
        }
        previous=now;paint.setColor(CosmicUi.LINE);track.set(left,y-r,right,y+r);canvas.drawRoundRect(track,r,r,paint);
        paint.setColor(CosmicUi.PURPLE);
        int saved=canvas.save();canvas.clipRect(left,y-r,right,y+r);
        if(fraction>=0){track.set(left,y-r,left+width*Math.max(0,shown),y+r);canvas.drawRoundRect(track,r,r,paint);}
        else if(active){
            // A moving segment signals waiting, never estimated completion.
            float phase=animate?(now%1800L)/1800f:.5f;
            float x=left+width*(phase*1.28f-.28f);track.set(x,y-r,x+width*.28f,y+r);canvas.drawRoundRect(track,r,r,paint);
        }
        canvas.restoreToCount(saved);
        if(animate&&(fraction<0||shown<fraction))postInvalidateDelayed(50);
    }
    protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;previous=0;invalidate();}
    protected void onDetachedFromWindow(){attached=false;super.onDetachedFromWindow();}
    protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);previous=0;if(visibility==VISIBLE)invalidate();}
}
