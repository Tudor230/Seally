package com.example.seally.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seally.camera.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbbellWorkoutsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onExerciseSelected: (ExerciseType) -> Unit = {},
) {
    var mSelectedExerciseIndex by rememberSaveable { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Form Correctors", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pick an exercise to start form checking.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = mSelectedExerciseIndex == 0,
                    onClick = {
                        mSelectedExerciseIndex = 0
                        onExerciseSelected(ExerciseType.SQUAT)
                    },
                    title = "Squat",
                    details = "Side-view squat depth and form checker",
                    duration = "Form check",
                )
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = mSelectedExerciseIndex == 1,
                    onClick = {
                        mSelectedExerciseIndex = 1
                        onExerciseSelected(ExerciseType.PLANK)
                    },
                    title = "Plank",
                    details = "Core alignment and hold feedback checker",
                    duration = "Form check",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = mSelectedExerciseIndex == 2,
                    onClick = {
                        mSelectedExerciseIndex = 2
                        onExerciseSelected(ExerciseType.PULLUP)
                    },
                    title = "Pull-up",
                    details = "Dead hang to top-position feedback checker",
                    duration = "Form check",
                )
            }
        }
    }
}

@Composable
private fun WorkoutPlanCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClick: () -> Unit,
    title: String,
    details: String,
    duration: String,
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(PaddingValues(14.dp)),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
