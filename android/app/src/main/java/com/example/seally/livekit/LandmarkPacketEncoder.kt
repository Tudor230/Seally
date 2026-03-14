package com.example.seally.livekit

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object LandmarkPacketEncoder {
    private const val mPacketVersion: Byte = 2
    private const val mCoordScale: Float = 10_000f
    private const val mHeaderBytes: Int = 16
    private const val mBytesPerLandmark: Int = 6
    private const val mFrontCameraFlag: Byte = 0x01

    fun encode(
        sequence: Long,
        timestampMs: Long,
        frameWidth: Int,
        frameHeight: Int,
        isFrontCamera: Boolean,
        landmarks: List<NormalizedLandmark>,
    ): ByteArray {
        val landmarkCount = landmarks.size.coerceIn(0, UShort.MAX_VALUE.toInt())
        val payload = ByteBuffer.allocate(mHeaderBytes + landmarkCount * mBytesPerLandmark)
            .order(ByteOrder.LITTLE_ENDIAN)

        payload.put(mPacketVersion)
        payload.put(if (isFrontCamera) mFrontCameraFlag else 0)
        payload.putShort(landmarkCount.toShort())
        payload.putInt(sequence.toInt())
        payload.putInt(timestampMs.toInt())
        payload.putShort(frameWidth.coerceIn(0, UShort.MAX_VALUE.toInt()).toShort())
        payload.putShort(frameHeight.coerceIn(0, UShort.MAX_VALUE.toInt()).toShort())

        for (index in 0 until landmarkCount) {
            val landmark = landmarks[index]
            payload.putShort(quantizeCoordinate(landmark.x()))
            payload.putShort(quantizeCoordinate(landmark.y()))
            payload.putShort(quantizeCoordinate(landmark.z()))
        }

        return payload.array()
    }

    private fun quantizeCoordinate(value: Float): Short {
        val scaledValue = (value * mCoordScale).roundToInt()
        return scaledValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}
