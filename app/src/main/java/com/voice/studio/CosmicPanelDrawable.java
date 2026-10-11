package com.voice.studio;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Static vector panels require no animation tasks or bitmap textures. */
final class CosmicPanelDrawable extends Drawable {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final RectF rect=new RectF(); final float d; final int accent;
    Shader fill;
    CosmicPanelDrawable(float density,int accent){d=density;this.accent=accent;}
    protected void onBoundsChange(Rect b){rect.set(b.left+d*.5f,b.top+d*.5f,b.right-d*.5f,b.bottom-d*.5f);fill=new LinearGradient(b.left,b.top,b.right,Math.max(b.top+1,b.bottom),new int[]{0xff192641,0xff101A30,0xff111A2D},null,Shader.TileMode.CLAMP);}
    public void draw(Canvas c){
        p.setStyle(Paint.Style.FILL);p.setShader(fill);p.setAlpha(255);c.drawRoundRect(rect,22*d,22*d,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(0xff344563);c.drawRoundRect(rect,22*d,22*d,p);
        p.setColor(accent);p.setAlpha(125);p.setStrokeWidth(2*d);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(rect.right-42*d,rect.top+8*d,rect.right-22*d,rect.top+8*d,p);
        c.drawLine(rect.left+22*d,rect.bottom-8*d,rect.left+42*d,rect.bottom-8*d,p);
    }
    public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
