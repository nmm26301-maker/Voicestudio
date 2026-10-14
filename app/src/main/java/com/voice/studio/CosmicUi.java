package com.voice.studio;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.os.PowerManager;
import android.view.*;
import android.widget.*;

/** Shared native styling; motion never owns click handling or audio state. */
final class CosmicUi {
    static final int BG=0xff090E24, SURFACE=0xff17213C, INK=0xffF2F4FF,
            MUTED=0xffB3BFDC, CYAN=0xff8BEEFF, PURPLE=0xffC6AFFF,
            LINE=0xff3C4C76, SECONDARY=0xff1F2D4C, SUCCESS=0xff79E2C6,
            PINK=0xffFFB0D9, PRIMARY_START=0xff8043C4, PRIMARY_END=0xff4E54AC;
    static final FastOutSlowInInterpolator EASE=new FastOutSlowInInterpolator();
    static int dp(Context c,int n) { return (int)(n*c.getResources().getDisplayMetrics().density+.5f); }
    static boolean motionAllowed(Context c) {
        return ValueAnimator.areAnimatorsEnabled() && c.getSharedPreferences("studio",0).getBoolean("ui-motion",true);
    }
    static boolean ambientAllowed(Context c) {
        PowerManager p=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        return motionAllowed(c) && (p==null || !p.isPowerSaveMode()) && StudioService.active.isEmpty();
    }
    static void install(Activity a) {
        a.getWindow().setStatusBarColor(BG); a.getWindow().setNavigationBarColor(BG);
        a.getWindow().getDecorView().setSystemUiVisibility(0);
        a.getWindow().setWindowAnimations(motionAllowed(a)?R.style.CosmicWindowMotion:0);
    }
    static Drawable panel(Context c,int accent) { return new CosmicPanelDrawable(c.getResources().getDisplayMetrics().density,accent); }
    static GradientDrawable rounded(Context c,int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c,radius)); return d;
    }
    static Drawable button(Context c,boolean primary) {
        GradientDrawable bg;
        if(primary) { bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{PRIMARY_START,PRIMARY_END}); bg.setCornerRadius(dp(c,20));bg.setStroke(dp(c,1),0xff9F83E6); }
        else { bg=rounded(c,SECONDARY,18); bg.setStroke(dp(c,1),LINE); }
        return new RippleDrawable(ColorStateList.valueOf(0x447CEBFA),bg,rounded(c,0xffFFFFFF,16));
    }
    static void press(View v) {
        v.setMinimumHeight(Math.max(v.getMinimumHeight(),dp(v.getContext(),48)));
        v.setFocusable(true); v.setTag(R.id.cosmic_motion_bound,true);
        v.setOnTouchListener((view,event)->{
            int action=event.getActionMasked();
            if(view.isEnabled() && motionAllowed(view.getContext()) && (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL)) {
                view.animate().cancel();
                if(Boolean.TRUE.equals(view.getTag(R.id.cosmic_reveal))) {
                    view.setAlpha(1); view.setTranslationX(0); view.setTranslationY(0); view.setTag(R.id.cosmic_reveal,null);
                }
                float scale=action==MotionEvent.ACTION_DOWN?.975f:1;
                view.animate().scaleX(scale).scaleY(scale).setStartDelay(0).setDuration(action==MotionEvent.ACTION_DOWN?100:180).setInterpolator(EASE).start();
            }
            return false;
        });
    }
    static boolean isForeground(View v) {
        while(v!=null) { if(Boolean.FALSE.equals(v.getTag(R.id.cosmic_foreground)))return false; android.view.ViewParent p=v.getParent();v=p instanceof View?(View)p:null; }
        return true;
    }
    static void enter(ViewGroup group,int direction) {
        if(!motionAllowed(group.getContext())) return;
        group.post(()->{
            if(!group.isAttachedToWindow() || !isForeground(group) || !motionAllowed(group.getContext())) return;
            for(int i=0;i<Math.min(9,group.getChildCount());i++) {
                View child=group.getChildAt(i); if(child.getVisibility()!=View.VISIBLE) continue;
                child.setTag(R.id.cosmic_reveal,true); child.setAlpha(0); child.setTranslationY(dp(child.getContext(),10)); child.setTranslationX(direction*dp(child.getContext(),8));
                child.animate().alpha(1).translationX(0).translationY(0).setStartDelay(Math.min(i*22,120)).setDuration(280).setInterpolator(EASE).withEndAction(()->child.setTag(R.id.cosmic_reveal,null)).start();
            }
        });
    }
    static void lifecycle(View v,boolean visible) {
        v.setTag(R.id.cosmic_foreground,visible);
        if(v instanceof SignalOrbView) ((SignalOrbView)v).foreground(visible);
        if(v instanceof DreamProgressView) ((DreamProgressView)v).foreground(visible);
        if(!visible) {
            if(Boolean.TRUE.equals(v.getTag(R.id.cosmic_motion_bound))) { v.animate().cancel(); v.setScaleX(1); v.setScaleY(1); }
            if(Boolean.TRUE.equals(v.getTag(R.id.cosmic_reveal))) { v.animate().cancel(); v.setAlpha(1); v.setTranslationX(0); v.setTranslationY(0); v.setTag(R.id.cosmic_reveal,null); }
        }
        if(v instanceof ViewGroup) { ViewGroup g=(ViewGroup)v; for(int i=0;i<g.getChildCount();i++) lifecycle(g.getChildAt(i),visible); }
    }
    static TextView label(Context c,String s,int size,int color,boolean bold) {
        TextView t=new TextView(c); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setIncludeFontPadding(false);
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD); return t;
    }
    static LinearLayout hero(Context c,String title,String subtitle,String badge,int accent) {
        LinearLayout row=new LinearLayout(c); row.setOrientation(0); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c,20),dp(c,20),dp(c,16),dp(c,20)); row.setBackground(panel(c,accent));
        LinearLayout words=new LinearLayout(c);words.setOrientation(1);
        TextView b=label(c,badge,10,accent,true);b.setLetterSpacing(.12f);words.addView(b);
        TextView t=label(c,title,24,INK,true); t.setPadding(0,dp(c,9),0,dp(c,7));words.addView(t);
        TextView sub=label(c,subtitle,12,MUTED,false);sub.setLineSpacing(dp(c,3),1);words.addView(sub);
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));row.addView(new SignalOrbView(c,accent,false),new LinearLayout.LayoutParams(dp(c,74),dp(c,74)));return row;
    }
    static LinearLayout navigation(Activity a,String selected,java.util.function.Consumer<String> action) {
        LinearLayout bar=new LinearLayout(a);bar.setOrientation(0);
        bar.setBackgroundColor(BG);bar.setPadding(dp(a,8),dp(a,8),dp(a,8),dp(a,10));
        String[] labels={"音色","合成","任务","作品"},icons={"mic","wave","tools","folder"};
        for(int i=0;i<labels.length;i++) {
            String label=labels[i];boolean chosen=label.equals(selected);
            LinearLayout item=new LinearLayout(a);item.setOrientation(1);item.setGravity(Gravity.CENTER);
            if(chosen)item.setBackground(panel(a,PURPLE));
            item.addView(new MainActivity.IconView(a,icons[i],chosen?CYAN:MUTED),
                new LinearLayout.LayoutParams(dp(a,24),dp(a,24)));
            TextView caption=label(a,label,12,chosen?CYAN:MUTED,chosen);
            caption.setPadding(0,dp(a,5),0,0);item.addView(caption);
            item.setContentDescription(label);item.setSelected(chosen);press(item);
            item.setOnClickListener(v->action.accept(label));
            bar.addView(item,new LinearLayout.LayoutParams(0,dp(a,60),1));
        }
        return bar;
    }
    static LinearLayout workflow(Context c,int active) {
        LinearLayout strip=new LinearLayout(c);strip.setOrientation(0);strip.setPadding(0,dp(c,10),0,dp(c,12));
        String[] steps={"录音","引擎","文字","合成","作品"};
        for(int i=0;i<steps.length;i++) {
            LinearLayout step=new LinearLayout(c);step.setOrientation(1);step.setGravity(Gravity.CENTER);
            boolean chosen=i==active;int color=chosen?CYAN:MUTED;
            TextView number=label(c,Integer.toString(i+1),13,color,true);
            number.setGravity(Gravity.CENTER);number.setBackground(rounded(c,chosen?SECONDARY:SURFACE,20));
            step.addView(number,new LinearLayout.LayoutParams(dp(c,30),dp(c,30)));
            TextView name=label(c,steps[i],11,color,chosen);name.setPadding(0,dp(c,5),0,0);step.addView(name);
            strip.addView(step,new LinearLayout.LayoutParams(0,-2,1));
        }
        strip.setContentDescription("录音或选音色、选引擎、输入文字、开始合成、检查并保存作品");
        return strip;
    }
    static void field(EditText t) {
        Context c=t.getContext();t.setTextColor(INK);t.setHintTextColor(MUTED);t.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));
        GradientDrawable bg=rounded(c,0xff111A33,18);bg.setStroke(dp(c,1),LINE);t.setBackground(bg);
    }
}
