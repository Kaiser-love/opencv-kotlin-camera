package com.wdy.camera.constant.preferences;

import com.wdy.camera.constant.enums.FabSortItem;

import java.util.LinkedList;
import java.util.List;

public class UserPreferences {

    private static UserPreferences instance = new UserPreferences();

    private static List<FabSortItem> defaultFabOrders;

    public static UserPreferences getInstance() {
        return instance;
    }

    static {
        defaultFabOrders = new LinkedList<>();
        defaultFabOrders.add(FabSortItem.CAMERA);
        defaultFabOrders.add(FabSortItem.SCAN);
    }

    private UserPreferences() {
    }

    public List<FabSortItem> getFabSortResult() {
        return defaultFabOrders;
    }

}
