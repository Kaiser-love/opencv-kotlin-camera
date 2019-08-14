package com.wdy.camera.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import com.wdy.camera.R;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


import static java.lang.Math.round;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;

public class TfLiteUtils {
    private static TfLiteUtils instance;
    private boolean init = false;
    private static final String MODEL_FILE = "mobile";
    private Interpreter tflite = null;
    private int[] ddims = {1, 3, 224, 224};

    public static TfLiteUtils getInstances() {
        if (instance == null) {
            synchronized (TfLiteUtils.class) {
                if (instance == null) {
                    instance = new TfLiteUtils();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        if (!init) {

            load_model(context);
            init = true;
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getResources().openRawResourceFd(R.raw.mobile);
//        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[][] predict_image(Bitmap bmp) {
        float[][] resultlabels = new float[4][2];
        Bitmap bmp1 = bmp;

        ByteBuffer inputData = getScaledMatrix(bmp, ddims);
        //****************************************
        //**********补充开始**********************
        //****************************************
        //System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

        //****resize****
        try {
            Mat mat_ori = new Mat();
            bitmapToMat(bmp, mat_ori);
            float ori_h;
            float ori_w;
            int res_h;
            int res_w;

            ori_h = bmp.getHeight();
            ori_w = bmp.getWidth();

            if (ori_h > ori_w) {
                res_w = 500;
                res_h = round(ori_h * (500 / ori_w));
            } else {
                res_h = 500;
                res_w = round(ori_w * (500 / ori_h));
            }

            float cov_h_ratio = ori_h / res_h;
            float cov_w_ratio = ori_w / res_w;

            Mat mat_ = new Mat();
            Imgproc.resize(mat_ori, mat_, new Size(res_w, res_h));
            Mat matf = new Mat();
            //mat_.convertTo(matf,CV_8UC3);
            Imgproc.cvtColor(mat_, matf, COLOR_RGBA2RGB);
            //****rsize****

            MatOfPoint max_box = new MatOfPoint();//=[[0,0],[0,res_h],[res_w,res_h],[res_w,0]];

            Point m_point1 = new Point(0, 0);
            Point m_point2 = new Point(0, res_h);
            Point m_point3 = new Point(res_w, res_h);
            Point m_point4 = new Point(res_w, 0);

            max_box.fromArray(m_point1, m_point2, m_point3, m_point4);

            double max_area = Imgproc.contourArea(max_box) / 20.0;

            //****defaultpoint****

            MatOfPoint2f edge_point = new MatOfPoint2f();

            //****defaultpoint****

            // ****hist
//            Mat dst = new Mat();
//
//            List<Mat> mv = new ArrayList<Mat>();
//            Core.split(matf, mv);
//
//            for (int i = 0; i < matf.channels(); i++)
//            {
//                Imgproc.equalizeHist(mv.get(i), mv.get(i));
//            }
//            Core.merge(mv, dst);

            //**********hist

            //****meanshift****
            //IplImage markers32f = cvCreateImage(cvGetSize(binary), IPL_DEPTH_32F, binary.nChannels());
            Mat meanimage = new Mat();
            //mat.convertTo();
            Imgproc.pyrMeanShiftFiltering(matf, meanimage, 15, 30, 1, new TermCriteria(TermCriteria.MAX_ITER + TermCriteria.EPS, 5, 1));
            //****meanshift****

            //****gray****
            Mat gray = new Mat();
            Imgproc.cvtColor(meanimage, gray, COLOR_BGR2GRAY);
            //****gray****

            //****equalizeHist****
            Mat eqH = new Mat();
            Imgproc.equalizeHist(gray, eqH);
            //****equalizeHist****

            //****gaussian****
            Mat Ggray = new Mat();
            Imgproc.GaussianBlur(eqH, Ggray, new Size(3, 3), 0);
            //****gaussian****

            //****canny****
            Mat edged = new Mat();
            //Mat thresholdImage=new Mat();
            Imgproc.Canny(Ggray, edged, 75, 200);
            //****canny****

            //****dilation****
            Mat dilation_edge = new Mat();
            Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.dilate(edged, dilation_edge, element);
            //****dilation****

            //****findcontour****
            List<MatOfPoint> cnts_0 = new ArrayList<>();
            Imgproc.findContours(dilation_edge, cnts_0, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);//,new Point(112,112));
            //****findcontour****

            //****select_area_retio****
            //remove the area which area ratio less than 0.5
            List<MatOfPoint> cnts_ = new ArrayList<>();
            for (int boxind = 0; boxind < cnts_0.size() - 1; boxind++) {
                double nowarea = Imgproc.contourArea(cnts_0.get(boxind));
                RotatedRect nowrect = Imgproc.minAreaRect(new MatOfPoint2f(cnts_0.get(boxind).toArray()));
                double nowrectarea = nowrect.size.area();
                double nowratio = nowrectarea / nowarea;


                if (nowratio > 0.5 && nowarea > max_area) {
                    //cnts_0.remove(boxind);
                    //boxind=boxind-1;
                    cnts_.add(cnts_0.get(boxind));
                }
            }

            //****select_area_retio****

            //****select_area****
            //TODO:1.delete the contours which has the area less than max_area

            //for(int arind=0;arind<cnts_0.size();arind++)
            //{
            //  double area=Imgproc.contourArea(cnts_0.get(arind));
            //if (area>max_area)
            //{
            //  cnts_.add(cnts_0.get(arind));
            //}
            //}
            //****select_area****

            //****sortcontour****
            boolean pop = false;
            while (pop == false) {
                int k = 0;
                //System.out.print(boxlist.size());
                for (int boxind = 0; boxind < cnts_.size() - 1; boxind++) {
                    double nowarea = Imgproc.contourArea(cnts_.get(boxind));
                    double nexarea = Imgproc.contourArea(cnts_.get(boxind + 1));
                    if (nowarea < nexarea) {
                        MatOfPoint nowtmp = cnts_.get(boxind);
                        MatOfPoint nextmp = cnts_.get(boxind + 1);

                        //tmp=cnts_.get(boxind);
                        cnts_.set(boxind, nextmp);
                        cnts_.set(boxind + 1, nowtmp);
                        k = k + 1;
                    }
                }
                if (k == 0) {
                    pop = true;
                }
            }

            //****sortcontour****

            //****top5****
            int maxiter;
            if (cnts_.size() < 8) {
                maxiter = cnts_.size();
            } else {
                maxiter = 8;
            }
            List<MatOfPoint> cnts = new ArrayList<>();
            for (int i = 0; i < maxiter; i++) {
                cnts.add(cnts_.get(i));
            }
            //****top5****


            //****approxPolyDP****
            List<Double> index_p = new ArrayList<Double>();
            List<MatOfPoint2f> screenCnt_tem = new ArrayList<>();

            for (int ci = 0; ci < cnts.size(); ci++) {
                MatOfPoint2f citmp = new MatOfPoint2f(cnts.get(ci).toArray());
                double peri = Imgproc.arcLength(citmp, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(citmp, approx, 0.02 * peri, true);

                //double tmp_area=Imgproc.contourArea(cnts.get(ci));
                int appsize;
                appsize = approx.size(0);
                if (appsize == 4) {
                    index_p.add(peri);
                    screenCnt_tem.add(approx);
                }
            }
            //****approxPolyDP****


            //****findmaxvalue****
            double maxindex_p = index_p.size() == 0 ? 0 : index_p.get(0);
            for (int maxind = 0; maxind < index_p.size(); maxind++) {
                if (maxindex_p < index_p.get(maxind)) {
                    maxindex_p = index_p.get(maxind);
                }
            }
            //****findmaxvalue****
            MatOfPoint2f screenCnt = new MatOfPoint2f();
            if (index_p.size() != 0) {
                screenCnt = screenCnt_tem.get(index_p.indexOf(maxindex_p));

                String labelProb_ = screenCnt.dump();
                String[] spoints = labelProb_.replace("[", "").replace("]", "").split(";|,");

                float lefttop_h = Float.parseFloat(spoints[1]) * cov_h_ratio;
                float lefttop_w = Float.parseFloat(spoints[0]) * cov_w_ratio;
                float leftbottom_h = Float.parseFloat(spoints[7]) * cov_h_ratio;
                float leftbottom_w = Float.parseFloat(spoints[6]) * cov_w_ratio;
                float rightbottom_h = Float.parseFloat(spoints[5]) * cov_h_ratio;
                float rightbottom_w = Float.parseFloat(spoints[4]) * cov_w_ratio;
                float righttop_h = Float.parseFloat(spoints[3]) * cov_h_ratio;
                float righttop_w = Float.parseFloat(spoints[2]) * cov_w_ratio;
                resultlabels[0][0] = lefttop_h;
                resultlabels[0][1] = lefttop_w;
                resultlabels[1][0] = leftbottom_h;
                resultlabels[1][1] = leftbottom_w;
                resultlabels[2][0] = rightbottom_h;
                resultlabels[2][1] = rightbottom_w;
                resultlabels[3][0] = righttop_h;
                resultlabels[3][1] = righttop_w;
            } else {
                //String labelProb_=screenCnt.dump();
                //String[] spoints =labelProb_.replace("[", "").replace("]", "").split(";|,");

                float lefttop_h = 0 * cov_h_ratio;
                float lefttop_w = 0 * cov_w_ratio;
                float leftbottom_h = 50 * cov_h_ratio;
                float leftbottom_w = 0 * cov_w_ratio;
                float rightbottom_h = 50 * cov_h_ratio;
                float rightbottom_w = 50 * cov_w_ratio;
                float righttop_h = 50 * cov_h_ratio;
                float righttop_w = 0 * cov_w_ratio;
                resultlabels[0][0] = lefttop_h;
                resultlabels[0][1] = lefttop_w;
                resultlabels[1][0] = leftbottom_h;
                resultlabels[1][1] = leftbottom_w;
                resultlabels[2][0] = rightbottom_h;
                resultlabels[2][1] = rightbottom_w;
                resultlabels[3][0] = righttop_h;
                resultlabels[3][1] = righttop_w;
            }


            //****************************
            //*******补充到此*************
            //****************************

            // Data format conversion takes too long
            // Log.d("inputData", Arrays.toString(inputData));
            //int[][] labelProbArray = new int[4][2];//screenCnt.dump();
            long start = System.currentTimeMillis();
            // get predict result
            //tflite.run(inputData, labelProbArray);
            //String[] threeinputs = {"foo", "bar"};
            //List threeinputs = new ArrayList();
            //threeinputs.add(inputData);
            //threeinputs.add(boxroisout);
            //threeinputs.add(bboxesout);
            //ThreeInput threeinputs =new ThreeInput(inputData,boxroisout,bboxesout);
            String labelProb = screenCnt.dump();

            //float[][] labelProbArray = new float[4][2];
            //outputs.;
            long end = System.currentTimeMillis();
            long time = end - start;
            //改到了这里
            //
            float[][] labelProbArray = new float[4][2];
            float[] results = new float[labelProbArray[0].length];
            //resultlabels = labelProbArray;


            System.arraycopy(labelProbArray[0], 0, results, 0, labelProbArray[0].length);
            // show predict result and time
//            int r = get_max_result(results);
//            String show_text = "result：" + r + "\nname：" + resultLabel.get(r) + "\nprobability：" + results[r] + "\ntime：" + time + "ms";
//            result_text.setText(show_text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultlabels;
    }

    public float[][] predict_imageWithModel(Bitmap bmp) {
        float[][] resultlabels = new float[4][2];
        // picture to float array
//        Bitmap bmp = PhotoUtil.getScaleBitmap(image_path);
        ByteBuffer inputData = getScaledMatrix(bmp, ddims);
        try {
            // Data format conversion takes too long
            // Log.d("inputData", Arrays.toString(inputData));
            float[][] labelProbArray = new float[4][2];
            long start = System.currentTimeMillis();
            // get predict result
            tflite.run(inputData, labelProbArray);
            long end = System.currentTimeMillis();
            long time = end - start;
            float[] results = new float[labelProbArray[0].length];
            resultlabels = labelProbArray;
            System.arraycopy(labelProbArray[0], 0, results, 0, labelProbArray[0].length);
            // show predict result and time
//            int r = get_max_result(results);
//            String show_text = "result：" + r + "\nname：" + resultLabel.get(r) + "\nprobability：" + results[r] + "\ntime：" + time + "ms";
//            result_text.setText(show_text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultlabels;
    }

    public static ByteBuffer getScaledMatrix(Bitmap bitmap, int[] ddims) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(ddims[0] * ddims[1] * ddims[2] * ddims[3] * 4);
        imgData.order(ByteOrder.nativeOrder());
        // get image pixel
        int[] pixels = new int[ddims[2] * ddims[3]];
        Bitmap bm = Bitmap.createScaledBitmap(bitmap, ddims[2], ddims[3], false);
        bm.getPixels(pixels, 0, bm.getWidth(), 0, 0, ddims[2], ddims[3]);
        int pixel = 0;
        for (int i = 0; i < ddims[2]; ++i) {
            for (int j = 0; j < ddims[3]; ++j) {
                final int val = pixels[pixel++];
                imgData.putFloat(((val >> 16) & 0xFF));
                imgData.putFloat((val >> 8) & 0xFF);
                imgData.putFloat(val & 0xFF);
            }
        }

        if (bm.isRecycled()) {
            bm.recycle();
        }
        return imgData;
    }


    private void load_model(Context context) {
        try {
            GpuDelegate delegate = new GpuDelegate();
            Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
            tflite = new Interpreter(loadModelFile(context), options);
//            tflite.setNumThreads(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class boxes {

        public boxes(int circle, int[][] points, int[] rects) {
            super();
            this.circle = circle;
            this.points = points;
            this.rects = rects;
        }

        private int circle;
        private int[][] points;
        private int[] rects;

        public int getcicle() {
            return circle;
        }

        public void setcircle(int circle) {
            this.circle = circle;
        }

        public int[][] getpoints() {
            return points;
        }

        public void setpoints(int[][] points) {
            this.points = points;
        }

        public int[] getrects() {
            return rects;
        }

        public void setrects(int[] rects) {
            this.rects = rects;
        }

    }


}
