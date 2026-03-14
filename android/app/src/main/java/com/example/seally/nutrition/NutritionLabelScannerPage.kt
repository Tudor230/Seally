package com.example.seally.nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

data class NutritionLabelScanResult(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val recognizedText: String,
)

@Composable
fun NutritionLabelScannerPage(
    onBack: () -> Unit,
    onScanResult: (NutritionLabelScanResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mContext = androidx.compose.ui.platform.LocalContext.current
    val mLifecycleOwner = LocalLifecycleOwner.current
    val mCoroutineScope = rememberCoroutineScope()
    val mMainExecutor = remember(mContext) { ContextCompat.getMainExecutor(mContext) }

    var mPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var mImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var mHasCompletedInitialPermissionCheck by remember { mutableStateOf(false) }
    var mHasCameraPermission by remember { mutableStateOf(false) }
    var mIsScanning by remember { mutableStateOf(false) }
    var mStatusMessage by remember { mutableStateOf<String?>(null) }
    val mIsOcrReady = true

    val mOcrEngine = remember(mContext) {
        OnDeviceNutritionOcrEngine()
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

    LaunchedEffect(mPreviewView, mHasCameraPermission, mLifecycleOwner) {
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
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    mLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )
                mImageCapture = imageCapture
            },
            mMainExecutor,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(mContext).get().unbindAll() }
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

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                enabled = mIsOcrReady && !mIsScanning,
                onClick = {
                    val imageCapture = mImageCapture
                    if (imageCapture == null) {
                        mStatusMessage = "Camera is not ready yet."
                        return@Button
                    }
                    if (!mIsOcrReady) {
                        mStatusMessage = "OCR engine is not ready."
                        return@Button
                    }

                    mIsScanning = true
                    mStatusMessage = "Capturing nutrition label..."
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

                                    mStatusMessage = "Reading nutrition label..."
                                    val ocrResult = mOcrEngine.detectText(bitmap)
                                    ocrResult.fold(
                                        onSuccess = { detectedText ->
                                            val parsedResult = NutritionLabelParser.parse(detectedText)
                                            if (parsedResult == null) {
                                                mStatusMessage = "No readable nutrition values found."
                                            } else {
                                                mStatusMessage = null
                                                onScanResult(parsedResult)
                                            }
                                        },
                                        onFailure = { error ->
                                            mStatusMessage = error.message ?: "OCR failed."
                                        },
                                    )
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
            ) {
                Text(if (mIsScanning) "Scanning..." else "Scan label")
            }
        }

        if (mIsScanning) {
            ScannerLoadingOverlay(
                message = mStatusMessage ?: "Processing...",
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!mStatusMessage.isNullOrBlank()) {
            Text(
                text = mStatusMessage!!,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(Color(0x88000000))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

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

private object NutritionLabelParser {
    private val mNumericValueRegex = Regex("""(\d{1,4}(?:[.,]\d{1,2})?)""")
    private val mValueWithUnitRegex = Regex(
        """(\d{1,4}(?:[.,]\d{1,2})?)\s*(kcal|cal|kj|g|mg|gram|grams)?""",
        RegexOption.IGNORE_CASE,
    )
    private val mCaloriesRegex = Regex("""(?i)(\d{1,4}(?:[.,]\d{1,2})?)\s*(kcal|cal)""")
    private val mKjRegex = Regex("""(?i)(\d{1,4}(?:[.,]\d{1,2})?)\s*kj""")
    private val mNameStopWords = listOf(
        "nutrition",
        "calorie",
        "energy",
        "protein",
        "carb",
        "fat",
        "sugar",
        "fiber",
        "sodium",
        "serving",
        "per ",
    )

    private val mFoodPresets = listOf(
        FoodPreset("Greek Yogurt", 120, 17, 7, 3, listOf("greek yogurt", "yogurt", "yoghurt")),
        FoodPreset("Protein Bar", 210, 20, 22, 7, listOf("protein bar")),
        FoodPreset("Peanut Butter", 190, 7, 8, 16, listOf("peanut butter")),
        FoodPreset("Milk", 125, 8, 12, 5, listOf("milk")),
        FoodPreset("Oatmeal", 150, 5, 27, 3, listOf("oatmeal", "oats")),
    )

    fun parse(rawText: String): NutritionLabelScanResult? {
        val lines = rawText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return null

        var name = extractFoodName(lines)
        var calories = extractCalories(lines, rawText)
        var protein = extractMacroValue(lines, rawText, listOf("protein", "proteins"))
        var carbs = extractMacroValue(
            lines,
            rawText,
            listOf("total carbohydrate", "carbohydrate", "carbohydrates", "carbs"),
        )
        var fats = extractMacroValue(lines, rawText, listOf("total fat", "fat", "fats", "lipids"))

        val matchedPreset = findPreset(name, rawText)
        if (name.isBlank() && matchedPreset != null) {
            name = matchedPreset.name
        }
        if (calories <= 0) calories = matchedPreset?.calories ?: 0
        if (protein <= 0) protein = matchedPreset?.protein ?: 0
        if (carbs <= 0) carbs = matchedPreset?.carbs ?: 0
        if (fats <= 0) fats = matchedPreset?.fats ?: 0

        if (name.isBlank() && calories == 0 && protein == 0 && carbs == 0 && fats == 0) {
            return null
        }

        return NutritionLabelScanResult(
            name = name.ifBlank { "Scanned food" },
            calories = calories.coerceAtLeast(0),
            protein = protein.coerceAtLeast(0),
            carbs = carbs.coerceAtLeast(0),
            fats = fats.coerceAtLeast(0),
            recognizedText = rawText.trim(),
        )
    }

    private fun extractFoodName(lines: List<String>): String {
        val topCandidates = lines.take(6)
        return topCandidates.firstOrNull { line ->
            val normalized = line.lowercase()
            val hasStopWord = mNameStopWords.any { normalized.contains(it) }
            val letterCount = line.count(Char::isLetter)
            val digitCount = line.count(Char::isDigit)
            val isMostlyLetters = letterCount >= (digitCount + 3)
            !hasStopWord && isMostlyLetters && line.length in 3..60
        }.orEmpty()
    }

    private fun extractCalories(lines: List<String>, rawText: String): Int {
        mCaloriesRegex.find(rawText)?.groupValues?.getOrNull(1)?.let(::parseNumber)?.let { value ->
            if (value > 0) return value
        }

        val kcalFromRows = extractValueNearKeyword(
            lines = lines,
            keywords = listOf("calories", "energy", "kcal"),
            preferUnits = setOf("kcal", "cal"),
        )
        if (kcalFromRows > 0) return kcalFromRows

        val kj = mKjRegex.find(rawText)?.groupValues?.getOrNull(1)?.let(::parseNumber) ?: 0
        if (kj > 0) {
            return (kj / 4.184f).roundToInt()
        }
        return 0
    }

    private fun extractMacroValue(
        lines: List<String>,
        rawText: String,
        keywords: List<String>,
    ): Int {
        val fromRows = extractValueNearKeyword(
            lines = lines,
            keywords = keywords,
            preferUnits = setOf("g", "gram", "grams"),
        )
        if (fromRows > 0) return fromRows

        // Handles OCR where nutrient keyword and numeric value land on different lines.
        val mergedText = rawText.lowercase().replace("\n", " ")
        val keywordPattern = keywords.joinToString("|") { Regex.escape(it.lowercase()) }
        val fallbackRegex = Regex(
            """(?i)(?:$keywordPattern)[^\d]{0,40}(\d{1,3}(?:[.,]\d{1,2})?)\s*(g|mg|gram|grams)?""",
        )
        fallbackRegex.find(mergedText)?.groupValues?.getOrNull(1)?.let(::parseNumber)?.let { value ->
            if (value > 0) return value
        }
        return 0
    }

    private fun extractValueNearKeyword(
        lines: List<String>,
        keywords: List<String>,
        preferUnits: Set<String>,
    ): Int {
        val indexes = lines.mapIndexedNotNull { index, line ->
            val normalized = line.lowercase()
            if (keywords.any { normalized.contains(it.lowercase()) }) index else null
        }
        if (indexes.isEmpty()) return 0

        indexes.forEach { index ->
            val start = (index - 1).coerceAtLeast(0)
            val end = (index + 2).coerceAtMost(lines.lastIndex)
            val windowText = lines.subList(start, end + 1).joinToString(" ")
            val values = mValueWithUnitRegex.findAll(windowText)
                .mapNotNull { match ->
                    val number = parseNumber(match.groupValues[1])
                    val unit = match.groupValues.getOrNull(2)?.lowercase().orEmpty()
                    if (number <= 0) null else number to unit
                }
                .toList()

            if (values.isEmpty()) return@forEach

            val preferred = values.firstOrNull { (_, unit) -> unit in preferUnits }
            if (preferred != null) return preferred.first

            return values.first().first
        }

        return 0
    }

    private fun findPreset(name: String, text: String): FoodPreset? {
        val normalizedText = "$name $text".lowercase()
        return mFoodPresets.firstOrNull { preset ->
            normalizedText.contains(preset.name.lowercase()) ||
                preset.aliases.any { alias -> normalizedText.contains(alias) }
        }
    }

    private fun parseNumber(value: String): Int {
        val normalized = value.trim().replace(",", ".")
        return normalized.toFloatOrNull()?.roundToInt() ?: 0
    }

    private data class FoodPreset(
        val name: String,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fats: Int,
        val aliases: List<String>,
    )
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
        Text("Camera permission is required to scan nutrition labels.")
        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 12.dp)) {
            Text("Grant camera permission")
        }
        Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
            Text("Open app settings")
        }
    }
}

@Composable
private fun ScannerLoadingOverlay(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color(0x66000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
