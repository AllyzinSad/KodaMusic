package com.koda.anime;

import org.json.*;
import java.util.*;

final class Catalog {
    interface Result { void accept(List<Anime> list, String error); }
    private static JSONArray cachedAll; private static long cacheTime;
    static void recommendations(Result callback) { Net.jsonArray("https://animestvs.org/animes-famosos",(a,e)->{
        if(e!=null){callback.accept(Collections.emptyList(),e.getMessage());return;}
        ArrayList<Anime> list=new ArrayList<>();if(a!=null)for(int i=0;i<Math.min(a.length(),12);i++){JSONObject j=a.optJSONObject(i);if(j!=null)list.add(fromSource(j));}
        callback.accept(list,null);
    }); }
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
    static Anime fromSource(JSONObject j){
        return new Anime("source-"+j.optString("id"),j.optString("titulo","Anime"),j.optString("imagem",""),
            j.optString("sinopse",""),j.optString("generos",""),j.optInt("episodios",0));
    }
    static void search(String query,String genre,Result callback){
        if(query.trim().isEmpty() && genre.isEmpty()){callback.accept(Collections.emptyList(),null);return;}
        if(query.trim().isEmpty()){
            long now=System.currentTimeMillis();
            if(cachedAll!=null && now-cacheTime<1800000){callback.accept(filter(cachedAll,"",genre),null);return;}
            Net.jsonArray("https://animestvs.org/animes",(a,e)->{
                if(e!=null){callback.accept(Collections.emptyList(),e.getMessage());return;}
                cachedAll=a;cacheTime=System.currentTimeMillis();callback.accept(filter(a,"",genre),null);
            });return;
        }
        Net.jsonArray("https://animestvs.org/animes?titulo="+Net.enc(query.trim()),(a,e)->{
            if(e!=null){callback.accept(Collections.emptyList(),e.getMessage());return;}
            callback.accept(filter(a,query,genre),null);
        });
    }
    static void episodes(Anime anime,Result callback){
        String type=anime.title.toLowerCase(Locale.ROOT).contains("dublado")?"animes-dublados":"animes-legendados";
        String endpoint="https://animestvs.org/"+type+"/"+Net.enc(anime.title).replace("+","%20")+"/episodios";
        Net.jsonArray(endpoint,(a,e)->{
            ArrayList<Anime> found=new ArrayList<>();
            if(a!=null)for(int i=0;i<a.length();i++){
                JSONObject j=a.optJSONObject(i);if(j==null)continue;
                String url=j.optString("link_video","");if(url.startsWith("https://"))found.add(new Anime("episode-"+j.optString("id"),j.optString("anime",anime.title),j.optString("image",anime.poster),j.optString("episodio"),j.optString("tipo"),0,url));
            }
            if(anime.playUrl.startsWith("https://")){
                boolean present=false;for(Anime x:found)if(x.playUrl.equals(anime.playUrl))present=true;
                if(!present)found.add(new Anime(anime.id,anime.title,anime.poster,anime.description,anime.genre,anime.episodes,anime.playUrl));
            }
            found.sort((x,y)->episodeNumber(x.description)-episodeNumber(y.description));
            callback.accept(found,e==null?null:e.getMessage());
        });
    }
    static int episodeNumber(String label){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\d+").matcher(label);return m.find()?Integer.parseInt(m.group()):0;}
    static int seasonNumber(String title){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(?:season|temporada)\\s*(\\d+)|(\\d+)(?:st|nd|rd|th|ª|º)?\\s*(?:season|temporada)").matcher(title);
        if(m.find())try{return Integer.parseInt(m.group(1)!=null?m.group(1):m.group(2));}catch(Exception ignored){}
        return 1;
    }
    static String episodeLabel(Anime a){return "Temporada "+seasonNumber(a.title)+" · "+(a.description.toLowerCase(Locale.ROOT).startsWith("epis")?a.description:"Episódio "+episodeNumber(a.description));}
    static String normalize(String title){return java.text.Normalizer.normalize(title.toLowerCase(Locale.ROOT).replaceAll("\\(.*?\\)",""),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]","");}
    private static List<Anime> filter(JSONArray data,String query,String genre){
        ArrayList<Anime> list=new ArrayList<>();if(data==null)return list;
        String q=normalize(query),g=normalize(genre);
        for(int i=0;i<data.length() && list.size()<40;i++){
            JSONObject j=data.optJSONObject(i);if(j==null)continue;
            if(!q.isEmpty() && !normalize(j.optString("titulo")).contains(q))continue;
            if(!g.isEmpty() && !normalize(j.optString("generos")).contains(g))continue;
            list.add(fromSource(j));
        }return list;
    }
    private static void get(String url,Result callback) { Net.json(url,(j,e)->{
        if(e!=null || j==null){callback.accept(Collections.emptyList(), e==null?"Resposta vazia":e.getMessage());return;}
        ArrayList<Anime> result=new ArrayList<>(); JSONArray data=j.optJSONArray("data");
        if(data!=null) for(int i=0;i<data.length();i++){JSONObject item=data.optJSONObject(i);if(item!=null)result.add(Anime.fromJikan(item));}
        callback.accept(result,null);
    }); }
}
