//
// Created by lzd on 18-8-28.
//

#ifndef MONITOR_SECRET_KEY_H
#define MONITOR_SECRET_KEY_H

#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_szu_lzd_monitor_utils_NativeInterface_serverKey(JNIEnv *env, jobject instance);


extern "C"
JNIEXPORT jstring JNICALL
Java_szu_lzd_monitor_utils_NativeInterface_clientKey(JNIEnv *env, jobject instance);


#endif //MONITOR_SECRET_KEY_H