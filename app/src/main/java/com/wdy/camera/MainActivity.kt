package com.wdy.camera

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.hjq.permissions.OnPermission
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.wdy.camera.extensions.toBitmap
import com.wdy.camera.utils.NativeInterface
import com.wdy.camera.utils.TfLiteUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main1.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import org.opencv.core.Mat


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    //    private val imageBitmap by lazy { (ContextCompat.getDrawable(this, R.drawable.lena) as BitmapDrawable).bitmap }
    private lateinit var mRgba: Mat
    private lateinit var mTmp: Mat
    private var state: Int = 0
    private var mSize0: Size? = null
    private var mIntermediateMat: Mat? = null
    private var mChannels: Array<MatOfInt>? = null
    private var mHistSize: MatOfInt? = null
    private val mHistSizeNum = 25
    private var mMat0: Mat? = null
    private var mBuff: FloatArray? = null
    private var mRanges: MatOfFloat? = null
    private var mP1: Point? = null
    private var mP2: Point? = null
    private var mColorsRGB: Array<Scalar>? = null
    private var mColorsHue: Array<Scalar>? = null
    private var pointCache: Array<Point>? = null
    private var mWhilte: Scalar? = null
    private var mSepiaKernel: Mat? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        TfLiteUtils.getInstances().init(this)
        val that = this
        XXPermissions.with(this)
                //.constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                //.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
                .permission(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE, Permission.CAMERA) //不指定权限则自动获取清单中的危险权限
                .request(object : OnPermission {
                    override fun hasPermission(granted: List<String>, isAll: Boolean) {
                        cameraView.setCameraIndex(0) // 0:后置 1:前置
                        cameraView.enableFpsMeter() //显示FPS
                        cameraView.setCvCameraViewListener(that)
                        cameraView.enableView()
                    }

                    override fun noPermission(denied: List<String>, quick: Boolean) {

                    }
                })
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mTmp = Mat(height, width, CvType.CV_8UC4)

        mIntermediateMat = Mat()
        mSize0 = Size()
        mChannels = arrayOf(MatOfInt(0), MatOfInt(1), MatOfInt(2))
        mBuff = FloatArray(mHistSizeNum)
        mHistSize = MatOfInt(mHistSizeNum)
        mRanges = MatOfFloat(0f, 256f)
        mMat0 = Mat()
        mColorsRGB = arrayOf(Scalar(200.0, 0.0, 0.0, 255.0), Scalar(0.0, 200.0, 0.0, 255.0), Scalar(0.0, 0.0, 200.0, 255.0))
        mColorsHue = arrayOf(Scalar(255.0, 0.0, 0.0, 255.0), Scalar(255.0, 60.0, 0.0, 255.0), Scalar(255.0, 120.0, 0.0, 255.0), Scalar(255.0, 180.0, 0.0, 255.0), Scalar(255.0, 240.0, 0.0, 255.0), Scalar(215.0, 213.0, 0.0, 255.0), Scalar(150.0, 255.0, 0.0, 255.0), Scalar(85.0, 255.0, 0.0, 255.0), Scalar(20.0, 255.0, 0.0, 255.0), Scalar(0.0, 255.0, 30.0, 255.0), Scalar(0.0, 255.0, 85.0, 255.0), Scalar(0.0, 255.0, 150.0, 255.0), Scalar(0.0, 255.0, 215.0, 255.0), Scalar(0.0, 234.0, 255.0, 255.0), Scalar(0.0, 170.0, 255.0, 255.0), Scalar(0.0, 120.0, 255.0, 255.0), Scalar(0.0, 60.0, 255.0, 255.0), Scalar(0.0, 0.0, 255.0, 255.0), Scalar(64.0, 0.0, 255.0, 255.0), Scalar(120.0, 0.0, 255.0, 255.0), Scalar(180.0, 0.0, 255.0, 255.0), Scalar(255.0, 0.0, 255.0, 255.0), Scalar(255.0, 0.0, 215.0, 255.0), Scalar(255.0, 0.0, 85.0, 255.0), Scalar(255.0, 0.0, 0.0, 255.0))
        mWhilte = Scalar.all(255.0)
        mP1 = Point()
        mP2 = Point()

        // Fill sepia kernel
        mSepiaKernel = Mat(4, 4, CvType.CV_32F)
        mSepiaKernel!!.put(0, 0, /* R */0.189, 0.769, 0.393, 0.0)
        mSepiaKernel!!.put(1, 0, /* G */0.168, 0.686, 0.349, 0.0)
        mSepiaKernel!!.put(2, 0, /* B */0.131, 0.534, 0.272, 0.0)
        mSepiaKernel!!.put(3, 0, /* A */0.000, 0.000, 0.000, 1.0)
    }

    override fun onCameraViewStopped() {
        mRgba.release()
        mTmp.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {
        mRgba = inputFrame?.rgba()!!
        val sizeRgba: Size = mRgba.size()
        val rgbaInnerWindow: Mat
        val rows = sizeRgba.height.toInt()
        val cols = sizeRgba.width.toInt()
        val left = cols / 8
        val top = rows / 8
        val width = cols * 3 / 4
        val height = rows * 3 / 4
        when (state) {
            0 -> {

            }
            1 -> {
                mRgba = inputFrame.gray()
            }
            2 -> {
                Imgproc.GaussianBlur(mRgba, mRgba, Size(125.toDouble(), 125.toDouble()), 0.toDouble())
            }
            3 -> {
                mRgba = inputFrame.gray()
                // 调用C++方法
                NativeInterface.getInstance().CannyDetect(mRgba.nativeObjAddr)
//                Imgproc.Canny(mRgba, mTmp, 20.toDouble(), 255.toDouble())
//                Imgproc.cvtColor(mTmp, mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
            }
            4 -> {
                // 调用模型检测方法
                val pointLeft = Point(200.0, 200.0)
                val pointRight = Point(500.0, 500.0)
                Imgproc.line(mRgba, pointLeft, pointRight, Scalar(0.0, 255.0, 0.0, 255.0))
                val rus = TfLiteUtils.getInstances().predict_imageWithModel(mRgba.toBitmap())
                val leftTop = Point()
                leftTop.x = if (rus[0][1].toInt() < 0) 0.0 else rus[0][1].toDouble()
                leftTop.y = if (rus[0][0].toInt() < 0) 0.0 else rus[0][0].toDouble()
                pointCache?.set(0, leftTop)

                val leftDown = Point()
                leftDown.x = if (rus[1][1].toInt() < 0) 0.0 else rus[1][1].toDouble()
                leftDown.y = if (rus[1][0].toInt() < 0) 0.0 else rus[1][0].toDouble()
                pointCache?.set(1, leftDown)

                val rightDown = Point()
                rightDown.x = if (rus[2][1].toInt() < 0) 0.0 else rus[2][1].toDouble()
                rightDown.y = if (rus[2][0].toInt() < 0) 0.0 else rus[2][0].toDouble()
                pointCache?.set(2, rightDown)

                val rightTop = Point()
                rightTop.x = if (rus[3][1].toInt() < 0) 0.0 else rus[3][1].toDouble()
                rightTop.y = if (rus[3][0].toInt() < 0) 0.0 else rus[3][0].toDouble()
                pointCache?.set(3, rightTop)
                println(pointCache?.get(0)?.x)
                println(pointCache?.get(0)?.y)
                println(pointCache?.get(1)?.x)
                println(pointCache?.get(1)?.y)
//                Imgproc.rectangle(mRgba, pointLeft, pointRight, Scalar(0.0, 255.0, 0.0, 255.0), 1)
            }
            5 -> {
                //Hist直方图计算
                val hist = Mat()
                var thikness = (sizeRgba.width / (mHistSizeNum + 10) / 5).toInt()
                if (thikness > 5) thikness = 5
                val offset = ((sizeRgba.width - (5 * mHistSizeNum + 4 * 10) * thikness) / 2).toInt()

                // RGB
                for (c in 0..2) {
                    Imgproc.calcHist(Arrays.asList(mRgba), mChannels?.get(c), mMat0, hist, mHistSize, mRanges)
                    Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
                    hist.get(0, 0, mBuff)
                    for (h in 0 until mHistSizeNum) {
                        mP2?.x = (offset + (c * (mHistSizeNum + 10) + h) * thikness).toDouble()
                        mP1?.x = mP2?.x
                        mP1?.y = sizeRgba.height - 1
                        mP2?.y = mP1?.y!! - 2 - mBuff?.get(h)!!.toInt()
                        Imgproc.line(mRgba, mP1, mP2, mColorsRGB?.get(c), thikness)
                    }
                }
                // Value and Hue
                Imgproc.cvtColor(mRgba, mTmp, Imgproc.COLOR_RGB2HSV_FULL)
                // Value
                Imgproc.calcHist(Arrays.asList(mTmp), mChannels?.get(2), mMat0, hist, mHistSize, mRanges)
                Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
                hist.get(0, 0, mBuff)
                for (h in 0 until mHistSizeNum) {
                    mP2?.x = (offset + (3 * (mHistSizeNum + 10) + h) * thikness).toDouble()
                    mP1?.x = mP2?.x
                    mP1?.y = sizeRgba.height - 1
                    mP2?.y = mP1?.y!! - 2 - mBuff?.get(h)!!.toInt()
                    Imgproc.line(mRgba, mP1, mP2, mWhilte, thikness)
                }
            }
            6 -> {
                // SEPIA(色调变换)
                rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width)
                Core.transform(rgbaInnerWindow, rgbaInnerWindow, mSepiaKernel!!)
                rgbaInnerWindow.release()
            }
            7 -> {
                // ZOOM放大镜
                val zoomCorner = mRgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10)
                val mZoomWindow = mRgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100)
                Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size())
                val wsize = mZoomWindow.size()
                Imgproc.rectangle(mZoomWindow, Point(1.0, 1.0), Point(wsize.width - 2, wsize.height - 2), Scalar(255.0, 0.0, 0.0, 255.0), 2)
                zoomCorner.release()
                mZoomWindow.release()
            }
            8 -> {
                // PIXELIZE像素化
                rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width)
                Imgproc.resize(rgbaInnerWindow, mIntermediateMat!!, mSize0!!, 0.1, 0.1, Imgproc.INTER_NEAREST)
                Imgproc.resize(mIntermediateMat!!, rgbaInnerWindow, rgbaInnerWindow.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)
                rgbaInnerWindow.release()
            }
        }
        return mRgba
    }


    override fun onDestroy() {
        camera_view?.disableView()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.action_gray_scale -> {
                    state = 1
                    true
                }
                R.id.action_gaussian_blur -> {
                    state = 2
                    true
                }
                R.id.action_canny -> {
                    state = 3
                    true
                }
                R.id.action_rectangle -> {
                    state = 4
                    true
                }
                R.id.action_Hist -> {
                    state = 5
                    true
                }
                R.id.action_SEPIA -> {
                    state = 6
                    true
                }
                R.id.action_zoom -> {
                    state = 7
                    true
                }
                R.id.action_pixelize -> {
                    state = 8
                    true
                }
                R.id.action_reset -> {
                    state = 0
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

}
