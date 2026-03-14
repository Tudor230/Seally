package com.example.seally.livekit

import androidx.camera.core.ImageProxy
import livekit.org.webrtc.CapturerObserver
import livekit.org.webrtc.JavaI420Buffer
import livekit.org.webrtc.SurfaceTextureHelper
import livekit.org.webrtc.VideoCapturer
import livekit.org.webrtc.VideoFrame
import java.nio.ByteBuffer

class CameraXFrameCapturer : VideoCapturer {
    private var mObserver: CapturerObserver? = null
    @Volatile
    private var mIsCapturing: Boolean = false

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper?,
        applicationContext: android.content.Context?,
        capturerObserver: CapturerObserver?,
    ) {
        mObserver = capturerObserver
    }

    override fun startCapture(width: Int, height: Int, framerate: Int) {
        mIsCapturing = true
    }

    override fun stopCapture() {
        mIsCapturing = false
    }

    override fun changeCaptureFormat(width: Int, height: Int, framerate: Int) = Unit

    override fun dispose() {
        mIsCapturing = false
        mObserver = null
    }

    override fun isScreencast(): Boolean = false

    fun pushImageProxy(imageProxy: ImageProxy) {
        if (!mIsCapturing) return
        val observer = mObserver ?: return
        val planes = imageProxy.planes
        if (planes.size < 3) return

        val width = imageProxy.width
        val height = imageProxy.height
        val chromaWidth = (width + 1) / 2
        val chromaHeight = (height + 1) / 2
        val buffer = JavaI420Buffer.allocate(width, height)

        try {
            copyPlane(planes[0], width, height, buffer.dataY)
            copyPlane(planes[1], chromaWidth, chromaHeight, buffer.dataU)
            copyPlane(planes[2], chromaWidth, chromaHeight, buffer.dataV)

            val frame = VideoFrame(
                buffer,
                imageProxy.imageInfo.rotationDegrees,
                imageProxy.imageInfo.timestamp.takeIf { it > 0L } ?: System.nanoTime(),
            )
            observer.onFrameCaptured(frame)
            frame.release()
        } catch (_: Throwable) {
            buffer.release()
        }
    }

    private fun copyPlane(
        plane: ImageProxy.PlaneProxy,
        width: Int,
        height: Int,
        destination: ByteBuffer,
    ) {
        val source = plane.buffer.duplicate()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        if (pixelStride == 1 && rowStride == width) {
            val length = width * height
            val originalLimit = source.limit()
            source.limit(source.position() + length)
            destination.put(source)
            source.limit(originalLimit)
            destination.rewind()
            return
        }

        val rowData = ByteArray(rowStride)
        for (row in 0 until height) {
            val required = if (pixelStride == 1) width else (width - 1) * pixelStride + 1
            source.get(rowData, 0, required)

            var column = 0
            while (column < width) {
                destination.put(rowData[column * pixelStride])
                column += 1
            }

            if (row < height - 1) {
                val skip = rowStride - required
                if (skip > 0) {
                    source.position(source.position() + skip)
                }
            }
        }
        destination.rewind()
    }
}
