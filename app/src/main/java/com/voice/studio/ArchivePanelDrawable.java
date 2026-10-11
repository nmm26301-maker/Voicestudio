package com.voice.studio;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;

/** Static orbital accent: no animation or bitmap allocation during synthesis. */
final class ArchivePanelDrawable extends GradientDrawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF orbit=new RectF();
    private final float d;
    ArchivePanelDrawable(float density) {
        super(Orientation.TL_BR,new int[]{0xff252D57,0xff172341,0xff172038}); d=density;
        setCornerRadius(24*d);setStroke(Math.max(1,(int)d),0xff424B7C);
    }
    @Override public void draw(Canvas canvas) {
        super.draw(canvas);
        float x=getBounds().right-57*d,y=getBounds().top+52*d;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*.8f);p.setColor(0x4567E8F9);
        canvas.drawCircle(x,y,31*d,p);canvas.drawCircle(x,y,21*d,p);
        int saved=canvas.save();canvas.rotate(-28,x,y);orbit.set(x-42*d,y-12*d,x+42*d,y+12*d);canvas.drawOval(orbit,p);canvas.restoreToCount(saved);
        p.setStyle(Paint.Style.FILL);p.setColor(0xffB9D8FF);canvas.drawCircle(x+24*d,y-20*d,2.5f*d,p);
        p.setColor(0x7767E8F9);canvas.drawCircle(x,y,4*d,p);
    }
}
