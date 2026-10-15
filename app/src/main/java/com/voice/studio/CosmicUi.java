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
    static final int BG=0xff11131D, SURFACE=0xff1B1E2B, INK=0xffF1F0F7,
            MUTED=0xffA7ABBD, CYAN=0xffB8B5F5, PURPLE=0xffC4B9F4,
            LINE=0xff303443, SECONDARY=0xff252936, SUCCESS=0xff9AD5BF,
            PINK=0xffD7BBD5, PRIMARY_START=0xff7766B5, PRIMARY_END=0xff6C64A5;
    static final FastOutSlowInInterpolator EASE=new FastOutSlowInInterpolator();
    static int dp(Context c,int n) { return (int)(n*c.getResources().getDisplayMetrics().density+.5f); }
    static boolean motionAllowed(Context c) {
        PowerManager power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        return ValueAnimator.areAnimatorsEnabled() && (power==null||!power.isPowerSaveMode())
            && c.getSharedPreferences("studio",0).getBoolean("ui-motion",true);
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
        if(primary) { bg=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{PRIMARY_START,PRIMARY_END}); bg.setCornerRadius(dp(c,16)); }
        else bg=rounded(c,SECONDARY,14);
        return new RippleDrawable(ColorStateList.valueOf(0x22C4B9F4),bg,rounded(c,0xffFFFFFF,16));
    }
    static void press(View v) {
        v.setMinimumHeight(Math.max(v.getMinimumHeight(),dp(v.getContext(),48)));
        v.setMinimumWidth(Math.max(v.getMinimumWidth(),dp(v.getContext(),48)));
        v.setFocusable(true); v.setTag(R.id.cosmic_motion_bound,true);
        v.setOnTouchListener((view,event)->{
            int action=event.getActionMasked();
            boolean release=action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL;
            if(release || (view.isEnabled() && action==MotionEvent.ACTION_DOWN)) {
                view.animate().cancel();
                if(Boolean.TRUE.equals(view.getTag(R.id.cosmic_reveal))) {
                    view.setAlpha(1); view.setTranslationX(0); view.setTranslationY(0); view.setTag(R.id.cosmic_reveal,null);
                }
                if(!motionAllowed(view.getContext())) {view.setScaleX(1);view.setScaleY(1);return false;}
                float scale=release?1:.99f;
                view.animate().scaleX(scale).scaleY(scale).setStartDelay(0).setDuration(release?120:70).setInterpolator(EASE).start();
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
            group.animate().cancel();group.setTag(R.id.cosmic_reveal,true);
            group.setAlpha(.65f);group.setTranslationY(dp(group.getContext(),4));group.setTranslationX(0);
            group.animate().alpha(1).translationY(0).setStartDelay(0).setDuration(180).setInterpolator(EASE)
                .withEndAction(()->group.setTag(R.id.cosmic_reveal,null)).start();
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
        row.setPadding(0,dp(c,4),0,dp(c,12));
        LinearLayout words=new LinearLayout(c);words.setOrientation(1);
        TextView t=label(c,title,22,INK,true); t.setPadding(0,dp(c,4),0,dp(c,7));words.addView(t);
        TextView sub=label(c,subtitle,12,MUTED,false);sub.setLineSpacing(dp(c,3),1);words.addView(sub);
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));return row;
    }
    static LinearLayout navigation(Activity a,String selected,java.util.function.Consumer<String> action) {
        LinearLayout bar=new LinearLayout(a);bar.setOrientation(0);
        bar.setBackgroundColor(BG);bar.setPadding(dp(a,12),dp(a,6),dp(a,12),dp(a,8));
        String[] labels={"音色","合成","任务","作品"},icons={"mic","wave","tools","folder"};
        for(int i=0;i<labels.length;i++) {
            String label=labels[i];boolean chosen=label.equals(selected);
            LinearLayout item=new LinearLayout(a);item.setOrientation(1);item.setGravity(Gravity.CENTER);
            item.setBackground(new RippleDrawable(ColorStateList.valueOf(0x20C4B9F4),
                rounded(a,chosen?SECONDARY:BG,16),rounded(a,0xffFFFFFF,16)));
            item.addView(new MainActivity.IconView(a,icons[i],chosen?CYAN:MUTED),
                new LinearLayout.LayoutParams(dp(a,24),dp(a,24)));
            TextView caption=label(a,label,12,chosen?CYAN:MUTED,chosen);
            caption.setPadding(0,dp(a,5),0,0);item.addView(caption);
            item.setContentDescription(label);item.setSelected(chosen);press(item);
            item.setOnClickListener(v->action.accept(label));
            bar.addView(item,new LinearLayout.LayoutParams(0,dp(a,56),1));
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
        StateListDrawable states=new StateListDrawable();
        GradientDrawable focused=rounded(c,BG,14);focused.setStroke(dp(c,1),PURPLE);
        GradientDrawable idle=rounded(c,BG,14);idle.setStroke(dp(c,1),LINE);
        states.addState(new int[]{android.R.attr.state_focused},focused);states.addState(new int[]{},idle);t.setBackground(states);
    }

    /** Optional detail stays in its original view tree, preserving form drafts. */
    static void disclosure(Context c,LinearLayout parent,String title,String subtitle,View body,boolean expanded) {
        LinearLayout group=new LinearLayout(c);group.setOrientation(1);
        LinearLayout header=new LinearLayout(c);header.setOrientation(0);header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(c,2),dp(c,12),dp(c,2),dp(c,12));
        LinearLayout words=new LinearLayout(c);words.setOrientation(1);
        words.addView(label(c,title,14,INK,true));
        if(!subtitle.isEmpty()){TextView sub=label(c,subtitle,12,MUTED,false);sub.setPadding(0,dp(c,5),dp(c,8),0);words.addView(sub);}
        header.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=label(c,expanded?"−":"＋",20,MUTED,false);arrow.setGravity(Gravity.CENTER);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(arrow,new LinearLayout.LayoutParams(dp(c,32),dp(c,32)));
        header.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18C4B9F4),null,rounded(c,0xffFFFFFF,12)));
        header.setContentDescription(title+(expanded?"，已展开，点击收起":"，已收起，点击展开"));
        header.setSelected(expanded);press(header);
        body.setVisibility(expanded?View.VISIBLE:View.GONE);
        group.addView(header,new LinearLayout.LayoutParams(-1,-2));group.addView(body,new LinearLayout.LayoutParams(-1,-2));
        header.setOnClickListener(v->{boolean open=body.getVisibility()!=View.VISIBLE;
            lifecycle(body,false);body.setVisibility(open?View.VISIBLE:View.GONE);lifecycle(body,open);
            arrow.setText(open?"−":"＋");header.setSelected(open);
            header.setContentDescription(title+(open?"，已展开，点击收起":"，已收起，点击展开"));
        });
        parent.addView(group,new LinearLayout.LayoutParams(-1,-2));
    }
}
