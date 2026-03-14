package com.example.seally.camera

data class FormFeedback(
    val mPrimaryCue: String? = null,
    val mStatus: ExerciseStatus = ExerciseStatus.INITIALIZING,
    val mRepCount: Int = 0,
    val mCurrentPhase: MovementPhase = MovementPhase.STANDING,
    val mHoldDurationMs: Long = 0L,
    val mIsCorrecting: Boolean = false,
    val mProblematicJoints: List<String> = emptyList(),
    val mErrorMessage: String? = null,
)

enum class ExerciseType {
    SQUAT,
    PLANK,
    PULLUP,
}

enum class ExerciseStatus {
    INITIALIZING,
    READY,
    ACTIVE,
    ERROR,
}

enum class MovementPhase {
    STANDING,
    DESCENDING,
    BOTTOM,
    ASCENDING,
}

data class PullUpBarCalibration(
    val mLeftX: Float,
    val mLeftY: Float,
    val mRightX: Float,
    val mRightY: Float,
) {
    val mBarY: Float
        get() = (mLeftY + mRightY) / 2f
}

enum class BodySide(
    val mShoulder: Int,
    val mHip: Int,
    val mKnee: Int,
    val mAnkle: Int,
) {
    LEFT(11, 23, 25, 27),
    RIGHT(12, 24, 26, 28),
}
