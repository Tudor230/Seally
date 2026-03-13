package com.example.seally.camera

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

private enum class PullUpPhase {
    DEAD_HANG,
    PULLING,
    TOP,
    LOWERING,
}

class PullUpFormFeedbackEngine {
    private var mRepCount: Int = 0
    private var mPhase: PullUpPhase = PullUpPhase.DEAD_HANG
    private var mHasReachedTopInCurrentRep: Boolean = false
    private var mPendingCue: String? = null
    private var mPendingCueJoints: List<String> = emptyList()
    private var mPendingCueFrames: Int = 0
    private var mPersistedCue: String? = null
    private var mPersistedCueJoints: List<String> = emptyList()
    private var mClearFrames: Int = 0

    fun process(
        normalizedLandmarks: List<NormalizedLandmark>,
        barCalibration: PullUpBarCalibration?,
    ): FormFeedback {
        if (barCalibration == null) {
            resetTrackingOnly()
            return FormFeedback(
                mPrimaryCue = "Tap left and right bar ends",
                mStatus = ExerciseStatus.INITIALIZING,
                mRepCount = mRepCount,
                mIsCorrecting = true,
                mProblematicJoints = listOf("left hand", "right hand"),
                mErrorMessage = "Tap left and right bar ends",
            )
        }

        val frontLandmarks = getFrontLandmarks(normalizedLandmarks) ?: return FormFeedback(
            mPrimaryCue = "Step into frame",
            mStatus = ExerciseStatus.ERROR,
            mRepCount = mRepCount,
            mIsCorrecting = true,
            mProblematicJoints = listOf("shoulders", "elbows", "wrists"),
            mErrorMessage = "Step into frame",
        )

        val leftElbowAngle = calculateAngleDeg(
            ax = frontLandmarks.mLeftShoulder.x(),
            ay = frontLandmarks.mLeftShoulder.y(),
            bx = frontLandmarks.mLeftElbow.x(),
            by = frontLandmarks.mLeftElbow.y(),
            cx = frontLandmarks.mLeftWrist.x(),
            cy = frontLandmarks.mLeftWrist.y(),
        )
        val rightElbowAngle = calculateAngleDeg(
            ax = frontLandmarks.mRightShoulder.x(),
            ay = frontLandmarks.mRightShoulder.y(),
            bx = frontLandmarks.mRightElbow.x(),
            by = frontLandmarks.mRightElbow.y(),
            cx = frontLandmarks.mRightWrist.x(),
            cy = frontLandmarks.mRightWrist.y(),
        )
        val averageElbowAngle = (leftElbowAngle + rightElbowAngle) / 2f
        val chinY = (frontLandmarks.mMouthLeft.y() + frontLandmarks.mMouthRight.y()) / 2f
        val isChinAboveBar = chinY < (barCalibration.mBarY - CHIN_BAR_MARGIN)
        val isBelowBar = chinY > (barCalibration.mBarY + CHIN_BAR_MARGIN)
        val isArmsExtended = averageElbowAngle >= DEAD_HANG_ELBOW_ANGLE_DEG

        when (mPhase) {
            PullUpPhase.DEAD_HANG -> {
                if (!isArmsExtended || !isBelowBar) {
                    mPhase = PullUpPhase.PULLING
                    mHasReachedTopInCurrentRep = false
                }
            }
            PullUpPhase.PULLING -> {
                if (isChinAboveBar) {
                    mPhase = PullUpPhase.TOP
                    mHasReachedTopInCurrentRep = true
                } else if (isBelowBar && isArmsExtended) {
                    mPhase = PullUpPhase.DEAD_HANG
                }
            }
            PullUpPhase.TOP -> {
                if (!isChinAboveBar) {
                    mPhase = PullUpPhase.LOWERING
                }
            }
            PullUpPhase.LOWERING -> {
                if (isBelowBar && isArmsExtended) {
                    if (mHasReachedTopInCurrentRep) {
                        mRepCount += 1
                    }
                    mPhase = PullUpPhase.DEAD_HANG
                    mHasReachedTopInCurrentRep = false
                }
            }
        }

        var frameCue: String? = null
        var frameJoints: List<String> = emptyList()
        if (mPhase == PullUpPhase.LOWERING && !isArmsExtended && isBelowBar) {
            frameCue = "Fully extend your arms"
            frameJoints = listOf("elbows", "wrists")
        } else if (mPhase == PullUpPhase.PULLING && isBelowBar && isArmsExtended && !mHasReachedTopInCurrentRep) {
            frameCue = "Pull higher"
            frameJoints = listOf("chin", "bar")
        }

        stabilizeCue(frameCue, frameJoints)
        val isCorrecting = mPersistedCue != null
        return FormFeedback(
            mPrimaryCue = mPersistedCue,
            mStatus = if (isCorrecting) ExerciseStatus.ERROR else ExerciseStatus.ACTIVE,
            mRepCount = mRepCount,
            mIsCorrecting = isCorrecting,
            mProblematicJoints = mPersistedCueJoints,
            mErrorMessage = mPersistedCue,
        )
    }

    fun reset() {
        mRepCount = 0
        mPhase = PullUpPhase.DEAD_HANG
        mHasReachedTopInCurrentRep = false
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mClearFrames = 0
    }

    private fun resetTrackingOnly() {
        mPhase = PullUpPhase.DEAD_HANG
        mHasReachedTopInCurrentRep = false
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mClearFrames = 0
    }

    private fun stabilizeCue(cue: String?, joints: List<String>) {
        if (cue == null) {
            mPendingCue = null
            mPendingCueFrames = 0
            mPendingCueJoints = emptyList()
            if (mPersistedCue != null) {
                mClearFrames += 1
                if (mClearFrames >= CLEARANCE_FRAMES) {
                    mPersistedCue = null
                    mPersistedCueJoints = emptyList()
                    mClearFrames = 0
                }
            }
            return
        }
        mClearFrames = 0
        if (cue != mPendingCue) {
            mPendingCue = cue
            mPendingCueJoints = joints
            mPendingCueFrames = 1
            return
        }
        mPendingCueFrames += 1
        if (mPendingCueFrames >= PERSISTENCE_FRAMES) {
            mPersistedCue = cue
            mPersistedCueJoints = joints
        }
    }

    private fun getFrontLandmarks(landmarks: List<NormalizedLandmark>): PullUpFrontLandmarks? {
        val neededIndexes = listOf(9, 10, 11, 12, 13, 14, 15, 16)
        if (neededIndexes.any { it >= landmarks.size }) return null
        val visibilityValues = neededIndexes.map { landmarks[it].visibility().orElse(0f) }
        if (visibilityValues.any { it < MIN_VISIBILITY }) return null
        return PullUpFrontLandmarks(
            mMouthLeft = landmarks[9],
            mMouthRight = landmarks[10],
            mLeftShoulder = landmarks[11],
            mRightShoulder = landmarks[12],
            mLeftElbow = landmarks[13],
            mRightElbow = landmarks[14],
            mLeftWrist = landmarks[15],
            mRightWrist = landmarks[16],
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

    private data class PullUpFrontLandmarks(
        val mMouthLeft: NormalizedLandmark,
        val mMouthRight: NormalizedLandmark,
        val mLeftShoulder: NormalizedLandmark,
        val mRightShoulder: NormalizedLandmark,
        val mLeftElbow: NormalizedLandmark,
        val mRightElbow: NormalizedLandmark,
        val mLeftWrist: NormalizedLandmark,
        val mRightWrist: NormalizedLandmark,
    )

    companion object {
        private const val MIN_VISIBILITY = 0.6f
        private const val DEAD_HANG_ELBOW_ANGLE_DEG = 155f
        private const val CHIN_BAR_MARGIN = 0.015f
        private const val PERSISTENCE_FRAMES = 8
        private const val CLEARANCE_FRAMES = 8
    }
}

private fun Float?.orElse(default: Float): Float = this ?: default
