package com.example.seally.livekit

import android.util.Log
import android.content.Context
import androidx.camera.core.ImageProxy
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.track.DataPublishReliability
import io.livekit.android.room.track.LocalVideoTrack

class LiveKitPublisher(context: Context) {
    private val mTag = "LiveKitPublisher"
    private val mRoom: Room = LiveKit.create(context.applicationContext)
    private val mCapturer = CameraXFrameCapturer()
    private var mLocalVideoTrack: LocalVideoTrack? = null
    private var mIsConnected: Boolean = false
    private var mIsVideoPublished: Boolean = false
    private var mFramesSubmitted: Long = 0L
    private var mLandmarkPacketsSent: Long = 0L
    private var mLastError: String? = null

    suspend fun connect(url: String, token: String): Result<Any> {
        if (mIsConnected) return Result.success(Unit)

        return runCatching {
            Log.d(mTag, "Connecting to LiveKit room...")
            mRoom.connect(url = url, token = token)
            Log.d(mTag, "Connected to LiveKit room, creating local track...")

            val localTrack = mRoom.localParticipant.createVideoTrack(
                name = "camera",
                capturer = mCapturer,
            )
            localTrack.start()
            localTrack.startCapture()
            val didPublish = mRoom.localParticipant.publishVideoTrack(localTrack)
            if (!didPublish) {
                localTrack.stopCapture()
                localTrack.stop()
                localTrack.dispose()
                mLastError = "publishVideoTrack returned false"
                Log.e(mTag, "Failed to publish camera track.")
                error("LiveKit camera track could not be published")
            }
            mLocalVideoTrack = localTrack
            mIsConnected = true
            mIsVideoPublished = true
            mLastError = null
            Log.d(mTag, "Local camera track published successfully.")
        }.onFailure {
            mLastError = it.message ?: "unknown connect/publish failure"
            Log.e(mTag, "LiveKit connect/publish failed: ${mLastError}", it)
        }
    }

    fun pushVideoFrame(imageProxy: ImageProxy) {
        if (!mIsConnected) return
        mCapturer.pushImageProxy(imageProxy)
        mFramesSubmitted += 1
    }

    suspend fun publishLandmarks(payload: ByteArray, topic: String): Result<Unit> {
        if (!mIsConnected) return Result.failure(IllegalStateException("LiveKit room is not connected"))
        return mRoom.localParticipant.publishData(
            data = payload,
            reliability = DataPublishReliability.LOSSY,
            topic = topic,
        ).onSuccess {
            mLandmarkPacketsSent += 1
        }.onFailure {
            mLastError = it.message ?: "landmark publish failed"
            Log.e(mTag, "Failed to publish landmarks: ${mLastError}", it)
        }
    }

    fun disconnect() {
        if (!mIsConnected) return
        mLocalVideoTrack?.let {
            it.stopCapture()
            it.stop()
            it.dispose()
        }
        mLocalVideoTrack = null
        mRoom.disconnect()
        mIsConnected = false
        mIsVideoPublished = false
        Log.d(mTag, "Disconnected from LiveKit room.")
    }

    fun getDebugStatus(): String {
        val errorPart = mLastError?.let { " error=$it" } ?: ""
        return "connected=$mIsConnected videoPublished=$mIsVideoPublished frames=$mFramesSubmitted landmarkPackets=$mLandmarkPacketsSent$errorPart"
    }
}
