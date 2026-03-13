package com.example.seally.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.ByteArrayOutputStream

class PoseLandmarkerHelper(
    private val mContext: Context,
    private val mModelAssetPath: String,
    private val mNumPoses: Int,
    private val mResultListener: (PoseLandmarkerResult) -> Unit,
    private val mErrorListener: (String) -> Unit,
) {
    private val mTaskGraphLock = Any()
    private var mPoseLandmarker: PoseLandmarker? = null
    private var mLastTimestampMs: Long = 0L
    private var mIsReadyForDetection: Boolean = false

    fun setup() {
        synchronized(mTaskGraphLock) {
            clearLocked()

            val started = createLandmarker(useGpu = true) || createLandmarker(useGpu = false)

            mIsReadyForDetection = started
            if (!started) {
                mErrorListener("Failed to initialize PoseLandmarker")
            }
        }
    }

    private fun createLandmarker(useGpu: Boolean): Boolean {
        return runCatching {
            val delegate = if (useGpu) Delegate.GPU else Delegate.CPU
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(mModelAssetPath)
                .setDelegate(delegate)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(mNumPoses)
                .setResultListener { result, _ ->
                    mResultListener(result)
                }
                .setErrorListener { error ->
                    mErrorListener(error.message ?: "Pose landmarker error")
                }
                .build()

            mPoseLandmarker = PoseLandmarker.createFromOptions(mContext, options)
            true
        }.getOrElse { exception ->
            if (!useGpu) {
                mErrorListener(exception.message ?: "Failed to initialize PoseLandmarker")
            }
            false
        }
    }

    @ExperimentalGetImage
    fun detectLiveStreamFrame(imageProxy: ImageProxy) {
        try {
            val mpImage = imageProxyToMpImage(imageProxy) ?: return
            val frameTimestampMs = normalizeTimestampMs(imageProxy.imageInfo.timestamp / NANOS_IN_MILLI) ?: return

            runCatching {
                synchronized(mTaskGraphLock) {
                    if (!mIsReadyForDetection) return@runCatching
                    mPoseLandmarker?.detectAsync(mpImage, frameTimestampMs)
                }
            }.onFailure {
                mErrorListener(it.message ?: "Pose detection failed")
            }
        } finally {
            imageProxy.close()
        }
    }

    @ExperimentalGetImage
    private fun imageProxyToMpImage(imageProxy: ImageProxy): MPImage? {
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            mErrorListener("Unsupported image format: ${imageProxy.format}")
            return null
        }

        val image = imageProxy.image ?: return null
        val bitmap = yuvToRgb(image, imageProxy)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val rotatedBitmap = if (rotationDegrees == 0) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        return BitmapImageBuilder(rotatedBitmap).build()
    }

    private fun yuvToRgb(image: Image, imageProxy: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), JPEG_QUALITY, outputStream)
        val imageBytes = outputStream.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // Drop out-of-order frames; forcing +1ms can make stale frames appear late on screen.
    private fun normalizeTimestampMs(cameraTimestampMs: Long): Long? {
        if (cameraTimestampMs <= mLastTimestampMs) return null
        mLastTimestampMs = cameraTimestampMs
        return cameraTimestampMs
    }

    fun clear() {
        synchronized(mTaskGraphLock) {
            clearLocked()
        }
    }

    private fun clearLocked() {
        mIsReadyForDetection = false
        mPoseLandmarker?.close()
        mPoseLandmarker = null
        mLastTimestampMs = 0L
    }

    companion object {
        private const val JPEG_QUALITY = 100
        private const val NANOS_IN_MILLI = 1_000_000L
    }
}
