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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbbellWorkoutsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    var selectedPlanIndex by rememberSaveable { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Workouts", fontWeight = FontWeight.SemiBold) },
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
                text = "Pick a workout to get started.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = selectedPlanIndex == 0,
                    onClick = { selectedPlanIndex = 0 },
                    title = "Full Body Starter",
                    details = "Goblet squat, dumbbell row, shoulder press",
                    duration = "20 min",
                )
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = selectedPlanIndex == 1,
                    onClick = { selectedPlanIndex = 1 },
                    title = "Upper Body Focus",
                    details = "Chest press, curls, triceps extension",
                    duration = "25 min",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = selectedPlanIndex == 2,
                    onClick = { selectedPlanIndex = 2 },
                    title = "Lower Body Builder",
                    details = "Romanian deadlift, lunges, calf raises",
                    duration = "25 min",
                )
                WorkoutPlanCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    isSelected = selectedPlanIndex == 3,
                    onClick = { selectedPlanIndex = 3 },
                    title = "Core & Conditioning",
                    details = "Russian twists, farmer carry, plank rows",
                    duration = "18 min",
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
