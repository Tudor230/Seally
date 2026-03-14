package com.example.seally.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

private val POSE_CONNECTIONS = listOf(
    0 to 1, 0 to 4, 1 to 2, 2 to 3, 3 to 7, 4 to 5, 5 to 6, 6 to 8,
    9 to 10, 11 to 12,
    11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21,
    17 to 19,
    12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22,
    18 to 20,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 24 to 26, 25 to 27, 26 to 28,
    27 to 29, 29 to 31,
    28 to 30, 30 to 32,
)

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    mViewModel: CameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mViewModel.uiState.collectAsState()

    var mPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var mSwitchSnapshot by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var mIsSwitchingLens by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        mViewModel.setCameraPermission(granted, isInitialCheck = false)
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        val granted = hasCameraPermission()
        mViewModel.setCameraPermission(granted, isInitialCheck = true)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mViewModel.setCameraPermission(hasCameraPermission(), isInitialCheck = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mViewModel.clearPoseOverlay()
            cameraExecutor.shutdown()
        }
    }

    if (!uiState.mHasCompletedInitialPermissionCheck) {
        StartupLoadingContent(modifier = modifier.fillMaxSize())
        return
    }

    if (!uiState.mHasCameraPermission) {
        PermissionRequiredContent(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                context.startActivity(intent)
            },
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val mMessageToAnnounce = uiState.mErrorMessage
        ?: uiState.mFormFeedback.mErrorMessage
        ?: uiState.mFormFeedback.mSpeechCue
    val mErrorSpeechAnnouncer = remember(context) { ErrorSpeechAnnouncer(context) }

    LaunchedEffect(mMessageToAnnounce) {
        mErrorSpeechAnnouncer.onErrorMessage(mMessageToAnnounce)
    }

    DisposableEffect(mErrorSpeechAnnouncer) {
        onDispose {
            mErrorSpeechAnnouncer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    mPreviewView = this
                }
            },
            update = { previewView ->
                mPreviewView = previewView
            },
        )

        LaunchedEffect(mPreviewView, uiState.mIsFrontCamera, lifecycleOwner) {
            val previewView = mPreviewView ?: return@LaunchedEffect
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (mIsSwitchingLens) {
                                    previewView.post {
                                        mIsSwitchingLens = false
                                        mSwitchSnapshot = null
                                    }
                                }
                                mViewModel.processFrame(imageProxy)
                            }
                        }

                    val lensFacing = if (uiState.mIsFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                    )
                },
                ContextCompat.getMainExecutor(context),
            )
        }

        PoseOverlay(
            landmarks = uiState.mLandmarks,
            frameWidth = uiState.mFrameWidth,
            frameHeight = uiState.mFrameHeight,
            isFrontCamera = uiState.mIsFrontCamera,
            modifier = Modifier.fillMaxSize(),
        )

        if (mIsSwitchingLens && mSwitchSnapshot != null) {
            Image(
                bitmap = mSwitchSnapshot!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (uiState.mIsStartupCameraLoading) {
            StartupLoadingContent(modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
        ) {
            Button(onClick = { mViewModel.toggleExerciseMode() }) {
                Text(
                    when (uiState.mSelectedExercise) {
                        ExerciseType.SQUAT -> "Switch to Plank"
                        ExerciseType.PLANK -> "Switch to Pullup"
                        ExerciseType.PULLUP -> "Switch to Squat"
                    },
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                mViewModel.onLensSwitchStarted()
                mSwitchSnapshot = mPreviewView?.bitmap?.asImageBitmap()
                mIsSwitchingLens = mSwitchSnapshot != null
                mViewModel.setFrontCamera(!uiState.mIsFrontCamera)
            }) {
                Text(if (uiState.mIsFrontCamera) "Back camera" else "Front camera")
            }
        }

        uiState.mErrorMessage?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .background(Color(0x88000000))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        FeedbackPanel(
            feedback = uiState.mFormFeedback,
            exerciseType = uiState.mSelectedExercise,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
        )
    }
}

@Composable
private fun FeedbackPanel(
    feedback: FormFeedback,
    exerciseType: ExerciseType,
    modifier: Modifier = Modifier,
) {
    val exerciseLabel = when (exerciseType) {
        ExerciseType.SQUAT -> "Squat"
        ExerciseType.PLANK -> "Plank"
        ExerciseType.PULLUP -> "Pullup"
    }
    val statusText = when (feedback.mStatus) {
        ExerciseStatus.INITIALIZING -> "Initializing"
        ExerciseStatus.READY -> "Ready"
        ExerciseStatus.ACTIVE -> "Active"
        ExerciseStatus.ERROR -> "Error"
    }

    val detailText = buildString {
        append("Exercise: ")
        append(exerciseLabel)
        append(" • Status: ")
        append(statusText)
        when (exerciseType) {
            ExerciseType.SQUAT -> {
                append(" • Reps: ")
                append(feedback.mRepCount)
                append(" • Phase: ")
                append(feedback.mCurrentPhase.name.lowercase().replaceFirstChar { it.titlecase() })
            }
            ExerciseType.PLANK -> {
                append(" • Timer: ")
                append(formatDuration(feedback.mHoldDurationMs))
            }
            ExerciseType.PULLUP -> {
                append(" • Reps: ")
                append(feedback.mRepCount)
            }
        }
    }

    val jointsText = if (feedback.mProblematicJoints.isEmpty()) {
        "Joints: -"
    } else {
        "Joints: ${feedback.mProblematicJoints.joinToString()}"
    }

    Column(
        modifier = modifier
            .background(Color(0x88000000))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = feedback.mErrorMessage ?: feedback.mPrimaryCue ?: "Good form",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = jointsText,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = detailText,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private class ErrorSpeechAnnouncer(context: Context) : TextToSpeech.OnInitListener {
    private val mTextToSpeech = TextToSpeech(context.applicationContext, this)
    private var mIsReady = false
    private var mPendingErrorMessage: String? = null
    private var mLastAnnouncedErrorMessage: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val languageResult = mTextToSpeech.setLanguage(Locale.getDefault())
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            return
        }

        mIsReady = true
        val pendingMessage = mPendingErrorMessage
        if (pendingMessage != null) {
            speakError(pendingMessage)
            mPendingErrorMessage = null
        }
    }

    fun onErrorMessage(message: String?) {
        val normalizedMessage = message
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (normalizedMessage == null) {
            mLastAnnouncedErrorMessage = null
            return
        }

        if (normalizedMessage == mLastAnnouncedErrorMessage) return

        if (!mIsReady) {
            mPendingErrorMessage = normalizedMessage
            return
        }

        speakError(normalizedMessage)
    }

    fun release() {
        mTextToSpeech.stop()
        mTextToSpeech.shutdown()
    }

    private fun speakError(message: String) {
        val utteranceId = "error_${message.hashCode()}"
        mTextToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        mLastAnnouncedErrorMessage = message
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera permission is required for live pose detection.")
        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 12.dp)) {
            Text("Grant camera permission")
        }
        Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
            Text("Open app settings")
        }
    }
}

@Composable
private fun PoseOverlay(
    landmarks: List<NormalizedLandmark>,
    frameWidth: Int,
    frameHeight: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (landmarks.isEmpty() || frameWidth <= 0 || frameHeight <= 0) return@Canvas

        val scale = max(size.width / frameWidth.toFloat(), size.height / frameHeight.toFloat())
        val scaledFrameWidth = frameWidth * scale
        val scaledFrameHeight = frameHeight * scale
        val offsetX = (size.width - scaledFrameWidth) / 2f
        val offsetY = (size.height - scaledFrameHeight) / 2f

        fun mapX(normalizedX: Float): Float {
            val mirroredX = if (isFrontCamera) 1f - normalizedX else normalizedX
            return (mirroredX * scaledFrameWidth) + offsetX
        }

        fun mapY(normalizedY: Float): Float = (normalizedY * scaledFrameHeight) + offsetY

        POSE_CONNECTIONS.forEach { (startIndex, endIndex) ->
            val start = landmarks.getOrNull(startIndex)
            val end = landmarks.getOrNull(endIndex)
            if (start != null && end != null) {
                drawLine(
                    color = Color.Cyan,
                    start = Offset(mapX(start.x()), mapY(start.y())),
                    end = Offset(mapX(end.x()), mapY(end.y())),
                    strokeWidth = 4f,
                )
            }
        }

        landmarks.forEach { landmark ->
            drawCircle(
                color = Color.Yellow,
                radius = 5f,
                center = Offset(mapX(landmark.x()), mapY(landmark.y())),
            )
        }
    }
}

@Composable
private fun StartupLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0x44000000)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
