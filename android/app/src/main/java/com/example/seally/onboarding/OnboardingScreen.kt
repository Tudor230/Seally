package com.example.seally.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.profile.ProfileViewModel
import com.example.seally.profile.UserProfile
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.LinearProgressBar

private enum class OnboardingStep {
    WELCOME,
    PROFILE,
    ACTIVITY,
    JOURNEY,
    SUMMARY,
}

private data class ActivityLevel(
    val title: String,
    val description: String,
    val multiplier: Float
)

private val mActivityLevels = listOf(
    ActivityLevel("Sedentary", "Little to no exercise, desk job", 28f),
    ActivityLevel("Lightly Active", "Light exercise 1-3 days/week", 30f),
    ActivityLevel("Moderately Active", "Moderate exercise 3-5 days/week", 32f),
    ActivityLevel("Very Active", "Hard exercise 6-7 days/week", 35f),
    ActivityLevel("Extra Active", "Very hard exercise, physical job", 37f)
)

private val mJourneyOptions = listOf(
    "Lose weight",
    "Gain weight",
    "Maintain weight",
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit = {},
) {
    val mContext = LocalContext.current
    val mViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(mContext))
    val mProfileState by mViewModel.profile.collectAsState()

    var mStep by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var mName by rememberSaveable { mutableStateOf("") }
    var mWeightKg by rememberSaveable { mutableStateOf("") }
    var mHeightCm by rememberSaveable { mutableStateOf("") }
    var mDesiredWeightKg by rememberSaveable { mutableStateOf("") }
    var mActivityType by rememberSaveable { mutableStateOf(mActivityLevels[1].title) }
    var mWorkoutDays by rememberSaveable { mutableIntStateOf(3) }
    var mJourneyGoal by rememberSaveable { mutableStateOf(mJourneyOptions.first()) }
    var mWaterTargetMl by rememberSaveable { mutableStateOf("2500") }

    // Sync from profile when it first loads
    LaunchedEffect(mProfileState) {
        mProfileState?.let { p ->
            if (mName.isEmpty()) mName = p.name
            if (mWeightKg.isEmpty()) mWeightKg = p.weightKg?.toCleanInput().orEmpty()
            if (mHeightCm.isEmpty()) mHeightCm = p.heightCm?.toString().orEmpty()
            if (mDesiredWeightKg.isEmpty()) mDesiredWeightKg = p.goalWeightKg?.toCleanInput().orEmpty()
            if (p.activityType.isNotBlank()) mActivityType = p.activityType
            p.workoutDaysPerWeek?.let { mWorkoutDays = it.coerceIn(1, 7) }
            if (p.journeyGoal.isNotBlank()) mJourneyGoal = p.journeyGoal
            mWaterTargetMl = p.waterTargetMl?.toString() ?: "2500"
        }
    }

    val mWeightValue = mWeightKg.toFloatOrNull()
    val mHeightValue = mHeightCm.toIntOrNull()
    val mDesiredWeightValue = mDesiredWeightKg.toFloatOrNull()
    val mWaterTargetValue = mWaterTargetMl.toIntOrNull()

    val mCanMoveFromProfile = mName.isNotBlank() &&
            mWeightValue != null && mWeightValue > 0 &&
            mHeightValue != null && mHeightValue > 0 &&
            mDesiredWeightValue != null && mDesiredWeightValue > 0

    val mProgress = when (mStep) {
        OnboardingStep.WELCOME -> 0.1f
        OnboardingStep.PROFILE -> 0.3f
        OnboardingStep.ACTIVITY -> 0.5f
        OnboardingStep.JOURNEY -> 0.7f
        OnboardingStep.SUMMARY -> 0.9f
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppScreenBackground(assetPath = "backgrounds/homepage.png", overlayTransparency = 0.8f)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (mStep != OnboardingStep.WELCOME) {
                    CenterAlignedTopAppBar(
                        title = {
                            LinearProgressBar(
                                progress = mProgress,
                                modifier = Modifier.width(200.dp),
                                filledColor = Color(0xFF00E5FF),
                                trackColor = Color.White.copy(alpha = 0.2f),
                                height = 8.dp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                mStep = when (mStep) {
                                    OnboardingStep.PROFILE -> OnboardingStep.WELCOME
                                    OnboardingStep.ACTIVITY -> OnboardingStep.PROFILE
                                    OnboardingStep.JOURNEY -> OnboardingStep.ACTIVITY
                                    OnboardingStep.SUMMARY -> OnboardingStep.JOURNEY
                                    else -> mStep
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = mStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } + fadeIn() with slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() with slideOutHorizontally { it } + fadeOut()
                    }
                },
                modifier = Modifier.padding(padding).fillMaxSize(),
                label = "OnboardingStepTransition"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep { mStep = OnboardingStep.PROFILE }
                        OnboardingStep.PROFILE -> ProfileStep(
                            mName, { mName = it },
                            mWeightKg, { mWeightKg = it.filterDecimal(6) },
                            mHeightCm, { mHeightCm = it.filterNumeric(3) },
                            mDesiredWeightKg, { mDesiredWeightKg = it.filterDecimal(6) },
                            mCanMoveFromProfile
                        ) { mStep = OnboardingStep.ACTIVITY }

                        OnboardingStep.ACTIVITY -> ActivityStep(
                            mActivityType, { mActivityType = it }
                        ) { mStep = OnboardingStep.JOURNEY }

                        OnboardingStep.JOURNEY -> JourneyStep(
                            mWorkoutDays, { mWorkoutDays = it },
                            mJourneyGoal, { mJourneyGoal = it },
                            mWaterTargetMl, { mWaterTargetMl = it.filterNumeric(5) },
                            mWaterTargetValue != null && mWaterTargetValue > 0
                        ) { mStep = OnboardingStep.SUMMARY }

                        OnboardingStep.SUMMARY -> SummaryStep(
                            mName, mWeightKg, mDesiredWeightKg, mHeightCm, mActivityType, mWorkoutDays, mJourneyGoal, mWaterTargetMl,
                            onFinish = {
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🦭", fontSize = 60.sp)
            }
        }
        
        Text(
            text = "Welcome to Seally",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Your personalized AI fitness companion. Let's get you set up for success.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(Modifier.height(40.dp))
        
        OnboardingButton(text = "GET STARTED", onClick = onNext)
    }
}

@Composable
private fun ProfileStep(
    name: String, onNameChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    goalWeight: String, onGoalWeightChange: (String) -> Unit,
    canMove: Boolean,
    onNext: () -> Unit
) {
    OnboardingCard(title = "Tell us about yourself") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OnboardingTextField(value = name, onValueChange = onNameChange, label = "Full Name")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingTextField(
                    value = weight, onValueChange = onWeightChange, 
                    label = "Weight (kg)", modifier = Modifier.weight(1f), 
                    keyboardType = KeyboardType.Decimal
                )
                OnboardingTextField(
                    value = height, onValueChange = onHeightChange, 
                    label = "Height (cm)", modifier = Modifier.weight(1f), 
                    keyboardType = KeyboardType.Number
                )
            }
            OnboardingTextField(
                value = goalWeight, onValueChange = onGoalWeightChange, 
                label = "Goal Weight (kg)", keyboardType = KeyboardType.Decimal
            )
        }
    }
    
    OnboardingButton(text = "CONTINUE", enabled = canMove, onClick = onNext)
}

@Composable
private fun ActivityStep(
    selected: String,
    onSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingCard(title = "Your Activity Level") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            mActivityLevels.forEach { level ->
                ActivityOption(
                    title = level.title,
                    description = level.description,
                    isSelected = selected == level.title,
                    onClick = { onSelected(level.title) }
                )
            }
        }
    }
    OnboardingButton(text = "CONTINUE", onClick = onNext)
}

@Composable
private fun JourneyStep(
    workoutDays: Int, onDaysChange: (Int) -> Unit,
    goal: String, onGoalChange: (String) -> Unit,
    water: String, onWaterChange: (String) -> Unit,
    canMove: Boolean,
    onNext: () -> Unit
) {
    OnboardingCard(title = "Your Weekly Goal") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Workout days per week", color = Color.White, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..7).forEach { day ->
                    DayChip(
                        day = day,
                        isSelected = workoutDays == day,
                        onClick = { onDaysChange(day) }
                    )
                }
            }
            
            Text("Journey Goal", color = Color.White, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mJourneyOptions.forEach { option ->
                    GoalOption(
                        title = option,
                        isSelected = goal == option,
                        onClick = { onGoalChange(option) }
                    )
                }
            }
            
            OnboardingTextField(
                value = water, onValueChange = onWaterChange,
                label = "Daily Water Goal (ml)", keyboardType = KeyboardType.Number
            )
        }
    }
    OnboardingButton(text = "CONTINUE", enabled = canMove, onClick = onNext)
}

@Composable
private fun SummaryStep(
    name: String, weight: String, goalWeight: String, height: String, activity: String, workouts: Int, goal: String, water: String,
    onFinish: () -> Unit
) {
    val weightVal = weight.toFloatOrNull() ?: 0f
    val activityLevel = mActivityLevels.find { it.title == activity }
    val maintenance = (weightVal * (activityLevel?.multiplier ?: 30f)).toInt()
    val recommended = when (goal) {
        "Lose weight" -> maintenance - 500
        "Gain weight" -> maintenance + 300
        else -> maintenance
    }

    OnboardingCard(title = "Your Plan is Ready!") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Great work, $name! Based on your profile, here's your starter plan:",
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Daily Intake", "$recommended kcal", Color(0xFF00E5FF))
                    SummaryRow("Maintenance", "$maintenance kcal", Color.White.copy(alpha = 0.6f))
                    SummaryRow("Water Goal", "$water ml", Color(0xFF2196F3))
                    SummaryRow("Workouts", "$workouts days/week", Color(0xFF4CAF50))
                }
            }
            
            Text(
                text = "You can adjust these goals later in your profile settings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
    
    OnboardingButton(text = "START MY JOURNEY", onClick = onFinish)
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.6f))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActivityOption(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = if (isSelected) Color(0xFF00E5FF) else Color.White, fontWeight = FontWeight.Bold)
            Text(description, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GoalOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = if (isSelected) Color(0xFF00E5FF) else Color.White)
            if (isSelected) Icon(Icons.Default.Check, null, tint = Color(0xFF00E5FF))
        }
    }
}

@Composable
private fun DayChip(day: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.6f),
        shape = CircleShape,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(day.toString(), color = if (isSelected) Color.Black else Color.White)
        }
    }
}

@Composable
private fun OnboardingCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
            focusedIndicatorColor = Color(0xFF00E5FF),
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f),
            cursorColor = Color(0xFF00E5FF)
        )
    )
}

@Composable
private fun OnboardingButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.1f),
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
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
