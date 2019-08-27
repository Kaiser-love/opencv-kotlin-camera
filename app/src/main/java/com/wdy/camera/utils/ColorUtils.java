package com.wdy.camera.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.graphics.drawable.DrawableCompat;


/**
 * @author shouh
 * @version $Id: ColorUtils, v 0.1 2018/6/6 22:14 shouh Exp$
 */
public class ColorUtils {
    public static Drawable tintDrawable(Context context, @DrawableRes int drawableRes, @ColorInt int color) {
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        final Drawable wrappedDrawable = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTintList(wrappedDrawable, ColorStateList.valueOf(color));
        return wrappedDrawable;
    }
}
