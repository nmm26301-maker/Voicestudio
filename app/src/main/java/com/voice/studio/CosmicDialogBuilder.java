package com.voice.studio;
import android.app.AlertDialog;
import android.content.Context;
/** Apply window motion without taking the caller's on-show validation listener. */
final class CosmicDialogBuilder extends AlertDialog.Builder {
    final Context owner;
    CosmicDialogBuilder(Context c){super(c);owner=c;}
    @Override public AlertDialog create(){AlertDialog d=super.create();if(d.getWindow()!=null)d.getWindow().setWindowAnimations(CosmicUi.motionAllowed(owner)?R.style.CosmicDialogMotion:0);return d;}
}
