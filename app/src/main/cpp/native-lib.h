//
// Created by lizhengda on 2018/6/30.
//

#ifndef MONITOR_NATIVE_LIB_H
#define MONITOR_NATIVE_LIB_H

#include <jni.h>
#include "PicUtils.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_wdy_camera_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */);

//extern "C"
//JNIEXPORT void JNICALL
//Java_szu_lzd_monitor_utils_NativeInterface_crop(JNIEnv *env, jobject instance, jlong inputBitmap,
//                                                jobjectArray points, jlong outBitmap) ;
extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_init(JNIEnv *env, jobject instance);
#endif //MONITOR_NATIVE_LIB_H

extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_crop(JNIEnv *env, jobject instance, jobject inputBitmap,
                                                jobjectArray points, jobject outBitmap);

JNIEXPORT jdouble JNICALL
Java_com_wdy_camera_utils_NativeInterface_contrast(JNIEnv *env, jobject instance,
                                                    jobject oldBitmap, jobject newBitmap,
                                                    jint algorithmType);

extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_scan(JNIEnv *env, jobject instance, jobject srcBitmap,
                                                jobjectArray outPoint);

