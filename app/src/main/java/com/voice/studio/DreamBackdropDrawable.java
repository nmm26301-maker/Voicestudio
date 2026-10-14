package com.voice.studio;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Quiet star field; cached shaders, no timer, no interaction or audio state. */
final class DreamBackdropDrawable extends Drawable {
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final float density;
    Shader sky,aurora;
    DreamBackdropDrawable(float density){this.density=density;}
    protected void onBoundsChange(Rect b){
        sky=new LinearGradient(0,b.top,b.right,b.bottom,new int[]{CosmicUi.BG,0xff101636,0xff131C40},null,Shader.TileMode.CLAMP);
        aurora=new RadialGradient(b.right*.88f,b.height()*.09f,Math.max(1,b.width()*.85f),new int[]{0x503D5DB2,0x202D427F,0x002D427F},null,Shader.TileMode.CLAMP);
    }
    public void draw(Canvas canvas){
        Rect b=getBounds();paint.setStyle(Paint.Style.FILL);paint.setShader(sky);canvas.drawRect(b,paint);
        paint.setShader(aurora);canvas.drawRect(b,paint);paint.setShader(null);
        for(int i=0;i<26;i++){
            float x=b.left+((i*137+53)%997)/997f*b.width(),y=b.top+((i*199+31)%991)/991f*b.height();
            paint.setColor(i%3==0?0x60B8A1E9:0x408BEEFF);canvas.drawCircle(x,y,(i%4==0?1.1f:.6f)*density,paint);
        }
    }
    public void setAlpha(int alpha){}public void setColorFilter(ColorFilter filter){}public int getOpacity(){return PixelFormat.OPAQUE;}
}
