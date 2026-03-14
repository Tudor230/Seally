package com.example.seally.camera

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.seally.livekit.LandmarkPacketEncoder
import com.example.seally.livekit.LiveKitPublisher
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CameraUiState(
    val mHasCameraPermission: Boolean = false,
    val mHasCompletedInitialPermissionCheck: Boolean = false,
    val mIsStartupCameraLoading: Boolean = false,
    val mIsFrontCamera: Boolean = true,
    val mLandmarks: List<NormalizedLandmark> = emptyList(),
    val mWorldLandmarks: List<Landmark> = emptyList(),
    val mErrorMessage: String? = null,
    val mFrameWidth: Int = 0,
    val mFrameHeight: Int = 0,
    val mSelectedExercise: ExerciseType = ExerciseType.SQUAT,
    val mFormFeedback: FormFeedback = FormFeedback(),
    val mLiveKitStatus: String = "LiveKit idle",
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val mTag = "CameraViewModel"
    private val mUiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = mUiState.asStateFlow()

    private var mPoseLandmarkerHelper: PoseLandmarkerHelper? = null
    private val mSquatFormFeedbackEngine = SquatFormFeedbackEngine()
    private val mPlankFormFeedbackEngine = PlankFormFeedbackEngine()
    private val mPullUpFormFeedbackEngine = PullUpFormFeedbackEngine()
    private var mIsPoseLandmarkerInitializing: Boolean = false
    @Volatile
    private var mAcceptFrames: Boolean = false
    @Volatile
    private var mSuppressLandmarkResults: Boolean = false
    private var mCurrentLensSwitchToken: Int = 0
    private var mPendingLensSwitchToken: Int? = null
    private var mHasAttemptedWarmup: Boolean = false
    private val mLiveKitPublisher = LiveKitPublisher(getApplication())
    private var mIsLiveKitConnecting: Boolean = false
    private var mLandmarkSequence: Long = 0L
    private var mLastLandmarkSentAtMs: Long = 0L
    private var mFrameCounter: Long = 0L
    private val mLiveKitUrl: String by lazy { readConfigString("livekit_url").trim() }
    private val mLiveKitToken: String by lazy { readConfigString("livekit_token").trim() }
    private val mLiveKitLandmarkTopic: String by lazy {
        readConfigString("livekit_landmark_topic").trim().ifBlank {
            DEFAULT_LANDMARK_TOPIC
        }
    }

    fun setCameraPermission(hasPermission: Boolean, isInitialCheck: Boolean) {
        val currentState = mUiState.value
        val shouldShowStartupLoading = isInitialCheck && hasPermission

        mUiState.update {
            it.copy(
                mHasCameraPermission = hasPermission,
                mHasCompletedInitialPermissionCheck = currentState.mHasCompletedInitialPermissionCheck || isInitialCheck,
                mIsStartupCameraLoading = when {
                    !hasPermission -> false
                    shouldShowStartupLoading -> true
                    else -> currentState.mIsStartupCameraLoading
                },
                mErrorMessage = if (hasPermission) currentState.mErrorMessage else null,
            )
        }

        if (!hasPermission) {
            mAcceptFrames = false
            mPoseLandmarkerHelper?.clear()
            mPoseLandmarkerHelper = null
            mIsPoseLandmarkerInitializing = false
            disconnectLiveKit()
            mSquatFormFeedbackEngine.reset()
            mPlankFormFeedbackEngine.reset()
            mPullUpFormFeedbackEngine.reset()
            mUiState.update { it.copy(mFormFeedback = FormFeedback()) }
            mUiState.update { it.copy(mLiveKitStatus = "LiveKit disabled (no camera permission)") }
            return
        }

        ensureLiveKitConnected()

        if (mPoseLandmarkerHelper == null && !mIsPoseLandmarkerInitializing) {
            initializePoseLandmarker()
            return
        }

        mAcceptFrames = !mIsPoseLandmarkerInitializing
    }

    fun preWarmPoseLandmarker() {
        if (mHasAttemptedWarmup) return
        mHasAttemptedWarmup = true

        if (mPoseLandmarkerHelper != null || mIsPoseLandmarkerInitializing) return
        initializePoseLandmarker()
    }

    private fun initializePoseLandmarker() {
        if (mIsPoseLandmarkerInitializing) return

        mIsPoseLandmarkerInitializing = true
        mAcceptFrames = false
        val previousHelper = mPoseLandmarkerHelper
        mPoseLandmarkerHelper = null

        viewModelScope.launch {
            try {
                val helper = withContext(Dispatchers.Default) {
                    previousHelper?.clear()
                    PoseLandmarkerHelper(
                        mContext = getApplication(),
                        mModelAssetPath = "models/pose_landmarker_full.task",
                        mNumPoses = 1,
                        mResultListener = { result ->
                            if (!mSuppressLandmarkResults) {
                                val firstPose = result.landmarks().firstOrNull().orEmpty()
                                val firstPoseWorld = result.worldLandmarks().firstOrNull().orEmpty()
                                val currentUiState = mUiState.value
                                val selectedExercise = currentUiState.mSelectedExercise
                                val feedback = when (selectedExercise) {
                                    ExerciseType.SQUAT -> mSquatFormFeedbackEngine.process(firstPose, firstPoseWorld)
                                    ExerciseType.PLANK -> mPlankFormFeedbackEngine.process(
                                        normalizedLandmarks = firstPose,
                                        worldLandmarks = firstPoseWorld,
                                        frameTimestampMs = SystemClock.elapsedRealtime(),
                                    )
                                    ExerciseType.PULLUP -> mPullUpFormFeedbackEngine.process(
                                        normalizedLandmarks = firstPose,
                                    )
                                }
                                mUiState.update {
                                    it.copy(
                                        mLandmarks = firstPose,
                                        mWorldLandmarks = firstPoseWorld,
                                        mFormFeedback = feedback,
                                    )
                                }
                                maybePublishLandmarks(firstPose)
                            }
                        },
                        mErrorListener = { message ->
                            mUiState.update {
                                it.copy(
                                    mErrorMessage = message,
                                    mIsStartupCameraLoading = false,
                                )
                            }
                        },
                    ).also { it.setup() }
                }

                mPoseLandmarkerHelper = helper
                mAcceptFrames = true
            } catch (exception: Exception) {
                mUiState.update {
                    it.copy(
                        mErrorMessage = exception.message ?: "Failed to initialize pose detector",
                        mIsStartupCameraLoading = false,
                    )
                }
                mAcceptFrames = false
            } finally {
                mIsPoseLandmarkerInitializing = false
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun processFrame(imageProxy: ImageProxy) {
        if (!mAcceptFrames || mIsPoseLandmarkerInitializing) {
            imageProxy.close()
            return
        }

        val pendingSwitchToken = mPendingLensSwitchToken
        if (pendingSwitchToken != null) {
            mPendingLensSwitchToken = null
            viewModelScope.launch {
                // Allow the new camera stream to warm up before accepting landmark callbacks.
                delay(LENS_SWITCH_RESULT_SUPPRESSION_MS)
                if (mCurrentLensSwitchToken == pendingSwitchToken) {
                    mSuppressLandmarkResults = false
                }
            }
        }

        if (mUiState.value.mIsStartupCameraLoading) {
            mUiState.update { it.copy(mIsStartupCameraLoading = false) }
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val isQuarterTurn = rotationDegrees == 90 || rotationDegrees == 270
        val frameWidth = if (isQuarterTurn) imageProxy.height else imageProxy.width
        val frameHeight = if (isQuarterTurn) imageProxy.width else imageProxy.height

        val currentState = mUiState.value
        if (currentState.mFrameWidth != frameWidth || currentState.mFrameHeight != frameHeight) {
            mUiState.update { it.copy(mFrameWidth = frameWidth, mFrameHeight = frameHeight) }
        }

        runCatching {
            mPoseLandmarkerHelper?.detectLiveStreamFrame(imageProxy)
        }.onFailure { error ->
            Log.e(mTag, "Pose processing failed", error)
        }
        imageProxy.close()
        mFrameCounter += 1L
        if (mFrameCounter % LIVEKIT_STATUS_UPDATE_EVERY_FRAMES == 0L) {
            mUiState.update { it.copy(mLiveKitStatus = "LiveKit ${mLiveKitPublisher.getDebugStatus()}") }
        }
    }

    fun onLensSwitchStarted() {
        mCurrentLensSwitchToken += 1
        mPendingLensSwitchToken = mCurrentLensSwitchToken
        mSuppressLandmarkResults = true
        clearPoseOverlay()
    }

    fun clearPoseOverlay() {
        val currentState = mUiState.value
        if (
            currentState.mLandmarks.isEmpty() &&
            currentState.mWorldLandmarks.isEmpty() &&
            currentState.mFrameWidth == 0 &&
            currentState.mFrameHeight == 0
        ) return

        mUiState.value = currentState.copy(
            mLandmarks = emptyList(),
            mWorldLandmarks = emptyList(),
            mFrameWidth = 0,
            mFrameHeight = 0,
            mFormFeedback = FormFeedback(),
        )
        mSquatFormFeedbackEngine.reset()
        mPlankFormFeedbackEngine.reset()
        mPullUpFormFeedbackEngine.reset()
    }

    fun setFrontCamera(isFrontCamera: Boolean) {
        val currentState = mUiState.value
        if (currentState.mIsFrontCamera == isFrontCamera) return
        mUiState.update { it.copy(mIsFrontCamera = isFrontCamera) }
    }

    fun setSelectedExercise(mExerciseType: ExerciseType) {
        val currentState = mUiState.value
        if (currentState.mSelectedExercise == mExerciseType) return
        mSquatFormFeedbackEngine.reset()
        mPlankFormFeedbackEngine.reset()
        mPullUpFormFeedbackEngine.reset()
        mUiState.update {
            it.copy(
                mSelectedExercise = mExerciseType,
                mFormFeedback = FormFeedback(),
            )
        }
    }

    fun toggleExerciseMode() {
        mSquatFormFeedbackEngine.reset()
        mPlankFormFeedbackEngine.reset()
        mPullUpFormFeedbackEngine.reset()
        mUiState.update { currentState ->
            currentState.copy(
                mSelectedExercise = when (currentState.mSelectedExercise) {
                    ExerciseType.SQUAT -> ExerciseType.PLANK
                    ExerciseType.PLANK -> ExerciseType.PULLUP
                    ExerciseType.PULLUP -> ExerciseType.SQUAT
                },
                mFormFeedback = FormFeedback(),
            )
        }
    }

    private fun ensureLiveKitConnected() {
        if (mLiveKitUrl.isBlank() || mLiveKitToken.isBlank()) {
            mUiState.update { it.copy(mLiveKitStatus = "LiveKit disabled (config missing)") }
            return
        }

        if (mIsLiveKitConnecting) return
        mIsLiveKitConnecting = true
        mUiState.update { it.copy(mLiveKitStatus = "LiveKit connecting...") }
        viewModelScope.launch {
            try {
                mLiveKitPublisher.connect(
                    url = mLiveKitUrl,
                    token = mLiveKitToken,
                ).onFailure { error ->
                    Log.e(mTag, "LiveKit connection failed", error)
                    mUiState.update {
                        it.copy(
                            mLiveKitStatus = "LiveKit failed: ${error.message ?: "unknown error"}",
                        )
                    }
                }.onSuccess {
                    mUiState.update { it.copy(mLiveKitStatus = "LiveKit ${mLiveKitPublisher.getDebugStatus()}") }
                }
            } finally {
                mIsLiveKitConnecting = false
            }
        }
    }

    private fun disconnectLiveKit() {
        mLandmarkSequence = 0L
        mLastLandmarkSentAtMs = 0L
        mFrameCounter = 0L
        mLiveKitPublisher.disconnect()
    }

    private fun maybePublishLandmarks(landmarks: List<NormalizedLandmark>) {
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - mLastLandmarkSentAtMs < LANDMARK_SEND_MIN_INTERVAL_MS) return

        val state = mUiState.value
        val payload = LandmarkPacketEncoder.encode(
            sequence = mLandmarkSequence,
            timestampMs = nowMs,
            frameWidth = state.mFrameWidth,
            frameHeight = state.mFrameHeight,
            isFrontCamera = state.mIsFrontCamera,
            landmarks = landmarks,
        )
        mLandmarkSequence += 1L

        if (payload.size > MAX_LIVEKIT_DATA_PACKET_BYTES) return
        mLastLandmarkSentAtMs = nowMs

        viewModelScope.launch {
            mLiveKitPublisher.publishLandmarks(
                payload = payload,
                topic = mLiveKitLandmarkTopic,
            ).onFailure {
                mUiState.update { state ->
                    state.copy(mLiveKitStatus = "LiveKit ${mLiveKitPublisher.getDebugStatus()}")
                }
            }
        }
    }

    private fun readConfigString(resourceName: String): String {
        val application = getApplication<Application>()
        val resourceId = application.resources.getIdentifier(resourceName, "string", application.packageName)
        if (resourceId == 0) return ""
        return application.getString(resourceId)
    }

    override fun onCleared() {
        mAcceptFrames = false
        mSuppressLandmarkResults = false
        mPendingLensSwitchToken = null
        mPoseLandmarkerHelper?.clear()
        mPoseLandmarkerHelper = null
        disconnectLiveKit()
        mSquatFormFeedbackEngine.reset()
        mPlankFormFeedbackEngine.reset()
        mPullUpFormFeedbackEngine.reset()
        super.onCleared()
    }

    companion object {
        private const val LENS_SWITCH_RESULT_SUPPRESSION_MS = 250L
        private const val LANDMARK_SEND_MIN_INTERVAL_MS = 33L
        private const val MAX_LIVEKIT_DATA_PACKET_BYTES = 15_000
        private const val DEFAULT_LANDMARK_TOPIC = "pose.binary.v2"
        private const val LIVEKIT_STATUS_UPDATE_EVERY_FRAMES = 45L
    }
}
