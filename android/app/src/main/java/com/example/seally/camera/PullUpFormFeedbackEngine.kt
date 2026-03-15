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
    private var mHasDetectedStartPosition: Boolean = false
    private var mHasReachedTopInCurrentRep: Boolean = false
    private var mHasAttemptedRepInCurrentCycle: Boolean = false
    private var mHasFailedHeightInCurrentRep: Boolean = false
    private var mHasStableHandsInCurrentRep: Boolean = true
    private var mHasAnnouncedGetIntoPosition: Boolean = false
    private var mLastMouthY: Float? = null
    private var mWristAnchor: WristAnchor? = null
    private var mPendingCue: String? = null
    private var mPendingCueJoints: List<String> = emptyList()
    private var mPendingCueFrames: Int = 0
    private var mPersistedCue: String? = null
    private var mPersistedCueJoints: List<String> = emptyList()
    private var mClearFrames: Int = 0
    private var mLastSpeechTimestamp: Long = 0

    fun process(
        normalizedLandmarks: List<NormalizedLandmark>,
    ): FormFeedback {
        val frontLandmarks = getFrontLandmarks(normalizedLandmarks) ?: return FormFeedback(
            mPrimaryCue = "Step into frame",
            mStatus = ExerciseStatus.ERROR,
            mRepCount = mRepCount,
            mIsCorrecting = true,
            mProblematicJoints = listOf("shoulders", "elbows", "wrists"),
            mErrorMessage = maybeSpeak("Step into frame"),
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
        val shouldersY = (frontLandmarks.mLeftShoulder.y() + frontLandmarks.mRightShoulder.y()) / 2f
        val mouthY = (frontLandmarks.mMouthLeft.y() + frontLandmarks.mMouthRight.y()) / 2f
        val handsY = (frontLandmarks.mLeftWrist.y() + frontLandmarks.mRightWrist.y()) / 2f
        val isArmsExtended = averageElbowAngle >= DEAD_HANG_ELBOW_ANGLE_DEG
        val isMouthAboveHands = mouthY < (handsY - MOUTH_ABOVE_HANDS_MARGIN)
        val isMouthBelowHands = mouthY > (handsY + MOUTH_BELOW_HANDS_MARGIN)
        val isBottomPosition = isArmsExtended && isMouthBelowHands
        val isStandingNormally = isStandingNormally(
            shouldersY = shouldersY,
            handsY = handsY,
            mouthY = mouthY,
            elbowAngle = averageElbowAngle,
        )
        val previousMouthY = mLastMouthY
        val isMouthMovingDown = previousMouthY != null && mouthY > (previousMouthY + MOUTH_DIRECTION_DELTA)
        mLastMouthY = mouthY

        var speechCue: String? = null
        val isHandsStable = isHandsStable(frontLandmarks)
        if (!isHandsStable) {
            mHasStableHandsInCurrentRep = false
        }

        if (!mHasDetectedStartPosition) {
            mPhase = PullUpPhase.DEAD_HANG
            if (isStandingNormally && !mHasAnnouncedGetIntoPosition) {
                speechCue = GET_INTO_POSITION_CUE
                mHasAnnouncedGetIntoPosition = true
            }
            if (isBottomPosition) {
                mWristAnchor = WristAnchor(
                    mLeftX = frontLandmarks.mLeftWrist.x(),
                    mLeftY = frontLandmarks.mLeftWrist.y(),
                    mRightX = frontLandmarks.mRightWrist.x(),
                    mRightY = frontLandmarks.mRightWrist.y(),
                )
                mHasStableHandsInCurrentRep = true
                if (isHandsStable) {
                    mHasDetectedStartPosition = true
                    mHasReachedTopInCurrentRep = false
                    mHasAttemptedRepInCurrentCycle = false
                    mHasFailedHeightInCurrentRep = false
                    speechCue = BEGIN_PULL_UP_CUE
                }
            }
        } else {
            if (isStandingNormally && !isBottomPosition && mPhase == PullUpPhase.DEAD_HANG) {
                mHasDetectedStartPosition = false
                mHasReachedTopInCurrentRep = false
                mHasAttemptedRepInCurrentCycle = false
                mHasFailedHeightInCurrentRep = false
                mHasStableHandsInCurrentRep = true
                mWristAnchor = null
                mHasAnnouncedGetIntoPosition = false
            } else {
                when (mPhase) {
                    PullUpPhase.DEAD_HANG -> {
                        if (isBottomPosition) {
                            mWristAnchor = WristAnchor(
                                mLeftX = frontLandmarks.mLeftWrist.x(),
                                mLeftY = frontLandmarks.mLeftWrist.y(),
                                mRightX = frontLandmarks.mRightWrist.x(),
                                mRightY = frontLandmarks.mRightWrist.y(),
                            )
                            mHasStableHandsInCurrentRep = true
                        } else {
                            mPhase = PullUpPhase.PULLING
                            mHasAttemptedRepInCurrentCycle = true
                            mHasReachedTopInCurrentRep = false
                            mHasFailedHeightInCurrentRep = false
                        }
                    }
                    PullUpPhase.PULLING -> {
                        if (isMouthAboveHands) {
                            mPhase = PullUpPhase.TOP
                            if (!mHasReachedTopInCurrentRep) {
                                mHasReachedTopInCurrentRep = true
                                if (mHasStableHandsInCurrentRep) {
                                    mRepCount += 1
                                }
                                speechCue = REP_COMPLETE_CUE
                            }
                        } else if (isMouthMovingDown) {
                            mPhase = PullUpPhase.LOWERING
                            mHasFailedHeightInCurrentRep = true
                        } else if (isBottomPosition && mHasAttemptedRepInCurrentCycle && !mHasReachedTopInCurrentRep) {
                            mPhase = PullUpPhase.DEAD_HANG
                            mHasFailedHeightInCurrentRep = true
                        }
                    }
                    PullUpPhase.TOP -> {
                        if (!isMouthAboveHands) {
                            mPhase = PullUpPhase.LOWERING
                        }
                    }
                    PullUpPhase.LOWERING -> {
                        if (isBottomPosition) {
                            mPhase = PullUpPhase.DEAD_HANG
                            mHasReachedTopInCurrentRep = false
                            mHasAttemptedRepInCurrentCycle = false
                            mHasStableHandsInCurrentRep = true
                        }
                    }
                }
            }
        }

        var frameCue: String? = null
        var frameJoints: List<String> = emptyList()
        if (!isHandsStable) {
            frameCue = "Keep hands steady"
            frameJoints = listOf("left wrist", "right wrist")
        } else if (!mHasDetectedStartPosition) {
            frameCue = if (isStandingNormally) "Get into position" else "Get into dead hang"
            frameJoints = listOf("shoulders", "elbows", "wrists")
        } else if (mHasFailedHeightInCurrentRep && (mPhase == PullUpPhase.LOWERING || mPhase == PullUpPhase.DEAD_HANG)) {
            frameCue = "Not high enough"
            frameJoints = listOf("mouth", "wrists")
        } else if ((mPhase == PullUpPhase.LOWERING || mPhase == PullUpPhase.DEAD_HANG) && !isArmsExtended && isMouthBelowHands) {
            frameCue = "Fully extend your arms"
            frameJoints = listOf("elbows", "wrists")
        } else if (mPhase == PullUpPhase.PULLING && !mHasReachedTopInCurrentRep) {
            frameCue = "Bring your mouth above your hands"
            frameJoints = listOf("mouth", "wrists")
        }

        if (speechCue == null) {
            speechCue = frameCue
        }
        speechCue = maybeSpeak(speechCue)

        stabilizeCue(frameCue, frameJoints)
        val isCorrecting = mPersistedCue != null
        return FormFeedback(
            mPrimaryCue = mPersistedCue,
            mSpeechCue = speechCue,
            mStatus = when {
                isCorrecting -> ExerciseStatus.ERROR
                !mHasDetectedStartPosition || mPhase == PullUpPhase.DEAD_HANG -> ExerciseStatus.READY
                else -> ExerciseStatus.ACTIVE
            },
            mRepCount = mRepCount,
            mIsCorrecting = isCorrecting,
            mProblematicJoints = mPersistedCueJoints,
            mErrorMessage = mPersistedCue,
        )
    }

    fun reset() {
        mRepCount = 0
        mPhase = PullUpPhase.DEAD_HANG
        mHasDetectedStartPosition = false
        mHasReachedTopInCurrentRep = false
        mHasAttemptedRepInCurrentCycle = false
        mHasFailedHeightInCurrentRep = false
        mHasStableHandsInCurrentRep = true
        mHasAnnouncedGetIntoPosition = false
        mLastMouthY = null
        mWristAnchor = null
        mPendingCue = null
        mPendingCueJoints = emptyList()
        mPendingCueFrames = 0
        mPersistedCue = null
        mPersistedCueJoints = emptyList()
        mClearFrames = 0
        mLastSpeechTimestamp = 0
    }

    private fun isStandingNormally(
        shouldersY: Float,
        handsY: Float,
        mouthY: Float,
        elbowAngle: Float,
    ): Boolean {
        val wristsBelowShoulders = handsY > (shouldersY + STANDING_WRISTS_BELOW_SHOULDERS_MARGIN)
        val mouthAboveHands = mouthY < (handsY - STANDING_MOUTH_ABOVE_HANDS_MARGIN)
        val armsRelaxed = elbowAngle >= STANDING_ELBOW_MIN_ANGLE_DEG
        return wristsBelowShoulders && mouthAboveHands && armsRelaxed
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

    private fun isHandsStable(landmarks: PullUpFrontLandmarks): Boolean {
        val anchor = mWristAnchor ?: return true
        val leftXStable = abs(landmarks.mLeftWrist.x() - anchor.mLeftX) <= WRIST_STABILITY_TOLERANCE
        val leftYStable = abs(landmarks.mLeftWrist.y() - anchor.mLeftY) <= WRIST_STABILITY_TOLERANCE
        val rightXStable = abs(landmarks.mRightWrist.x() - anchor.mRightX) <= WRIST_STABILITY_TOLERANCE
        val rightYStable = abs(landmarks.mRightWrist.y() - anchor.mRightY) <= WRIST_STABILITY_TOLERANCE
        return leftXStable && leftYStable && rightXStable && rightYStable
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

    private data class WristAnchor(
        val mLeftX: Float,
        val mLeftY: Float,
        val mRightX: Float,
        val mRightY: Float,
    )

    companion object {
        private const val MIN_VISIBILITY = 0.6f
        private const val DEAD_HANG_ELBOW_ANGLE_DEG = 155f
        private const val MOUTH_ABOVE_HANDS_MARGIN = 0.015f
        private const val MOUTH_BELOW_HANDS_MARGIN = 0.01f
        private const val MOUTH_DIRECTION_DELTA = 0.004f
        private const val STANDING_WRISTS_BELOW_SHOULDERS_MARGIN = 0.06f
        private const val STANDING_MOUTH_ABOVE_HANDS_MARGIN = 0.03f
        private const val STANDING_ELBOW_MIN_ANGLE_DEG = 135f
        private const val WRIST_STABILITY_TOLERANCE = 0.09f
        private const val PERSISTENCE_FRAMES = 8
        private const val CLEARANCE_FRAMES = 8
        private const val GET_INTO_POSITION_CUE = "Get into position"
        private const val BEGIN_PULL_UP_CUE = "Begin pull-up"
        private const val REP_COMPLETE_CUE = "Rep complete"
    }
}

private fun Float?.orElse(default: Float): Float = this ?: default
