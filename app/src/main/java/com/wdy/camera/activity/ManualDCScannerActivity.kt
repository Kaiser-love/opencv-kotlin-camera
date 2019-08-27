package com.wdy.camera.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.Gravity
import android.widget.Toast
import androidx.core.view.WindowCompat
import com.wdy.camera.R
import luyao.util.ktx.base.BaseActivity
import kotlinx.android.synthetic.main.activity_manual_dc_scanner.*
import net.doo.snap.camera.PictureCallback
import net.doo.snap.dcscanner.DCScanner
import net.doo.snap.lib.detector.ContourDetector

import io.scanbot.sdk.ScanbotSDK

class ManualDCScannerActivity : BaseActivity(), PictureCallback {

    override fun getLayoutResId() = R.layout.activity_manual_dc_scanner

    private var flashEnabled = false
    private var dcScanner: DCScanner? = null

    override fun initView() {
//        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY)
        supportActionBar?.hide()
        camera.setCameraOpenCallback {
            camera.postDelayed({
                camera.useFlash(flashEnabled)
                camera.continuousFocus()
            }, 700)
        }
        camera.addPictureCallback(this)
        val scanbotSDK = ScanbotSDK(this)
        dcScanner = scanbotSDK.dcScanner()
        flash.setOnClickListener {
            flashEnabled = !flashEnabled
            camera.useFlash(flashEnabled)
        }
        take_picture_btn.setOnClickListener {
            camera.takePicture(false)
        }

        Toast.makeText(
                this,
                if (scanbotSDK.isLicenseActive)
                    "License is active"
                else
                    "License is expired",
                Toast.LENGTH_LONG
        ).show()
    }

    override fun initData() {
    }

    override fun onResume() {
        super.onResume()
        camera.onResume()
    }

    override fun onPause() {
        super.onPause()
        camera.onPause()
    }

    override fun onPictureTaken(image: ByteArray?, imageOrientation: Int) {
        // Here we get the full image from the camera.
        // Implement a suitable async(!) detection and image handling here.

        // Decode Bitmap from bytes of original image:
        val options = BitmapFactory.Options()
        options.inSampleSize = 2 // use 1 for full, no downscaled image.
        var originalBitmap = BitmapFactory.decodeByteArray(image, 0, image!!.size, options)

        // rotate original image if required:
        if (imageOrientation > 0) {
            val matrix = Matrix()
            matrix.setRotate(imageOrientation.toFloat(), originalBitmap.width / 2f, originalBitmap.height / 2f)
            originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, false)
        }

        // Run document detection on original image:
        val detector = ContourDetector()
        detector.detect(originalBitmap)
        val documentImage = detector.processImageAndRelease(originalBitmap, detector.polygonF, ContourDetector.IMAGE_FILTER_NONE)

        // Show the cropped image as thumbnail preview
        val thumbnailImage = resizeImage(documentImage, 600f, 600f)
        runOnUiThread {
            resultImageView.setImageBitmap(thumbnailImage)
            // continue with camera preview
            camera.continuousFocus()
            camera.startPreview()
        }

        // And finally run DC recognition on prepared document image:
        val resultInfo = dcScanner?.recognizeDCBitmap(documentImage, 0)
        try {
            if (resultInfo != null && resultInfo.recognitionSuccessful) {
                Toast.makeText(
                        this,
                        resultInfo.toString(),
                        Toast.LENGTH_LONG
                ).show()
            } else {
                runOnUiThread {
                    val toast = Toast.makeText(this@ManualDCScannerActivity, "No DC content was recognized!", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                }
            }
        } catch (e: Exception) {
            println(e)
        }

        // reset preview image
        resultImageView.postDelayed({ resultImageView.setImageBitmap(null) }, 1000)
    }

    private fun resizeImage(bitmap: Bitmap, width: Float, height: Float): Bitmap {
        val oldWidth = bitmap.width.toFloat()
        val oldHeight = bitmap.height.toFloat()
        val scaleFactor = if (oldWidth > oldHeight) width / oldWidth else height / oldHeight

        val scaledWidth = Math.round(oldWidth * scaleFactor)
        val scaledHeight = Math.round(oldHeight * scaleFactor)

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)
    }
}