package com.example.seally.goals

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.data.local.entity.NutritionFoodEntryEntity
import com.example.seally.data.local.entity.NutritionLogEntity
import com.example.seally.data.repository.DailyGoalProgressRepository
import com.example.seally.data.repository.ExerciseLogRepository
import com.example.seally.data.repository.NutritionFoodEntryRepository
import com.example.seally.data.repository.NutritionLogRepository
import com.example.seally.data.repository.TargetRepository
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.TopHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max

enum class GoalChartType {
    LINE,
    BAR,
    LOADING_BAR,
}

private const val GOAL_CHART_HEADROOM_FACTOR = 1.08f
private const val EXERCISE_GOAL_MAX_DAYS = 7f

enum class GoalDirection {
    AT_LEAST,
    AT_MOST,
}

enum class GoalMetric(
    val mLabel: String,
    val mUnit: String,
    val mAccentColor: Color,
    val mChartType: GoalChartType,
    val mGoalDirection: GoalDirection,
    val mSuggestedTarget: Float,
    val mDefaultLabels: List<String>,
    val mIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    STEPS(
        mLabel = "Steps",
        mUnit = "steps",
        mAccentColor = Color(0xFF63B95B),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedTarget = 10_000f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.TrendingUp
    ),
    WEIGHT(
        mLabel = "Weight",
        mUnit = "kg",
        mAccentColor = Color(0xFFE39A55),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedTarget = 68f,
        mDefaultLabels = emptyList(),
        mIcon = Icons.Default.Flag
    ),
    RUNNING(
        mLabel = "Running",
        mUnit = "km",
        mAccentColor = Color(0xFFB17AE0),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedTarget = 50f,
        mDefaultLabels = emptyList(),
        mIcon = Icons.Default.TrendingUp
    ),
    WATER(
        mLabel = "Water",
        mUnit = "ml",
        mAccentColor = Color(0xFF4D8EFF),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedTarget = 2_500f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.LocalDrink
    ),
    CALORIES(
        mLabel = "Calories",
        mUnit = "kcal",
        mAccentColor = Color(0xFFDE7C64),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedTarget = 2_000f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Restaurant
    ),
    PROTEIN(
        mLabel = "Protein",
        mUnit = "g",
        mAccentColor = Color(0xFFE91E63),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedTarget = 140f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Restaurant
    ),
    CARBS(
        mLabel = "Carbs",
        mUnit = "g",
        mAccentColor = Color(0xFF4CAF50),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedTarget = 220f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Restaurant
    ),
    FATS(
        mLabel = "Fats",
        mUnit = "g",
        mAccentColor = Color(0xFFFF9800),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedTarget = 70f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Restaurant
    ),
    SUGARS(
        mLabel = "Sugars",
        mUnit = "g",
        mAccentColor = Color(0xFF9C27B0),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedTarget = 50f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Restaurant
    ),
    FIBERS(
        mLabel = "Fibers",
        mUnit = "g",
        mAccentColor = Color(0xFF795548),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedTarget = 30f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Restaurant
    ),
    EXERCISE_DAYS(
        mLabel = "Exercise",
        mUnit = "days/week",
        mAccentColor = Color(0xFF2196F3),
        mChartType = GoalChartType.LOADING_BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedTarget = 5f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.FitnessCenter
    ),
}

data class GoalUiModel(
    val mId: Long,
    val mMetric: GoalMetric,
    val mCurrentValue: Float,
    val mTargetValue: Float,
    val mHistoryValues: List<Float>,
    val mChartLabels: List<String>,
)

@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val mViewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory)
    val mGoals by mViewModel.mGoals.collectAsState()
    var mShowAddGoalDialog by remember { mutableStateOf(false) }
    var mSelectedGoal by remember { mutableStateOf<GoalUiModel?>(null) }

    val mTrackedMetrics = mGoals.map { it.mMetric }.toSet()
    val mAvailableMetrics = GoalMetric.entries.filterNot { it in mTrackedMetrics }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AppScreenBackground(assetPath = "backgrounds/goals.png")

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeader(
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Active Goals",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (mGoals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tracking goals yet.\nStart by adding one below!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize().weight(1f),
                    ) {
                        items(items = mGoals, key = { it.mId }) { mGoal ->
                            GoalCard(
                                goal = mGoal,
                                onClick = { mSelectedGoal = mGoal },
                            )
                        }
                    }
                }
            }
        }

        // --- ADD GOAL BUTTON (Large & Stylized at Bottom Right) ---
        if (mAvailableMetrics.isNotEmpty()) {
            FloatingActionButton(
                onClick = { mShowAddGoalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(72.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", modifier = Modifier.size(36.dp))
            }
        }
    }

    if (mShowAddGoalDialog) {
        AddGoalDialog(
            availableMetrics = mAvailableMetrics,
            onDismiss = { mShowAddGoalDialog = false },
            onGoalAdded = { mMetric, mCurrent, mTarget ->
                mViewModel.addGoal(
                    mMetric = mMetric,
                    mCurrent = mCurrent,
                    mTarget = mTarget,
                )
                mShowAddGoalDialog = false
            },
        )
    }

    mSelectedGoal?.let { mGoal ->
        GoalDetailsDialog(
            goal = mGoal,
            onDismiss = { mSelectedGoal = null },
            onDelete = {
                mViewModel.deleteGoal(mGoal.mId)
                mSelectedGoal = null
            },
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalUiModel,
    onClick: () -> Unit,
) {
    val mProgress = goal.progress()
    val mColor = goal.mMetric.mAccentColor
    val mIsGoalCompleted = mProgress >= 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = mColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = goal.mMetric.mIcon,
                                contentDescription = null,
                                tint = mColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.mMetric.mLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (mIsGoalCompleted) "Goal Achieved!" else "In Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (mIsGoalCompleted) mColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                    CircularProgressIndicator(
                        progress = { mProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = mColor,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = mColor.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${(mProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = goal.formatCurrent(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = goal.formatTarget(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GoalChart(
                values = goal.mHistoryValues,
                labels = goal.mChartLabels,
                targetValue = goal.mTargetValue,
                chartType = goal.mMetric.mChartType,
                chartColor = mColor,
                gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                axisColor = MaterialTheme.colorScheme.outlineVariant,
                isGoalCompleted = mIsGoalCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            )
        }
    }
}

@Composable
private fun GoalDetailsDialog(
    goal: GoalUiModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val mProgress = goal.progress()
    val mColor = goal.mMetric.mAccentColor
    val mIsGoalCompleted = mProgress >= 1f
    var mShowDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${goal.mMetric.mLabel} Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Surface(
                    color = mColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = mColor, fontWeight = FontWeight.Bold)
                            Text(goal.formatCurrent(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GOAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(goal.formatTarget(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                GoalChart(
                    values = goal.mHistoryValues,
                    labels = goal.mChartLabels,
                    valueLabels = goal.historyValueLabels(),
                    targetValue = goal.mTargetValue,
                    chartType = goal.mMetric.mChartType,
                    chartColor = mColor,
                    gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    axisColor = MaterialTheme.colorScheme.outlineVariant,
                    isGoalCompleted = mIsGoalCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("Close Details")
                }
                Button(
                    onClick = { mShowDeleteConfirm = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete Goal")
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )

    if (mShowDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { mShowDeleteConfirm = false },
            title = { Text("Stop tracking goal?") },
            text = { Text("Are you sure you want to delete '${goal.mMetric.mLabel}'? All history will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        mShowDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { mShowDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun GoalChart(
    values: List<Float>,
    labels: List<String>,
    valueLabels: List<String> = emptyList(),
    targetValue: Float = 1f,
    chartType: GoalChartType,
    chartColor: Color,
    gridColor: Color,
    labelColor: Color,
    axisColor: Color,
    isGoalCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val mWidth = size.width
            val mHeight = size.height
            val mHorizontalPadding = 4.dp.toPx()
            val mTopY = 6.dp.toPx()
            val mBottomY = mHeight - 8.dp.toPx()
            val mChartWidth = mWidth - (mHorizontalPadding * 2f)
            val mGoalValue = 1f
            val mMaxEntryValue = values.maxOrNull() ?: 0f
            val mScaleBase = max(mGoalValue, mMaxEntryValue)
            val mScaleMax = (mScaleBase * GOAL_CHART_HEADROOM_FACTOR).coerceAtLeast(1f)
            val mStepX = if (values.size > 1) mChartWidth / (values.size - 1) else 0f
            val mGoalLineColor = Color(
                red = axisColor.red * 0.45f,
                green = axisColor.green * 0.45f,
                blue = axisColor.blue * 0.45f,
                alpha = 1f,
            )
            val mGoalLineY = mBottomY - ((mBottomY - mTopY) * (mGoalValue / mScaleMax).coerceIn(0f, 1f))

            if (chartType == GoalChartType.LOADING_BAR) {
                val mLastRatio = values.lastOrNull()?.coerceAtLeast(0f) ?: 0f
                val mTargetDays = targetValue.coerceIn(1f, EXERCISE_GOAL_MAX_DAYS)
                val mCurrentDays = (mLastRatio * mTargetDays).coerceAtLeast(0f)
                val mProgress = (mCurrentDays / EXERCISE_GOAL_MAX_DAYS).coerceIn(0f, 1f)
                val mTrackTop = mTopY + ((mBottomY - mTopY) * 0.38f)
                val mTrackHeight = (mBottomY - mTopY) * 0.28f
                val mTrackWidth = mChartWidth
                val mFillWidth = mTrackWidth * mProgress
                val mSegmentWidth = mTrackWidth / EXERCISE_GOAL_MAX_DAYS
                val mGoalMarkerX = mHorizontalPadding + (mSegmentWidth * mTargetDays)

                drawRoundRect(
                    color = axisColor.copy(alpha = 0.22f),
                    topLeft = Offset(mHorizontalPadding, mTrackTop),
                    size = Size(mTrackWidth, mTrackHeight),
                    cornerRadius = CornerRadius(mTrackHeight / 2f, mTrackHeight / 2f),
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(chartColor.copy(alpha = 0.9f), chartColor),
                    ),
                    topLeft = Offset(mHorizontalPadding, mTrackTop),
                    size = Size(mFillWidth, mTrackHeight),
                    cornerRadius = CornerRadius(mTrackHeight / 2f, mTrackHeight / 2f),
                )
                for (segment in 1 until EXERCISE_GOAL_MAX_DAYS.toInt()) {
                    val mX = mHorizontalPadding + (mSegmentWidth * segment)
                    drawLine(
                        color = axisColor.copy(alpha = 0.3f),
                        start = Offset(mX, mTrackTop + 1.dp.toPx()),
                        end = Offset(mX, mTrackTop + mTrackHeight - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawLine(
                    color = axisColor.copy(alpha = 0.95f),
                    start = Offset(mGoalMarkerX, mTrackTop - 4.dp.toPx()),
                    end = Offset(mGoalMarkerX, mTrackTop + mTrackHeight + 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else if (chartType == GoalChartType.BAR) {
                val mSlotWidth = mChartWidth / values.size
                val mBarWidth = mSlotWidth * if (isGoalCompleted) 0.74f else 0.58f
                values.forEachIndexed { mIndex, mValue ->
                    val mNormalized = (mValue / mScaleMax).coerceIn(0f, 1f)
                    val mBarHeight = (mBottomY - mTopY) * mNormalized
                    val mCenterX = mHorizontalPadding + (mIndex * mSlotWidth) + (mSlotWidth / 2f)
                    val mLeft = mCenterX - (mBarWidth / 2f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(chartColor, chartColor.copy(alpha = 0.4f)),
                        ),
                        topLeft = Offset(mLeft, mBottomY - mBarHeight),
                        size = Size(mBarWidth, mBarHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    )
                }
            } else {
                val mPoints = values.mapIndexed { mIndex, mValue ->
                    val mX = mHorizontalPadding + mIndex * mStepX
                    val mNormalized = (mValue / mScaleMax).coerceIn(0f, 1f)
                    val mY = mBottomY - ((mBottomY - mTopY) * mNormalized)
                    Offset(mX, mY)
                }

                val mLinePath = Path().apply {
                    if (mPoints.isNotEmpty()) {
                        moveTo(mPoints.first().x, mPoints.first().y)
                        mPoints.drop(1).forEach { mPoint -> lineTo(mPoint.x, mPoint.y) }
                    }
                }

                if (mPoints.isNotEmpty()) {
                    val mFillPath = Path().apply {
                        addPath(mLinePath)
                        lineTo(mPoints.last().x, mBottomY)
                        lineTo(mPoints.first().x, mBottomY)
                        close()
                    }

                    drawPath(
                        path = mFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(chartColor.copy(alpha = 0.35f), chartColor.copy(alpha = 0.06f)),
                            startY = mTopY,
                            endY = mBottomY,
                        ),
                        style = Fill,
                    )

                    drawPath(
                        path = mLinePath,
                        color = chartColor,
                        style = Stroke(
                            width = if (isGoalCompleted) 3.8.dp.toPx() else 2.3.dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )

                    mPoints.forEach { mPoint ->
                        drawCircle(
                            color = chartColor.copy(alpha = 0.9f),
                            radius = 2.2.dp.toPx(),
                            center = mPoint,
                        )
                    }
                }
            }

            if (chartType != GoalChartType.LOADING_BAR) {
                drawLine(
                    color = mGoalLineColor,
                    start = Offset(mHorizontalPadding, mGoalLineY),
                    end = Offset(mWidth - mHorizontalPadding, mGoalLineY),
                    strokeWidth = 3.2.dp.toPx(),
                )

                drawLine(
                    color = axisColor.copy(alpha = 0.45f),
                    start = Offset(mHorizontalPadding, mBottomY),
                    end = Offset(mWidth - mHorizontalPadding, mBottomY),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
        }

        if (labels.isNotEmpty() && chartType != GoalChartType.LOADING_BAR) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                labels.forEach { mLabel ->
                    Text(
                        text = mLabel,
                        color = labelColor,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (valueLabels.isNotEmpty() && chartType != GoalChartType.LOADING_BAR) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                valueLabels.forEach { mValue ->
                    Text(
                        text = mValue,
                        color = labelColor.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    availableMetrics: List<GoalMetric>,
    onDismiss: () -> Unit,
    onGoalAdded: (metric: GoalMetric, current: Float, target: Float) -> Unit,
) {
    var mSelectedMetric by remember { mutableStateOf(availableMetrics.firstOrNull()) }
    var mTargetValue by remember {
        mutableStateOf(availableMetrics.firstOrNull()?.mSuggestedTarget.toInputValue())
    }
    var mSelectedDirection by remember { 
        mutableStateOf(availableMetrics.firstOrNull()?.mGoalDirection ?: GoalDirection.AT_LEAST) 
    }
    var mShowWarningDialog by remember { mutableStateOf(false) }

    val mCommonSenseDirections = remember {
        mapOf(
            GoalMetric.STEPS to GoalDirection.AT_LEAST,
            GoalMetric.WEIGHT to GoalDirection.AT_MOST,
            GoalMetric.RUNNING to GoalDirection.AT_LEAST,
            GoalMetric.WATER to GoalDirection.AT_LEAST,
            GoalMetric.CALORIES to GoalDirection.AT_MOST,
            GoalMetric.PROTEIN to GoalDirection.AT_LEAST,
            GoalMetric.CARBS to GoalDirection.AT_MOST,
            GoalMetric.FATS to GoalDirection.AT_MOST,
            GoalMetric.SUGARS to GoalDirection.AT_MOST,
            GoalMetric.FIBERS to GoalDirection.AT_LEAST,
            GoalMetric.EXERCISE_DAYS to GoalDirection.AT_LEAST,
        )
    }

    LaunchedEffect(mSelectedMetric) {
        mSelectedMetric?.let { mMetric ->
            mTargetValue = mMetric.mSuggestedTarget.toInputValue()
            mSelectedDirection = mMetric.mGoalDirection
        }
    }

    val mParsedTarget = mTargetValue.toFloatOrNull() ?: 0f
    val mExerciseTargetTooHigh = mSelectedMetric == GoalMetric.EXERCISE_DAYS && mParsedTarget > EXERCISE_GOAL_MAX_DAYS
    val mTarget = mParsedTarget
    val mCanAdd = mSelectedMetric != null && mTarget > 0f && !mExerciseTargetTooHigh

    if (mShowWarningDialog) {
        AlertDialog(
            onDismissRequest = { mShowWarningDialog = false },
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Tracking '${mSelectedMetric?.mLabel}' to stay '${if (mSelectedDirection == GoalDirection.AT_LEAST) "above" else "below"}' the target is unusual based on common health advice. Do you want to proceed?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        mShowWarningDialog = false
                        mSelectedMetric?.let { onGoalAdded(it, 0f, mTarget) }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { mShowWarningDialog = false }) {
                    Text("Change Direction")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "New Health Goal", 
                fontWeight = FontWeight.ExtraBold, 
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Metric Grid Selector
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableMetrics) { metric ->
                        val isSelected = mSelectedMetric == metric
                        Surface(
                            onClick = { mSelectedMetric = metric },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) metric.mAccentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.aspectRatio(1f),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    metric.mIcon, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isSelected) Color.White else metric.mAccentColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    metric.mLabel, 
                                    style = MaterialTheme.typography.labelMedium, 
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center, 
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Target Value",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = mTargetValue,
                        onValueChange = { newValue ->
                            mTargetValue = newValue.filter(Char::isDigit)
                        },
                        singleLine = true,
                        placeholder = { Text("Enter value") },
                        suffix = { mSelectedMetric?.let { Text(it.mUnit, fontWeight = FontWeight.Bold) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        isError = mExerciseTargetTooHigh,
                        supportingText = {
                            if (mExerciseTargetTooHigh) {
                                Text("Exercise goal cannot be greater than 7 days per week.")
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = mSelectedMetric?.mAccentColor ?: MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Goal Logic",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GoalDirectionButton(
                            label = "Reach Above",
                            icon = Icons.Default.TrendingUp,
                            isSelected = mSelectedDirection == GoalDirection.AT_LEAST,
                            accentColor = mSelectedMetric?.mAccentColor ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { mSelectedDirection = GoalDirection.AT_LEAST }
                        )
                        GoalDirectionButton(
                            label = "Stay Below",
                            icon = Icons.Default.Flag,
                            isSelected = mSelectedDirection == GoalDirection.AT_MOST,
                            accentColor = mSelectedMetric?.mAccentColor ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { mSelectedDirection = GoalDirection.AT_MOST }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    mSelectedMetric?.let { metric ->
                        val logicalDirection = mCommonSenseDirections[metric]
                        if (logicalDirection != null && logicalDirection != mSelectedDirection) {
                            mShowWarningDialog = true
                        } else {
                            onGoalAdded(metric, 0f, mTarget)
                        }
                    } 
                },
                enabled = mCanAdd,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mSelectedMetric?.mAccentColor ?: MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Start Tracking Goal", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun GoalDirectionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

fun GoalUiModel.progress(): Float {
    return mMetric.calculateProgress(
        currentValue = mCurrentValue,
        targetValue = mTargetValue,
    )
}

private fun GoalUiModel.formattedCurrentAndTarget(): String {
    return "${formatMetricValue(mCurrentValue, mMetric)} / ${formatMetricValue(mTargetValue, mMetric)}"
}

private fun GoalUiModel.formatCurrent(): String = formatMetricValue(mCurrentValue, mMetric)

private fun GoalUiModel.formatTarget(): String = formatMetricValue(mTargetValue, mMetric)

private fun GoalUiModel.historyValueLabels(): List<String> {
    return mHistoryValues.map { mRatio ->
        formatCompactMetricValue(mRatio * mTargetValue)
    }
}

private fun formatMetricValue(value: Float, metric: GoalMetric): String {
    val mFormattedValue = if (value % 1f == 0f) {
        String.format(Locale.US, "%,d", value.toInt())
    } else {
        String.format(Locale.US, "%,.1f", value)
    }
    return "$mFormattedValue ${metric.mUnit}"
}

private fun formatCompactMetricValue(value: Float): String {
    return when {
        value >= 1000f -> String.format(Locale.US, "%.1fk", value / 1000f)
        value % 1f == 0f -> String.format(Locale.US, "%d", value.toInt())
        else -> String.format(Locale.US, "%.1f", value)
    }
}

private fun GoalMetric.calculateProgress(
    currentValue: Float,
    targetValue: Float,
): Float {
    if (targetValue <= 0f) return 0f

    return when (mGoalDirection) {
        GoalDirection.AT_LEAST -> (currentValue / targetValue).coerceIn(0f, 1f)
        GoalDirection.AT_MOST -> (currentValue / targetValue).coerceIn(0f, 1f)
    }
}

private fun Float?.toInputValue(): String {
    if (this == null) return ""
    val mValue = this
    return if (mValue % 1f == 0f) {
        mValue.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", mValue)
    }
}

class GoalsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mTargetRepository = TargetRepository(application)
    private val mDailyGoalProgressRepository = DailyGoalProgressRepository(application)
    private val mNutritionFoodEntryRepository = NutritionFoodEntryRepository(application)
    private val mNutritionLogRepository = NutritionLogRepository(application)
    private val mExerciseLogRepository = ExerciseLogRepository(application)

    private val mGoalsState = MutableStateFlow<List<GoalUiModel>>(emptyList())
    val mGoals: StateFlow<List<GoalUiModel>> = mGoalsState.asStateFlow()
    private var mHasBackfilledRecentHistory = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now().toString()
            combine(
                mTargetRepository.observeTargets(),
                mDailyGoalProgressRepository.observeByDate(today),
                mNutritionFoodEntryRepository.observeByDate(today),
                mNutritionLogRepository.observeByDate(today),
                mExerciseLogRepository.observeByDate(today),
            ) { targets, todayProgress, foodEntries, nutritionLog, _ ->
                GoalSyncSnapshot(
                    mTargets = targets,
                    mTodayProgress = todayProgress,
                    mFoodEntries = foodEntries,
                    mNutritionLog = nutritionLog,
                )
            }.collect { snapshot ->
                if (!mHasBackfilledRecentHistory) {
                    backfillDerivedGoalProgressForRecentDays(snapshot.mTargets)
                    mHasBackfilledRecentHistory = true
                }

                syncDerivedGoalProgressForDate(
                    date = today,
                    targets = snapshot.mTargets,
                    existingProgressForDate = snapshot.mTodayProgress,
                    derivedProgress = deriveProgressFromDailySources(
                        date = today,
                        foodEntries = snapshot.mFoodEntries,
                        nutritionLog = snapshot.mNutritionLog,
                    ),
                )

                val progressMap = snapshot.mTodayProgress.associateBy { it.goalName }
                val uiModels = snapshot.mTargets.mapNotNull { target ->
                    val metric = GoalMetric.entries.firstOrNull { it.name == target.goalName } ?: return@mapNotNull null
                    val todayCurrentValue = progressMap[target.goalName]?.progressValue?.toFloat() ?: 0f
                    val historyDates = (HISTORY_POINT_COUNT - 1 downTo 0)
                        .map { offset -> LocalDate.now().minusDays(offset.toLong()).toString() }
                    val progressByDate = mDailyGoalProgressRepository
                        .getRecentByGoalName(target.goalName, HISTORY_LOOKBACK_COUNT)
                        .associateBy { it.date }
                    val recentProgress = historyDates.map { date ->
                        val currentValue = progressByDate[date]?.progressValue?.toFloat() ?: 0f
                        if (target.targetValue > 0.0) {
                            (currentValue / target.targetValue.toFloat()).coerceAtLeast(0f)
                        } else {
                            0f
                        }
                    }
                    GoalUiModel(
                        mId = metric.toGoalId(),
                        mMetric = metric,
                        mCurrentValue = todayCurrentValue,
                        mTargetValue = target.targetValue.toFloat(),
                        mHistoryValues = recentProgress,
                        mChartLabels = metric.mDefaultLabels,
                    )
                }
                mGoalsState.value = uiModels.sortedBy { it.mId }
            }
        }
    }

    fun addGoal(
        mMetric: GoalMetric,
        mCurrent: Float,
        mTarget: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (mMetric == GoalMetric.EXERCISE_DAYS && mTarget > EXERCISE_GOAL_MAX_DAYS) {
                return@launch
            }
            val mGoalName = mMetric.name
            val mTargetToSave = if (mMetric == GoalMetric.EXERCISE_DAYS) {
                mTarget.coerceIn(1f, EXERCISE_GOAL_MAX_DAYS)
            } else {
                mTarget
            }
            mTargetRepository.upsertTarget(
                goalName = mGoalName,
                targetValue = mTargetToSave.toDouble(),
            )
            mDailyGoalProgressRepository.setProgress(
                goalName = mGoalName,
                date = LocalDate.now().toString(),
                progressValue = mCurrent.toDouble(),
            )
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val goalName = GoalMetric.entries.firstOrNull { it.toGoalId() == id }?.name ?: return@launch
            mTargetRepository.deleteByGoalName(goalName)
        }
    }

    private suspend fun syncDerivedGoalProgressForDate(
        date: String,
        targets: List<com.example.seally.data.local.entity.TargetEntity>,
        existingProgressForDate: List<com.example.seally.data.local.entity.DailyGoalProgressEntity>,
        derivedProgress: Map<String, Double>,
    ) {
        if (derivedProgress.isEmpty()) return

        val currentProgress = existingProgressForDate.associateBy { it.goalName }
        for ((goalName, progressValue) in derivedProgress) {
            if (targets.none { it.goalName == goalName }) continue
            val existingValue = currentProgress[goalName]?.progressValue
            if (existingValue == null || kotlin.math.abs(existingValue - progressValue) > PROGRESS_EPSILON) {
                mDailyGoalProgressRepository.setProgressIfTargetExists(
                    goalName = goalName,
                    date = date,
                    progressValue = progressValue,
                )
            }
        }
    }

    private suspend fun backfillDerivedGoalProgressForRecentDays(
        targets: List<com.example.seally.data.local.entity.TargetEntity>,
    ) {
        val dates = (HISTORY_POINT_COUNT - 1 downTo 0)
            .map { offset -> LocalDate.now().minusDays(offset.toLong()).toString() }
        for (date in dates) {
            val foodEntries = mNutritionFoodEntryRepository.getByDate(date)
            val nutritionLog = mNutritionLogRepository.getByDate(date)
            val progressForDate = mDailyGoalProgressRepository.getByDate(date)
            syncDerivedGoalProgressForDate(
                date = date,
                targets = targets,
                existingProgressForDate = progressForDate,
                derivedProgress = deriveProgressFromDailySources(
                    date = date,
                    foodEntries = foodEntries,
                    nutritionLog = nutritionLog,
                ),
            )
        }
    }

    private suspend fun deriveProgressFromDailySources(
        date: String,
        foodEntries: List<NutritionFoodEntryEntity>,
        nutritionLog: NutritionLogEntity?,
    ): Map<String, Double> {
        val calories = foodEntries.sumOf { it.calories }.toDouble()
        val protein = foodEntries.sumOf { it.protein }.toDouble()
        val carbs = foodEntries.sumOf { it.carbs }.toDouble()
        val fats = foodEntries.sumOf { it.fats }.toDouble()
        val sugars = foodEntries.sumOf { it.sugars }.toDouble()
        val fibers = foodEntries.sumOf { it.fibers }.toDouble()
        val water = (nutritionLog?.waterMl ?: 0).coerceAtLeast(0).toDouble()
        val exerciseDaysInWeek = countExerciseDaysInWindow(endDate = date).toDouble()

        return mapOf(
            GoalMetric.WATER.name to water,
            GoalMetric.CALORIES.name to calories,
            GoalMetric.PROTEIN.name to protein,
            GoalMetric.CARBS.name to carbs,
            GoalMetric.FATS.name to fats,
            GoalMetric.SUGARS.name to sugars,
            GoalMetric.FIBERS.name to fibers,
            GoalMetric.EXERCISE_DAYS.name to exerciseDaysInWeek,
        )
    }

    private suspend fun countExerciseDaysInWindow(endDate: String): Int {
        val mEndDate = LocalDate.parse(endDate)
        val mStartDate = mEndDate.minusDays((HISTORY_POINT_COUNT - 1).toLong()).toString()
        return mExerciseLogRepository
            .getLoggedDatesBetween(startDate = mStartDate, endDate = endDate)
            .size
    }

    private data class GoalSyncSnapshot(
        val mTargets: List<com.example.seally.data.local.entity.TargetEntity>,
        val mTodayProgress: List<com.example.seally.data.local.entity.DailyGoalProgressEntity>,
        val mFoodEntries: List<NutritionFoodEntryEntity>,
        val mNutritionLog: NutritionLogEntity?,
    )

    companion object {
        private const val HISTORY_POINT_COUNT = 7
        private const val HISTORY_LOOKBACK_COUNT = 30
        private const val PROGRESS_EPSILON = 0.0001

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val mApplication = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return GoalsViewModel(mApplication) as T
            }
        }
    }
}

private fun GoalMetric.toGoalId(): Long = ordinal.toLong() + 1L
