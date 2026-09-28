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
        page(query,genre,0,40,callback);
    }
    static void page(String query,String genre,int offset,int limit,Result callback){
        if(cachedAll!=null && System.currentTimeMillis()-cacheTime<1800000){callback.accept(filter(cachedAll,query,genre,offset,limit),null);return;}
        Net.jsonArray("https://animestvs.org/animes",(a,e)->{
            if(e!=null){callback.accept(Collections.emptyList(),e.getMessage());return;}
            cachedAll=a;cacheTime=System.currentTimeMillis();callback.accept(filter(a,query,genre,offset,limit),null);
        });
    }
    static void kitsuSearch(String query,Result callback){
        if(query.trim().isEmpty()){callback.accept(Collections.emptyList(),null);return;}
        Net.json("https://kitsu.io/api/edge/anime?filter%5Btext%5D="+Net.enc(query)+"&page%5Blimit%5D=12",(json,error)->{
            if(error!=null||json==null){callback.accept(Collections.emptyList(),error==null?"Fonte indisponível":error.getMessage());return;}
            ArrayList<Anime> list=new ArrayList<>();JSONArray data=json.optJSONArray("data");
            if(data!=null)for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;JSONObject attr=o.optJSONObject("attributes");if(attr==null)continue;
                JSONObject image=attr.optJSONObject("posterImage");String poster=image==null?"":image.optString("large","");
                list.add(new Anime("kitsu-"+o.optString("id"),attr.optString("canonicalTitle","Anime"),poster,attr.optString("synopsis",""),"Kitsu · metadados",attr.optInt("episodeCount",0)));
            }callback.accept(list,null);
        });
    }
    static void related(Anime anime,Result callback){
        if(cachedAll!=null && System.currentTimeMillis()-cacheTime<1800000){callback.accept(relatedFilter(cachedAll,anime),null);return;}
        Net.jsonArray("https://animestvs.org/animes",(a,e)->{
            if(e!=null){callback.accept(Collections.singletonList(anime),e.getMessage());return;}
            cachedAll=a;cacheTime=System.currentTimeMillis();callback.accept(relatedFilter(a,anime),null);
        });
    }
    private static List<Anime> relatedFilter(JSONArray data,Anime current){
        ArrayList<Anime> list=new ArrayList<>();String key=franchiseKey(current.title);
        if(data!=null && key.length()>7)for(int i=0;i<data.length() && list.size()<24;i++){
            JSONObject j=data.optJSONObject(i);if(j!=null && key.equals(franchiseKey(j.optString("titulo"))))list.add(fromSource(j));
        }
        boolean hasCurrent=false;for(Anime item:list)if(item.title.equalsIgnoreCase(current.title))hasCurrent=true;
        if(!hasCurrent)list.add(current);
        list.sort((a,b)->{int n=seasonNumber(a.title)-seasonNumber(b.title);return n!=0?n:a.title.compareToIgnoreCase(b.title);});return list;
    }
    private static String franchiseKey(String title){
        String value=normalize(title).replaceFirst("^(?:5toubun|gotoubun)","gotoubun");String last;
        do{last=value;value=value.replaceFirst("(?:\\d+(?:st|nd|rd|th)?season|season\\d+|s\\d+|movie|special|ova|\\d+)$","");}while(!last.equals(value));
        return value;
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
        m=java.util.regex.Pattern.compile("(?:\\s|^)s?(\\d+)$",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(title.trim());
        if(m.find())try{return Integer.parseInt(m.group(1));}catch(Exception ignored){}
        return 1;
    }
    static String seasonLabel(String title){if(title.contains("*")||title.contains("∽"))return "Especial";if(title.toLowerCase(Locale.ROOT).contains("movie"))return "Filme";return "Temporada "+seasonNumber(title);}
    static String episodeLabel(Anime a){return seasonLabel(a.title)+" · "+(a.description.toLowerCase(Locale.ROOT).startsWith("epis")?a.description:"Episódio "+episodeNumber(a.description));}
    static String normalize(String title){return java.text.Normalizer.normalize(title.toLowerCase(Locale.ROOT).replaceAll("\\(.*?\\)",""),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]","");}
    private static List<Anime> filter(JSONArray data,String query,String genre,int offset,int limit){
        ArrayList<Anime> list=new ArrayList<>();if(data==null)return list;
        String q=normalize(query).replaceFirst("^5toubun","gotoubun"),g=normalize(genre);int skipped=0;
        for(int i=0;i<data.length() && list.size()<limit;i++){
            JSONObject j=data.optJSONObject(i);if(j==null)continue;
            if(!q.isEmpty() && !normalize(j.optString("titulo")).replaceFirst("^5toubun","gotoubun").contains(q))continue;
            if(!g.isEmpty() && !normalize(j.optString("generos")).contains(g))continue;
            if(skipped++<offset)continue;
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
