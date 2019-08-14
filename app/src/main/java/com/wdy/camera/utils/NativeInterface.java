package com.wdy.camera.utils;

import android.graphics.Bitmap;
import android.graphics.Point;

public class NativeInterface {

    public static final int ALGORITHM_TYPE_HASH = 1;
    public static final int ALGORITHM_TYPE_CALC = 0;

    public native void crop(Bitmap inputBitmap, Point[] points, Bitmap outBitmap);

    /**
     * 对图像进行Canndy边缘检测
     *
     * @param matAddr 灰度图像的Mat地址
     */
    public native void CannyDetect(long matAddr);

    public native void init();

    /**
     * algorithmType:
     * const int ALGORITHM_TYPE_HASH = 1;
     * const int ALGORITHM_TYPE_CALC = 0;
     *
     * @param oldBitmap
     * @param newBitmap
     * @param algorithmType
     * @return
     */
    public native double contrast(Bitmap oldBitmap, Bitmap newBitmap, int algorithmType);


    public native void scan(Bitmap srcBitmap, Point[] outPoint);

    public static NativeInterface getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;

        private NativeInterface singleton;

        //JVM会保证此方法绝对只调用一次
        Singleton() {
            singleton = new NativeInterface();
        }

        public NativeInterface getInstance() {
            return singleton;
        }
    }

}
