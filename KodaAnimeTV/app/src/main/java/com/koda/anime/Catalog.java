package com.koda.anime;

import org.json.*;
import java.util.*;

final class Catalog {
    interface Result { void accept(List<Anime> list, String error); }
    static void recommendations(Result callback) { get("https://api.jikan.moe/v4/top/anime?filter=airing&limit=12",callback); }
    static void recent(Result callback) {
        Net.jsonArray("https://animestvs.org/episodios-recentes",(items,error)->{
            ArrayList<Anime> playable=new ArrayList<>();Set<String> ids=new HashSet<>();
            if(error==null && items!=null)for(int i=0;i<Math.min(items.length(),30);i++){
                JSONObject j=items.optJSONObject(i);if(j==null)continue;
                String video=j.optString("link_video","");String id=j.optString("id","");
                if(!video.startsWith("https://")||id.isEmpty()||!ids.add(id))continue;
                playable.add(new Anime("episode-"+id,j.optString("anime","Anime"),j.optString("image",""),
                    j.optString("episodio","Episódio recente"),j.optString("tipo","Anime"),0,video));
            }
            if(!playable.isEmpty())callback.accept(playable,null);
            else get("https://api.jikan.moe/v4/seasons/now?limit=12",callback);
        });
    }
    static void search(String query, Result callback) { get("https://api.jikan.moe/v4/anime?q="+Net.enc(query)+"&limit=20",callback); }
    private static void get(String url,Result callback) { Net.json(url,(j,e)->{
        if(e!=null || j==null){callback.accept(Collections.emptyList(), e==null?"Resposta vazia":e.getMessage());return;}
        ArrayList<Anime> result=new ArrayList<>(); JSONArray data=j.optJSONArray("data");
        if(data!=null) for(int i=0;i<data.length();i++){JSONObject item=data.optJSONObject(i);if(item!=null)result.add(Anime.fromJikan(item));}
        callback.accept(result,null);
    }); }
}
