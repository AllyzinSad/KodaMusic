package com.koda.anime;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Net {
    interface Done<T> { void accept(T value, Exception error); }
    static final ExecutorService WORK = Executors.newFixedThreadPool(4);
    static final Handler UI = new Handler(Looper.getMainLooper());
    private static final java.util.concurrent.ThreadPoolExecutor IMAGES = new java.util.concurrent.ThreadPoolExecutor(3,3,0L,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.LinkedBlockingQueue<Runnable>(120),new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
    private static final android.util.LruCache<String,Bitmap> IMAGE_CACHE = new android.util.LruCache<String,Bitmap>(8*1024*1024){@Override protected int sizeOf(String key,Bitmap value){return value.getByteCount();}};
    private static final java.util.Map<String,Pending> PENDING = new java.util.HashMap<>();
    private static final java.util.LinkedHashMap<String,Cached> CACHE = new java.util.LinkedHashMap<>();
    private static final class Cached { Object value; long time; Cached(Object v){value=v;time=System.currentTimeMillis();} }
    private static final class Pending { java.util.List<Done<Object>> callbacks=new java.util.ArrayList<>(); Runnable timeout; java.util.concurrent.Future<?> work; }
    private static volatile CatalogDiskCache diskCache;
    static void initialize(android.content.Context context){if(diskCache==null)diskCache=new CatalogDiskCache(new File(context.getApplicationContext().getFilesDir(),"catalog-cache"));}
    static void clearImageQueue(){IMAGES.getQueue().clear();}
    static void json(String url, Done<JSONObject> done){get(url,false,(v,e)->done.accept((JSONObject)v,e));}
    static void jsonArray(String url, Done<JSONArray> done){get(url,true,(v,e)->done.accept((JSONArray)v,e));}
    private static synchronized void get(String url,boolean array,Done<Object> done){
        String key=(array?"array:":"object:")+url;
        Cached cached=CACHE.get(key);
        if(cached!=null && System.currentTimeMillis()-cached.time<300000){UI.post(()->done.accept(cached.value,null));return;}
        Pending existing=PENDING.get(key);
        if(existing!=null){existing.callbacks.add(done);return;}
        Pending pending=new Pending();pending.callbacks.add(done);PENDING.put(key,pending);
        pending.timeout=()->{finish(key,pending,null,new IOException("A conexão demorou demais. Tente novamente."));if(pending.work!=null)pending.work.cancel(true);};
        UI.postDelayed(pending.timeout,35000);
        pending.work=WORK.submit(()->{
            CatalogDiskCache disk=diskCache;
            Object fallback=null;
            if(disk!=null){
                long now=System.currentTimeMillis();
                String fresh=disk.read(url,3600000,now);
                try { if(fresh!=null){Object value=parse(fresh,array);UI.post(()->finish(key,pending,value,null));return;} }
                catch(Exception ignored) { /* Invalid local data must not prevent a network retry. */ }
                String stale=disk.read(url,7L*24*3600000,now);
                try {if(stale!=null)fallback=parse(stale,array);}catch(Exception ignored){}
            }
            final Object offline=fallback;
            try {
                String body=request(url,null);Object value=parse(body,array);
                if(disk!=null)disk.write(url,body);
                UI.post(()->finish(key,pending,value,null));
            } catch(Exception error){UI.post(()->finish(key,pending,offline,offline==null?error:null));}
        });
    }
    private static Object parse(String body,boolean array) throws org.json.JSONException {
        return array?new JSONArray(body):new JSONObject(body);
    }

    private static synchronized void finish(String key,Pending pending,Object value,Exception error){
        if(PENDING.get(key)!=pending)return;
        PENDING.remove(key);UI.removeCallbacks(pending.timeout);
        if(value!=null){CACHE.put(key,new Cached(value));while(CACHE.size()>8)CACHE.remove(CACHE.keySet().iterator().next());}
        for(Done<Object> callback:pending.callbacks)callback.accept(value,error);
        pending.callbacks.clear();
    }
    static void post(String url, String body, Done<JSONObject> done) { WORK.execute(() -> {
        try { JSONObject value = new JSONObject(request(url, body)); UI.post(() -> done.accept(value, null)); }
        catch (Exception e) { UI.post(() -> done.accept(null, e)); }
    }); }
    static void jsonBearer(String url, String token, Done<JSONObject> done) { WORK.execute(() -> {
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(12000);
            c.setRequestProperty("Authorization","Bearer "+token);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
                JSONObject value=new JSONObject(out.toString("UTF-8"));UI.post(()->done.accept(value,null));
            }
        } catch(Exception e){UI.post(()->done.accept(null,e));}
        finally{if(c!=null)c.disconnect();}
    }); }
    static String request(String url, String body) throws Exception {
        URL u = new URL(url); if (!"https".equalsIgnoreCase(u.getProtocol())) throw new IOException("Configure uma URL HTTPS.");
        HttpURLConnection c = (HttpURLConnection) u.openConnection(); c.setConnectTimeout(10000); c.setReadTimeout(25000);
        c.setRequestProperty("User-Agent", "KodaAnimeTV/0.10 (Android)");
        if (body != null) { c.setRequestMethod("POST"); c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));} }
        InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new IOException("HTTP " + c.getResponseCode());
        try (InputStream stream=in; ByteArrayOutputStream bytes=new ByteArrayOutputStream()) { byte[] b=new byte[8192]; int n; while((n=stream.read(b))!=-1) {bytes.write(b,0,n); if(bytes.size()>8_000_000) throw new IOException("Resposta muito grande");} return bytes.toString("UTF-8"); }
        finally { c.disconnect(); }
    }
    static String enc(String s) { try { return URLEncoder.encode(s, "UTF-8"); } catch(Exception e){return s;} }
    static void image(String url, ImageView view) {
        if (url == null || !url.startsWith("https://")) return;
        view.setTag(url);
        Bitmap cached=IMAGE_CACHE.get(url);if(cached!=null){view.setImageBitmap(cached);return;}
        IMAGES.execute(() -> { try {
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(7000); c.setReadTimeout(8000);
            try(InputStream in=c.getInputStream()) { BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=2;Bitmap b=BitmapFactory.decodeStream(in,null,opts);if(b!=null)IMAGE_CACHE.put(url,b); UI.post(() -> {if(url.equals(view.getTag()) && b!=null) view.setImageBitmap(b);}); }
            finally { c.disconnect(); }
        } catch(Exception ignored) {} });
    }
}
