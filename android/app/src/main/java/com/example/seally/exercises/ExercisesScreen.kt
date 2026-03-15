package com.example.seally.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seally.calendar.CalendarScreen
import com.example.seally.camera.CameraScreen
import com.example.seally.camera.CameraViewModel
import com.example.seally.camera.ExerciseType
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.repository.ExerciseLogRepository
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.TopHeader
import java.time.LocalDate

@Composable
fun ExercisesScreen(
    modifier: Modifier = Modifier,
    mCameraViewModel: CameraViewModel,
    onLeftActionClick: () -> Unit = {},
    onRightActionClick: () -> Unit = {},
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    var showDumbbellPage by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var mSelectedExerciseForChecker by remember { mutableStateOf<ExerciseType?>(null) }
    val mIsOnSubpage = mSelectedExerciseForChecker != null || showDumbbellPage || showCalendar

    LaunchedEffect(mIsOnSubpage) {
        onDetailVisibilityChanged(!mIsOnSubpage)
    }

    BackHandler(enabled = mIsOnSubpage) {
        when {
            mSelectedExerciseForChecker != null -> {
                mCameraViewModel.persistExerciseSessionOnExit()
                mSelectedExerciseForChecker = null
            }
            showDumbbellPage -> showDumbbellPage = false
            showCalendar -> showCalendar = false
        }
    }

    if (mSelectedExerciseForChecker != null) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraScreen(
                modifier = Modifier.fillMaxSize(),
                mViewModel = mCameraViewModel,
                mShowExerciseGuideOnEntry = true,
            )
            IconButton(
                onClick = {
                    mCameraViewModel.persistExerciseSessionOnExit()
                    mSelectedExerciseForChecker = null
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        return
    }

    if (showDumbbellPage) {
        DumbbellWorkoutsScreen(
            modifier = modifier,
            onBackClick = { showDumbbellPage = false },
            onExerciseSelected = { mExercise ->
                mCameraViewModel.setSelectedExercise(mExercise)
                mSelectedExerciseForChecker = mExercise
            },
        )
        return
    }

    if (showCalendar) {
        CalendarScreen(
            modifier = modifier,
            onBackClick = { showCalendar = false },
        )
        return
    }

    val context = LocalContext.current
    val mExerciseLogRepository = remember(context) { ExerciseLogRepository(context.applicationContext) }
    val mTodayDate = remember { LocalDate.now().toString() }
    val mTodayWorkout = mExerciseLogRepository.observeByDate(mTodayDate).collectAsState(initial = emptyList()).value
    val musclesImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/seals/muscles.png")
        .build()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AppScreenBackground(assetPath = "backgrounds/gym.png")

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeader(onProfileClick = onProfileClick)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Workout of the day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (mTodayWorkout.isEmpty()) {
                        Text(
                            text = "You do not have anything planned for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { showCalendar = true }) {
                            Text("Plan workout")
                        }
                    } else {
                        val mSummary = mTodayWorkout
                            .take(2)
                            .joinToString(" • ") { it.toWorkoutSummary() }
                        Text(
                            text = mSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { showCalendar = true }) {
                            Text("Open planned workout")
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f))
        }

        // --- Action Buttons (Similar to Nutrition Section) ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FloatingActionButton(
                onClick = {
                    onLeftActionClick()
                    showDumbbellPage = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = "Workouts", modifier = Modifier.size(32.dp))
            }

            FloatingActionButton(
                onClick = {
                    onRightActionClick()
                    showCalendar = true
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "History", modifier = Modifier.size(32.dp))
            }
        }

        // Seal/character - consistent anchoring
        AsyncImage(
            model = musclesImageRequest,
            contentDescription = "Seal",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.75f)
                .padding(bottom = 20.dp)
        )
    }
}

private fun ExerciseLogEntity.toWorkoutSummary(): String {
    val mName = exerciseName.ifBlank { "Exercise" }
    val mMetricLabel = if (metric.startsWith("sets:")) "reps" else metric.ifBlank { "units" }
    val mValueLabel = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else String.format("%.1f", quantity)
    return "$mName ($mValueLabel $mMetricLabel)"
}
