//
// Created by lzd on 18-7-24.
//

#ifndef MONITOR_SCANNER_H
#define MONITOR_SCANNER_H


#include <opencv2/opencv.hpp>

namespace scanner{

    class Scanner {
    public:
        int resizeThreshold = 500;

        Scanner(cv::Mat& bitmap);
        virtual ~Scanner();
        std::vector<cv::Point> scanPoint();
    private:
        cv::Mat srcBitmap;
        float resizeScale = 1.0f;

        cv::Mat resizeImage();

        cv::Mat preprocessImage(cv::Mat& image);

        std::vector<cv::Point> selectPoints(std::vector<cv::Point> points, int selectTimes);

        std::vector<cv::Point> sortPointClockwise(std::vector<cv::Point> vector);

        int gimp_transform_polygon_is_convex(std::vector<cv::Point> vector);

        long long pointSideLine(cv::Point& lineP1, cv::Point& lineP2, cv::Point& point);
    };



}


#endif //MONITOR_SCANNER_H
