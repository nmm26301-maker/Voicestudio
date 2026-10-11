package com.voice.studio;

import java.util.*;
import org.json.*;

final class WorksCatalog {
    static List<JSONObject> select(JSONArray history, String query, boolean favorites, int sort) {
        String needle=query.toLowerCase(Locale.ROOT).trim(); List<JSONObject> out=new ArrayList<>();
        for(int i=0;i<history.length();i++) {
            JSONObject item=history.optJSONObject(i);
            if(item==null || item.optLong("deletedAt")>0 || favorites && !item.optBoolean("favorite")) continue;
            String title=item.optString("title").trim(); if(title.isEmpty())title=AudioChecks.shortTitle(item.optString("text"));
            String hay=title+" "+item.optString("text")+" "+item.optString("voice");
            if(hay.toLowerCase(Locale.ROOT).contains(needle))out.add(item);
        }
        out.sort((a,b)->sort==1 ? Long.compare(a.optLong("created"),b.optLong("created"))
                : sort==2 ? Long.compare(b.optLong("duration"),a.optLong("duration"))
                : Long.compare(b.optLong("created"),a.optLong("created")));
        return out;
    }
    static long totalDuration(JSONArray history) {
        long total=0; for(int i=0;i<history.length();i++) { JSONObject j=history.optJSONObject(i); if(j!=null && j.optLong("deletedAt")<=0)total+=Math.max(0,j.optLong("duration")); } return total;
    }
    static String duration(long millis) {
        long seconds=Math.max(0,millis/1000);
        return seconds>=3600 ? String.format(Locale.US,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60) : String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60);
    }
}
