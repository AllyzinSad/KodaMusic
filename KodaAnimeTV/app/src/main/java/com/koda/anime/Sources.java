package com.koda.anime;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

final class Sources {
    interface Result { void accept(List<Episode> episodes, String error); }
    static final class Episode { final String label,url,provider; Episode(String l,String u,String p){label=l;url=u;provider=p;} }
    static void find(Context context, Anime anime, Result callback) {
        SharedPreferences p=context.getSharedPreferences("sources",0);
        String hallan=p.getString("hallan","").trim(); String sugoi=p.getString("sugoi","").trim();
        if(hallan.isEmpty() && sugoi.isEmpty()){callback.accept(Collections.emptyList(),"Configure a URL HTTPS de pelo menos uma fonte em Configurações.");return;}
        ArrayList<Episode> found=new ArrayList<>(); ArrayList<String> errors=new ArrayList<>();
        int total=(hallan.isEmpty()?0:1)+(sugoi.isEmpty()?0:1); int[] completed={0};
        Runnable finish=()->{completed[0]++;if(completed[0]==total)callback.accept(found,found.isEmpty()?android.text.TextUtils.join(" · ",errors):null);};
        if(!hallan.isEmpty()) {
            // Projeto api-animesonline-cc: /search/:title -> /anime/:id -> /episode/:id.
            Net.json(clean(hallan)+"/search/"+Net.enc(anime.title).replace("+","%20"),(search,e)->{
                if(e!=null){errors.add("Fonte 1: "+e.getMessage());finish.run();return;}
                JSONObject match=firstObject(search, "animes","results","data");
                String id=match==null?"":id(match); if(id.isEmpty()){errors.add("Fonte 1: anime não encontrado");finish.run();return;}
                Net.json(clean(hallan)+"/anime/"+Net.enc(id),(details,err)->{
                    if(err!=null){errors.add("Fonte 1: "+err.getMessage());finish.run();return;}
                    JSONArray eps=findArray(details,"episodes","episodios","data");
                    if(eps==null){errors.add("Fonte 1: formato de episódios diferente");finish.run();return;}
                    ArrayList<String> ids=new ArrayList<>();
                    for(int i=0;i<Math.min(eps.length(),60);i++){JSONObject ep=eps.optJSONObject(i);if(ep!=null && !id(ep).isEmpty()) ids.add(id(ep));}
                    if(ids.isEmpty()){errors.add("Fonte 1: nenhum episódio com ID");finish.run();return;}
                    for(int i=0;i<ids.size();i++)found.add(new Episode("Episódio "+(i+1)+" · Fonte 1",clean(hallan)+"/episode/"+Net.enc(ids.get(i)),"api-animesonline-cc"));
                    finish.run();
                });
            });
        }
        if(!sugoi.isEmpty()) {
            // SugoiAPI: /episode/:anime-slug/:temporada/:numero-episodio.
            String slug=anime.title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]","").trim().replaceAll(" +","-");
            Net.json(clean(sugoi)+"/episode/"+Net.enc(slug)+"/1/1",(j,e)->{
                if(e!=null){errors.add("Fonte 2: "+e.getMessage());finish.run();return;}
                JSONArray providers=findArray(j,"data","providers");
                if(providers!=null)for(int i=0;i<providers.length();i++){
                    JSONObject provider=providers.optJSONObject(i); if(provider==null)continue;
                    JSONArray eps=provider.optJSONArray("episodes");
                    if(eps!=null)for(int n=0;n<eps.length();n++){
                        String u=videoUrl(eps.optJSONObject(n)); if(!u.isEmpty())found.add(new Episode("Episódio 1 · "+provider.optString("name","Fonte 2"),u,"SugoiAPI"));
                    }
                }
                if(found.isEmpty())errors.add("Fonte 2: sem URL de vídeo reconhecida");finish.run();
            });
        }
    }
    static void resolve(Episode episode, Net.Done<String> callback){
        if(!episode.provider.equals("api-animesonline-cc")){callback.accept(episode.url,null);return;}
        Net.json(episode.url,(json,error)->{
            if(error!=null){callback.accept(null,error);return;}
            String url=videoUrl(json);
            if(url.isEmpty())callback.accept(null,new IllegalStateException("A fonte não retornou MP4/HLS reconhecível."));
            else callback.accept(url,null);
        });
    }
    private static String clean(String s){return s.replaceAll("/+$","");}
    private static String id(JSONObject o){return o.optString("id",o.optString("anime_id",o.optString("episode_id","")));}
    private static JSONObject firstObject(JSONObject j,String...keys){
        if(j==null)return null;for(String key:keys){JSONArray a=j.optJSONArray(key);if(a!=null && a.length()>0)return a.optJSONObject(0);JSONObject o=j.optJSONObject(key);if(o!=null)return o;}
        return id(j).isEmpty()?null:j;
    }
    private static JSONArray findArray(JSONObject j,String...keys){if(j==null)return null;for(String k:keys){JSONArray a=j.optJSONArray(k);if(a!=null)return a;JSONObject o=j.optJSONObject(k);if(o!=null){JSONArray sub=findArray(o,"episodes","results","data");if(sub!=null)return sub;}}return null;}
    private static String videoUrl(JSONObject j){
        if(j==null)return "";
        for(String k:new String[]{"url","video","video_url","stream","episode","src","link","dub","sub"}){
            String s=j.optString(k,"");if(s.startsWith("https://") && (s.contains(".mp4")||s.contains(".m3u8")))return s;
            JSONObject o=j.optJSONObject(k);if(o!=null){String u=videoUrl(o);if(!u.isEmpty())return u;}
            JSONArray a=j.optJSONArray(k);if(a!=null)for(int i=0;i<a.length();i++){String u=videoUrl(a.optJSONObject(i));if(!u.isEmpty())return u;}
        }
        return "";
    }
}
