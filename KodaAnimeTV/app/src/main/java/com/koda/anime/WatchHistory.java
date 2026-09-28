package com.koda.anime;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

final class WatchHistory {
    static final class Entry {
        final String animeId,title,poster,episode,url;
        final long position,duration,updated;
        Entry(String id,String t,String p,String e,String u,long pos,long dur,long time){animeId=id;title=t;poster=p;episode=e;url=u;position=pos;duration=dur;updated=time;}
        boolean watched(){return duration>0 && position>=duration*9/10;}
        String key(){return animeId+"|"+episode;}
    }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("watch_history",0);}
    static void save(Context c, String id,String title,String poster,String episode,String url,long position,long duration){
        if(url==null||url.isEmpty())return;
        try {
            JSONArray a=raw(c);JSONArray out=new JSONArray();String key=id+"|"+episode;
            JSONObject current=new JSONObject();current.put("id",id);current.put("title",title);current.put("poster",poster);current.put("episode",episode);current.put("url",url);current.put("position",Math.max(0,position));current.put("duration",Math.max(0,duration));current.put("updated",System.currentTimeMillis());out.put(current);
            for(int i=0;i<a.length() && out.length()<100;i++){JSONObject o=a.optJSONObject(i);if(o!=null && !(o.optString("id")+"|"+o.optString("episode")).equals(key))out.put(o);}
            prefs(c).edit().putString("entries",out.toString()).apply();
        }catch(Exception ignored){}
    }
    static List<Entry> all(Context c){ArrayList<Entry> entries=new ArrayList<>();JSONArray a=raw(c);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)entries.add(new Entry(o.optString("id"),o.optString("title"),o.optString("poster"),o.optString("episode"),o.optString("url"),o.optLong("position"),o.optLong("duration"),o.optLong("updated")));}return entries;}
    static Entry find(Context c,String id,String episode){for(Entry e:all(c))if(e.animeId.equals(id)&&e.episode.equals(episode))return e;return null;}
    private static JSONArray raw(Context c){try{return new JSONArray(prefs(c).getString("entries","[]"));}catch(Exception e){return new JSONArray();}}
    private WatchHistory(){}
}
