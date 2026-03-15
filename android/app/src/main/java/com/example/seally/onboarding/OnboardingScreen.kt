package com.example.seally.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.profile.ProfileViewModel
import com.example.seally.profile.UserProfile

private enum class OnboardingStep {
    PROFILE,
    ACTIVITY,
    JOURNEY,
    SUMMARY,
}

private val mActivityTypePlaceholders = listOf(
    "Activity Option 1",
    "Activity Option 2",
    "Activity Option 3",
    "Activity Option 4",
)

private val mJourneyOptions = listOf(
    "Lose weight",
    "Gain weight",
    "Maintain weight",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit = {},
) {
    val mContext = LocalContext.current
    val mViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(mContext))
    val mProfile by mViewModel.profile.collectAsState()

    var mStep by rememberSaveable { mutableStateOf(OnboardingStep.PROFILE) }
    var mName by rememberSaveable { mutableStateOf("") }
    var mWeightKg by rememberSaveable { mutableStateOf("") }
    var mHeightCm by rememberSaveable { mutableStateOf("") }
    var mDesiredWeightKg by rememberSaveable { mutableStateOf("") }
    var mActivityType by rememberSaveable { mutableStateOf("") }
    var mWorkoutDays by rememberSaveable { mutableIntStateOf(3) }
    var mJourneyGoal by rememberSaveable { mutableStateOf(mJourneyOptions.first()) }
    var mWaterTargetMl by rememberSaveable { mutableStateOf("2500") }

    LaunchedEffect(mProfile) {
        mName = mProfile.name
        mWeightKg = mProfile.weightKg?.toCleanInput().orEmpty()
        mHeightCm = mProfile.heightCm?.toString().orEmpty()
        mDesiredWeightKg = mProfile.goalWeightKg?.toCleanInput().orEmpty()
        if (mProfile.activityType.isNotBlank()) mActivityType = mProfile.activityType
        mProfile.workoutDaysPerWeek?.let { mWorkoutDays = it.coerceIn(1, 7) }
        if (mProfile.journeyGoal.isNotBlank()) mJourneyGoal = mProfile.journeyGoal
        mWaterTargetMl = mProfile.waterTargetMl?.toString() ?: "2500"
    }

    val mWeightValue = mWeightKg.toFloatOrNull()
    val mHeightValue = mHeightCm.toIntOrNull()
    val mDesiredWeightValue = mDesiredWeightKg.toFloatOrNull()
    val mWaterTargetValue = mWaterTargetMl.toIntOrNull()

    val mCanMoveFromProfile = mName.isNotBlank() &&
        mWeightValue != null &&
        mHeightValue != null &&
        mDesiredWeightValue != null
    val mCanMoveFromActivity = mActivityType.isNotBlank()
    val mCanMoveFromJourney = mWaterTargetValue != null && mWaterTargetValue > 0

    Scaffold(modifier = modifier.fillMaxSize()) { mInnerPadding ->
        Column(
            modifier = Modifier
                .padding(mInnerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Welcome to Seally",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when (mStep) {
                    OnboardingStep.PROFILE -> "Tell us a bit about you."
                    OnboardingStep.ACTIVITY -> "Choose your app activity type."
                    OnboardingStep.JOURNEY -> "Define your weekly journey."
                    OnboardingStep.SUMMARY -> "Here is your starter summary."
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (mStep) {
                        OnboardingStep.PROFILE -> {
                            OutlinedTextField(
                                value = mName,
                                onValueChange = { mName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = mWeightKg,
                                onValueChange = { mWeightKg = it.filterDecimal(6) },
                                label = { Text("Current weight (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = mHeightCm,
                                onValueChange = { mHeightCm = it.filterNumeric(3) },
                                label = { Text("Height (cm)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = mDesiredWeightKg,
                                onValueChange = { mDesiredWeightKg = it.filterDecimal(6) },
                                label = { Text("Desired weight (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        OnboardingStep.ACTIVITY -> {
                            Text(
                                text = "Select one option (placeholder list):",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                mActivityTypePlaceholders.forEach { mOption ->
                                    FilterChip(
                                        selected = mActivityType == mOption,
                                        onClick = { mActivityType = mOption },
                                        label = { Text(mOption) },
                                    )
                                }
                            }
                        }

                        OnboardingStep.JOURNEY -> {
                            Text(
                                text = "Workout days per week: $mWorkoutDays",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                (1..7).forEach { mDayCount ->
                                    FilterChip(
                                        selected = mWorkoutDays == mDayCount,
                                        onClick = { mWorkoutDays = mDayCount },
                                        label = { Text(mDayCount.toString()) },
                                    )
                                }
                            }

                            Text(
                                text = "Journey goal",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                mJourneyOptions.forEach { mOption ->
                                    FilterChip(
                                        selected = mJourneyGoal == mOption,
                                        onClick = { mJourneyGoal = mOption },
                                        label = { Text(mOption) },
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = mWaterTargetMl,
                                onValueChange = { mWaterTargetMl = it.filterNumeric(5) },
                                label = { Text("Daily water target (ml)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        OnboardingStep.SUMMARY -> {
                            val mMaintenanceCalories = calculateMaintenanceCalories(
                                mWeightKg = mWeightValue ?: 0f,
                                mWorkoutDays = mWorkoutDays,
                            )
                            val mRecommendedCalories = when (mJourneyGoal) {
                                "Lose weight" -> mMaintenanceCalories - 500
                                "Gain weight" -> mMaintenanceCalories + 300
                                else -> mMaintenanceCalories
                            }

                            Text("Name: $mName")
                            Text("Current / Desired: ${mWeightKg}kg -> ${mDesiredWeightKg}kg")
                            Text("Height: ${mHeightCm}cm")
                            Text("Activity type: $mActivityType")
                            Text("Workouts: $mWorkoutDays days/week")
                            Text("Journey goal: $mJourneyGoal")
                            Text("Water target: ${mWaterTargetMl} ml/day")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Starter recommendation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Estimated maintenance: $mMaintenanceCalories kcal/day")
                            Text("Recommended intake: $mRecommendedCalories kcal/day")
                            Text(
                                text = "Formula: maintenance = weight * activityMultiplier, then journey adjustment.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = {
                        mStep = when (mStep) {
                            OnboardingStep.PROFILE -> OnboardingStep.PROFILE
                            OnboardingStep.ACTIVITY -> OnboardingStep.PROFILE
                            OnboardingStep.JOURNEY -> OnboardingStep.ACTIVITY
                            OnboardingStep.SUMMARY -> OnboardingStep.JOURNEY
                        }
                    },
                    enabled = mStep != OnboardingStep.PROFILE,
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        when (mStep) {
                            OnboardingStep.PROFILE -> mStep = OnboardingStep.ACTIVITY
                            OnboardingStep.ACTIVITY -> mStep = OnboardingStep.JOURNEY
                            OnboardingStep.JOURNEY -> mStep = OnboardingStep.SUMMARY
                            OnboardingStep.SUMMARY -> {
                                mViewModel.save(
                                    UserProfile(
                                        name = mName.trim(),
                                        heightCm = mHeightValue,
                                        weightKg = mWeightValue,
                                        goalWeightKg = mDesiredWeightValue,
                                        activityType = mActivityType,
                                        workoutDaysPerWeek = mWorkoutDays,
                                        journeyGoal = mJourneyGoal,
                                        waterTargetMl = mWaterTargetValue,
                                        onboardingCompleted = true,
                                    ),
                                )
                                onCompleted()
                            }
                        }
                    },
                    enabled = when (mStep) {
                        OnboardingStep.PROFILE -> mCanMoveFromProfile
                        OnboardingStep.ACTIVITY -> mCanMoveFromActivity
                        OnboardingStep.JOURNEY -> mCanMoveFromJourney
                        OnboardingStep.SUMMARY -> true
                    },
                ) {
                    Text(if (mStep == OnboardingStep.SUMMARY) "Start journey" else "Next")
                }
            }
        }
    }
}

private fun calculateMaintenanceCalories(
    mWeightKg: Float,
    mWorkoutDays: Int,
): Int {
    if (mWeightKg <= 0f) return 0
    val mActivityMultiplier = when (mWorkoutDays.coerceIn(1, 7)) {
        1 -> 28f
        2 -> 30f
        3 -> 32f
        4 -> 34f
        5 -> 35f
        6 -> 36f
        else -> 37f
    }
    return (mWeightKg * mActivityMultiplier).toInt()
}

private fun String.filterNumeric(maxLen: Int): String {
    val mFiltered = filter { it.isDigit() }
    return if (mFiltered.length <= maxLen) mFiltered else mFiltered.take(maxLen)
}

private fun String.filterDecimal(maxLen: Int): String {
    val mFiltered = buildString {
        var mDotUsed = false
        for (mChar in this@filterDecimal) {
            if (mChar.isDigit()) {
                append(mChar)
            } else if (mChar == '.' && !mDotUsed) {
                mDotUsed = true
                append(mChar)
            }
        }
    }
    return if (mFiltered.length <= maxLen) mFiltered else mFiltered.take(maxLen)
}

private fun Float.toCleanInput(): String {
    val mText = toString()
    return if (mText.contains(".")) mText.trimEnd('0').trimEnd('.') else mText
}
