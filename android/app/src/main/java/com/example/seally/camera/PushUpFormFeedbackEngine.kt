package com.example.seally.camera

import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

class PushUpFormFeedbackEngine {
    private val mElbowAngleWindow = ArrayDeque<Float>()
    private var mRepCount: Int = 0
    private var mCurrentPhase: MovementPhase = MovementPhase.STANDING
    private var mLastSmoothedElbowAngle: Float? = null
    private var mMinElbowAngleInRep: Float = Float.MAX_VALUE
    private var mHasDetectedStartPosition: Boolean = false
    private var mHasReachedDepthInCurrentRep: Boolean = false
    private var mHasAttemptedRepInCurrentCycle: Boolean = false
    private var mHasAnnouncedUpInCurrentRep: Boolean = false
    private var mPersistedCue: String? = null
    private var mPersistedCueJoints: List<String> = emptyList()
    private var mPendingCue: String? = null
    private var mPendingCueJoints: List<String> = emptyList()
    private var mPendingCueFrames: Int = 0
    private var mClearFrames: Int = 0

    fun process(
        normalizedLandmarks: List<NormalizedLandmark>,
        worldLandmarks: List<Landmark>,
    ): FormFeedback {
        val selectedSide = selectBodySide(normalizedLandmarks) ?: return stepIntoFrameFeedback()
        val sideLandmarks = getSideLandmarks(selectedSide, normalizedLandmarks, worldLandmarks) ?: return stepIntoFrameFeedback()

        val elbowAngle = calculateAngleDeg(
            ax = sideLandmarks.mShoulder.x(),
            ay = sideLandmarks.mShoulder.y(),
            bx = sideLandmarks.mElbow.x(),
            by = sideLandmarks.mElbow.y(),
            cx = sideLandmarks.mWrist.x(),
            cy = sideLandmarks.mWrist.y(),
        )
        val kneeAngle = calculateAngleDeg(
            ax = sideLandmarks.mHip.x(),
            ay = sideLandmarks.mHip.y(),
            bx = sideLandmarks.mKnee.x(),
            by = sideLandmarks.mKnee.y(),
            cx = sideLandmarks.mAnkle.x(),
            cy = sideLandmarks.mAnkle.y(),
        )
        val hipAngle = calculateAngleDeg(
            ax = sideLandmarks.mShoulder.x(),
            ay = sideLandmarks.mShoulder.y(),
            bx = sideLandmarks.mHip.x(),
            by = sideLandmarks.mHip.y(),
            cx = sideLandmarks.mAnkle.x(),
            cy = sideLandmarks.mAnkle.y(),
        )
        val smoothedElbowAngle = smoothElbowAngle(elbowAngle)
        val previousPhase = mCurrentPhase
        mCurrentPhase = determinePhase(smoothedElbowAngle, previousPhase)
        val shoulderAnkleXDistance = abs(sideLandmarks.mShoulder.x() - sideLandmarks.mAnkle.x())
        val shoulderAnkleYDistance = abs(sideLandmarks.mShoulder.y() - sideLandmarks.mAnkle.y())
        val isInPushUpPosition = shoulderAnkleXDistance >= MIN_SHOULDER_ANKLE_SPAN_M &&
            shoulderAnkleXDistance >= (shoulderAnkleYDistance * PUSHUP_HORIZONTAL_RATIO)
        val neutralHipY = (sideLandmarks.mShoulder.y() + sideLandmarks.mAnkle.y()) / 2f
        val hipYOffset = sideLandmarks.mHip.y() - neutralHipY
        val isKneesStraight = kneeAngle >= PUSHUP_MIN_KNEE_ANGLE_DEG
        val isHipsAligned = hipAngle >= PUSHUP_MIN_HIP_ANGLE_DEG &&
            abs(hipYOffset) <= PUSHUP_HIP_Y_OFFSET_M
        val isArmsStraight = smoothedElbowAngle >= START_ELBOW_ANGLE_DEG
        val isStartPosition = isInPushUpPosition && isKneesStraight && isHipsAligned && isArmsStraight

        var frameCue: String? = null
        var frameJoints: List<String> = emptyList()
        var speechCue: String? = null

        if (!isInPushUpPosition) {
            frameCue = "Get into position"
            frameJoints = listOf("shoulder", "hip", "ankle")
            clearRepCycle()
            mHasDetectedStartPosition = false
        } else if (!isKneesStraight) {
            frameCue = "Lift your knees off the ground"
            frameJoints = listOf("hip", "knee", "ankle")
        } else if (!isHipsAligned) {
            frameCue = if (hipYOffset > PUSHUP_HIP_Y_OFFSET_M) {
                "Lift your hips"
            } else if (hipYOffset < -PUSHUP_HIP_Y_OFFSET_M) {
                "Lower your hips"
            } else {
                "Keep your body in a straight line"
            }
            frameJoints = listOf("shoulder", "hip", "ankle")
        } else if (!mHasDetectedStartPosition) {
            if (isArmsStraight) {
                mHasDetectedStartPosition = true
                mHasReachedDepthInCurrentRep = false
                mHasAttemptedRepInCurrentCycle = false
                mHasAnnouncedUpInCurrentRep = false
                speechCue = BEGIN_PUSHUP_CUE
            } else {
                frameCue = "Straighten your arms"
                frameJoints = listOf("shoulder", "elbow", "wrist")
            }
        } else {
            if (isStartPosition && previousPhase == MovementPhase.ASCENDING && mHasReachedDepthInCurrentRep) {
                mRepCount += 1
                speechCue = REP_COMPLETE_CUE
                clearRepCycle()
            } else if (isStartPosition && mHasAttemptedRepInCurrentCycle && !mHasReachedDepthInCurrentRep) {
                frameCue = "Not deep enough"
                frameJoints = listOf("shoulder", "elbow", "wrist")
                clearRepCycle()
            } else {
                if (!isStartPosition && previousPhase == MovementPhase.STANDING) {
                    mHasAttemptedRepInCurrentCycle = true
                }
                if (!mHasReachedDepthInCurrentRep && mMinElbowAngleInRep <= REQUIRED_DEPTH_ELBOW_ANGLE_DEG) {
                    mHasReachedDepthInCurrentRep = true
                    if (!mHasAnnouncedUpInCurrentRep) {
                        mHasAnnouncedUpInCurrentRep = true
                        speechCue = UP_CUE
                    }
                }
            }
        }

        if (!isStartPosition) {
            mMinElbowAngleInRep = minOf(mMinElbowAngleInRep, smoothedElbowAngle)
        } else if (!mHasAttemptedRepInCurrentCycle) {
            mMinElbowAngleInRep = Float.MAX_VALUE
        }
        mLastSmoothedElbowAngle = smoothedElbowAngle
        stabilizeCue(frameCue, frameJoints)

        val isCorrecting = mPersistedCue != null
        val status = when {
            isCorrecting -> ExerciseStatus.ERROR
            mCurrentPhase == MovementPhase.STANDING -> ExerciseStatus.READY
            else -> ExerciseStatus.ACTIVE
        }
        return FormFeedback(
            mPrimaryCue = mPersistedCue,
            mSpeechCue = speechCue,
            mStatus = status,
            mRepCount = mRepCount,
            mCurrentPhase = mCurrentPhase,
            mIsCorrecting = isCorrecting,
            mProblematicJoints = mPersistedCueJoints,
            mErrorMessage = mPersistedCue,
        )
    }

    fun reset() {
        mElbowAngleWindow.clear()
        mRepCount = 0
        mCurrentPhase = MovementPhase.STANDING
        mLastSmoothedElbowAngle = null
        mMinElbowAngleInRep = Float.MAX_VALUE
        mHasDetectedStartPosition = false
        mHasReachedDepthInCurrentRep = false
        mHasAttemptedRepInCurrentCycle = false
        mHasAnnouncedUpInCurrentRep = false
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mClearFrames = 0
    }

    private fun stepIntoFrameFeedback(): FormFeedback {
        mElbowAngleWindow.clear()
        mCurrentPhase = MovementPhase.STANDING
        mLastSmoothedElbowAngle = null
        mMinElbowAngleInRep = Float.MAX_VALUE
        mHasDetectedStartPosition = false
        mHasReachedDepthInCurrentRep = false
        mHasAttemptedRepInCurrentCycle = false
        mHasAnnouncedUpInCurrentRep = false
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mClearFrames = 0
        return FormFeedback(
            mPrimaryCue = "Step into frame",
            mStatus = ExerciseStatus.ERROR,
            mRepCount = mRepCount,
            mCurrentPhase = MovementPhase.STANDING,
            mIsCorrecting = true,
            mProblematicJoints = listOf("shoulder", "elbow", "wrist", "hip", "knee", "ankle"),
            mErrorMessage = "Step into frame",
        )
    }

    private fun clearRepCycle() {
        mHasReachedDepthInCurrentRep = false
        mHasAttemptedRepInCurrentCycle = false
        mHasAnnouncedUpInCurrentRep = false
        mMinElbowAngleInRep = Float.MAX_VALUE
    }

    private fun smoothElbowAngle(elbowAngle: Float): Float {
        mElbowAngleWindow.addLast(elbowAngle)
        if (mElbowAngleWindow.size > SMOOTHING_WINDOW_FRAMES) {
            mElbowAngleWindow.removeFirst()
        }
        return mElbowAngleWindow.average().toFloat()
    }

    private fun determinePhase(
        elbowAngle: Float,
        previousPhase: MovementPhase,
    ): MovementPhase {
        val previousAngle = mLastSmoothedElbowAngle
        val angleDelta = if (previousAngle != null) elbowAngle - previousAngle else 0f
        val isIncreasing = angleDelta > ANGLE_DELTA_EPSILON
        val isDecreasing = angleDelta < -ANGLE_DELTA_EPSILON

        return when {
            elbowAngle >= START_ELBOW_ANGLE_DEG -> MovementPhase.STANDING
            elbowAngle <= BOTTOM_ELBOW_ANGLE_DEG -> MovementPhase.BOTTOM
            isDecreasing -> MovementPhase.DESCENDING
            isIncreasing -> MovementPhase.ASCENDING
            else -> previousPhase
        }
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
    ): PushUpSideLandmarks? {
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

        return PushUpSideLandmarks(
            mShoulder = worldLandmarks[side.mShoulder],
            mElbow = worldLandmarks[side.mElbow],
            mWrist = worldLandmarks[side.mWrist],
            mHip = worldLandmarks[side.mHip],
            mKnee = worldLandmarks[side.mKnee],
            mAnkle = worldLandmarks[side.mAnkle],
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

    private data class PushUpSideLandmarks(
        val mShoulder: Landmark,
        val mElbow: Landmark,
        val mWrist: Landmark,
        val mHip: Landmark,
        val mKnee: Landmark,
        val mAnkle: Landmark,
    )

    companion object {
        private const val MIN_VISIBILITY = 0.6f
        private const val PUSHUP_MIN_HIP_ANGLE_DEG = 155f
        private const val PUSHUP_MIN_KNEE_ANGLE_DEG = 155f
        private const val PUSHUP_HIP_Y_OFFSET_M = 0.08f
        private const val MIN_SHOULDER_ANKLE_SPAN_M = 0.25f
        private const val PUSHUP_HORIZONTAL_RATIO = 1.1f
        private const val START_ELBOW_ANGLE_DEG = 155f
        private const val BOTTOM_ELBOW_ANGLE_DEG = 90f
        private const val REQUIRED_DEPTH_ELBOW_ANGLE_DEG = 95f
        private const val ANGLE_DELTA_EPSILON = 0.5f
        private const val SMOOTHING_WINDOW_FRAMES = 5
        private const val PERSISTENCE_FRAMES = 8
        private const val CLEARANCE_FRAMES = 8
        private const val BEGIN_PUSHUP_CUE = "Begin pushup"
        private const val UP_CUE = "Up"
        private const val REP_COMPLETE_CUE = "Rep complete"
    }
}

private fun Float?.orElse(default: Float): Float = this ?: default
