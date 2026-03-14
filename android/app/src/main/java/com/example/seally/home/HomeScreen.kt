package com.example.seally.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seally.goals.GoalUiModel
import com.example.seally.goals.GoalsViewModel
import com.example.seally.goals.progress
import com.example.seally.ui.components.TopHeader
import kotlinx.coroutines.delay

private enum class HomeMetricSlot {
    Left,
    Right,
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val goalsViewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory)
    val trackedGoals by goalsViewModel.mGoals.collectAsState()

    var leftGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var rightGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectingSlot by remember { mutableStateOf<HomeMetricSlot?>(null) }

    val motivationalMessages = remember {
        listOf(
            "Small steps every day add up to big changes.",
            "Consistency beats intensity. Keep going!",
            "You are building momentum. Don’t break the streak.",
            "Progress is progress, no matter how small.",
            "Show up today. Future you will thank you.",
        )
    }
    var messageIndex by rememberSaveable { mutableIntStateOf(0) }
    var shouldShowMessage by rememberSaveable { mutableStateOf(true) }

    val leftGoal = trackedGoals.firstOrNull { it.mId == leftGoalId }
    val rightGoal = trackedGoals.firstOrNull { it.mId == rightGoalId }

    LaunchedEffect(trackedGoals) {
        if (leftGoalId != null && leftGoal == null) {
            leftGoalId = null
        }
        if (rightGoalId != null && rightGoal == null) {
            rightGoalId = null
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            shouldShowMessage = true
            delay(4_000)
            shouldShowMessage = false
            delay(8_000)
            messageIndex = (messageIndex + 1) % motivationalMessages.size
        }
    }

    val musclesImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/seals/muscles.png")
        .build()

    val backgroundRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/backgrounds/homepage.png")
        .build()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AsyncImage(
            model = backgroundRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.7f,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopHeader(
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick,
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeGoalGaugeSlot(
                goal = leftGoal,
                onClick = { selectingSlot = HomeMetricSlot.Left },
            )
            HomeGoalGaugeSlot(
                goal = rightGoal,
                onClick = { selectingSlot = HomeMetricSlot.Right },
            )
        }

        AsyncImage(
            model = musclesImageRequest,
            contentDescription = "Seal Character",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.75f)
                .padding(bottom = 20.dp),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 52.dp)
                .alpha(if (shouldShowMessage) 1f else 0f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            tonalElevation = 1.dp,
        ) {
            Text(
                text = motivationalMessages[messageIndex],
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }

    selectingSlot?.let { slot ->
        GoalPickerDialog(
            goals = trackedGoals,
            onDismiss = { selectingSlot = null },
            onGoalSelected = { goal ->
                when (slot) {
                    HomeMetricSlot.Left -> leftGoalId = goal.mId
                    HomeMetricSlot.Right -> rightGoalId = goal.mId
                }
                selectingSlot = null
            },
            onClearSelection = {
                when (slot) {
                    HomeMetricSlot.Left -> leftGoalId = null
                    HomeMetricSlot.Right -> rightGoalId = null
                }
                selectingSlot = null
            },
        )
    }
}

@Composable
private fun HomeGoalGaugeSlot(
    goal: GoalUiModel?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(68.4.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (goal == null) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Select metric",
                    modifier = Modifier.size(28.5.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                val progress = goal.progress()
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(57.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = goal.mMetric.mLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalPickerDialog(
    goals: List<GoalUiModel>,
    onDismiss: () -> Unit,
    onGoalSelected: (GoalUiModel) -> Unit,
    onClearSelection: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select metric to track") },
        text = {
            if (goals.isEmpty()) {
                Text("No goals available yet. Add goals from the Goals page first.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    goals.forEach { goal ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGoalSelected(goal) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = goal.mMetric.mLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${goal.mCurrentValue.toInt()} / ${goal.mTargetValue.toInt()} ${goal.mMetric.mUnit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onClearSelection) {
                Text("Clear")
            }
        },
        shape = RoundedCornerShape(24.dp),
    )
}
