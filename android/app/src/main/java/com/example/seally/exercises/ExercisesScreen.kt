package com.example.seally.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.seally.calendar.CalendarScreen
import com.example.seally.camera.CameraScreen
import com.example.seally.camera.CameraViewModel
import com.example.seally.camera.ExerciseType
import com.example.seally.data.local.entity.CalendarPlanEntryEntity
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.repository.CalendarPlanRepository
import com.example.seally.data.repository.ExerciseLogRepository
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.TopHeader
import com.example.seally.xp.XpCalculators
import java.time.LocalDate

@Composable
fun ExercisesScreen(
    modifier: Modifier = Modifier,
    mCameraViewModel: CameraViewModel,
    onLeftActionClick: () -> Unit = {},
    onRightActionClick: () -> Unit = {},
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    var showDumbbellPage by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var mSelectedExerciseForChecker by remember { mutableStateOf<ExerciseType?>(null) }
    var mExerciseSetupDialogExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var mShouldShowSessionXpDialog by remember { mutableStateOf(false) }
    var mLastSessionEarnedXp by remember { mutableIntStateOf(0) }
    var mLastSessionValue by remember { mutableIntStateOf(0) } // Reps or Seconds
    var mLastSessionMetricLabel by remember { mutableStateOf("reps") } // "reps" or "seconds"
    val mIsOnSubpage = mSelectedExerciseForChecker != null || showDumbbellPage || showCalendar
    val mCameraUiState by mCameraViewModel.uiState.collectAsState()

    fun finishExerciseAndShowXp() {
        val exercise = mCameraUiState.mSelectedExercise
        val (xp, value, label) = when (exercise) {
            ExerciseType.PLANK -> {
                val seconds = (mCameraUiState.mFormFeedback.mMaxHoldDurationMs / 1000L).toInt().coerceAtLeast(0)
                Triple(XpCalculators.totalPlankXpForSeconds(seconds), seconds, "seconds")
            }
            else -> {
                val reps = mCameraUiState.mFormFeedback.mRepCount.coerceAtLeast(0)
                Triple(XpCalculators.exerciseXpForRepDelta(reps), reps, "reps")
            }
        }
        
        mCameraViewModel.persistExerciseSessionOnExit()
        mSelectedExerciseForChecker = null
        
        mLastSessionEarnedXp = xp
        mLastSessionValue = value
        mLastSessionMetricLabel = label
        mShouldShowSessionXpDialog = true
    }

    LaunchedEffect(mIsOnSubpage) {
        onDetailVisibilityChanged(!mIsOnSubpage)
    }

    BackHandler(enabled = mIsOnSubpage) {
        when {
            mSelectedExerciseForChecker != null -> {
                finishExerciseAndShowXp()
            }
            showDumbbellPage -> showDumbbellPage = false
            showCalendar -> showCalendar = false
        }
    }

    if (mShouldShowSessionXpDialog) {
        ExerciseResultDialog(
            onDismiss = { mShouldShowSessionXpDialog = false },
            xpEarned = mLastSessionEarnedXp,
            value = mLastSessionValue,
            metricLabel = mLastSessionMetricLabel
        )
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
                    finishExerciseAndShowXp()
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
                mExerciseSetupDialogExercise = mExercise
            },
        )
        mExerciseSetupDialogExercise?.let { mExercise ->
            ExerciseTargetSetupDialog(
                mExerciseType = mExercise,
                onDismiss = { mExerciseSetupDialogExercise = null },
                onStart = { targetUnits ->
                    mCameraViewModel.startExerciseSession(
                        mExerciseType = mExercise,
                        mTargetUnits = targetUnits,
                    )
                    mSelectedExerciseForChecker = mExercise
                    mExerciseSetupDialogExercise = null
                },
            )
        }
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
    val mCalendarPlanRepository = remember(context) { CalendarPlanRepository(context.applicationContext) }
    val mTodayDate = remember { LocalDate.now().toString() }
    val mTodayWorkout = mExerciseLogRepository.observeByDate(mTodayDate).collectAsState(initial = emptyList()).value
    val mTodayPlannedWorkout = mCalendarPlanRepository.observeByDate(mTodayDate).collectAsState(initial = emptyList()).value
    val mPreviewWorkoutSummary = if (mTodayWorkout.isNotEmpty()) {
        mTodayWorkout.take(2).joinToString(" • ") { it.toWorkoutSummary() }
    } else {
        mTodayPlannedWorkout.take(2).joinToString(" • ") { it.toWorkoutSummary() }
    }
    val mHasWorkoutForToday = mTodayWorkout.isNotEmpty() || mTodayPlannedWorkout.isNotEmpty()
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
            TopHeader(
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick,
            )

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

                    if (!mHasWorkoutForToday) {
                        Text(
                            text = "You do not have anything planned for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { showCalendar = true }) {
                            Text("Plan workout")
                        }
                    } else {
                        Text(
                            text = mPreviewWorkoutSummary,
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

    }
}

@Composable
private fun ExerciseResultDialog(
    onDismiss: () -> Unit,
    xpEarned: Int,
    value: Int,
    metricLabel: String
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Text(
                    text = "Exercise done!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Value (Reps/Seconds)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$value",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = metricLabel.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Right: XP
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "+$xpEarned",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "XP Earned",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Confirm Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Nice")
                }
            }
        }
    }
}

@Composable
private fun ExerciseTargetSetupDialog(
    mExerciseType: ExerciseType,
    onDismiss: () -> Unit,
    onStart: (targetUnits: Int) -> Unit,
) {
    val isPlank = mExerciseType == ExerciseType.PLANK
    var mTargetUnits by remember(mExerciseType) {
        mutableIntStateOf(if (isPlank) 30 else 10)
    }
    
    val mExpectedXp = if (isPlank) {
        XpCalculators.totalPlankXpForSeconds(mTargetUnits)
    } else {
        XpCalculators.exerciseXpForRepDelta(mTargetUnits)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Set Your Goal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPlank) "How many seconds?" else "How many reps?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Number Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    FilledIconButton(
                        onClick = { 
                            val step = if (isPlank) 5 else 1
                            if (mTargetUnits > step) mTargetUnits -= step 
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "$mTargetUnits",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 60.dp),
                        textAlign = TextAlign.Center
                    )

                    FilledIconButton(
                        onClick = { 
                            val step = if (isPlank) 5 else 1
                            mTargetUnits += step 
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // XP Preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Reward:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "+$mExpectedXp XP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onStart(mTargetUnits) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}

private fun ExerciseLogEntity.toWorkoutSummary(): String {
    val mName = exerciseName.ifBlank { "Exercise" }
    val mMetricLabel = if (metric.startsWith("sets:")) "reps" else metric.ifBlank { "units" }
    val mValueLabel = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else String.format("%.1f", quantity)
    return "$mName ($mValueLabel $mMetricLabel)"
}

private fun CalendarPlanEntryEntity.toWorkoutSummary(): String {
    val mName = exerciseName.ifBlank { "Exercise" }
    val mMetricLabel = metric.ifBlank { "units" }
    val mValueLabel = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else String.format("%.1f", quantity)
    return "$mName ($mValueLabel $mMetricLabel)"
}
