package com.koda.anime;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Vector decorative foliage. Scales cleanly on 1080p/4K and never uses a rectangular bitmap. */
final class KodaDecorView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vein=new Paint(Paint.ANTI_ALIAS_FLAG);

    KodaDecorView(Context c){
        super(c);setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        vein.setStyle(Paint.Style.STROKE);vein.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
        p.setShader(new LinearGradient(w*.58f,0,w,0,0x00FF1635,0x7FFF1635,Shader.TileMode.CLAMP));p.setStrokeWidth(Math.max(2,w*.0014f));
        Path ribbon=new Path();ribbon.moveTo(w*.63f,h*.025f);ribbon.cubicTo(w*.76f,h*.07f,w*.86f,h*.005f,w*.98f,h*.055f);c.drawPath(ribbon,p);p.setShader(null);

        drawCluster(c,w*.94f,h*.08f,1f,-18);
        drawCluster(c,w*.055f,h*.90f,.82f,158);
        drawPetal(c,w*.90f,h*.18f,w*.012f,w*.007f,-24,0x99FF1635);
        drawPetal(c,w*.955f,h*.23f,w*.009f,w*.005f,28,0x77FF1635);
        drawPetal(c,w*.10f,h*.83f,w*.010f,w*.006f,-32,0x66FF1635);
    }

    private void drawCluster(Canvas c,float x,float y,float scale,float rotation){
        c.save();c.translate(x,y);c.rotate(rotation);
        float base=Math.min(getWidth(),getHeight())*.035f*scale;
        for(int i=0;i<11;i++){
            float a=(i-5)*16f;float r=base*(1.0f+(i%3)*.58f);
            double rad=Math.toRadians(a-90);
            float px=(float)Math.cos(rad)*r*.9f,py=(float)Math.sin(rad)*r;
            drawPetal(c,px,py,base*.54f,base*.22f,a+(i%2==0?18:-12),0xAAFF1635);
        }
        for(int i=0;i<6;i++){
            float px=-base*.35f*i,py=base*.42f*i;
            drawPetal(c,px,py,base*.48f,base*.20f,-35+i*9,0x88D60028);
        }
        c.restore();
    }

    private void drawPetal(Canvas c,float x,float y,float rx,float ry,float rotation,int color){
        c.save();c.translate(x,y);c.rotate(rotation);
        p.setShader(new LinearGradient(-rx,0,rx,0,0xFF760014,color,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);
        Path q=new Path();q.moveTo(-rx,0);q.cubicTo(-rx*.2f,-ry*1.6f,rx*.55f,-ry,rx,0);q.cubicTo(rx*.35f,ry*1.3f,-rx*.4f,ry*1.1f,-rx,0);q.close();c.drawPath(q,p);p.setShader(null);
        vein.setColor(0x99FF4962);vein.setStrokeWidth(Math.max(1,rx*.045f));c.drawLine(-rx*.55f,0,rx*.55f,0,vein);
        c.restore();
    }
}
