package com.voice.studio;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Static vector panels require no animation tasks or bitmap textures. */
final class CosmicPanelDrawable extends Drawable {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final RectF rect=new RectF(); final float d; final int accent;
    Shader fill;
    CosmicPanelDrawable(float density,int accent){d=density;this.accent=accent;}
    protected void onBoundsChange(Rect b){rect.set(b.left+d*.5f,b.top+d*.5f,b.right-d*.5f,b.bottom-d*.5f);fill=new LinearGradient(b.left,b.top,b.right,Math.max(b.top+1,b.bottom),new int[]{0xff1D2B50,0xff15223E,0xff221F46},null,Shader.TileMode.CLAMP);}
    public void draw(Canvas c){
        p.setStyle(Paint.Style.FILL);p.setShader(fill);p.setAlpha(255);c.drawRoundRect(rect,26*d,26*d,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(CosmicUi.LINE);c.drawRoundRect(rect,26*d,26*d,p);
        p.setColor(accent);p.setAlpha(115);p.setStrokeWidth(1.5f*d);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(rect.left+26*d,rect.top+d,rect.left+64*d,rect.top+d,p);
        p.setColor(CosmicUi.PINK);p.setAlpha(85);c.drawLine(rect.right-64*d,rect.bottom-d,rect.right-26*d,rect.bottom-d,p);
    }
    public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
