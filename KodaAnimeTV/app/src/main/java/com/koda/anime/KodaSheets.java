package com.koda.anime;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.ArrayDeque;

final class KodaSheets {
    static Drawable pair(Context c,int resId,boolean focused){
        Bitmap src=BitmapFactory.decodeResource(c.getResources(),resId);
        if(src==null)return null;
        Bitmap crop;
        if(src.getWidth()>=src.getHeight()*2){
            int half=src.getWidth()/2;
            int x=focused?half:0;
            crop=Bitmap.createBitmap(src,x,0,focused?src.getWidth()-half:half,src.getHeight());
        }else{
            int half=src.getHeight()/2;
            int y=focused?half:0;
            crop=Bitmap.createBitmap(src,0,y,src.getWidth(),focused?src.getHeight()-half:half);
        }
        return new BitmapDrawable(c.getResources(),cleanAndTrim(crop));
    }

    static Drawable single(Context c,int resId){
        Bitmap src=BitmapFactory.decodeResource(c.getResources(),resId);
        return src==null?null:new BitmapDrawable(c.getResources(),cleanAndTrim(src.copy(Bitmap.Config.ARGB_8888,true)));
    }

    private static Bitmap cleanAndTrim(Bitmap input){
        Bitmap b=input.getConfig()==Bitmap.Config.ARGB_8888&&input.isMutable()?input:input.copy(Bitmap.Config.ARGB_8888,true);
        int w=b.getWidth(),h=b.getHeight(),n=w*h;
        int[] px=new int[n];b.getPixels(px,0,w,0,0,w,h);
        boolean[] seen=new boolean[n];ArrayDeque<Integer> q=new ArrayDeque<>();
        for(int x=0;x<w;x++){seed(px,seen,q,x,w,h);seed(px,seen,q,(h-1)*w+x,w,h);}
        for(int y=0;y<h;y++){seed(px,seen,q,y*w,w,h);seed(px,seen,q,y*w+w-1,w,h);}
        while(!q.isEmpty()){
            int i=q.removeFirst(),x=i%w,y=i/w;
            if(x>0)seed(px,seen,q,i-1,w,h);if(x+1<w)seed(px,seen,q,i+1,w,h);
            if(y>0)seed(px,seen,q,i-w,w,h);if(y+1<h)seed(px,seen,q,i+w,w,h);
        }
        int minX=w,minY=h,maxX=-1,maxY=-1;
        for(int i=0;i<n;i++){
            if(seen[i])px[i]&=0x00FFFFFF;
            if(Color.alpha(px[i])>0){int x=i%w,y=i/w;if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;}
        }
        b.setPixels(px,0,w,0,0,w,h);
        if(maxX<minX||maxY<minY)return b;
        int pad=2;minX=Math.max(0,minX-pad);minY=Math.max(0,minY-pad);maxX=Math.min(w-1,maxX+pad);maxY=Math.min(h-1,maxY+pad);
        return Bitmap.createBitmap(b,minX,minY,maxX-minX+1,maxY-minY+1);
    }

    private static void seed(int[] px,boolean[] seen,ArrayDeque<Integer> q,int i,int w,int h){
        if(i<0||i>=px.length||seen[i])return;
        int c=px[i],r=Color.red(c),g=Color.green(c),b=Color.blue(c);
        int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
        if(max<42&&max-min<20){seen[i]=true;q.addLast(i);}
    }
    private KodaSheets(){}
}
