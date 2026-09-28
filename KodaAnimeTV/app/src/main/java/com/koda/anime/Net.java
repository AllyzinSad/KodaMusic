package com.koda.anime;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Net {
    interface Done<T> { void accept(T value, Exception error); }
    static final ExecutorService WORK = Executors.newFixedThreadPool(4);
    static final Handler UI = new Handler(Looper.getMainLooper());
    static void json(String url, Done<JSONObject> done) { WORK.execute(() -> {
        try { JSONObject value = new JSONObject(request(url, null)); UI.post(() -> done.accept(value, null)); }
        catch (Exception e) { UI.post(() -> done.accept(null, e)); }
    }); }
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
        HttpURLConnection c = (HttpURLConnection) u.openConnection(); c.setConnectTimeout(9000); c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "KodaAnimeTV/0.1 (Android TV)");
        if (body != null) { c.setRequestMethod("POST"); c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));} }
        InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new IOException("HTTP " + c.getResponseCode());
        try (InputStream stream=in; ByteArrayOutputStream bytes=new ByteArrayOutputStream()) { byte[] b=new byte[8192]; int n; while((n=stream.read(b))!=-1) {bytes.write(b,0,n); if(bytes.size()>2_000_000) throw new IOException("Resposta muito grande");} return bytes.toString("UTF-8"); }
        finally { c.disconnect(); }
    }
    static String enc(String s) { try { return URLEncoder.encode(s, "UTF-8"); } catch(Exception e){return s;} }
    static void image(String url, ImageView view) {
        if (url == null || !url.startsWith("https://")) return;
        view.setTag(url);
        WORK.execute(() -> { try {
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(7000); c.setReadTimeout(8000);
            try(InputStream in=c.getInputStream()) { Bitmap b=BitmapFactory.decodeStream(in); UI.post(() -> {if(url.equals(view.getTag()) && b!=null) view.setImageBitmap(b);}); }
            finally { c.disconnect(); }
        } catch(Exception ignored) {} });
    }
}
