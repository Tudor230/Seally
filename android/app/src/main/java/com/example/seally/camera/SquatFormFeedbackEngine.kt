package com.example.seally.camera

import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

class SquatFormFeedbackEngine {
    private val mKneeAngleWindow = ArrayDeque<Float>()
    private var mRepCount: Int = 0
    private var mCurrentPhase: MovementPhase = MovementPhase.STANDING
    private var mLastSmoothedKneeAngle: Float? = null
    private var mMinKneeAngleInRep: Float = Float.MAX_VALUE
    private var mPersistedCue: String? = null
    private var mPersistedCueJoints: List<String> = emptyList()
    private var mPendingCue: String? = null
    private var mPendingCueJoints: List<String> = emptyList()
    private var mPendingCueFrames: Int = 0
    private var mClearFrames: Int = 0
    private var mHasDetectedStartPosition: Boolean = false
    private var mHasReachedDepthInCurrentRep: Boolean = false
    private var mHasAnnouncedNowUpInCurrentRep: Boolean = false
    private var mLastSpeechTimestamp: Long = 0

    fun process(
        normalizedLandmarks: List<NormalizedLandmark>,
        worldLandmarks: List<Landmark>,
    ): FormFeedback {
        val selectedSide = selectBodySide(normalizedLandmarks) ?: return stepIntoFrameFeedback()
        val sideLandmarks = getSideLandmarks(selectedSide, normalizedLandmarks, worldLandmarks) ?: return stepIntoFrameFeedback()

        val rawKneeAngle = calculateAngleDeg(
            ax = sideLandmarks.mHip.x(),
            ay = sideLandmarks.mHip.y(),
            bx = sideLandmarks.mKnee.x(),
            by = sideLandmarks.mKnee.y(),
            cx = sideLandmarks.mAnkle.x(),
            cy = sideLandmarks.mAnkle.y(),
        )
        val smoothedKneeAngle = smoothKneeAngle(rawKneeAngle)

        var frameCue: String? = null
        var frameJoints: List<String> = emptyList()
        var speechCue: String? = null
        val previousPhase = mCurrentPhase
        mCurrentPhase = determinePhase(smoothedKneeAngle, previousPhase)
        val forwardDirection = calculateForwardDirection(sideLandmarks)

        val startPositionDetected = isStartPosition(
            landmarks = sideLandmarks,
            kneeAngle = smoothedKneeAngle,
            forwardDirection = forwardDirection,
        )
        if (startPositionDetected && !mHasDetectedStartPosition) {
            mHasDetectedStartPosition = true
            mHasReachedDepthInCurrentRep = false
            mHasAnnouncedNowUpInCurrentRep = false
            speechCue = START_EXERCISE_CUE
        }

        if (
            mHasDetectedStartPosition &&
            !mHasReachedDepthInCurrentRep &&
            smoothedKneeAngle < REQUIRED_DEPTH_ANGLE_DEG
        ) {
            mHasReachedDepthInCurrentRep = true
            if (!mHasAnnouncedNowUpInCurrentRep) {
                mHasAnnouncedNowUpInCurrentRep = true
                speechCue = NOW_UP_CUE
            }
        }

        if (mCurrentPhase == MovementPhase.STANDING) {
            mMinKneeAngleInRep = Float.MAX_VALUE
        } else {
            mMinKneeAngleInRep = minOf(mMinKneeAngleInRep, smoothedKneeAngle)
        }

        val transitionedToAscending = (previousPhase == MovementPhase.DESCENDING || previousPhase == MovementPhase.BOTTOM) &&
            mCurrentPhase == MovementPhase.ASCENDING
        if (transitionedToAscending && mMinKneeAngleInRep > REQUIRED_DEPTH_ANGLE_DEG) {
            frameCue = "Not going low enough"
            frameJoints = listOf("hip", "knee", "ankle")
        }

        if (frameCue == null && (mCurrentPhase == MovementPhase.DESCENDING || mCurrentPhase == MovementPhase.BOTTOM)) {
            val shoulderForwardDelta = (sideLandmarks.mShoulder.x() - sideLandmarks.mAnkle.x()) * forwardDirection
            if (shoulderForwardDelta > SHOULDER_FORWARD_MAX_M) {
                frameCue = "Keep your chest up"
                frameJoints = listOf("shoulder", "ankle")
            } else if ((sideLandmarks.mKnee.x() - sideLandmarks.mFootIndex.x()) > KNEE_FORWARD_MAX_M) {
                frameCue = "Knees over mid-foot"
                frameJoints = listOf("knee", "ankle")
            }
        }

        if (speechCue == null) {
            speechCue = frameCue
        }
        speechCue = maybeSpeak(speechCue)

        val hasCompletedRep = mHasDetectedStartPosition &&
            mHasReachedDepthInCurrentRep &&
            previousPhase == MovementPhase.ASCENDING &&
            mCurrentPhase == MovementPhase.STANDING &&
            mMinKneeAngleInRep <= REQUIRED_DEPTH_ANGLE_DEG
        if (hasCompletedRep) {
            mRepCount += 1
            mHasReachedDepthInCurrentRep = false
            mHasAnnouncedNowUpInCurrentRep = false
        }

        mLastSmoothedKneeAngle = smoothedKneeAngle
        stabilizeCue(frameCue, frameJoints)

        val isCorrecting = mPersistedCue != null
        val status = when {
            isCorrecting -> ExerciseStatus.ERROR
            mCurrentPhase == MovementPhase.STANDING -> ExerciseStatus.READY
            else -> ExerciseStatus.ACTIVE
        }
        val debugMinKneeAngle = if (mMinKneeAngleInRep == Float.MAX_VALUE) null else mMinKneeAngleInRep

        return FormFeedback(
            mPrimaryCue = mPersistedCue,
            mSpeechCue = speechCue,
            mStatus = status,
            mRepCount = mRepCount,
            mCurrentPhase = mCurrentPhase,
            mDebugKneeAngleDeg = smoothedKneeAngle,
            mDebugMinKneeAngleDeg = debugMinKneeAngle,
            mIsCorrecting = isCorrecting,
            mProblematicJoints = mPersistedCueJoints,
            mErrorMessage = mPersistedCue,
        )
    }

    fun reset() {
        mKneeAngleWindow.clear()
        mRepCount = 0
        mCurrentPhase = MovementPhase.STANDING
        mLastSmoothedKneeAngle = null
        mMinKneeAngleInRep = Float.MAX_VALUE
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mClearFrames = 0
        mHasDetectedStartPosition = false
        mHasReachedDepthInCurrentRep = false
        mHasAnnouncedNowUpInCurrentRep = false
        mLastSpeechTimestamp = 0
    }

    private fun stepIntoFrameFeedback(): FormFeedback {
        mKneeAngleWindow.clear()
        mCurrentPhase = MovementPhase.STANDING
        mLastSmoothedKneeAngle = null
        mMinKneeAngleInRep = Float.MAX_VALUE
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mClearFrames = 0
        mHasDetectedStartPosition = false
        mHasReachedDepthInCurrentRep = false
        mHasAnnouncedNowUpInCurrentRep = false
        val stepIntoFrameCue = maybeSpeak("Step into frame")
        return FormFeedback(
            mPrimaryCue = "Step into frame",
            mStatus = ExerciseStatus.ERROR,
            mRepCount = mRepCount,
            mCurrentPhase = MovementPhase.STANDING,
            mIsCorrecting = true,
            mProblematicJoints = listOf("shoulder", "hip", "knee", "ankle"),
            mErrorMessage = stepIntoFrameCue,
        )
    }

    private fun isStartPosition(
        landmarks: SideLandmarks,
        kneeAngle: Float,
        forwardDirection: Float,
    ): Boolean {
        val elbowAngle = calculateAngleDeg(
            ax = landmarks.mShoulder.x(),
            ay = landmarks.mShoulder.y(),
            bx = landmarks.mElbow.x(),
            by = landmarks.mElbow.y(),
            cx = landmarks.mWrist.x(),
            cy = landmarks.mWrist.y(),
        )
        val isArmStraight = elbowAngle >= ARM_STRAIGHT_MIN_ANGLE_DEG
        val wristForwardDelta = (landmarks.mWrist.x() - landmarks.mShoulder.x()) * forwardDirection
        val isArmForward = wristForwardDelta >= ARM_FORWARD_MIN_DISTANCE_M
        val isArmAtShoulderHeight = abs(landmarks.mWrist.y() - landmarks.mShoulder.y()) <= ARM_HEIGHT_TOLERANCE_M
        val isStanding = kneeAngle > STANDING_ANGLE_DEG
        return isStanding && isArmStraight && isArmForward && isArmAtShoulderHeight
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
    ): SideLandmarks? {
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

        return SideLandmarks(
            mShoulder = worldLandmarks[side.mShoulder],
            mElbow = worldLandmarks[side.mElbow],
            mWrist = worldLandmarks[side.mWrist],
            mHip = worldLandmarks[side.mHip],
            mKnee = worldLandmarks[side.mKnee],
            mAnkle = worldLandmarks[side.mAnkle],
            mFootIndex = worldLandmarks[side.mFootIndex],
        )
    }

    private fun smoothKneeAngle(kneeAngle: Float): Float {
        mKneeAngleWindow.addLast(kneeAngle)
        if (mKneeAngleWindow.size > SMOOTHING_WINDOW_FRAMES) {
            mKneeAngleWindow.removeFirst()
        }
        return mKneeAngleWindow.average().toFloat()
    }

    private fun determinePhase(
        kneeAngle: Float,
        previousPhase: MovementPhase,
    ): MovementPhase {
        val previousAngle = mLastSmoothedKneeAngle
        val angleDelta = if (previousAngle != null) kneeAngle - previousAngle else 0f
        val isIncreasing = angleDelta > ANGLE_DELTA_EPSILON
        val isDecreasing = angleDelta < -ANGLE_DELTA_EPSILON

        return when {
            kneeAngle > STANDING_ANGLE_DEG -> MovementPhase.STANDING
            kneeAngle < BOTTOM_ANGLE_DEG -> MovementPhase.BOTTOM
            isIncreasing -> MovementPhase.ASCENDING
            isDecreasing && kneeAngle < DESCENDING_THRESHOLD_DEG -> MovementPhase.DESCENDING
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

    private fun calculateForwardDirection(landmarks: SideLandmarks): Float {
        val kneeToAnkle = landmarks.mKnee.x() - landmarks.mAnkle.x()
        if (abs(kneeToAnkle) > DIRECTION_EPSILON_M) return if (kneeToAnkle >= 0f) 1f else -1f
        val hipToAnkle = landmarks.mHip.x() - landmarks.mAnkle.x()
        return if (hipToAnkle >= 0f) 1f else -1f
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

    private data class SideLandmarks(
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
        private const val STANDING_ANGLE_DEG = 165f
        private const val DESCENDING_THRESHOLD_DEG = 160f
        private const val BOTTOM_ANGLE_DEG = 100f
        private const val REQUIRED_DEPTH_ANGLE_DEG = 90f
        private const val SHOULDER_FORWARD_MAX_M = 0.16f
        private const val KNEE_FORWARD_MAX_M = 0.05f
        private const val ARM_STRAIGHT_MIN_ANGLE_DEG = 160f
        private const val ARM_FORWARD_MIN_DISTANCE_M = 0.08f
        private const val ARM_HEIGHT_TOLERANCE_M = 0.12f
        private const val DIRECTION_EPSILON_M = 0.01f
        private const val ANGLE_DELTA_EPSILON = 0.5f
        private const val SMOOTHING_WINDOW_FRAMES = 5
        private const val PERSISTENCE_FRAMES = 10
        private const val CLEARANCE_FRAMES = 10
        private const val START_EXERCISE_CUE = "Start exercise"
        private const val NOW_UP_CUE = "Now up"
    }
}

private fun Float?.orElse(default: Float): Float = this ?: default
