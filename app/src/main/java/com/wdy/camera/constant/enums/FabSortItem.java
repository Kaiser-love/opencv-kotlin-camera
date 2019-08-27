package com.wdy.camera.constant.enums;



import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.wdy.camera.R;


public enum FabSortItem {
    CAMERA(R.string.fab_camera, R.drawable.ic_description_black_24dp),
    SCAN(R.string.fab_scanbot, R.drawable.ic_description_black_24dp);

    @StringRes
    public final int nameRes;

    @DrawableRes
    public final int iconRes;

    FabSortItem(int nameRes, @DrawableRes int iconRes) {
        this.nameRes = nameRes;
        this.iconRes = iconRes;
    }
}
