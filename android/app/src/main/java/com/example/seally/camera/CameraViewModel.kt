package com.example.seally.camera

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.seally.data.repository.ExerciseLogRepository
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
import java.time.LocalDate

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
    val mRoomCode: String = "",
    val mIsLiveKitConnected: Boolean = false,
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val mTag = "CameraViewModel"
    private val mUiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = mUiState.asStateFlow()

    private var mPoseLandmarkerHelper: PoseLandmarkerHelper? = null
    private val mSquatFormFeedbackEngine = SquatFormFeedbackEngine()
    private val mPlankFormFeedbackEngine = PlankFormFeedbackEngine()
    private val mPullUpFormFeedbackEngine = PullUpFormFeedbackEngine()
    private val mPushUpFormFeedbackEngine = PushUpFormFeedbackEngine()
    private var mIsPoseLandmarkerInitializing: Boolean = false
    @Volatile
    private var mAcceptFrames: Boolean = false
    @Volatile
    private var mSuppressLandmarkResults: Boolean = false
    private var mCurrentLensSwitchToken: Int = 0
    private var mPendingLensSwitchToken: Int? = null
    private var mHasAttemptedWarmup: Boolean = false
    private val mLiveKitPublisher = LiveKitPublisher(getApplication())
    private val mExerciseLogRepository = ExerciseLogRepository(getApplication())
    private var mIsLiveKitConnecting: Boolean = false
    private var mLandmarkSequence: Long = 0L
    private var mLastLandmarkSentAtMs: Long = 0L
    private var mFrameCounter: Long = 0L
    private val mLiveKitUrl: String by lazy { readConfigString("livekit_url").trim() }
    private val mLiveKitApiKey: String by lazy { readConfigString("livekit_api_key").trim() }
    private val mLiveKitApiSecret: String by lazy { readConfigString("livekit_api_secret").trim() }
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
            mPushUpFormFeedbackEngine.reset()
            mUiState.update { it.copy(mFormFeedback = FormFeedback()) }
            mUiState.update { it.copy(mLiveKitStatus = "LiveKit disabled (no camera permission)") }
            return
        }

        if (mPoseLandmarkerHelper == null && !mIsPoseLandmarkerInitializing) {
            initializePoseLandmarker()
            return
        }

        mAcceptFrames = !mIsPoseLandmarkerInitializing
    }

    fun setRoomCode(roomCode: String) {
        mUiState.update { it.copy(mRoomCode = roomCode.trim().uppercase()) }
    }

    fun connectToLiveKit() {
        val roomCode = mUiState.value.mRoomCode
        if (roomCode.isBlank()) {
            mUiState.update { it.copy(mLiveKitStatus = "Enter a room code first") }
            return
        }
        ensureLiveKitConnected(roomCode)
    }

    fun disconnectFromLiveKit() {
        disconnectLiveKit()
        mUiState.update {
            it.copy(
                mIsLiveKitConnected = false,
                mLiveKitStatus = "Disconnected",
            )
        }
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
                                    ExerciseType.PUSHUP -> mPushUpFormFeedbackEngine.process(
                                        normalizedLandmarks = firstPose,
                                        worldLandmarks = firstPoseWorld,
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
        mPushUpFormFeedbackEngine.reset()
    }

    fun setFrontCamera(isFrontCamera: Boolean) {
        val currentState = mUiState.value
        if (currentState.mIsFrontCamera == isFrontCamera) return
        mUiState.update { it.copy(mIsFrontCamera = isFrontCamera) }
    }

    fun setSelectedExercise(mExerciseType: ExerciseType) {
        val currentState = mUiState.value
        if (currentState.mSelectedExercise == mExerciseType) return
        persistCurrentExerciseSessionIfNeeded(currentState)
        resetExerciseEnginesAndFeedback()
        mUiState.update {
            it.copy(
                mSelectedExercise = mExerciseType,
            )
        }
    }

    fun toggleExerciseMode() {
        val currentState = mUiState.value
        persistCurrentExerciseSessionIfNeeded(currentState)
        resetExerciseEnginesAndFeedback()
        mUiState.update { state ->
            state.copy(
                mSelectedExercise = when (state.mSelectedExercise) {
                    ExerciseType.SQUAT -> ExerciseType.PLANK
                    ExerciseType.PLANK -> ExerciseType.PULLUP
                    ExerciseType.PULLUP -> ExerciseType.PUSHUP
                    ExerciseType.PUSHUP -> ExerciseType.SQUAT
                },
            )
        }
    }

    fun persistExerciseSessionOnExit() {
        persistCurrentExerciseSessionIfNeeded(mUiState.value)
        resetExerciseEnginesAndFeedback()
    }

    private fun persistCurrentExerciseSessionIfNeeded(state: CameraUiState) {
        val exerciseType = state.mSelectedExercise
        val feedback = state.mFormFeedback
        val quantity = when (exerciseType) {
            ExerciseType.SQUAT, ExerciseType.PULLUP, ExerciseType.PUSHUP -> feedback.mRepCount.toDouble()
            ExerciseType.PLANK -> feedback.mHoldDurationMs / 1000.0
        }
        if (quantity <= 0.0) return

        val metric = when (exerciseType) {
            ExerciseType.SQUAT, ExerciseType.PULLUP, ExerciseType.PUSHUP -> "reps"
            ExerciseType.PLANK -> "seconds"
        }
        val exerciseName = when (exerciseType) {
            ExerciseType.SQUAT -> "Squat"
            ExerciseType.PLANK -> "Plank"
            ExerciseType.PULLUP -> "Pull-up"
            ExerciseType.PUSHUP -> "Push-up"
        }
        val date = LocalDate.now().toString()

        viewModelScope.launch {
            runCatching {
                mExerciseLogRepository.addLog(
                    exerciseName = exerciseName,
                    quantity = quantity,
                    metric = metric,
                    date = date,
                )
            }.onFailure { error ->
                Log.e(mTag, "Failed to persist exercise session", error)
            }
        }
    }

    private fun resetExerciseEnginesAndFeedback() {
        mSquatFormFeedbackEngine.reset()
        mPlankFormFeedbackEngine.reset()
        mPullUpFormFeedbackEngine.reset()
        mPushUpFormFeedbackEngine.reset()
        mUiState.update { it.copy(mFormFeedback = FormFeedback()) }
    }

    private fun ensureLiveKitConnected(roomCode: String) {
        if (mLiveKitUrl.isBlank() || mLiveKitApiKey.isBlank() || mLiveKitApiSecret.isBlank()) {
            mUiState.update { it.copy(mLiveKitStatus = "LiveKit disabled (config missing)") }
            return
        }

        if (mIsLiveKitConnecting) return
        mIsLiveKitConnecting = true
        mUiState.update { it.copy(mLiveKitStatus = "LiveKit connecting...") }
        viewModelScope.launch {
            try {
                val token = com.example.seally.livekit.LiveKitTokenGenerator.generatePublisherToken(
                    roomCode = roomCode,
                    apiKey = mLiveKitApiKey,
                    apiSecret = mLiveKitApiSecret,
                )
                mLiveKitPublisher.connect(
                    url = mLiveKitUrl,
                    token = token,
                ).onFailure { error ->
                    Log.e(mTag, "LiveKit connection failed", error)
                    mUiState.update {
                        it.copy(
                            mLiveKitStatus = "LiveKit failed: ${error.message ?: "unknown error"}",
                            mIsLiveKitConnected = false,
                        )
                    }
                }.onSuccess {
                    mUiState.update {
                        it.copy(
                            mIsLiveKitConnected = true,
                            mLiveKitStatus = "LiveKit ${mLiveKitPublisher.getDebugStatus()}",
                        )
                    }
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

    private fun jointNameToIndex(jointName: String): List<Int> {
        val name = jointName.lowercase().replace(" ", "_")
        return when (name) {
            "nose" -> listOf(0)
            "left_eye_inner" -> listOf(1)
            "left_eye" -> listOf(2)
            "left_eye_outer" -> listOf(3)
            "right_eye_inner" -> listOf(4)
            "right_eye" -> listOf(5)
            "right_eye_outer" -> listOf(6)
            "left_ear" -> listOf(7)
            "right_ear" -> listOf(8)
            "mouth_left" -> listOf(9)
            "mouth_right" -> listOf(10)
            "mouth" -> listOf(9, 10)
            "left_shoulder" -> listOf(11)
            "right_shoulder" -> listOf(12)
            "shoulder" -> listOf(11, 12)
            "left_elbow" -> listOf(13)
            "right_elbow" -> listOf(14)
            "elbow" -> listOf(13, 14)
            "elbows" -> listOf(13, 14)
            "left_wrist" -> listOf(15)
            "right_wrist" -> listOf(16)
            "wrist" -> listOf(15, 16)
            "wrists" -> listOf(15, 16)
            "left_pinky" -> listOf(17)
            "right_pinky" -> listOf(18)
            "left_index" -> listOf(19)
            "right_index" -> listOf(20)
            "left_thumb" -> listOf(21)
            "right_thumb" -> listOf(22)
            "left_hip" -> listOf(23)
            "right_hip" -> listOf(24)
            "hip" -> listOf(23, 24)
            "left_knee" -> listOf(25)
            "right_knee" -> listOf(26)
            "knee" -> listOf(25, 26)
            "left_ankle" -> listOf(27)
            "right_ankle" -> listOf(28)
            "ankle" -> listOf(27, 28)
            "left_heel" -> listOf(29)
            "right_heel" -> listOf(30)
            "left_foot_index" -> listOf(31)
            "right_foot_index" -> listOf(32)
            else -> emptyList()
        }
    }

    private fun maybePublishLandmarks(landmarks: List<NormalizedLandmark>) {
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - mLastLandmarkSentAtMs < LANDMARK_SEND_MIN_INTERVAL_MS) return

        val state = mUiState.value
        val problematicJointIndices = state.mFormFeedback.mProblematicJoints.flatMap { jointName ->
            jointNameToIndex(jointName)
        }
        Log.d(mTag, "Problematic joints: ${state.mFormFeedback.mProblematicJoints} -> indices: $problematicJointIndices")
        val payload = LandmarkPacketEncoder.encode(
            sequence = mLandmarkSequence,
            timestampMs = nowMs,
            frameWidth = state.mFrameWidth,
            frameHeight = state.mFrameHeight,
            isFrontCamera = state.mIsFrontCamera,
            landmarks = landmarks,
            problematicJoints = problematicJointIndices,
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
        mPushUpFormFeedbackEngine.reset()
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
