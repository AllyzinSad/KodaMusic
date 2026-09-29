package com.koda.anime;
import android.graphics.*;
import android.graphics.drawable.Drawable;

final class NavIcon extends Drawable {
    private final String name; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private int color;
    NavIcon(String name,int color){this.name=name;this.color=color;}
    void setTintColor(int c){color=c;invalidateSelf();}
    @Override public void draw(Canvas canvas){canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        Path path=new Path();
        if(name.equals("Início")){path.moveTo(2,11);path.lineTo(12,2);path.lineTo(22,11);path.moveTo(5,9);path.lineTo(5,22);path.lineTo(10,22);path.lineTo(10,15);path.lineTo(14,15);path.lineTo(14,22);path.lineTo(19,22);path.lineTo(19,9);canvas.drawPath(path,p);}
        else if(name.equals("Lançados")){path.moveTo(12,2);path.lineTo(14.5f,8.5f);path.lineTo(21,11);path.lineTo(14.5f,13.5f);path.lineTo(12,20);path.lineTo(9.5f,13.5f);path.lineTo(3,11);path.lineTo(9.5f,8.5f);path.close();canvas.drawPath(path,p);}
        else if(name.equals("Recomendados")){path.moveTo(12,22);path.cubicTo(3,18,5,10,9,7);path.cubicTo(10,5,10,3,10,2);path.cubicTo(16,5,20,10,19,15);path.cubicTo(18,20,15,22,12,22);canvas.drawPath(path,p);}
        else if(name.equals("Pesquisa")){canvas.drawCircle(10,10,7,p);canvas.drawLine(15,15,22,22,p);}
        else if(name.equals("Favoritos")){path.moveTo(12,21);path.cubicTo(-6,9,4,-3,12,6);path.cubicTo(20,-3,30,9,12,21);canvas.drawPath(path,p);}
        else if(name.equals("Histórico")){canvas.drawCircle(12,12,9,p);canvas.drawLine(12,6,12,12,p);canvas.drawLine(12,12,17,15,p);}
        else {canvas.drawCircle(12,12,6,p);canvas.drawCircle(12,12,2,p);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,12,12);canvas.drawLine(12,2,12,5,p);canvas.restore();}}
        canvas.restore();}
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
