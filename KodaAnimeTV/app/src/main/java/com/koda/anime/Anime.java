package com.koda.anime;

import org.json.*;
import java.util.*;

final class Anime {
    final String id,title,poster,description,genre;
    final int episodes;
    Anime(String id,String title,String poster,String description,String genre,int episodes) {
        this.id=id; this.title=title; this.poster=poster; this.description=description; this.genre=genre; this.episodes=episodes;
    }
    static Anime fromJikan(JSONObject j) {
        JSONObject jpg=j.optJSONObject("images"); jpg=jpg==null?null:jpg.optJSONObject("jpg");
        String poster=jpg==null?"":jpg.optString("large_image_url",jpg.optString("image_url",""));
        JSONArray genres=j.optJSONArray("genres"); String genre="";
        if(genres!=null && genres.length()>0) genre=genres.optJSONObject(0).optString("name","");
        return new Anime(String.valueOf(j.optInt("mal_id")), j.optString("title", "Sem título"), poster, j.optString("synopsis",""),genre,j.optInt("episodes",0));
    }
}
