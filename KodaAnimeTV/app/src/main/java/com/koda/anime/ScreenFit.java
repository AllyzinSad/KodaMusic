package com.koda.anime;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Gravity;
import android.widget.FrameLayout;

/** Fits a 1000 x 562.5 design into the available landscape window. */
final class ScreenFit {
    final float pixelsPerUnit;
    final int width, height;
    ScreenFit(Activity activity) {
        Point bounds = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(bounds);
        boolean television = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
                || !activity.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
        float safeFraction = television ? .94f : .98f;
        pixelsPerUnit = Math.min(bounds.x * safeFraction / 1000f,
                bounds.y * safeFraction / 562.5f);
        width = Math.round(1000 * pixelsPerUnit);
        height = Math.round(562.5f * pixelsPerUnit);
    }
    int px(float units) { return Math.round(units * pixelsPerUnit); }
    float textScale(Activity activity) {
        return pixelsPerUnit / activity.getResources().getDisplayMetrics().density;
    }
    FrameLayout.LayoutParams centered() {
        return new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
    }
}
