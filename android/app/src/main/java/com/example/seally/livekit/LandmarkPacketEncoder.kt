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
    private const val mProblematicJointsFlag: Byte = 0x02

    fun encode(
        sequence: Long,
        timestampMs: Long,
        frameWidth: Int,
        frameHeight: Int,
        isFrontCamera: Boolean,
        landmarks: List<NormalizedLandmark>,
        problematicJoints: List<Int> = emptyList(),
    ): ByteArray {
        val hasProblematicJoints = problematicJoints.isNotEmpty()
        val landmarkCount = landmarks.size.coerceIn(0, UShort.MAX_VALUE.toInt())
        val problematicBitmask = if (hasProblematicJoints) {
            problematicJoints.fold(0) { mask, jointIndex ->
                mask or (1 shl (jointIndex and 0x1F))
            }
        } else {
            0
        }

        val extraBytes = if (hasProblematicJoints) 4 else 0
        val payload = ByteBuffer.allocate(mHeaderBytes + landmarkCount * mBytesPerLandmark + extraBytes)
            .order(ByteOrder.LITTLE_ENDIAN)

        var flags = 0
        if (isFrontCamera) flags = flags or mFrontCameraFlag.toInt()
        if (hasProblematicJoints) flags = flags or mProblematicJointsFlag.toInt()

        payload.put(mPacketVersion)
        payload.put(flags.toByte())
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

        if (hasProblematicJoints) {
            payload.putInt(problematicBitmask)
        }

        return payload.array()
    }

    private fun quantizeCoordinate(value: Float): Short {
        val scaledValue = (value * mCoordScale).roundToInt()
        return scaledValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}
