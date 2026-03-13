package com.example.seally.camera

import android.app.Application
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val mUiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = mUiState.asStateFlow()

    private var mPoseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var mIsPoseLandmarkerInitializing: Boolean = false
    @Volatile
    private var mAcceptFrames: Boolean = false
    @Volatile
    private var mSuppressLandmarkResults: Boolean = false
    private var mCurrentLensSwitchToken: Int = 0
    private var mPendingLensSwitchToken: Int? = null
    private var mHasAttemptedWarmup: Boolean = false

    fun setCameraPermission(hasPermission: Boolean, isInitialCheck: Boolean) {
        val currentState = mUiState.value
        val shouldShowStartupLoading = isInitialCheck && hasPermission

        mUiState.value = currentState.copy(
            mHasCameraPermission = hasPermission,
            mHasCompletedInitialPermissionCheck = currentState.mHasCompletedInitialPermissionCheck || isInitialCheck,
            mIsStartupCameraLoading = when {
                !hasPermission -> false
                shouldShowStartupLoading -> true
                else -> currentState.mIsStartupCameraLoading
            },
            mErrorMessage = if (hasPermission) currentState.mErrorMessage else null,
        )

        if (!hasPermission) {
            mAcceptFrames = false
            mPoseLandmarkerHelper?.clear()
            mPoseLandmarkerHelper = null
            mIsPoseLandmarkerInitializing = false
            return
        }

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
                                mUiState.value = mUiState.value.copy(
                                    mLandmarks = firstPose,
                                    mWorldLandmarks = firstPoseWorld,
                                    mErrorMessage = null,
                                )
                            }
                        },
                        mErrorListener = { message ->
                            mUiState.value = mUiState.value.copy(
                                mErrorMessage = message,
                                mIsStartupCameraLoading = false,
                            )
                        },
                    ).also { it.setup() }
                }

                mPoseLandmarkerHelper = helper
                mAcceptFrames = true
            } catch (exception: Exception) {
                mUiState.value = mUiState.value.copy(
                    mErrorMessage = exception.message ?: "Failed to initialize pose detector",
                    mIsStartupCameraLoading = false,
                )
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
            mUiState.value = mUiState.value.copy(mIsStartupCameraLoading = false)
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val isQuarterTurn = rotationDegrees == 90 || rotationDegrees == 270
        val frameWidth = if (isQuarterTurn) imageProxy.height else imageProxy.width
        val frameHeight = if (isQuarterTurn) imageProxy.width else imageProxy.height

        val currentState = mUiState.value
        if (currentState.mFrameWidth != frameWidth || currentState.mFrameHeight != frameHeight) {
            mUiState.value = currentState.copy(mFrameWidth = frameWidth, mFrameHeight = frameHeight)
        }

        // Analyzer already runs on a background executor, so avoid hopping back to Main.
        mPoseLandmarkerHelper?.detectLiveStreamFrame(imageProxy) ?: imageProxy.close()
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
        )
    }

    fun setFrontCamera(isFrontCamera: Boolean) {
        val currentState = mUiState.value
        if (currentState.mIsFrontCamera == isFrontCamera) return
        mUiState.value = currentState.copy(mIsFrontCamera = isFrontCamera)
    }

    override fun onCleared() {
        mAcceptFrames = false
        mSuppressLandmarkResults = false
        mPendingLensSwitchToken = null
        mPoseLandmarkerHelper?.clear()
        mPoseLandmarkerHelper = null
        super.onCleared()
    }

    companion object {
        private const val LENS_SWITCH_RESULT_SUPPRESSION_MS = 250L
    }
}
