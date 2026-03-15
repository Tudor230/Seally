package com.example.seally.camera

import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

class PlankFormFeedbackEngine {
    private var mAccumulatedGoodFormMs: Long = 0L
    private var mMaxGoodFormMs: Long = 0L
    private var mLastGoodFormTimestampMs: Long? = null
    private var mLastSpeechTimestamp: Long = 0L

    fun process(
        normalizedLandmarks: List<NormalizedLandmark>,
        worldLandmarks: List<Landmark>,
        frameTimestampMs: Long,
    ): FormFeedback {
        val selectedSide = selectBodySide(normalizedLandmarks) ?: return stepIntoFrameFeedback()
        val sideLandmarks = getSideLandmarks(selectedSide, normalizedLandmarks, worldLandmarks) ?: return stepIntoFrameFeedback()

        val hipAngle = calculateAngleDeg(
            ax = sideLandmarks.mShoulder.x(),
            ay = sideLandmarks.mShoulder.y(),
            bx = sideLandmarks.mHip.x(),
            by = sideLandmarks.mHip.y(),
            cx = sideLandmarks.mAnkle.x(),
            cy = sideLandmarks.mAnkle.y(),
        )
        val kneeAngle = calculateAngleDeg(
            ax = sideLandmarks.mHip.x(),
            ay = sideLandmarks.mHip.y(),
            bx = sideLandmarks.mKnee.x(),
            by = sideLandmarks.mKnee.y(),
            cx = sideLandmarks.mAnkle.x(),
            cy = sideLandmarks.mAnkle.y(),
        )
        val elbowAngle = calculateAngleDeg(
            ax = sideLandmarks.mShoulder.x(),
            ay = sideLandmarks.mShoulder.y(),
            bx = sideLandmarks.mElbow.x(),
            by = sideLandmarks.mElbow.y(),
            cx = sideLandmarks.mWrist.x(),
            cy = sideLandmarks.mWrist.y(),
        )
        val neutralHipY = (sideLandmarks.mShoulder.y() + sideLandmarks.mAnkle.y()) / 2f
        val hipYOffset = sideLandmarks.mHip.y() - neutralHipY
        val hipElbowYDistance = abs(sideLandmarks.mHip.y() - sideLandmarks.mElbow.y())

        var cue: String? = null
        var joints: List<String> = emptyList()

        val shoulderAnkleXDistance = abs(sideLandmarks.mShoulder.x() - sideLandmarks.mAnkle.x())
        val shoulderAnkleYDistance = abs(sideLandmarks.mShoulder.y() - sideLandmarks.mAnkle.y())
        val isInPlankPosition = shoulderAnkleXDistance >= MIN_SHOULDER_ANKLE_SPAN_M &&
            shoulderAnkleXDistance >= (shoulderAnkleYDistance * PLANK_HORIZONTAL_RATIO)

        if (!isInPlankPosition) {
            cue = "Get into position"
            joints = listOf("shoulder", "hip", "knee", "ankle")
        } else if (elbowAngle > PLANK_MAX_ELBOW_ANGLE_DEG) {
            cue = "Get on your elbows"
            joints = listOf("elbow", "wrist")
        } else if (kneeAngle < PLANK_MIN_KNEE_ANGLE_DEG) {
            cue = "Lift your knees off the ground"
            joints = listOf("hip", "knee", "ankle")
        } else if (hipElbowYDistance <= PLANK_HIP_ELBOW_LEVEL_TOLERANCE_M) {
            cue = "Lift your hips"
            joints = listOf("shoulder", "elbow", "hip", "knee", "ankle")
        } else if (hipAngle < PLANK_MIN_HIP_ANGLE_DEG) {
            cue = if (hipYOffset < -PLANK_HIP_Y_OFFSET_M) {
                "Lower your hips"
            } else {
                "Keep your body in a straight line"
            }
            joints = listOf("shoulder", "elbow", "hip", "knee", "ankle")
        }

        val speechCue = maybeSpeak(cue)

        val isGoodForm = cue == null
        updateTimer(isGoodForm = isGoodForm, frameTimestampMs = frameTimestampMs)

        return FormFeedback(
            mPrimaryCue = cue,
            mSpeechCue = speechCue,
            mStatus = if (isGoodForm) ExerciseStatus.ACTIVE else ExerciseStatus.ERROR,
            mCurrentPhase = MovementPhase.STANDING,
            mHoldDurationMs = mAccumulatedGoodFormMs,
            mMaxHoldDurationMs = mMaxGoodFormMs,
            mIsCorrecting = !isGoodForm,
            mProblematicJoints = joints,
            mErrorMessage = speechCue ?: cue,
        )
    }

    fun reset() {
        mAccumulatedGoodFormMs = 0L
        mMaxGoodFormMs = 0L
        mLastGoodFormTimestampMs = null
        mLastSpeechTimestamp = 0L
    }

    private fun stepIntoFrameFeedback(): FormFeedback {
        mLastGoodFormTimestampMs = null
        val stepIntoFrameCue = maybeSpeak("Step into frame")
        return FormFeedback(
            mPrimaryCue = "Step into frame",
            mStatus = ExerciseStatus.ERROR,
            mCurrentPhase = MovementPhase.STANDING,
            mHoldDurationMs = mAccumulatedGoodFormMs,
            mMaxHoldDurationMs = mMaxGoodFormMs,
            mIsCorrecting = true,
            mProblematicJoints = listOf("shoulder", "hip", "ankle"),
            mErrorMessage = stepIntoFrameCue,
        )
    }

    private fun updateTimer(isGoodForm: Boolean, frameTimestampMs: Long) {
        if (!isGoodForm) {
            mMaxGoodFormMs = maxOf(mMaxGoodFormMs, mAccumulatedGoodFormMs)
            mLastGoodFormTimestampMs = null
            mAccumulatedGoodFormMs = 0L
            return
        }

        val lastGoodTimestamp = mLastGoodFormTimestampMs
        if (lastGoodTimestamp != null) {
            mAccumulatedGoodFormMs += (frameTimestampMs - lastGoodTimestamp).coerceAtLeast(0L)
        }
        mMaxGoodFormMs = maxOf(mMaxGoodFormMs, mAccumulatedGoodFormMs)
        mLastGoodFormTimestampMs = frameTimestampMs
    }

    private fun selectBodySide(landmarks: List<NormalizedLandmark>): BodySide? {
        if (landmarks.size <= BodySide.RIGHT.mAnkle) return null
        val leftScore = averageVisibility(landmarks, BodySide.LEFT)
        val rightScore = averageVisibility(landmarks, BodySide.RIGHT)
        val selected = if (leftScore >= rightScore) BodySide.LEFT else BodySide.RIGHT
        return if (averageVisibility(landmarks, selected) >= MIN_VISIBILITY) selected else null
    }

    private fun averageVisibility(
        landmarks: List<NormalizedLandmark>,
        side: BodySide,
    ): Float {
        val values = listOf(
            landmarks[side.mShoulder].visibility().orElse(0f),
            landmarks[side.mElbow].visibility().orElse(0f),
            landmarks[side.mWrist].visibility().orElse(0f),
            landmarks[side.mHip].visibility().orElse(0f),
            landmarks[side.mKnee].visibility().orElse(0f),
            landmarks[side.mAnkle].visibility().orElse(0f),
        )
        return values.average().toFloat()
    }

    private fun getSideLandmarks(
        side: BodySide,
        normalizedLandmarks: List<NormalizedLandmark>,
        worldLandmarks: List<Landmark>,
    ): PlankSideLandmarks? {
        if (worldLandmarks.size <= side.mAnkle || normalizedLandmarks.size <= side.mAnkle) return null
        val selectedVisibility = listOf(
            normalizedLandmarks[side.mShoulder].visibility().orElse(0f),
            normalizedLandmarks[side.mElbow].visibility().orElse(0f),
            normalizedLandmarks[side.mWrist].visibility().orElse(0f),
            normalizedLandmarks[side.mHip].visibility().orElse(0f),
            normalizedLandmarks[side.mKnee].visibility().orElse(0f),
            normalizedLandmarks[side.mAnkle].visibility().orElse(0f),
        )
        if (selectedVisibility.any { it < MIN_VISIBILITY }) return null

        return PlankSideLandmarks(
            mShoulder = worldLandmarks[side.mShoulder],
            mElbow = worldLandmarks[side.mElbow],
            mWrist = worldLandmarks[side.mWrist],
            mHip = worldLandmarks[side.mHip],
            mKnee = worldLandmarks[side.mKnee],
            mAnkle = worldLandmarks[side.mAnkle],
            mFootIndex = worldLandmarks[side.mFootIndex],
        )
    }

    private fun calculateAngleDeg(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
    ): Float {
        val abx = ax - bx
        val aby = ay - by
        val cbx = cx - bx
        val cby = cy - by
        val dot = (abx * cbx) + (aby * cby)
        val cross = (abx * cby) - (aby * cbx)
        return abs(Math.toDegrees(atan2(cross.toDouble(), dot.toDouble()))).toFloat()
    }

    private fun maybeSpeak(message: String?): String? {
        if (message == null) return null
        val now = System.currentTimeMillis()
        return if (now - mLastSpeechTimestamp >= 2000L) {
            mLastSpeechTimestamp = now
            message
        } else {
            null
        }
    }

    private data class PlankSideLandmarks(
        val mShoulder: Landmark,
        val mElbow: Landmark,
        val mWrist: Landmark,
        val mHip: Landmark,
        val mKnee: Landmark,
        val mAnkle: Landmark,
        val mFootIndex: Landmark,
    )

    companion object {
        private const val MIN_VISIBILITY = 0.6f
        private const val PLANK_MAX_ELBOW_ANGLE_DEG = 120f
        private const val PLANK_MIN_HIP_ANGLE_DEG = 155f
        private const val PLANK_MIN_KNEE_ANGLE_DEG = 155f
        private const val PLANK_HIP_Y_OFFSET_M = 0.08f
        private const val PLANK_HIP_ELBOW_LEVEL_TOLERANCE_M = 0.16f
        private const val MIN_SHOULDER_ANKLE_SPAN_M = 0.25f
        private const val PLANK_HORIZONTAL_RATIO = 1.1f
    }
}

private fun Float?.orElse(default: Float): Float = this ?: default
