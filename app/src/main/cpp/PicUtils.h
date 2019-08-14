//
// Created by lizhengda on 2018/6/30.
//

#ifndef MONITOR_PICUTILS_H
#define MONITOR_PICUTILS_H

#include <jni.h>
#include <string>
#include <android/log.h>
#include <stdlib.h>
#include <stdio.h>
#include <vector>
#include "opencv2/core/cvdef.h"
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"

using namespace cv;
using namespace std;

Point *pointsToNative(JNIEnv *env, jobjectArray points_);

static struct {
    jclass jClassPoint;
    jmethodID jMethodInit;
    jfieldID jFieldIDX;
    jfieldID jFieldIDY;
} gPointInfo;

Mat RGBToLab(Mat &m);

void native_crop(JNIEnv *env, jobject inputBitmap,
                 jobjectArray points, jobject outBitmap);

void initClassInfo(JNIEnv *env);

double calcHistM(Mat pic1, Mat pic2);

int hashSimilarity(Mat pic1, Mat pic2);

void native_scan(JNIEnv *env,
                 jobject srcBitmap, jobjectArray outPoint_);

#endif //MONITOR_PICUTILS_H
