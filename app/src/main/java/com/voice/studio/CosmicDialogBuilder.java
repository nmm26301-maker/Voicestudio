package com.voice.studio;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

/** Consistent native dialogs; failed form validation must not discard input. */
final class CosmicDialogBuilder extends AlertDialog.Builder {
    interface Submission { void run() throws Exception; }
    final Context owner;
    Submission submission;
    CosmicDialogBuilder(Context c) { super(c); owner = c; }
    CosmicDialogBuilder submit(String label, Submission action) {
        submission = action;
        setPositiveButton(label, null);
        return this;
    }
    @Override public AlertDialog create() {
        AlertDialog dialog = super.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setWindowAnimations(CosmicUi.motionAllowed(owner) ? R.style.CosmicDialogMotion : 0);
        if (submission != null) dialog.setOnShowListener(ignored ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                try { submission.run(); dialog.dismiss(); }
                catch (Exception error) {
                    Toast.makeText(owner, error.getMessage() == null ? "输入无效，请检查后重试" : error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }));
        return dialog;
    }
}
