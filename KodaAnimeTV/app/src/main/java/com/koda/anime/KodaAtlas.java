package com.koda.anime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Loads the approved Koda UI/player sheets from small text chunks in assets. */
final class KodaAtlas {
    private static Bitmap PLAYER, UI;

    private static Bitmap load(Context context,String... files){
        StringBuilder encoded=new StringBuilder();
        byte[] buffer=new byte[4096];
        try{
            for(String file:files){
                try(InputStream in=context.getAssets().open("koda/"+file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                    encoded.append(out.toString("UTF-8").replaceAll("\\s+",""));
                }
            }
            byte[] bytes=Base64.decode(encoded.toString(),Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }catch(Exception ignored){return null;}
    }

    static Bitmap player(Context c){
        if(PLAYER==null)PLAYER=load(c,"v3_player_00.b64","v3_player_01.b64","v3_player_02.b64");
        return PLAYER;
    }
    static Bitmap ui(Context c){
        if(UI==null)UI=load(c,"v3_ui_00.b64","v3_ui_01.b64","v3_ui_02.b64");
        return UI;
    }

    static Drawable crop(Context c,Bitmap atlas,int x,int y,int w,int h){
        if(atlas==null)return null;
        x=Math.max(0,Math.min(x,atlas.getWidth()-1));y=Math.max(0,Math.min(y,atlas.getHeight()-1));
        w=Math.max(1,Math.min(w,atlas.getWidth()-x));h=Math.max(1,Math.min(h,atlas.getHeight()-y));
        return new BitmapDrawable(c.getResources(),Bitmap.createBitmap(atlas,x,y,w,h));
    }
    static Drawable player(Context c,int x,int y,int w,int h){return crop(c,player(c),x,y,w,h);}
    static Drawable ui(Context c,int x,int y,int w,int h){return crop(c,ui(c),x,y,w,h);}
    private KodaAtlas(){}
}
