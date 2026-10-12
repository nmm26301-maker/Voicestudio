package com.voice.studio;

import java.util.*;

/** Resolve only declared dimension names, never guess an unnamed dynamic output. */
final class OmniOutputShape {
    static final long MAX_ELEMENTS = 64L * 1024 * 1024 / 4;
    static void bind(Map<String,Long> symbols, Set<String> conflicts, String[] names, long[] actual) {
        if (names == null || actual == null || names.length != actual.length) return;
        for (int i=0;i<names.length;i++) {
            String name=names[i];
            if (name==null || name.isEmpty()) continue;
            Long prior=symbols.get(name);
            if (actual[i]<=0 || (prior!=null && prior.longValue()!=actual[i])) conflicts.add(name);
            else symbols.put(name,actual[i]);
        }
    }
    static long[] resolve(long[] declared, String[] names, Map<String,Long> symbols, Set<String> conflicts) {
        if (declared==null || (names!=null && names.length!=declared.length)) return null;
        long[] shape=declared.clone();long count=1;
        for (int i=0;i<shape.length;i++) {
            if (shape[i]<0) {
                String name=names==null ? null : names[i];
                Long value=name==null || conflicts.contains(name) ? null : symbols.get(name);
                if (value==null) return null;
                shape[i]=value;
            }
            if (shape[i]<=0 || count>MAX_ELEMENTS/shape[i]) return null;
            count*=shape[i];
        }
        return shape;
    }
}
