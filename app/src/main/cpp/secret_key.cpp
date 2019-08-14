//
// Created by lzd on 18-8-28.
//

#include <string>
#include "secret_key.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_szu_lzd_monitor_utils_NativeInterface_serverKey(JNIEnv *env, jobject instance) {
    std::string hello = "Hello from C++dsds";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT jstring JNICALL
Java_szu_lzd_monitor_utils_NativeInterface_clientKey(JNIEnv *env, jobject instance) {
    std::string hello = "Hello from C++dsds";
    return env->NewStringUTF(hello.c_str());
}