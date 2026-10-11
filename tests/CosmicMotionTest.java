package com.voice.studio;
/** Compare the actual vendored lookup curve to an independent Bezier solver. */
public final class CosmicMotionTest {
    public static void main(String[] args) {
        FastOutSlowInInterpolator easing=new FastOutSlowInInterpolator();int checks=0;float previous=-1;
        for(int i=0;i<=4000;i++) {
            float t=i/4000f,value=easing.getInterpolation(t);
            if(!Float.isFinite(value)||value<0||value>1||value<previous)throw new AssertionError("Invalid curve at "+t);
            checks++;previous=value;
            double low=0,high=1;
            for(int n=0;n<32;n++){double u=(low+high)*.5,x=3*(1-u)*(1-u)*u*.4+3*(1-u)*u*u*.2+u*u*u;if(x<t)low=u;else high=u;}
            double u=(low+high)*.5,expected=3*(1-u)*u*u+u*u*u;
            if(Math.abs(expected-value)>.0004)throw new AssertionError("Bezier mismatch "+t);checks++;
        }
        if(easing.getInterpolation(-1)!=0||easing.getInterpolation(2)!=1)throw new AssertionError("Clamp");checks++;
        System.out.println("Motion curve: "+checks+" checks passed (host Java; Android animation lifecycle not simulated).");
    }
}
