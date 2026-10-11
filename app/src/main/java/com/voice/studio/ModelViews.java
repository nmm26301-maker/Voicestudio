package com.voice.studio;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import java.util.Locale;

final class ModelViews {
    static final int INK = CosmicUi.INK, MUTED = CosmicUi.MUTED, PURPLE = CosmicUi.PURPLE;
    final Activity owner; final ScrollView scroll; final LinearLayout root;
    ModelViews(Activity owner) {
        this.owner = owner;
        CosmicUi.install(owner);
        scroll = new ScrollView(owner); scroll.setBackgroundColor(CosmicUi.BG);
        root = new LinearLayout(owner); root.setOrientation(1); root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(root); owner.setContentView(scroll); CosmicUi.enter(root,1);
    }
    int dp(int n) { return (int)(owner.getResources().getDisplayMetrics().density * n + .5f); }
    static String mib(long bytes) { return String.format(Locale.US, "%.1f", bytes / 1048576.0); }
    LinearLayout card() {
        LinearLayout box = new LinearLayout(owner); box.setOrientation(1); box.setPadding(dp(18), dp(14), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xffFFFFFF); bg.setCornerRadius(dp(24)); box.setBackground(CosmicUi.panel(owner,CosmicUi.CYAN));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(18); root.addView(box, p); return box;
    }
    TextView label(LinearLayout parent, String text, int size, int color, boolean bold) {
        TextView view = new TextView(owner); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setLineSpacing(dp(3), 1); view.setTextIsSelectable(true); if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(8); parent.addView(view, p); return view;
    }
    Button action(LinearLayout parent, String text, boolean primary, Runnable task) {
        Button button = new Button(owner); button.setText(text); button.setTextSize(14); button.setAllCaps(false);
        button.setTextColor(primary ? 0xffFFFFFF : PURPLE); button.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(primary ? PURPLE : 0xffEFEBFF); bg.setCornerRadius(dp(18)); button.setBackground(CosmicUi.button(owner,primary)); CosmicUi.press(button);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(10); parent.addView(button, p);
        button.setOnClickListener(v -> task.run()); return button;
    }
}
