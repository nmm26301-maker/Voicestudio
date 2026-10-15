package com.voice.studio;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Static, low contrast wash. No star field, callbacks or bitmap assets. */
final class DreamBackdropDrawable extends Drawable {
    final Paint paint=new Paint();Shader wash;
    DreamBackdropDrawable(float density){}
    protected void onBoundsChange(Rect b){
        wash=new LinearGradient(0,b.top,0,Math.max(1,b.height()),
            new int[]{0xff191A29,CosmicUi.BG,CosmicUi.BG},new float[]{0,.32f,1},Shader.TileMode.CLAMP);
    }
    public void draw(Canvas canvas){paint.setShader(wash);canvas.drawRect(getBounds(),paint);}
    public void setAlpha(int alpha){}public void setColorFilter(ColorFilter filter){}public int getOpacity(){return PixelFormat.OPAQUE;}
}
