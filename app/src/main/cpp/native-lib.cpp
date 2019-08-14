#include <jni.h>
#include <string>
#include <android/log.h>
#include <stdlib.h>
#include <stdio.h>
#include "opencv2/core/cvdef.h"
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <vector>
#include "android_utils.h"
#include "PicUtils.h"

//extern "C"
//{
//#include "libavcodec/avcodec.h"
//#include "libavformat/avformat.h"
//}

const int ALGORITHM_TYPE_HASH = 1;
const int ALGORITHM_TYPE_CALC = 0;

using namespace cv;
using namespace std;
extern "C" JNIEXPORT jstring JNICALL
Java_com_wdy_camera_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++dsds";
    return env->NewStringUTF(hello.c_str());
}
/**
 * @brief 使用Canndy算子检测图像中的对象轮廓
 * @param matAddrGray, Mat图像的内存地址
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_CannyDetect(
        JNIEnv *env,
        jobject thiz,
        jlong matAddrGray) {
    Mat &grayMat = *(Mat *) matAddrGray;
    Canny(grayMat, grayMat, 50, 100);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_init(JNIEnv *env, jobject instance) {
    initClassInfo(env);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_crop(JNIEnv *env, jobject instance, jobject inputBitmap,
                                               jobjectArray points, jobject outBitmap) {

    // TODO
    native_crop(env, inputBitmap, points, outBitmap);

}
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_wdy_camera_utils_NativeInterface_contrast(JNIEnv *env, jobject instance,
                                                   jobject oldBitmap, jobject newBitmap,
                                                   jint algorithmType) {
    Mat oldBitmapMat;
    Mat newBitmapMat;
    bitmap_to_mat(env, oldBitmap, oldBitmapMat);
    bitmap_to_mat(env, newBitmap, newBitmapMat);
    double value = -1;
    switch (algorithmType) {
        case ALGORITHM_TYPE_CALC:
            value = calcHistM(oldBitmapMat, newBitmapMat);
            break;
        case ALGORITHM_TYPE_HASH:

            value = hashSimilarity(oldBitmapMat, newBitmapMat);
        default:
            break;
    }
    oldBitmapMat.release();
    newBitmapMat.release();
    return value;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wdy_camera_utils_NativeInterface_scan(JNIEnv *env, jobject instance,
                                               jobject srcBitmap, jobjectArray outPoint) {

    native_scan(env, srcBitmap, outPoint);
}
