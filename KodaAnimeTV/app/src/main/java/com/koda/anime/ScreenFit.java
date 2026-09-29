package com.koda.anime;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowMetrics;
import android.widget.FrameLayout;

/**
 * Responsive TV layout helper.
 * Uses the real usable display size instead of forcing a fixed 16:9 virtual canvas.
 * Design units still keep typography/spacing proportional, while the container expands
 * to the actual screen (including ultrawide and unusual TV resolutions).
 */
final class ScreenFit {
    static final float DESIGN_W=1000f, DESIGN_H=562.5f;
    final float pixelsPerUnit;
    final int width,height,screenWidth,screenHeight,marginX,marginY;

    ScreenFit(Activity activity) {
        int sw,sh;
        if(Build.VERSION.SDK_INT>=30){
            WindowMetrics metrics=activity.getWindowManager().getCurrentWindowMetrics();
            Rect b=metrics.getBounds(); sw=b.width(); sh=b.height();
        }else{
            Point p=new Point();activity.getWindowManager().getDefaultDisplay().getSize(p);sw=p.x;sh=p.y;
        }
        screenWidth=sw;screenHeight=sh;
        boolean tv=!activity.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
        float safeX=tv?.022f:.008f;
        float safeY=tv?.026f:.008f;
        marginX=Math.round(sw*safeX);marginY=Math.round(sh*safeY);
        width=Math.max(1,sw-marginX*2);height=Math.max(1,sh-marginY*2);
        pixelsPerUnit=Math.min(width/DESIGN_W,height/DESIGN_H);
    }

    static void immersive(Activity activity){
        if(Build.VERSION.SDK_INT>=30){
            activity.getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c=activity.getWindow().getInsetsController();
            if(c!=null){
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }else{
            activity.getWindow().getDecorView().setSystemUiVisibility(
                5894|android.view.View.SYSTEM_UI_FLAG_FULLSCREEN|
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    int px(float units){return Math.round(units*pixelsPerUnit);}
    float textScale(Activity activity){return pixelsPerUnit/activity.getResources().getDisplayMetrics().density;}
    FrameLayout.LayoutParams centered(){return new FrameLayout.LayoutParams(width,height,Gravity.CENTER);}

    int recommendedCardWidth(int availablePx){
        int gap=Math.max(10,px(14));
        float visible=width>=px(1150)?6.0f:width>=px(900)?5.45f:4.75f;
        int usable=Math.max(px(500),availablePx);
        return Math.max(px(96),Math.round((usable-gap*(visible-1))/visible));
    }

    int railHeight(int availablePx){
        int card=recommendedCardWidth(availablePx);
        return Math.round(card*1.50f)+px(44);
    }
}