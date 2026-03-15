package com.example.seally.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Range
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// UI Polish Colors
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorError = Color(0xFFF44336)
private val ColorWarning = Color(0xFFFF9800)
private val ColorInfo = Color(0xFF2196F3)
private val ColorOverlay = Color(0xAA000000)
private val ColorSkeleton = Color(0xFF00E5FF)
private val ColorJoint = Color(0xFFFFFF00)

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    mViewModel: CameraViewModel = viewModel(),
    mShowExerciseGuideOnEntry: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mViewModel.uiState.collectAsState()

    var mPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var mSwitchSnapshot by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var mIsSwitchingLens by remember { mutableStateOf(false) }
    var mHasShownEntryGuide by remember { mutableStateOf(false) }

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

    LaunchedEffect(
        uiState.mHasCompletedInitialPermissionCheck,
        uiState.mHasCameraPermission,
        uiState.mIsStartupCameraLoading,
    ) {
        if (
            !uiState.mHasCompletedInitialPermissionCheck ||
            !uiState.mHasCameraPermission ||
            uiState.mIsStartupCameraLoading
        ) {
            return@LaunchedEffect
        }
    }

    val mMessageToAnnounce = uiState.mErrorMessage ?: uiState.mFormFeedback.mSpeechCue
    val mErrorSpeechAnnouncer = remember(context) { ErrorSpeechAnnouncer(context) }

    LaunchedEffect(mMessageToAnnounce) {
        mErrorSpeechAnnouncer.onErrorMessage(mMessageToAnnounce)
    }

    DisposableEffect(mErrorSpeechAnnouncer) {
        onDispose {
            mErrorSpeechAnnouncer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
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

                    val imageAnalysisBuilder = ImageAnalysis.Builder()
                    Camera2Interop.Extender(imageAnalysisBuilder).setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        Range(TARGET_CAMERA_FPS, TARGET_CAMERA_FPS),
                    )
                    val imageAnalyzer = imageAnalysisBuilder
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
            problematicJoints = uiState.mFormFeedback.mProblematicJoints,
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

        // Top Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    mViewModel.onLensSwitchStarted()
                    mSwitchSnapshot = mPreviewView?.bitmap?.asImageBitmap()
                    mIsSwitchingLens = mSwitchSnapshot != null
                    mViewModel.setFrontCamera(!uiState.mIsFrontCamera)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ColorOverlay,
                    contentColor = Color.White
                )
            ) {
                Text(if (uiState.mIsFrontCamera) "Back" else "Front")
            }
            var mIsDialogOpen by remember { mutableStateOf(false) }
            Button(
                onClick = { mIsDialogOpen = true },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ColorOverlay,
                    contentColor = Color.White
                )
            ) {
                Text("LiveKit")
            }

            LiveKitConnectionDialog(
                mViewModel = mViewModel,
                uiState = uiState,
                isOpen = mIsDialogOpen,
                onDismiss = { mIsDialogOpen = false },
            )
        }

        // Status / Error Messages (Floating)
        uiState.mErrorMessage?.let { message ->
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = ColorError.copy(alpha = 0.8f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 80.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        // Main Feedback UI
        FeedbackContent(
            feedback = uiState.mFormFeedback,
            exerciseType = uiState.mSelectedExercise,
            onOpenGuide = {
                context.startActivity(createExerciseGuideIntent(context, uiState.mSelectedExercise))
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        )


        // Calibration Guide Overlay
        if (uiState.mFormFeedback.mStatus == ExerciseStatus.INITIALIZING || uiState.mFormFeedback.mStatus == ExerciseStatus.READY) {
            CalibrationOverlay(status = uiState.mFormFeedback.mStatus)
        }
    }
}

@Composable
private fun CalibrationOverlay(status: ExerciseStatus) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8f
            val cornerLength = 60f
            val color = if (status == ExerciseStatus.READY) ColorSuccess else Color.White.copy(alpha = alpha)

            // Draw corners to suggest framing
            // Top-left
            drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

            // Top-right
            drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

            // Bottom-left
            drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)

            // Bottom-right
            drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (status == ExerciseStatus.READY) "READY!" else "POSITION YOUR BODY",
                style = MaterialTheme.typography.headlineMedium,
                color = if (status == ExerciseStatus.READY) ColorSuccess else Color.White,
                fontWeight = FontWeight.Bold
            )
            if (status == ExerciseStatus.INITIALIZING) {
                Text(
                    text = "Ensure your full body is visible",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FeedbackContent(
    feedback: FormFeedback,
    exerciseType: ExerciseType,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exerciseLabel = when (exerciseType) {
        ExerciseType.SQUAT -> "Squat"
        ExerciseType.PLANK -> "Plank"
        ExerciseType.PULLUP -> "Pull-up"
        ExerciseType.PUSHUP -> "Push-up"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Big Counter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Exercise Label Tag with Help Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                androidx.compose.material3.Surface(
                    color = ColorOverlay,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = exerciseLabel.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                
                Spacer(Modifier.width(6.dp))
                
                // Help Button - Styled to match the label height
                androidx.compose.material3.Surface(
                    onClick = onOpenGuide,
                    color = ColorOverlay,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = "?",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Big Counter
            val counterValue = when (exerciseType) {
                ExerciseType.PLANK -> formatDuration(feedback.mHoldDurationMs)
                else -> feedback.mRepCount.toString()
            }
            val counterLabel = when (exerciseType) {
                ExerciseType.PLANK -> "TIME"
                else -> "REPS"
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = counterLabel,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = counterValue,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Main Feedback Card
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = ColorOverlay),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (feedback.mStatus) {
                        ExerciseStatus.READY -> ColorSuccess
                        ExerciseStatus.ACTIVE -> ColorInfo
                        ExerciseStatus.ERROR -> ColorError
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = feedback.mStatus.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }

                Text(
                    text = feedback.mErrorMessage ?: feedback.mPrimaryCue ?: "Keep going!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )

                if (feedback.mProblematicJoints.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ColorWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Check your: ${feedback.mProblematicJoints.joinToString(", ")}",
                            color = ColorWarning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun createExerciseGuideIntent(context: Context, exerciseType: ExerciseType): Intent {
    val guideActivityClass = when (exerciseType) {
        ExerciseType.SQUAT -> SquatGuideActivity::class.java
        ExerciseType.PLANK -> PlankGuideActivity::class.java
        ExerciseType.PULLUP -> PullupGuideActivity::class.java
        ExerciseType.PUSHUP -> PushupGuideActivity::class.java
    }
    return Intent(context, guideActivityClass)
}

private const val TARGET_CAMERA_FPS = 30

private class ErrorSpeechAnnouncer(context: Context) : TextToSpeech.OnInitListener {
    private val mTextToSpeech = TextToSpeech(context.applicationContext, this)
    private var mIsReady = false
    private var mPendingErrorMessage: String? = null
    private var mLastAnnouncedErrorMessage: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            mIsReady = false
            return
        }

        val defaultLanguageResult = mTextToSpeech.setLanguage(Locale.getDefault())
        if (
            defaultLanguageResult == TextToSpeech.LANG_MISSING_DATA ||
            defaultLanguageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            mTextToSpeech.setLanguage(Locale.US)
        }

        // Keep TTS usable even if locale selection reports unsupported.
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
        Text("Camera permission is required for live pose detection.", color = Color.White)
        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 12.dp)) {
            Text("Grant camera permission")
        }
        Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
            Text("Open app settings")
        }
    }
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

@Composable
private fun PoseOverlay(
    landmarks: List<NormalizedLandmark>,
    problematicJoints: List<String>,
    frameWidth: Int,
    frameHeight: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
) {
    val problematicIndices = remember(problematicJoints) {
        problematicJoints.flatMap { jointNameToIndex(it) }.toSet()
    }

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

        // Draw connections
        POSE_CONNECTIONS.forEach { (startIndex, endIndex) ->
            val start = landmarks.getOrNull(startIndex)
            val end = landmarks.getOrNull(endIndex)
            if (start != null && end != null) {
                val isProblematic = problematicIndices.contains(startIndex) || problematicIndices.contains(endIndex)
                drawLine(
                    color = if (isProblematic) ColorError else ColorSkeleton,
                    start = Offset(mapX(start.x()), mapY(start.y())),
                    end = Offset(mapX(end.x()), mapY(end.y())),
                    strokeWidth = if (isProblematic) 8f else 4f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        // Draw landmarks
        landmarks.forEachIndexed { index, landmark ->
            val isProblematic = problematicIndices.contains(index)
            drawCircle(
                color = if (isProblematic) ColorError else ColorJoint,
                radius = if (isProblematic) 10f else 6f,
                center = Offset(mapX(landmark.x()), mapY(landmark.y())),
            )
            if (isProblematic) {
                drawCircle(
                    color = ColorError.copy(alpha = 0.3f),
                    radius = 20f,
                    center = Offset(mapX(landmark.x()), mapY(landmark.y())),
                )
            }
        }
    }
}

@Composable
private fun StartupLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ColorSkeleton)
            Spacer(Modifier.height(16.dp))
            Text("Initializing Pose Engine...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Composable
private fun LiveKitConnectionDialog(
    mViewModel: CameraViewModel,
    uiState: CameraUiState,
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    var mRoomCodeInput by remember { mutableStateOf(uiState.mRoomCode) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            mRoomCodeInput = uiState.mRoomCode
        }
    }

    LaunchedEffect(uiState.mIsLiveKitConnected) {
        if (uiState.mIsLiveKitConnected) {
            onDismiss()
        }
    }

    if (isOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF262626),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Remote Session",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Enter a 6-digit room code to stream your landmarks to a coach or remote viewer.")
                    OutlinedTextField(
                        value = mRoomCodeInput,
                        onValueChange = { value ->
                            mRoomCodeInput = value.take(6).uppercase()
                            mViewModel.setRoomCode(mRoomCodeInput)
                        },
                        label = { Text("Room Code", color = Color(0xFF00E5FF)) },
                        placeholder = { Text("ABC123", color = Color.Gray) },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedIndicatorColor = Color(0xFF00E5FF),
                            unfocusedIndicatorColor = Color.Gray,
                            cursorColor = Color(0xFF00E5FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (uiState.mIsLiveKitConnected) ColorSuccess else Color.Gray,
                                    CircleShape
                                )
                        )
                        Text(
                            text = uiState.mLiveKitStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.mIsLiveKitConnected) ColorSuccess else Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                if (uiState.mIsLiveKitConnected) {
                    Button(
                        onClick = {
                            mViewModel.disconnectFromLiveKit()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                    ) {
                        Text("Disconnect", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            mViewModel.connectToLiveKit()
                        },
                        enabled = mRoomCodeInput.length == 6,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connect", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Close")
                }
            },
        )
    }
}
