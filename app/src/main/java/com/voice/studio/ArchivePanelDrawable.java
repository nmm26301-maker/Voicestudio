package com.voice.studio;
import android.graphics.drawable.GradientDrawable;
/** Archive summary uses the same quiet surface as the rest of the app. */
final class ArchivePanelDrawable extends GradientDrawable {
    ArchivePanelDrawable(float density){setColor(CosmicUi.SURFACE);setCornerRadius(20*density);setStroke(Math.max(1,(int)density),CosmicUi.LINE);}
}
