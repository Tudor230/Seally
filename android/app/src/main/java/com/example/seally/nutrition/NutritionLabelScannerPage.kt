package com.example.seally.nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

data class NutritionLabelScanResult(
    val name: String,
    // Per-serving values (used for quarter/half/full/multiple serving options)
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val sugars: Int = 0,
    val fibers: Int = 0,
    // Per-100g values (used for direct grams option)
    val caloriesPer100g: Int = calories,
    val proteinPer100g: Int = protein,
    val carbsPer100g: Int = carbs,
    val fatsPer100g: Int = fats,
    val sugarsPer100g: Int = sugars,
    val fibersPer100g: Int = fibers,
    val recognizedText: String,
)

private const val NUTRITION_SCANNER_LOG_TAG = "NutritionScanner"

private enum class LiveDetectionType {
    Barcode,
    NutritionLabel,
}

private data class NormalizedBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private data class LiveDetectionResult(
    val type: LiveDetectionType,
    val bounds: NormalizedBoundingBox,
)

private data class BarcodeCaptureResult(
    val allValues: List<String>,
    val selectedValue: String?,
)

@Composable
fun NutritionLabelScannerPage(
    onBack: () -> Unit,
    onScanResult: (NutritionLabelScanResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mContext = LocalContext.current
    val mLifecycleOwner = LocalLifecycleOwner.current
    val mCoroutineScope = rememberCoroutineScope()
    val mMainExecutor = remember(mContext) { ContextCompat.getMainExecutor(mContext) }

    var mPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var mImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var mLiveDetection by remember { mutableStateOf<LiveDetectionResult?>(null) }
    var mHasCompletedInitialPermissionCheck by remember { mutableStateOf(false) }
    var mHasCameraPermission by remember { mutableStateOf(false) }
    var mIsScanning by remember { mutableStateOf(false) }
    var mStatusMessage by remember { mutableStateOf<String?>(null) }
    val mIsBarcodeReady = true
    val mIsOcrReady = true

    val mBarcodeEngine = remember(mContext) {
        OnDeviceBarcodeScannerEngine()
    }
    val mLiveDetectionEngine = remember(mContext) {
        OnDeviceLiveDetectionEngine()
    }
    val mOcrEngine = remember(mContext) {
        OnDeviceNutritionOcrEngine()
    }
    val mOpenFoodFactsClient = remember(mContext) {
        OpenFoodFactsApiClient()
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        mHasCameraPermission = granted
        mHasCompletedInitialPermissionCheck = true
    }

    LaunchedEffect(Unit) {
        val granted = hasCameraPermission()
        mHasCameraPermission = granted
        mHasCompletedInitialPermissionCheck = true
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(mLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mHasCameraPermission = hasCameraPermission()
            }
        }
        mLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { mLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mPreviewView, mHasCameraPermission, mLifecycleOwner, mLiveDetectionEngine) {
        val previewView = mPreviewView ?: return@LaunchedEffect
        if (!mHasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }
                val imageCapture = ImageCapture.Builder().build()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(mMainExecutor) { imageProxy ->
                            mLiveDetectionEngine.analyze(imageProxy) { detectionResult ->
                                mLiveDetection = detectionResult
                            }
                        }
                    }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    mLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis,
                )
                mImageCapture = imageCapture
            },
            mMainExecutor,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(mContext).get().unbindAll() }
            mBarcodeEngine.release()
            mLiveDetectionEngine.release()
            mOcrEngine.release()
        }
    }

    if (!mHasCompletedInitialPermissionCheck) {
        ScannerLoadingOverlay(
            message = "Checking camera permission...",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    if (!mHasCameraPermission) {
        PermissionRequiredContent(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", mContext.packageName, null))
                mContext.startActivity(intent)
            },
            modifier = modifier.fillMaxSize(),
        )
        return
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
            update = { previewView -> mPreviewView = previewView },
        )

        // Camera Overlay Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp)
                .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        )

        mLiveDetection?.let { detectionResult ->
            LiveDetectionOverlay(
                detection = detectionResult,
                modifier = Modifier.fillMaxSize(),
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        // Top Status Badge
        AnimatedVisibility(
            visible = !mStatusMessage.isNullOrBlank() && !mIsScanning,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = mStatusMessage!!,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Point at a barcode or nutrition label",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
            )

            FloatingActionButton(
                onClick = {
                    val imageCapture = mImageCapture
                    if (imageCapture == null) {
                        mStatusMessage = "Camera is not ready yet."
                        return@FloatingActionButton
                    }
                    mIsScanning = true
                    mStatusMessage = "Capturing image..."
                    val photoFile = File.createTempFile("nutrition_scan_", ".jpg", mContext.cacheDir)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        mMainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                mCoroutineScope.launch {
                                    val bitmap = withContext(Dispatchers.IO) {
                                        BitmapFactory.decodeFile(photoFile.absolutePath)
                                    }
                                    photoFile.delete()

                                    if (bitmap == null) {
                                        mStatusMessage = "Failed to decode captured image."
                                        mIsScanning = false
                                        return@launch
                                    }

                                    var mShouldRunOcrFallback = true
                                    mStatusMessage = "Scanning barcode..."
                                    val barcodeResult = withContext(Dispatchers.Default) {
                                        mBarcodeEngine.detectBarcode(bitmap)
                                    }
                                    barcodeResult.fold(
                                        onSuccess = { barcodeScanResult ->
                                            val barcodeValue = barcodeScanResult.selectedValue
                                            if (!barcodeValue.isNullOrBlank()) {
                                                mShouldRunOcrFallback = false
                                                mStatusMessage = "Barcode detected. Fetching product..."
                                                val productResult = mOpenFoodFactsClient.fetchProductByBarcode(barcodeValue)
                                                productResult.fold(
                                                    onSuccess = { apiResult ->
                                                        mStatusMessage = null
                                                        onScanResult(apiResult)
                                                    },
                                                    onFailure = { error ->
                                                        mStatusMessage = error.message ?: "OFF lookup failed."
                                                    },
                                                )
                                            }
                                        },
                                        onFailure = {
                                            mStatusMessage = "Barcode scan failed. Reading label..."
                                        },
                                    )

                                    if (mShouldRunOcrFallback) {
                                        mStatusMessage = "Reading nutrition label..."
                                        val ocrResult = withContext(Dispatchers.Default) {
                                            mOcrEngine.detectText(bitmap)
                                        }
                                        ocrResult.fold(
                                            onSuccess = { detectedText ->
                                                val parsedResult = NutritionLabelParser.parse(detectedText)
                                                if (parsedResult == null) {
                                                    mStatusMessage = "No nutrition values found."
                                                } else {
                                                    mStatusMessage = null
                                                    onScanResult(parsedResult)
                                                }
                                            },
                                            onFailure = { error ->
                                                mStatusMessage = error.message ?: "OCR failed."
                                            },
                                        )
                                    }
                                    mIsScanning = false
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                photoFile.delete()
                                mIsScanning = false
                                mStatusMessage = exception.message ?: "Failed to capture image."
                            }
                        },
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(84.dp)
            ) {
                if (mIsScanning) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                } else {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", modifier = Modifier.size(36.dp))
                }
            }
        }

        if (mIsScanning) {
            ScannerLoadingOverlay(
                message = mStatusMessage ?: "Processing...",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LiveDetectionOverlay(
    detection: LiveDetectionResult,
    modifier: Modifier = Modifier,
) {
    val mStrokeColor = when (detection.type) {
        LiveDetectionType.Barcode -> Color(0xFF29DB65)
        LiveDetectionType.NutritionLabel -> Color(0xFF34A9FF)
    }
    val mDetectionLabel = when (detection.type) {
        LiveDetectionType.Barcode -> "Barcode detected"
        LiveDetectionType.NutritionLabel -> "Nutrition label detected"
    }
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = size.width * detection.bounds.left
            val top = size.height * detection.bounds.top
            val right = size.width * detection.bounds.right
            val bottom = size.height * detection.bounds.bottom
            val width = (right - left).coerceAtLeast(0f)
            val height = (bottom - top).coerceAtLeast(0f)
            if (width > 0f && height > 0f) {
                drawRect(
                    color = mStrokeColor,
                    topLeft = Offset(x = left, y = top),
                    size = Size(width = width, height = height),
                    style = Stroke(width = 8f),
                )
            }
        }
        
        Surface(
            color = mStrokeColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Text(
                text = mDetectionLabel,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Camera Access Needed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We need camera access to scan nutrition labels and barcodes for your health tracking.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Grant Permission")
            }
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun ScannerLoadingOverlay(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(strokeWidth = 6.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ... (rest of the engines and parser code remains the same as it's logic, not UI)

private class OnDeviceNutritionOcrEngine {
    private val mRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun detectText(bitmap: Bitmap): Result<String> {
        return runCatching {
            val detectedText = suspendCancellableCoroutine<String> { continuation ->
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                mRecognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) {
                            continuation.resume(result.text)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
            }
            val text = detectedText.trim()
            if (text.isBlank()) {
                throw IllegalStateException("No readable text detected on the label.")
            }
            text
        }
    }

    fun release() {
        mRecognizer.close()
    }
}

private class OnDeviceBarcodeScannerEngine {
    private val mScanner = BarcodeScanning.getClient()

    suspend fun detectBarcode(bitmap: Bitmap): Result<BarcodeCaptureResult> {
        return runCatching {
            val barcodes = suspendCancellableCoroutine<List<Barcode>> { continuation ->
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                mScanner.process(inputImage)
                    .addOnSuccessListener { detectedBarcodes ->
                        if (continuation.isActive) {
                            continuation.resume(detectedBarcodes)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
            }
            val allValues = barcodes.mapNotNull { barcode ->
                barcode.rawValue?.trim()?.takeIf(String::isNotBlank)
            }
            BarcodeCaptureResult(
                allValues = allValues,
                selectedValue = allValues.firstOrNull(),
            )
        }
    }

    fun release() {
        mScanner.close()
    }
}

private class OnDeviceLiveDetectionEngine {
    private val mBarcodeScanner = BarcodeScanning.getClient()
    private val mTextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val mIsFrameBeingProcessed = AtomicBoolean(false)
    private val mNutritionKeywords = listOf(
        "nutrition",
        "calories",
        "energy",
        "protein",
        "carbohydrate",
        "carbs",
        "fat",
        "fats",
        "sugar",
    )

    @OptIn(ExperimentalGetImage::class)
    fun analyze(
        imageProxy: ImageProxy,
        onDetection: (LiveDetectionResult?) -> Unit,
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        if (!mIsFrameBeingProcessed.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val frameWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            mediaImage.height
        } else {
            mediaImage.width
        }
        val frameHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            mediaImage.width
        } else {
            mediaImage.height
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        fun finish(result: LiveDetectionResult?) {
            onDetection(result)
            mIsFrameBeingProcessed.set(false)
            imageProxy.close()
        }

        fun normalizedFromRect(rect: Rect): NormalizedBoundingBox? {
            if (frameWidth <= 0 || frameHeight <= 0) return null
            val left = (rect.left.toFloat() / frameWidth).coerceIn(0f, 1f)
            val top = (rect.top.toFloat() / frameHeight).coerceIn(0f, 1f)
            val right = (rect.right.toFloat() / frameWidth).coerceIn(0f, 1f)
            val bottom = (rect.bottom.toFloat() / frameHeight).coerceIn(0f, 1f)
            if (right <= left || bottom <= top) return null
            return NormalizedBoundingBox(left = left, top = top, right = right, bottom = bottom)
        }

        fun runNutritionLabelDetection() {
            mTextRecognizer.process(inputImage)
                .addOnSuccessListener { textResult ->
                    val nutritionBlocks = textResult.textBlocks.filter { block ->
                        val normalizedText = block.text.lowercase()
                        mNutritionKeywords.any { keyword -> normalizedText.contains(keyword) }
                    }
                    val mergedRect = mergeRectangles(nutritionBlocks.mapNotNull { block -> block.boundingBox })
                    val normalizedRect = mergedRect?.let(::normalizedFromRect)
                    if (normalizedRect == null) {
                        finish(null)
                    } else {
                        finish(
                            LiveDetectionResult(
                                type = LiveDetectionType.NutritionLabel,
                                bounds = normalizedRect,
                            ),
                        )
                    }
                }
                .addOnFailureListener {
                    finish(null)
                }
        }

        mBarcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val detectedBarcode = barcodes.firstOrNull { barcode ->
                    !barcode.rawValue.isNullOrBlank() && barcode.boundingBox != null
                }
                val normalizedRect = detectedBarcode?.boundingBox?.let(::normalizedFromRect)
                if (normalizedRect == null) {
                    runNutritionLabelDetection()
                } else {
                    finish(
                        LiveDetectionResult(
                            type = LiveDetectionType.Barcode,
                            bounds = normalizedRect,
                        ),
                    )
                }
            }
            .addOnFailureListener {
                runNutritionLabelDetection()
            }
    }

    fun release() {
        mBarcodeScanner.close()
        mTextRecognizer.close()
    }

    private fun mergeRectangles(rectangles: List<Rect>): Rect? {
        if (rectangles.isEmpty()) return null
        val left = rectangles.minOf { rect -> rect.left }
        val top = rectangles.minOf { rect -> rect.top }
        val right = rectangles.maxOf { rect -> rect.right }
        val bottom = rectangles.maxOf { rect -> rect.bottom }
        return Rect(left, top, right, bottom)
    }
}

private class OpenFoodFactsApiClient {
    suspend fun fetchProductByBarcode(barcode: String): Result<NutritionLabelScanResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val sanitizedBarcode = barcode.filter(Char::isDigit)
                require(sanitizedBarcode.isNotBlank()) { "Detected barcode is invalid." }

                val endpoint = "https://world.openfoodfacts.org/api/v2/product/" +
                    "${Uri.encode(sanitizedBarcode)}.json" +
                    "?fields=product_name,product_name_en,nutriments"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000_000
                    readTimeout = 10_000_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Seally/1.0 (support@seally.app)")
                }

                try {
                    val responseCode = connection.responseCode
                    val responseStream = if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    val body = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (responseCode !in 200..299) {
                        throw IllegalStateException("OFF request failed ($responseCode).")
                    }
                    val jsonObject = JSONObject(body)
                    if (jsonObject.optInt("status", 0) != 1) {
                        throw IllegalStateException("No product found.")
                    }
                    val productObject = jsonObject.optJSONObject("product")
                        ?: throw IllegalStateException("No product data.")
                    val nutriments = productObject.optJSONObject("nutriments") ?: JSONObject()

                    val caloriesPerServing = nutriments.readNumericValue("energy-kcal_serving", "energy-kcal")?.roundToInt()
                    val proteinPerServing = nutriments.readNumericValue("proteins_serving", "proteins")?.roundToInt()
                    val carbsPerServing = nutriments.readNumericValue("carbohydrates_serving", "carbohydrates")?.roundToInt()
                    val fatsPerServing = nutriments.readNumericValue("fat_serving", "fat")?.roundToInt()
                    val sugarsPerServing = nutriments.readNumericValue("sugars_serving", "sugars")?.roundToInt()
                    val fibersPerServing = nutriments.readNumericValue("fiber_serving", "fiber")?.roundToInt()

                    val caloriesPer100g = nutriments.readNumericValue("energy-kcal_100g", "energy-kcal")?.roundToInt()
                    val proteinPer100g = nutriments.readNumericValue("proteins_100g", "proteins")?.roundToInt()
                    val carbsPer100g = nutriments.readNumericValue("carbohydrates_100g", "carbohydrates")?.roundToInt()
                    val fatsPer100g = nutriments.readNumericValue("fat_100g", "fat")?.roundToInt()
                    val sugarsPer100g = nutriments.readNumericValue("sugars_100g", "sugars")?.roundToInt()
                    val fibersPer100g = nutriments.readNumericValue("fiber_100g", "fiber")?.roundToInt()
                    
                    val productName = productObject.optString("product_name")
                        .ifBlank { productObject.optString("product_name_en") }
                        .ifBlank { "Scanned product" }

                    NutritionLabelScanResult(
                        name = productName,
                        calories = (caloriesPerServing ?: caloriesPer100g ?: 0).coerceAtLeast(0),
                        protein = (proteinPerServing ?: proteinPer100g ?: 0).coerceAtLeast(0),
                        carbs = (carbsPerServing ?: carbsPer100g ?: 0).coerceAtLeast(0),
                        fats = (fatsPerServing ?: fatsPer100g ?: 0).coerceAtLeast(0),
                        sugars = (sugarsPerServing ?: sugarsPer100g ?: 0).coerceAtLeast(0),
                        fibers = (fibersPerServing ?: fibersPer100g ?: 0).coerceAtLeast(0),
                        caloriesPer100g = (caloriesPer100g ?: caloriesPerServing ?: 0).coerceAtLeast(0),
                        proteinPer100g = (proteinPer100g ?: proteinPerServing ?: 0).coerceAtLeast(0),
                        carbsPer100g = (carbsPer100g ?: carbsPerServing ?: 0).coerceAtLeast(0),
                        fatsPer100g = (fatsPer100g ?: fatsPerServing ?: 0).coerceAtLeast(0),
                        sugarsPer100g = (sugarsPer100g ?: sugarsPerServing ?: 0).coerceAtLeast(0),
                        fibersPer100g = (fibersPer100g ?: fibersPerServing ?: 0).coerceAtLeast(0),
                        recognizedText = "barcode:$sanitizedBarcode",
                    )
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private fun JSONObject.readNumericValue(vararg keys: String): Float? {
        keys.forEach { key ->
            if (!has(key)) return@forEach
            when (val value = opt(key)) {
                is Number -> return value.toFloat()
                is String -> {
                    val parsedValue = value.trim().replace(",", ".").toFloatOrNull()
                    if (parsedValue != null) return parsedValue
                }
            }
        }
        return null
    }
}

private object NutritionLabelParser {
    private val mValueWithUnitRegex = Regex(
        """(\d{1,4}(?:[.,]\d{1,2})?)\s*(kcal|cal|kj|g|mg|gram|grams)?""",
        RegexOption.IGNORE_CASE,
    )
    private val mCaloriesRegex = Regex("""(?i)(\d{1,4}(?:[.,]\d{1,2})?)\s*(kcal|cal)""")
    private val mKjRegex = Regex("""(?i)(\d{1,4}(?:[.,]\d{1,2})?)\s*kj""")
    private val mNameStopWords = listOf(
        "nutrition", "calorie", "energy", "protein", "carb", "fat", "sugar", "fiber", "sodium", "serving", "per "
    )

    fun parse(rawText: String): NutritionLabelScanResult? {
        val lines = rawText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return null

        var name = extractFoodName(lines)
        var calories = extractCalories(lines, rawText)
        var protein = extractMacroValue(lines, rawText, listOf("protein", "proteins"))
        var carbs = extractMacroValue(lines, rawText, listOf("total carbohydrate", "carbohydrate", "carbohydrates", "carbs"))
        var fats = extractMacroValue(lines, rawText, listOf("total fat", "fat", "fats", "lipids"))
        var sugars = extractMacroValue(lines, rawText, listOf("sugars", "sugar", "total sugars"))
        var fibers = extractMacroValue(lines, rawText, listOf("fiber", "fibre", "dietary fiber"))

        if (name.isBlank() && calories == 0 && protein == 0 && carbs == 0 && fats == 0 && sugars == 0 && fibers == 0) {
            return null
        }

        return NutritionLabelScanResult(
            name = name.ifBlank { "Scanned food" },
            calories = calories,
            protein = protein,
            carbs = carbs,
            fats = fats,
            sugars = sugars,
            fibers = fibers,
            recognizedText = rawText.trim(),
        )
    }

    private fun extractFoodName(lines: List<String>): String {
        return lines.take(6).firstOrNull { line ->
            val normalized = line.lowercase()
            val hasStopWord = mNameStopWords.any { normalized.contains(it) }
            val letterCount = line.count(Char::isLetter)
            val digitCount = line.count(Char::isDigit)
            !hasStopWord && letterCount >= (digitCount + 3) && line.length in 3..60
        }.orEmpty()
    }

    private fun extractCalories(lines: List<String>, rawText: String): Int {
        mCaloriesRegex.find(rawText)?.groupValues?.getOrNull(1)?.let(::parseNumber)?.let { if (it > 0) return it }
        val kcalFromRows = extractValueNearKeyword(lines, listOf("calories", "energy", "kcal"), setOf("kcal", "cal"))
        if (kcalFromRows > 0) return kcalFromRows
        val kj = mKjRegex.find(rawText)?.groupValues?.getOrNull(1)?.let(::parseNumber) ?: 0
        if (kj > 0) return (kj / 4.184f).roundToInt()
        return 0
    }

    private fun extractMacroValue(lines: List<String>, rawText: String, keywords: List<String>): Int {
        val fromRows = extractValueNearKeyword(lines, keywords, setOf("g", "gram", "grams"))
        if (fromRows > 0) return fromRows
        val keywordPattern = keywords.joinToString("|") { Regex.escape(it.lowercase()) }
        val fallbackRegex = Regex("""(?i)(?:$keywordPattern)[^\d]{0,40}(\d{1,3}(?:[.,]\d{1,2})?)\s*(g|mg|gram|grams)?""")
        fallbackRegex.find(rawText.replace("\n", " "))?.groupValues?.getOrNull(1)?.let(::parseNumber)?.let { if (it > 0) return it }
        return 0
    }

    private fun extractValueNearKeyword(lines: List<String>, keywords: List<String>, preferUnits: Set<String>): Int {
        val indexes = lines.mapIndexedNotNull { index, line -> if (keywords.any { line.lowercase().contains(it) }) index else null }
        indexes.forEach { index ->
            val windowText = lines.subList((index - 1).coerceAtLeast(0), (index + 2).coerceAtMost(lines.lastIndex) + 1).joinToString(" ")
            val values = mValueWithUnitRegex.findAll(windowText).mapNotNull { match ->
                val number = parseNumber(match.groupValues[1])
                val unit = match.groupValues.getOrNull(2)?.lowercase().orEmpty()
                if (number <= 0) null else number to unit
            }.toList()
            if (values.isNotEmpty()) {
                val preferred = values.firstOrNull { it.second in preferUnits }
                return preferred?.first ?: values.first().first
            }
        }
        return 0
    }

    private fun parseNumber(value: String): Int = value.replace(",", ".").toFloatOrNull()?.roundToInt() ?: 0
}
