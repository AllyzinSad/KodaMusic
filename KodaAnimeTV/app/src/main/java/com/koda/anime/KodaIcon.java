package com.koda.anime;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Resolution-independent Koda UI icons. No raster scaling, no sprite sheets. */
final class KodaIcon extends Drawable {
    static final String PLAY="play", PAUSE="pause", REWIND10="rewind10", FORWARD10="forward10",
        NEXT="next", SKIP="skip", QUALITY="quality", AUDIO="audio", SUBTITLES="subtitles", INFO="info";

    private final String kind;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color;

    KodaIcon(String kind,int color){this.kind=kind;this.color=color;}

    @Override public void draw(Canvas c){
        Rect b=getBounds();float w=b.width(),h=b.height(),s=Math.min(w,h);
        c.save();c.translate(b.left,b.top);c.scale(w/100f,h/100f);
        p.setColor(color);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);

        if(PLAY.equals(kind)){
            p.setStyle(Paint.Style.FILL);Path q=new Path();q.moveTo(35,22);q.lineTo(77,50);q.lineTo(35,78);q.close();c.drawPath(q,p);
        }else if(PAUSE.equals(kind)){
            p.setStyle(Paint.Style.FILL);c.drawRoundRect(29,22,43,78,5,5,p);c.drawRoundRect(57,22,71,78,5,5,p);
        }else if(REWIND10.equals(kind)||FORWARD10.equals(kind)){
            boolean f=FORWARD10.equals(kind);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);
            RectF arc=new RectF(22,24,78,80);
            c.drawArc(arc,f?215:-35,285,false,p);
            p.setStyle(Paint.Style.FILL);Path a=new Path();
            if(f){a.moveTo(77,20);a.lineTo(91,34);a.lineTo(72,38);a.close();}
            else{a.moveTo(23,20);a.lineTo(9,34);a.lineTo(28,38);a.close();}
            c.drawPath(a,p);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));p.setTextSize(31);c.drawText("10",50,62,p);
        }else if(NEXT.equals(kind)){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawRoundRect(18,24,82,76,6,6,p);
            p.setStyle(Paint.Style.FILL);Path q=new Path();q.moveTo(39,35);q.lineTo(65,50);q.lineTo(39,65);q.close();c.drawPath(q,p);
        }else if(SKIP.equals(kind)){
            p.setStyle(Paint.Style.FILL);
            Path a=new Path();a.moveTo(20,28);a.lineTo(49,50);a.lineTo(20,72);a.close();c.drawPath(a,p);
            Path d=new Path();d.moveTo(50,28);d.lineTo(79,50);d.lineTo(50,72);d.close();c.drawPath(d,p);
            c.drawRoundRect(82,27,89,73,3,3,p);
        }else if(QUALITY.equals(kind)){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);
            c.drawCircle(50,50,18,p);c.drawCircle(50,50,5,p);
            for(int i=0;i<8;i++){c.save();c.rotate(i*45,50,50);c.drawLine(50,14,50,27,p);c.restore();}
        }else if(AUDIO.equals(kind)){
            p.setStyle(Paint.Style.FILL);Path q=new Path();q.moveTo(18,42);q.lineTo(34,42);q.lineTo(52,27);q.lineTo(52,73);q.lineTo(34,58);q.lineTo(18,58);q.close();c.drawPath(q,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawArc(new RectF(48,30,78,70),-50,100,false,p);c.drawArc(new RectF(45,18,92,82),-45,90,false,p);
        }else if(SUBTITLES.equals(kind)){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawRoundRect(16,22,84,72,7,7,p);
            c.drawLine(29,42,47,42,p);c.drawLine(55,42,72,42,p);c.drawLine(29,57,45,57,p);c.drawLine(53,57,72,57,p);
            Path q=new Path();q.moveTo(58,72);q.lineTo(69,84);q.lineTo(68,72);c.drawPath(q,p);
        }else if(INFO.equals(kind)){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawCircle(50,50,31,p);
            p.setStyle(Paint.Style.FILL);c.drawCircle(50,34,4,p);c.drawRoundRect(46,44,54,69,4,4,p);
        }
        c.restore();
    }

    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
