package com.koda.anime;

import android.content.*;
import android.net.Uri;
import android.widget.Toast;
import androidx.media3.common.C;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.offline.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class AnimeDownloads {
    static final class Item {
        final String id,animeId,title,episode,url;
        Item(String id,String animeId,String title,String episode,String url){this.id=id;this.animeId=animeId;this.title=title;this.episode=episode;this.url=url;}
    }

    private static DatabaseProvider db; private static SimpleCache cache; private static DownloadManager manager;

    static synchronized DownloadManager manager(Context c){
        Context app=c.getApplicationContext();
        if(db==null)db=new StandaloneDatabaseProvider(app);
        if(cache==null)cache=new SimpleCache(new File(app.getFilesDir(),"anime-downloads"),new NoOpCacheEvictor(),db);
        if(manager==null){
            DataSource.Factory upstream=new DefaultHttpDataSource.Factory().setUserAgent("KodaAnimeAndroid/0.1");
            manager=new DownloadManager(app,db,cache,upstream,java.util.concurrent.Executors.newFixedThreadPool(3));
            manager.setMaxParallelDownloads(2);manager.resumeDownloads();
        }
        return manager;
    }

    static synchronized DataSource.Factory dataSourceFactory(Context c){
        manager(c);
        return new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(new DefaultHttpDataSource.Factory().setUserAgent("KodaAnimeAndroid/0.1")).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    static void enqueue(Context c,Anime anime,Anime ep){
        if(ep.playUrl==null||!ep.playUrl.startsWith("https://")){Toast.makeText(c,"Link de download inválido.",Toast.LENGTH_SHORT).show();return;}
        String animeId=Catalog.normalize(anime.title.replaceAll("(?i)\\s*\\(dublado\\)\\s*$",""));
        String id=animeId+"-"+Math.max(1,Catalog.episodeNumber(ep.description));
        saveMeta(c,new Item(id,animeId,anime.title,ep.description,ep.playUrl));
        DownloadRequest request=new DownloadRequest.Builder(id,Uri.parse(ep.playUrl)).setData((anime.title+" · "+ep.description).getBytes(StandardCharsets.UTF_8)).build();
        AnimeDownloadService.add(c,request);
        Toast.makeText(c,"Download adicionado.",Toast.LENGTH_SHORT).show();
    }

    static void remove(Context c,String id){AnimeDownloadService.remove(c,id);c.getSharedPreferences("anime_downloads",0).edit().remove(id).apply();}

    static List<Item> items(Context c){
        ArrayList<Item> out=new ArrayList<>();
        for(Map.Entry<String,?> e:c.getSharedPreferences("anime_downloads",0).getAll().entrySet()){
            try{org.json.JSONObject j=new org.json.JSONObject(String.valueOf(e.getValue()));out.add(new Item(e.getKey(),j.optString("animeId"),j.optString("title"),j.optString("episode"),j.optString("url")));}catch(Exception ignored){}
        }
        out.sort((a,b)->a.title.equalsIgnoreCase(b.title)?Catalog.episodeNumber(a.episode)-Catalog.episodeNumber(b.episode):a.title.compareToIgnoreCase(b.title));return out;
    }

    static String statusLabel(Context c,String id){
        try{
            Download d=manager(c).getDownloadIndex().getDownload(id);
            if(d==null)return "Aguardando";
            switch(d.state){
                case Download.STATE_COMPLETED:return "Concluído";
                case Download.STATE_DOWNLOADING:return d.getPercentDownloaded()>=0?Math.round(d.getPercentDownloaded())+"%":"Baixando";
                case Download.STATE_QUEUED:return "Na fila";
                case Download.STATE_FAILED:return "Falhou";
                case Download.STATE_STOPPED:return "Pausado";
                case Download.STATE_REMOVING:return "Removendo";
                case Download.STATE_RESTARTING:return "Reiniciando";
                default:return "Aguardando";
            }
        }catch(Exception e){return "Aguardando";}
    }

    private static void saveMeta(Context c,Item i){
        try{org.json.JSONObject j=new org.json.JSONObject();j.put("animeId",i.animeId);j.put("title",i.title);j.put("episode",i.episode);j.put("url",i.url);c.getSharedPreferences("anime_downloads",0).edit().putString(i.id,j.toString()).apply();}catch(Exception ignored){}
    }
    private AnimeDownloads(){}
}
