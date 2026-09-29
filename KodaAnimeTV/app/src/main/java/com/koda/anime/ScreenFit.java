package com.koda.anime;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Gravity;
import android.widget.FrameLayout;

/**
 * Safe responsive layout for TV.
 * Keeps the stable display API used by the working build, but scales from a 1280x720
 * design and uses almost the whole real screen instead of a fixed 1000x562 canvas.
 */
final class ScreenFit {
    static final float DESIGN_W=1280f, DESIGN_H=720f;
    final float pixelsPerUnit;
    final int width,height,screenWidth,screenHeight;

    ScreenFit(Activity activity){
        Point bounds=new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(bounds);
        screenWidth=Math.max(1,bounds.x);
        screenHeight=Math.max(1,bounds.y);
        boolean tv=(activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
                || !activity.getPackageManager().hasSystemFeature("android.hardware.touchscreen");

        float safe=tv?.965f:.99f;
        width=Math.round(screenWidth*safe);
        height=Math.round(screenHeight*safe);
        pixelsPerUnit=Math.min(width/DESIGN_W,height/DESIGN_H);
    }

    int px(float units){return Math.max(1,Math.round(units*pixelsPerUnit));}

    float textScale(Activity activity){
        return pixelsPerUnit/activity.getResources().getDisplayMetrics().density;
    }

    FrameLayout.LayoutParams centered(){
        return new FrameLayout.LayoutParams(width,height,Gravity.CENTER);
    }

    int posterWidth(int availablePx){
        int gap=px(14);
        float count=availablePx>=px(1050)?5.8f:availablePx>=px(820)?5.25f:4.65f;
        return Math.max(px(112),Math.round((availablePx-gap*(count-1))/count));
    }
}