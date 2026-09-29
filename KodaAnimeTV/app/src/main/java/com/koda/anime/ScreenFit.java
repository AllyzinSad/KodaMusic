package com.koda.anime;

import android.app.Activity;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

final class ScreenFit {
    static final float DESIGN_W=1000f, DESIGN_H=562.5f;
    final float pixelsPerUnit;
    final int width,height,screenWidth,screenHeight,marginX,marginY;

    ScreenFit(Activity activity){
        Point p=new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(p);
        screenWidth=Math.max(1,p.x);
        screenHeight=Math.max(1,p.y);
        boolean tv=!activity.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
        float safeX=tv?.022f:.008f;
        float safeY=tv?.026f:.008f;
        marginX=Math.round(screenWidth*safeX);
        marginY=Math.round(screenHeight*safeY);
        width=Math.max(1,screenWidth-marginX*2);
        height=Math.max(1,screenHeight-marginY*2);
        pixelsPerUnit=Math.min(width/DESIGN_W,height/DESIGN_H);
    }

    static void immersive(Activity activity){
        activity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    int px(float units){return Math.round(units*pixelsPerUnit);}
    float textScale(Activity activity){return pixelsPerUnit/activity.getResources().getDisplayMetrics().density;}
    FrameLayout.LayoutParams centered(){return new FrameLayout.LayoutParams(width,height,Gravity.CENTER);}

    int recommendedCardWidth(int availablePx){
        int gap=Math.max(10,px(12));
        float visible=width>=px(1150)?6.15f:width>=px(900)?5.45f:4.65f;
        int usable=Math.max(px(500),availablePx);
        return Math.max(px(92),Math.round((usable-gap*(visible-1))/visible));
    }
}