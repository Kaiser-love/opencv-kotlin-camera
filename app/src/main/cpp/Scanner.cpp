//
// Created by lzd on 18-7-24.
//

#include <opencv2/imgproc/types_c.h>
#include "Scanner.h"

using namespace scanner;

static bool sortByArea(const std::vector<cv::Point> &v1, const std::vector<cv::Point> &v2) {
    double v1Area = fabs(contourArea(cv::Mat(v1)));
    double v2Area = fabs(contourArea(cv::Mat(v2)));
    return v1Area > v2Area;
}

Scanner::Scanner(cv::Mat &bitmap) {
    srcBitmap = bitmap;
}

Scanner::~Scanner() {
}

std::vector<cv::Point> Scanner::scanPoint() {
    //缩小图片尺寸
    cv::Mat image = resizeImage();
    //预处理图片
    cv::Mat scanImage = preprocessImage(image);
    std::vector<std::vector<cv::Point>> contours;
    //提取边框
    findContours(scanImage, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
    //按面积排序
    sort(contours.begin(), contours.end(), sortByArea);
    std::vector<cv::Point> result;
    if (contours.size() > 0) {
        std::vector<cv::Point> contour = contours[0];
        double arc = arcLength(contour, true);
        std::vector<cv::Point> outDP;
        //多变形逼近
        approxPolyDP(cv::Mat(contour), outDP, 0.2 * arc, true);//调整轮廓点的精度
        //筛选去除相近的点
        std::vector<cv::Point> selectedPoints = selectPoints(outDP, 1);
        if (selectedPoints.size() != 4) {
            //如果筛选出来之后不是四边形，那么使用最小矩形包裹
            cv::RotatedRect rect = minAreaRect(contour);
            cv::Point2f p[4];
            rect.points(p);
            result.push_back(p[0]);
            result.push_back(p[1]);
            result.push_back(p[2]);
            result.push_back(p[3]);
        } else {
            result = selectedPoints;
        }
        for (cv::Point &p : result) {
            p.x *= resizeScale;
            p.y *= resizeScale;
        }
    }
    // 按左上，右上，右下，左下排序
    std::vector<cv::Point> r = sortPointClockwise(result);
    if(gimp_transform_polygon_is_convex(r)<=0){
        cv::RotatedRect rect = minAreaRect(result);
        cv::Point2f p[4];
        rect.points(p);
        result.clear();
        result.push_back(p[0]);
        result.push_back(p[1]);
        result.push_back(p[2]);
        result.push_back(p[3]);
    }
    return r;
}



cv::Mat Scanner::resizeImage() {
    int width = srcBitmap.cols;
    int height = srcBitmap.rows;
    int maxSize = width > height ? width : height;
    if (maxSize > resizeThreshold) {
        resizeScale = 1.0f * maxSize / resizeThreshold;
        width = static_cast<int>(width / resizeScale);
        height = static_cast<int>(height / resizeScale);
        cv::Size size(width, height);
        cv::Mat resizedBitmap(size, CV_8UC3);
        resize(srcBitmap, resizedBitmap, size);
        return resizedBitmap;
    }
    return srcBitmap;
}

cv::Mat Scanner::preprocessImage(cv::Mat &image) {
    cv::Mat grayMat;
    cvtColor(image, grayMat, CV_BGR2GRAY);
    cv::Mat blurMat;
    GaussianBlur(grayMat, blurMat, cv::Size(5, 5), 0);
    cv::Mat cannyMat;
    Canny(blurMat, cannyMat, 0, 5);
    cv::Mat thresholdMat;
    threshold(cannyMat, thresholdMat, 0, 255, CV_THRESH_OTSU);
    return cannyMat;
}

std::vector<cv::Point> Scanner::selectPoints(std::vector<cv::Point> points, int selectTimes) {
    if (points.size() > 4) {
        double arc = arcLength(points, true);
        std::vector<cv::Point>::iterator itor = points.begin();
        while (itor != points.end()) {
            if (points.size() == 4) {
                return points;
            }
            cv::Point &p = *itor;
            if (itor != points.begin()) {
                cv::Point &lastP = *(itor - 1);
                double pointLength = sqrt(pow((p.x - lastP.x), 2) + pow((p.y - lastP.y), 2));
                if (pointLength < arc * 0.1 * selectTimes && points.size() > 4) {
                    itor = points.erase(itor);
                    continue;
                }
            }
            itor++;
        }
        if (points.size() > 4) {
            return selectPoints(points, selectTimes + 1);
        }
    }
    return points;
}

std::vector<cv::Point> Scanner::sortPointClockwise(std::vector<cv::Point> points) {
    if (points.size() != 4) {
        return points;
    }

    cv::Point unFoundPoint;
    std::vector<cv::Point> result = {unFoundPoint, unFoundPoint, unFoundPoint, unFoundPoint};

    long minDistance = -1;
    for (cv::Point &point : points) {
        long distance = point.x * point.x + point.y * point.y;
        if (minDistance == -1 || distance < minDistance) {
            result[0] = point;
            minDistance = distance;
        }
    }
    if (result[0] != unFoundPoint) {
        cv::Point &leftTop = result[0];
        points.erase(std::remove(points.begin(), points.end(), leftTop));
        if ((pointSideLine(leftTop, points[0], points[1]) *
             pointSideLine(leftTop, points[0], points[2])) < 0) {
            result[2] = points[0];
        } else if ((pointSideLine(leftTop, points[1], points[0]) *
                    pointSideLine(leftTop, points[1], points[2])) < 0) {
            result[2] = points[1];
        } else if ((pointSideLine(leftTop, points[2], points[0]) *
                    pointSideLine(leftTop, points[2], points[1])) < 0) {
            result[2] = points[2];
        }
    }
    if (result[0] != unFoundPoint && result[2] != unFoundPoint) {
        cv::Point &leftTop = result[0];
        cv::Point &rightBottom = result[2];
        points.erase(std::remove(points.begin(), points.end(), rightBottom));
        if (pointSideLine(leftTop, rightBottom, points[0]) > 0) {
            result[1] = points[0];
            result[3] = points[1];
        } else {
            result[1] = points[1];
            result[3] = points[0];
        }
    }

    if (result[0] != unFoundPoint && result[1] != unFoundPoint && result[2] != unFoundPoint &&
        result[3] != unFoundPoint) {
        return result;
    }

    return points;
}

long long Scanner::pointSideLine(cv::Point &lineP1, cv::Point &lineP2, cv::Point &point) {
    long x1 = lineP1.x;
    long y1 = lineP1.y;
    long x2 = lineP2.x;
    long y2 = lineP2.y;
    long x = point.x;
    long y = point.y;
    return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
}

int Scanner::gimp_transform_polygon_is_convex(std::vector<cv::Point> vector) {
    double z1, z2, z3, z4;
    vector = sortPointClockwise(vector);
    cv::Point &leftTop = vector[0];
    cv::Point &rightTop = vector[1];
    cv::Point &rightBottom = vector[2];
    cv::Point &leftBottom = vector[3];


    z1 = ((rightTop.x - leftTop.x) * (rightBottom.y - leftTop.y) -
          (rightBottom.x - leftTop.x) * (rightTop.y - leftTop.y));
    z2 = ((rightBottom.x - leftTop.x) * (leftBottom.y - leftTop.y) -
          (leftBottom.x - leftTop.x) * (rightBottom.y - leftTop.y));
    z3 = ((rightBottom.x - rightTop.x) * (leftBottom.y - rightTop.y) -
          (leftBottom.x - rightTop.x) * (rightBottom.y - rightTop.y));
    z4 = ((leftBottom.x - rightTop.x) * (leftTop.y - rightTop.y) -
          (leftTop.x - rightTop.x) * (leftBottom.y - rightTop.y));

    return (z1 * z2 > 0) && (z3 * z4 > 0);
}
