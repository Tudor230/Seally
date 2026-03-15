package com.example.seally.goals

import android.app.Application
import android.content.Context
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
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
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.TopHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

enum class GoalChartType {
    LINE,
    BAR,
}

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
    val mSuggestedCurrent: Float,
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
        mSuggestedCurrent = 8_000f,
        mSuggestedTarget = 10_000f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.TrendingUp
    ),
    WEIGHT(
        mLabel = "Weight",
        mUnit = "lbs",
        mAccentColor = Color(0xFFE39A55),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedCurrent = 170f,
        mSuggestedTarget = 150f,
        mDefaultLabels = emptyList(),
        mIcon = Icons.Default.Flag
    ),
    RUNNING(
        mLabel = "Running",
        mUnit = "km",
        mAccentColor = Color(0xFFB17AE0),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_LEAST,
        mSuggestedCurrent = 30f,
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
        mSuggestedCurrent = 1_800f,
        mSuggestedTarget = 2_500f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.TrendingUp
    ),
    CALORIES(
        mLabel = "Calories",
        mUnit = "kcal",
        mAccentColor = Color(0xFFDE7C64),
        mChartType = GoalChartType.BAR,
        mGoalDirection = GoalDirection.AT_MOST,
        mSuggestedCurrent = 2_300f,
        mSuggestedTarget = 2_000f,
        mDefaultLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        mIcon = Icons.Default.Flag
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
            TopHeader(onProfileClick = onProfileClick)

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
                chartType = goal.mMetric.mChartType,
                chartColor = mColor,
                gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                axisColor = MaterialTheme.colorScheme.outlineVariant,
                isGoalCompleted = mIsGoalCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
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
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Close Details")
            }
        },
        dismissButton = {
            IconButton(onClick = { mShowDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
            val mMaxValue = max(1f, values.maxOrNull() ?: 1f)
            val mStepX = if (values.size > 1) mChartWidth / (values.size - 1) else 0f

            listOf(0.25f, 0.5f, 0.75f).forEach { mFraction ->
                val mY = mTopY + (mBottomY - mTopY) * mFraction
                val mDashWidth = 6.dp.toPx()
                val mGap = 5.dp.toPx()
                var mX = mHorizontalPadding
                while (mX < mWidth - mHorizontalPadding) {
                    drawLine(
                        color = gridColor,
                        start = Offset(mX, mY),
                        end = Offset((mX + mDashWidth).coerceAtMost(mWidth - mHorizontalPadding), mY),
                        strokeWidth = 1.dp.toPx(),
                    )
                    mX += mDashWidth + mGap
                }
            }

            if (chartType == GoalChartType.BAR) {
                val mBarWidth = (mChartWidth / values.size) * if (isGoalCompleted) 0.74f else 0.58f
                values.forEachIndexed { mIndex, mValue ->
                    val mNormalized = (mValue / mMaxValue).coerceIn(0f, 1f)
                    val mBarHeight = (mBottomY - mTopY) * mNormalized
                    val mLeft = mHorizontalPadding + (mIndex * (mChartWidth / values.size)) + 3.dp.toPx()
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
                    val mNormalized = (mValue / mMaxValue).coerceIn(0f, 1f)
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

            drawLine(
                color = axisColor,
                start = Offset(mHorizontalPadding, mBottomY),
                end = Offset(mWidth - mHorizontalPadding, mBottomY),
                strokeWidth = 1.4.dp.toPx(),
            )
        }

        if (labels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
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
    }
}

@Composable
private fun AddGoalDialog(
    availableMetrics: List<GoalMetric>,
    onDismiss: () -> Unit,
    onGoalAdded: (metric: GoalMetric, current: Float, target: Float) -> Unit,
) {
    var mSelectedMetric by remember { mutableStateOf(availableMetrics.firstOrNull()) }
    var mCurrentValue by remember {
        mutableStateOf(availableMetrics.firstOrNull()?.mSuggestedCurrent.toInputValue())
    }
    var mTargetValue by remember {
        mutableStateOf(availableMetrics.firstOrNull()?.mSuggestedTarget.toInputValue())
    }

    LaunchedEffect(mSelectedMetric) {
        mSelectedMetric?.let { mMetric ->
            mCurrentValue = mMetric.mSuggestedCurrent.toInputValue()
            mTargetValue = mMetric.mSuggestedTarget.toInputValue()
        }
    }

    val mCurrent = mCurrentValue.toFloatOrNull() ?: 0f
    val mTarget = mTargetValue.toFloatOrNull() ?: 0f
    val mCanAdd = mSelectedMetric != null && mTarget > 0f && mCurrent >= 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Place New Goal", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Metric Grid Selector
                Text(
                    text = "SELECT METRIC",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMetrics) { metric ->
                        val isSelected = mSelectedMetric == metric
                        Surface(
                            onClick = { mSelectedMetric = metric },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) metric.mAccentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.aspectRatio(1f),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(metric.mIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(metric.mLabel, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
                            }
                        }
                    }
                }

                mSelectedMetric?.let { mMetric ->
                    val mDirectionText = if (mMetric.mGoalDirection == GoalDirection.AT_LEAST) {
                        "Set a target of at least ${mMetric.mUnit}"
                    } else {
                        "Stay below a target of ${mMetric.mUnit}"
                    }
                    Text(
                        text = mDirectionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = mCurrentValue,
                    onValueChange = { mCurrentValue = it },
                    singleLine = true,
                    label = { Text("Starting Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )

                OutlinedTextField(
                    value = mTargetValue,
                    onValueChange = { mTargetValue = it },
                    singleLine = true,
                    label = { Text("Goal Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { mSelectedMetric?.let { onGoalAdded(it, mCurrent, mTarget) } },
                enabled = mCanAdd,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Tracking Goal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(32.dp)
    )
}

private fun buildDefaultGoals(): List<GoalUiModel> = listOf(
    GoalUiModel(
        mId = 1L,
        mMetric = GoalMetric.STEPS,
        mCurrentValue = 7_200f,
        mTargetValue = 10_000f,
        mHistoryValues = listOf(0.18f, 0.25f, 0.43f, 0.39f, 0.57f, 0.44f, 0.53f),
        mChartLabels = GoalMetric.STEPS.mDefaultLabels,
    ),
    GoalUiModel(
        mId = 3L,
        mMetric = GoalMetric.WATER,
        mCurrentValue = 1_700f,
        mTargetValue = 2_500f,
        mHistoryValues = listOf(0.35f, 0.42f, 0.48f, 0.55f, 0.62f, 0.64f, 0.68f),
        mChartLabels = GoalMetric.WATER.mDefaultLabels,
    ),
)

private fun buildTrendValues(progress: Float): List<Float> {
    val mEnd = progress.coerceIn(0.15f, 1f)
    return listOf(
        (mEnd * 0.35f).coerceAtLeast(0.1f),
        (mEnd * 0.42f).coerceAtLeast(0.13f),
        (mEnd * 0.58f).coerceAtLeast(0.16f),
        (mEnd * 0.5f).coerceAtLeast(0.14f),
        (mEnd * 0.72f).coerceAtLeast(0.2f),
        (mEnd * 0.64f).coerceAtLeast(0.18f),
        mEnd,
    )
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

private fun formatMetricValue(value: Float, metric: GoalMetric): String {
    val mFormattedValue = if (value % 1f == 0f) {
        String.format(Locale.US, "%,d", value.toInt())
    } else {
        String.format(Locale.US, "%,.1f", value)
    }
    return "$mFormattedValue ${metric.mUnit}"
}

private fun GoalMetric.calculateProgress(
    currentValue: Float,
    targetValue: Float,
): Float {
    if (targetValue <= 0f) return 0f

    return when (mGoalDirection) {
        GoalDirection.AT_LEAST -> (currentValue / targetValue).coerceIn(0f, 1f)
        GoalDirection.AT_MOST -> {
            if (currentValue <= 0f) {
                1f
            } else {
                (targetValue / currentValue).coerceIn(0f, 1f)
            }
        }
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

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val mId: Long,
    val mMetricKey: String,
    val mCurrentValue: Float,
    val mTargetValue: Float,
    val mHistoryValues: String,
    val mChartLabels: String,
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY mId ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY mId ASC")
    suspend fun getAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE mId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)
}

@Database(
    entities = [GoalEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class GoalsDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var mInstance: GoalsDatabase? = null

        fun getInstance(context: Context): GoalsDatabase {
            return mInstance ?: synchronized(this) {
                mInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GoalsDatabase::class.java,
                    "seally_goals.db",
                ).build().also { mInstance = it }
            }
        }
    }
}

private class GoalsRepository(
    private val mGoalDao: GoalDao,
) {
    suspend fun getGoals(): List<GoalUiModel> {
        return mGoalDao.getAll().mapNotNull { mEntity ->
            val mMetric = GoalMetric.entries.firstOrNull { it.name == mEntity.mMetricKey }
                ?: return@mapNotNull null
            GoalUiModel(
                mId = mEntity.mId,
                mMetric = mMetric,
                mCurrentValue = mEntity.mCurrentValue,
                mTargetValue = mEntity.mTargetValue,
                mHistoryValues = decodeFloatList(mEntity.mHistoryValues),
                mChartLabels = decodeStringList(mEntity.mChartLabels),
            )
        }
    }

    fun observeGoals(): Flow<List<GoalUiModel>> {
        return mGoalDao.observeAll().map { entities ->
            entities.mapNotNull { mEntity ->
                val mMetric = GoalMetric.entries.firstOrNull { it.name == mEntity.mMetricKey }
                    ?: return@mapNotNull null
                GoalUiModel(
                    mId = mEntity.mId,
                    mMetric = mMetric,
                    mCurrentValue = mEntity.mCurrentValue,
                    mTargetValue = mEntity.mTargetValue,
                    mHistoryValues = decodeFloatList(mEntity.mHistoryValues),
                    mChartLabels = decodeStringList(mEntity.mChartLabels),
                )
            }
        }
    }

    suspend fun upsertGoal(goal: GoalUiModel) {
        mGoalDao.upsert(goal.toEntity())
    }

    suspend fun deleteGoal(id: Long) {
        mGoalDao.deleteById(id)
    }

    suspend fun replaceAllGoals(goals: List<GoalUiModel>) {
        mGoalDao.deleteAll()
        mGoalDao.insertAll(goals.map { it.toEntity() })
    }

    private fun GoalUiModel.toEntity(): GoalEntity {
        return GoalEntity(
            mId = mId,
            mMetricKey = mMetric.name,
            mCurrentValue = mCurrentValue,
            mTargetValue = mTargetValue,
            mHistoryValues = encodeFloatList(mHistoryValues),
            mChartLabels = encodeStringList(mChartLabels),
        )
    }

    private fun encodeFloatList(values: List<Float>): String = values.joinToString(",")

    private fun decodeFloatList(encoded: String): List<Float> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(",").mapNotNull { it.toFloatOrNull() }
    }

    private fun encodeStringList(values: List<String>): String = values.joinToString("|")

    private fun decodeStringList(encoded: String): List<String> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("|")
    }
}

class GoalsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mRepository = GoalsRepository(
        mGoalDao = GoalsDatabase.getInstance(application).goalDao(),
    )

    private val mGoalsState = MutableStateFlow<List<GoalUiModel>>(emptyList())
    val mGoals: StateFlow<List<GoalUiModel>> = mGoalsState.asStateFlow()

    init {
        // 1) Ensure defaults exist
        viewModelScope.launch(Dispatchers.IO) {
            val mStoredGoals = mRepository.getGoals()
            if (mStoredGoals.isEmpty()) {
                mRepository.replaceAllGoals(buildDefaultGoals())
            }
        }

        // 2) Always observe the DB so UI refreshes instantly on any change
        viewModelScope.launch {
            mRepository.observeGoals().collect { mGoalsFromDb ->
                mGoalsState.value = mGoalsFromDb
            }
        }
    }

    fun addGoal(
        mMetric: GoalMetric,
        mCurrent: Float,
        mTarget: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val mCurrentGoals = mGoalsState.value
            val mNextId = (mCurrentGoals.maxOfOrNull { it.mId } ?: 0L) + 1L
            val mProgress = mMetric.calculateProgress(mCurrent, mTarget)
            val mNewGoal = GoalUiModel(
                mId = mNextId,
                mMetric = mMetric,
                mCurrentValue = mCurrent,
                mTargetValue = mTarget,
                mHistoryValues = buildTrendValues(mProgress),
                mChartLabels = mMetric.mDefaultLabels,
            )
            mRepository.upsertGoal(mNewGoal)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mRepository.deleteGoal(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val mApplication = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return GoalsViewModel(mApplication) as T
            }
        }
    }
}
