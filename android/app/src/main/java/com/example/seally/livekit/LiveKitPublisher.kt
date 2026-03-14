package com.example.seally.livekit

import android.util.Log
import android.content.Context
import android.os.SystemClock
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.track.DataPublishReliability

class LiveKitPublisher(context: Context) {
    private val mTag = "LiveKitPublisher"
    private val mRoom: Room = LiveKit.create(context.applicationContext)
    private var mIsConnected: Boolean = false
    private var mIsVideoPublished: Boolean = false
    private var mLandmarkPacketsSent: Long = 0L
    private var mLandmarkPacketsPerSecond: Int = 0
    private var mSendPpsWindowStartMs: Long = 0L
    private var mSendPpsWindowPacketCount: Int = 0
    private var mLastError: String? = null

    suspend fun connect(url: String, token: String): Result<Any> {
        if (mIsConnected) return Result.success(Unit)

        return runCatching {
            Log.d(mTag, "Connecting to LiveKit room...")
            mRoom.connect(url = url, token = token)
            Log.d(mTag, "Connected to LiveKit room (landmarks-only mode).")
            mIsConnected = true
            mIsVideoPublished = false
            mLastError = null
        }.onFailure {
            mLastError = it.message ?: "unknown connect/publish failure"
            Log.e(mTag, "LiveKit connect/publish failed: ${mLastError}", it)
        }
    }

    suspend fun publishLandmarks(payload: ByteArray, topic: String): Result<Unit> {
        if (!mIsConnected) return Result.failure(IllegalStateException("LiveKit room is not connected"))
        val lossyResult = mRoom.localParticipant.publishData(
            data = payload,
            reliability = DataPublishReliability.LOSSY,
            topic = topic,
        )

        if (lossyResult.isSuccess) {
            mLandmarkPacketsSent += 1
            updateSendPps()
            return Result.success(Unit)
        }

        val lossyError = lossyResult.exceptionOrNull()
        val shouldFallbackToReliable = lossyError?.message
            ?.contains("channel not established", ignoreCase = true) == true
        if (shouldFallbackToReliable) {
            return mRoom.localParticipant.publishData(
                data = payload,
                reliability = DataPublishReliability.RELIABLE,
                topic = topic,
            ).onSuccess {
                mLandmarkPacketsSent += 1
                updateSendPps()
                mLastError = null
            }.onFailure {
                mLastError = it.message ?: "landmark reliable publish failed"
                Log.e(mTag, "Reliable landmark publish fallback failed: ${mLastError}", it)
            }
        }

        mLastError = lossyError?.message ?: "landmark publish failed"
        if (lossyError != null) {
            Log.e(mTag, "Failed to publish landmarks: ${mLastError}", lossyError)
        }
        return Result.failure(lossyError ?: IllegalStateException("landmark publish failed"))
    }

    fun disconnect() {
        if (!mIsConnected) return
        mRoom.disconnect()
        mIsConnected = false
        mIsVideoPublished = false
        mLandmarkPacketsPerSecond = 0
        mSendPpsWindowStartMs = 0L
        mSendPpsWindowPacketCount = 0
        Log.d(mTag, "Disconnected from LiveKit room.")
    }

    fun getDebugStatus(): String {
        val errorPart = mLastError?.let { " error=$it" } ?: ""
        return "connected=$mIsConnected videoPublished=$mIsVideoPublished landmarkPackets=$mLandmarkPacketsSent landmarkPps=$mLandmarkPacketsPerSecond$errorPart"
    }

    private fun updateSendPps() {
        val nowMs = SystemClock.elapsedRealtime()
        if (mSendPpsWindowStartMs == 0L) {
            mSendPpsWindowStartMs = nowMs
        }
        mSendPpsWindowPacketCount += 1
        val elapsedMs = nowMs - mSendPpsWindowStartMs
        if (elapsedMs < 1_000L) return

        mLandmarkPacketsPerSecond = ((mSendPpsWindowPacketCount * 1_000L) / elapsedMs).toInt()
        mSendPpsWindowStartMs = nowMs
        mSendPpsWindowPacketCount = 0
    }
}
