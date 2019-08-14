//
// Created by lizhengda on 2018/6/30.
//

#include "PicUtils.h"
#include "android_utils.h"
#include "Scanner.h"
#include <vector>
#include <opencv2/imgproc/types_c.h>
#include <opencv2/imgcodecs/legacy/constants_c.h>

void native_crop(JNIEnv *env, jobject srcBitmap, jobjectArray points_, jobject outBitmap) {
    Point *points = pointsToNative(env, points_);
//    if (points.size() != 4) {
//        return;
//    }
    Point leftTop = points[0];
    Point rightTop = points[1];
    Point rightBottom = points[2];
    Point leftBottom = points[3];

    Mat srcBitmapMat;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);
//
    AndroidBitmapInfo outBitmapInfo;
    AndroidBitmap_getInfo(env, outBitmap, &outBitmapInfo);
    Mat dstBitmapMat;
    int newHeight = outBitmapInfo.height;
    int newWidth = outBitmapInfo.width;
    dstBitmapMat = Mat::zeros(newHeight, newWidth, srcBitmapMat.type());

    Point2f dstTriangle[4];
    Point2f srcTriangle[4];

    srcTriangle[0] = Point2f(leftTop.x, leftTop.y);
    srcTriangle[1] = Point2f(rightTop.x, rightTop.y);
    srcTriangle[2] = Point2f(leftBottom.x, leftBottom.y);
    srcTriangle[3] = Point2f(rightBottom.x, rightBottom.y);

    dstTriangle[0] = Point2f(0, 0);
    dstTriangle[1] = Point2f(newWidth, 0);
    dstTriangle[2] = Point2f(0, newHeight);
    dstTriangle[3] = Point2f(newWidth, newHeight);

    Mat transform = getPerspectiveTransform(srcTriangle, dstTriangle);
    warpPerspective(srcBitmapMat, dstBitmapMat, transform, dstBitmapMat.size());

    mat_to_bitmap(env, dstBitmapMat, outBitmap);
}

Mat jpegToMat(char *data) {
    vector<char> vdata;
    vdata.insert(vdata.end(), data, data + strlen(data));
    Mat jpegimage = imdecode(Mat(vdata), CV_LOAD_IMAGE_COLOR);
    return jpegimage;
}

Point *pointsToNative(JNIEnv *env, jobjectArray points_) {
    int arrayLength = env->GetArrayLength(points_);
    Point *result = new Point[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
        jobject point_ = env->GetObjectArrayElement(points_, i);
        int pX = env->GetIntField(point_, gPointInfo.jFieldIDX);
        int pY = env->GetIntField(point_, gPointInfo.jFieldIDY);
        result[i] = Point(pX, pY);
    }
    return result;
}

Mat RGBToLab(Mat &m) {
    Mat_<Vec3f> I = m;//定义一个Mat_型的mat，便于运算
    for (int i = 0; i < I.rows; ++i) {
        for (int j = 0; j < I.cols; ++j) {//矩阵运算，一行三列的RGB*[3*3]的参数矩阵
            double L = 0.3811 * I(i, j)[0] + 0.5783 * I(i, j)[1] + 0.0402 * I(i, j)[2];
            double M = 0.1967 * I(i, j)[0] + 0.7244 * I(i, j)[1] + 0.0782 * I(i, j)[2];
            double S = 0.0241 * I(i, j)[0] + 0.1288 * I(i, j)[1] + 0.8444 * I(i, j)[2];
            if (L == 0) L = 1;
            if (M == 0) M = 1;
            if (S == 0) S = 1;
            L = log(L);//求自然对数底
            M = log(M);
            S = log(S);
            I(i, j)[0] = (float) ((L + M + S) / sqrt(3.0));//运算一下重新赋值
            I(i, j)[1] = (float) ((L + M - 2 * S) / sqrt(6.0));
            I(i, j)[2] = (float) ((L - M) / sqrt(2.0));
        }
    }
    return I;
}

static jobject createJavaPoint(JNIEnv *env, Point point_) {
    return env->NewObject(gPointInfo.jClassPoint, gPointInfo.jMethodInit, point_.x, point_.y);
}

void ImgRotate(const Mat &picture, Mat &picturexz, double option) {

    cv::Point2f center(picture.cols / 2, picture.rows / 2);
    cv::Mat rot = cv::getRotationMatrix2D(center, option, 1);
    cv::Rect bbox = cv::RotatedRect(center, picture.size(), option).boundingRect();

    rot.at<double>(0, 2) += bbox.width / 2.0 - center.x;
    rot.at<double>(1, 2) += bbox.height / 2.0 - center.y;

    cv::warpAffine(picture, picturexz, rot, bbox.size());

}

void native_scan(JNIEnv *env,
                 jobject srcBitmap, jobjectArray outPoint_) {
    if (env->GetArrayLength(outPoint_) != 4) {
        return;
    }
    Mat srcBitmapMat;
    bitmap_to_mat(env, srcBitmap, srcBitmapMat);
    Mat bgrData(srcBitmapMat.rows, srcBitmapMat.cols, CV_8UC3);
    cvtColor(srcBitmapMat, bgrData, CV_RGBA2BGR);
    scanner::Scanner docScanner(bgrData);
    std::vector<Point> scanPoints = docScanner.scanPoint();
    if (scanPoints.size() == 4) {
        for (int i = 0; i < 4; ++i) {
            env->SetObjectArrayElement(outPoint_, i, createJavaPoint(env, scanPoints[i]));
        }
    }
}


void initClassInfo(JNIEnv *env) {
    gPointInfo.jClassPoint = reinterpret_cast<jclass>(env->NewGlobalRef(
            env->FindClass("android/graphics/Point")));
    gPointInfo.jMethodInit = env->GetMethodID(gPointInfo.jClassPoint, "<init>", "(II)V");
    gPointInfo.jFieldIDX = env->GetFieldID(gPointInfo.jClassPoint, "x", "I");
    gPointInfo.jFieldIDY = env->GetFieldID(gPointInfo.jClassPoint, "y", "I");
}


//直方图计算相似度
double calcHistM(Mat pic1, Mat pic2) {
    //计算相似度
    double dSimilarity;
    if (pic2.channels() == 1) {//单通道时，
        int histSize = 256;
        float range[] = {0, 256};
        const float *histRange = {range};
        bool uniform = true;
        bool accumulate = false;

        Mat hist1, hist2;

        calcHist(&pic2, 1, 0, cv::Mat(), hist1, 1, &histSize, &histRange, uniform, accumulate);
        normalize(hist1, hist1, 0, 1, cv::NORM_MINMAX, -1, cv::Mat());

        calcHist(&pic1, 1, 0, cv::Mat(), hist2, 1, &histSize, &histRange, uniform, accumulate);
        normalize(hist2, hist2, 0, 1, cv::NORM_MINMAX, -1, cv::Mat());

        dSimilarity = cv::compareHist(hist1, hist2,
                                      CV_COMP_CORREL);//,CV_COMP_CHISQR,CV_COMP_INTERSECT,CV_COMP_BHATTACHARYYA    CV_COMP_CORREL

        //cout << "similarity = " << dSimilarity << endl;
        return dSimilarity;
    } else {//三通道时
        cvtColor(pic2, pic2, COLOR_BGR2HSV);
        cvtColor(pic1, pic1, COLOR_BGR2HSV);

        int h_bins = 50, s_bins = 60;
        int histSize[] = {h_bins, s_bins};
        float h_ranges[] = {0, 180};
        float s_ranges[] = {0, 256};
        const float *ranges[] = {h_ranges, s_ranges};
        int channels[] = {0, 1};

        MatND hist1, hist2;

        calcHist(&pic2, 1, channels, cv::Mat(), hist1, 2, histSize, ranges, true, false);
        normalize(hist1, hist1, 0, 1, cv::NORM_MINMAX, -1, cv::Mat());

        calcHist(&pic1, 1, channels, cv::Mat(), hist2, 2, histSize, ranges, true, false);
        normalize(hist2, hist2, 0, 1, cv::NORM_MINMAX, -1, cv::Mat());

        dSimilarity = cv::compareHist(hist1, hist2,
                                      CV_COMP_CORREL); //,CV_COMP_CHISQR,CV_COMP_INTERSECT,CV_COMP_BHATTACHARYYA  CV_COMP_CORREL

        //cout << "similarity = " << dSimilarity << endl;
    }
    return dSimilarity;
}

//感知哈希算法相似度
int hashSimilarity(Mat pic1, Mat pic2) {
    Mat matDst1, matDst2;

    resize(pic1, matDst1, Size(8, 8), 0, 0, INTER_CUBIC);
    resize(pic2, matDst2, Size(8, 8), 0, 0, INTER_CUBIC);


    cv::cvtColor(matDst1, matDst1, CV_BGR2GRAY);
    cv::cvtColor(matDst2, matDst2, CV_BGR2GRAY);

    int iAvg1 = 0, iAvg2 = 0;
    int arr1[64], arr2[64];

    for (int i = 0; i < 8; i++) {
        uchar *data1 = matDst1.ptr<uchar>(i);
        uchar *data2 = matDst2.ptr<uchar>(i);

        int tmp = i * 8;

        for (int j = 0; j < 8; j++) {
            int tmp1 = tmp + j;

            arr1[tmp1] = data1[j] / 4 * 4;
            arr2[tmp1] = data2[j] / 4 * 4;

            iAvg1 += arr1[tmp1];
            iAvg2 += arr2[tmp1];
        }
    }

    iAvg1 /= 64;
    iAvg2 /= 64;

    for (int i = 0; i < 64; i++) {
        arr1[i] = (arr1[i] >= iAvg1) ? 1 : 0;
        arr2[i] = (arr2[i] >= iAvg2) ? 1 : 0;
    }

    int iDiffNum = 0;

    for (int i = 0; i < 64; i++)
        if (arr1[i] != arr2[i])
            ++iDiffNum;

    return iDiffNum;
}

int use_demo(int argc, char *argv[]) {
//    if (argc < 2) {
//        cout << "请输入 图片1 和图片2 的路径，中间空格分开" << endl << endl;
//        return 0;
//    }
//    Mat img1 = imread(argv[1]);//("F:\\shiyandata\\1.jpg");
//    Mat img2 = imread(argv[2]);//("F:\\shiyandata\\3.jpg");
//    Mat img1_r, img2_r;
//
//    resize(img1, img1_r, Size(), 0.3, 0.3, INTER_LINEAR);
//    resize(img2, img2_r, Size(), 0.3, 0.3, INTER_LINEAR);
//
//    namedWindow("1", WINDOW_AUTOSIZE);
//    imshow("1", img1_r);
//
//    namedWindow("2", WINDOW_AUTOSIZE);
//    imshow("2", img2_r);
//
//    double simi_score = calcHistM(img1_r, img2_r);
//    int hash_score = hashSimilarity(img1_r, img2_r);
//
//    cout << "直方图匹配方法:  " << simi_score << "  ... 小于0.8 ：两张图片存在较大变化，值在0-10 之间" << endl << endl;
//
//    cout << "哈希相似度方法: " << hash_score << "  ... 小于等于6 ：两张图片相同，大于等于9：两张图片不同，7-8：两张有轻微变化" << endl;
//    //else if (hash_score > 10)
//    //	cout << "哈希相似度计算距离:" << hash_score << "... they are two different images!" << endl;
//    //else
//    //	cout << "哈希相似度计算距离:" << hash_score << "... two image are somewhat similar!" << endl;
//
//    waitKey(0);
//
//    system("pause");
//    destroyAllWindows();

    return 0;

}