package com.voice.studio;
import android.graphics.drawable.GradientDrawable;
/** One quiet surface shared by cards; no ornamental edge lines or shaders. */
final class CosmicPanelDrawable extends GradientDrawable {
    CosmicPanelDrawable(float density,int accent){
        setColor(CosmicUi.SURFACE);setCornerRadius(20*density);
        setStroke(Math.max(1,(int)density),CosmicUi.LINE);
    }
}
