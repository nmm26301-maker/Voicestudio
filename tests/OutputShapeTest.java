package com.voice.studio;

import java.util.*;

public class OutputShapeTest {
    static int checks;
    static void check(boolean ok,String label) { checks++;if(!ok)throw new AssertionError(label); }
    public static void main(String[] args) {
        for (int sequence=1;sequence<=850;sequence++) {
            Map<String,Long> values=new HashMap<>();Set<String> conflicts=new HashSet<>();
            OmniOutputShape.bind(values,conflicts,new String[]{"batch","","S"},new long[]{2,8,sequence});
            OmniOutputShape.bind(values,conflicts,new String[]{"batch","S"},new long[]{2,sequence});
            long[] declared={-1,8,-1,1025};
            long[] actual=OmniOutputShape.resolve(declared,new String[]{"batch","","S",""},values,conflicts);
            check(Arrays.equals(actual,new long[]{2,8,sequence,1025}),"supported declared symbols resolve exactly");
            check(Arrays.equals(declared,new long[]{-1,8,-1,1025}),"model metadata not mutated");
            check(conflicts.isEmpty(),"matching input symbols remain valid");
            check(OmniOutputShape.resolve(new long[]{-1},new String[]{"missing"},values,conflicts)==null,"unknown output falls back");
            OmniOutputShape.bind(values,conflicts,new String[]{"S"},new long[]{sequence+1});
            check(OmniOutputShape.resolve(declared,new String[]{"batch","","S",""},values,conflicts)==null,"conflicting symbol never guessed");
            check(Arrays.equals(OmniOutputShape.resolve(new long[]{-1},new String[]{"batch"},values,conflicts),new long[]{2}),"unrelated conflict does not poison known output");
        }
        Map<String,Long> values=new HashMap<>();Set<String> conflicts=new HashSet<>();
        check(Arrays.equals(OmniOutputShape.resolve(new long[]{2,8,7},null,values,conflicts),new long[]{2,8,7}),"static shape needs no names");
        check(OmniOutputShape.resolve(new long[]{-1},null,values,conflicts)==null,"unnamed dynamic dimension");
        check(OmniOutputShape.resolve(new long[]{1},new String[]{},values,conflicts)==null,"malformed dimension names");
        check(OmniOutputShape.resolve(new long[]{0},null,values,conflicts)==null,"empty output not preallocated");
        check(OmniOutputShape.resolve(new long[]{Long.MAX_VALUE,8},null,values,conflicts)==null,"overflow and huge allocation bounded");
        check(OmniOutputShape.resolve(new long[]{OmniOutputShape.MAX_ELEMENTS+1},null,values,conflicts)==null,"64 MiB cap");
        check(OmniOutputShape.resolve(new long[]{OmniOutputShape.MAX_ELEMENTS},null,values,conflicts)!=null,"exact cap accepted");
        OmniOutputShape.bind(values,conflicts,new String[]{"zero"},new long[]{0});
        check(OmniOutputShape.resolve(new long[]{-1},new String[]{"zero"},values,conflicts)==null,"invalid supplied input dimension");
        System.out.println("Output shape: "+checks+" checks passed; exact declared symbols, no guesses, conflicts and bounded allocation.");
    }
}
