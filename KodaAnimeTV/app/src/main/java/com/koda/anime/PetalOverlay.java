package com.koda.anime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Decorative red leaves/petals used in the approved Koda Anime TV visual language. */
final class PetalOverlay extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    PetalOverlay(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);paint.setStyle(Paint.Style.FILL);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        drawPetal(c,w*.965f,h*.08f,18,10,-28,0xAAFF1635);
        drawPetal(c,w*.925f,h*.135f,12,7,24,0x88D90025);
        drawPetal(c,w*.985f,h*.23f,16,9,-12,0x99FF1635);
        drawPetal(c,w*.94f,h*.88f,19,10,32,0x99FF1635);
        drawPetal(c,w*.985f,h*.80f,13,8,-36,0x77C50022);
        drawPetal(c,w*.035f,h*.91f,22,11,-22,0x88FF1635);
        drawPetal(c,w*.075f,h*.965f,14,8,38,0x66B90020);
        drawPetal(c,w*.16f,h*.055f,11,6,18,0x77FF1635);
    }
    private void drawPetal(Canvas c,float x,float y,float rx,float ry,float rotation,int color){
        c.save();c.translate(x,y);c.rotate(rotation);paint.setColor(color);
        Path p=new Path();p.moveTo(-rx,0);p.quadTo(0,-ry,rx,0);p.quadTo(0,ry,-rx,0);p.close();c.drawPath(p,paint);c.restore();
    }
}