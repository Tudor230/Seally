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
import com.example.seally.goals.GoalsViewModel
import com.example.seally.profile.ProfileViewModel
import com.example.seally.profile.UserProfile
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.LinearProgressBar

private enum class OnboardingStep {
    WELCOME,
    PROFILE,
    ACTIVITY,
    DEMOGRAPHICS,
    SUMMARY,
}

private data class ActivityLevel(
    val title: String,
    val description: String,
    val multiplier: Float
)

private val mActivityLevels = listOf(
    ActivityLevel("Sedentary", "Little to no exercise, desk job", 1.2f),
    ActivityLevel("Lightly Active", "Light exercise 1-3 days/week", 1.375f),
    ActivityLevel("Moderately Active", "Moderate exercise 3-5 days/week", 1.55f),
    ActivityLevel("Very Active", "Hard exercise 6-7 days/week", 1.725f),
    ActivityLevel("Extra Active", "Very hard exercise, physical job", 1.9f)
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit = {},
) {
    val mContext = LocalContext.current
    val mViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(mContext))
    val mGoalsViewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory)
    val mProfileState by mViewModel.profile.collectAsState()

    var mStep by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var mName by rememberSaveable { mutableStateOf("") }
    var mWeightKg by rememberSaveable { mutableStateOf("") }
    var mHeightCm by rememberSaveable { mutableStateOf("") }
    var mDesiredWeightKg by rememberSaveable { mutableStateOf("") }
    var mActivityType by rememberSaveable { mutableStateOf(mActivityLevels[1].title) }
    var mWorkoutDays by rememberSaveable { mutableIntStateOf(3) }
    var mAge by rememberSaveable { mutableStateOf("") }
    var mGender by rememberSaveable { mutableStateOf("") }
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
            if (p.age != null) mAge = p.age.toString()
            if (p.gender.isNotBlank()) mGender = p.gender
            mWaterTargetMl = p.waterTargetMl?.toString() ?: "2500"
        }
    }

    val mWeightValue = mWeightKg.toFloatOrNull()
    val mHeightValue = mHeightCm.toIntOrNull()
    val mDesiredWeightValue = mDesiredWeightKg.toFloatOrNull()
    val mAgeValue = mAge.toIntOrNull()
    val mWaterTargetValue = mWaterTargetMl.toIntOrNull()

    val mNameError = mName.isBlank()
    val mWeightError = mWeightKg.isNotBlank() &&
        (mWeightValue == null || mWeightValue !in 20f..400f)
    val mHeightError = mHeightCm.isNotBlank() &&
        (mHeightValue == null || mHeightValue !in 50..260)
    val mDesiredWeightError = mDesiredWeightKg.isNotBlank() && 
        (mDesiredWeightValue == null || mDesiredWeightValue !in 20f..400f)
    val mAgeError = mAge.isNotBlank() && (mAgeValue == null || mAgeValue !in 10..120)
    val mGenderError = mGender.isBlank()

    val mCanMoveFromProfile = !mNameError &&
            !mWeightError && mWeightValue != null &&
            !mHeightError && mHeightValue != null &&
            mDesiredWeightValue != null && mDesiredWeightValue in 20f..400f &&
            !mDesiredWeightError

    val mProgress = when (mStep) {
        OnboardingStep.WELCOME -> 0.1f
        OnboardingStep.PROFILE -> 0.3f
        OnboardingStep.ACTIVITY -> 0.5f
        OnboardingStep.DEMOGRAPHICS -> 0.7f
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
                                filledColor = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                height = 8.dp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                mStep = when (mStep) {
                                    OnboardingStep.PROFILE -> OnboardingStep.WELCOME
                                    OnboardingStep.ACTIVITY -> OnboardingStep.PROFILE
                                    OnboardingStep.DEMOGRAPHICS -> OnboardingStep.ACTIVITY
                                    OnboardingStep.SUMMARY -> OnboardingStep.DEMOGRAPHICS
                                    else -> mStep
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
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
                            mCanMoveFromProfile,
                            mNameError,
                            mWeightError,
                            mHeightError,
                            mDesiredWeightError
                        ) { mStep = OnboardingStep.ACTIVITY }

                        OnboardingStep.ACTIVITY -> ActivityStep(
                            mActivityType, { mActivityType = it }
                        ) { mStep = OnboardingStep.DEMOGRAPHICS }

                        OnboardingStep.DEMOGRAPHICS -> DemographicsStep(
                            mAge, { mAge = it.filterNumeric(3) },
                            mGender, { mGender = it },
                            !mAgeError && mAgeValue != null && !mGenderError,
                            mAgeError = mAgeError,
                            mGenderError = mGenderError,
                        ) { mStep = OnboardingStep.SUMMARY }

                        OnboardingStep.SUMMARY -> SummaryStep(
                            mName, mWeightKg, mDesiredWeightKg, mHeightCm, mActivityType, mWorkoutDays, mAge, mGender, mWaterTargetMl,
                            onFinish = {
                                val weightVal = mWeightValue ?: 0f
                                val heightVal = (mHeightValue ?: 0).toFloat()
                                val ageVal = mAgeValue ?: 30
                                val activityLevel = mActivityLevels.find { it.title == mActivityType }

                                val bmr = if (mGender == "male") {
                                    (10 * weightVal) + (6.25 * heightVal) - (5 * ageVal) + 5
                                } else {
                                    (10 * weightVal) + (6.25 * heightVal) - (5 * ageVal) - 161
                                }
                                val tdee = (bmr * (activityLevel?.multiplier ?: 1.2f)).toInt()
                                
                                val goalWeightVal = mDesiredWeightValue ?: weightVal
                                val caloriesTarget = when {
                                    weightVal > goalWeightVal -> tdee - 500
                                    weightVal < goalWeightVal -> tdee + 300
                                    else -> tdee
                                }

                                mViewModel.save(
                                    UserProfile(
                                        name = mName.trim(),
                                        heightCm = mHeightValue,
                                        weightKg = mWeightValue,
                                        goalWeightKg = mDesiredWeightValue,
                                        age = mAgeValue,
                                        gender = mGender,
                                        activityType = mActivityType,
                                        workoutDaysPerWeek = mWorkoutDays,
                                        waterTargetMl = mWaterTargetValue,
                                        onboardingCompleted = true,
                                    ),
                                )

                                mGoalsViewModel.createOnboardingGoals(
                                    workoutDaysPerWeek = mWorkoutDays,
                                    waterTargetMl = mWaterTargetValue ?: 2500,
                                    caloriesTarget = caloriesTarget,
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
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🦭", fontSize = 60.sp)
            }
        }
        
        Text(
            text = "Welcome to Seally",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Your personalized AI fitness companion. Let's get you set up for success.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
    nameError: Boolean = false,
    weightError: Boolean = false,
    heightError: Boolean = false,
    goalWeightError: Boolean = false,
    onNext: () -> Unit
) {
    OnboardingCard(title = "Tell us about yourself") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OnboardingTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Full Name",
                isError = nameError,
                errorMessage = "Name is required",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingTextField(
                    value = weight, onValueChange = onWeightChange,
                    label = "Weight (kg)", modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                    isError = weightError,
                    errorMessage = "20–400 kg",
                )
                OnboardingTextField(
                    value = height, onValueChange = onHeightChange,
                    label = "Height (cm)", modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    isError = heightError,
                    errorMessage = "50–260 cm",
                )
            }
            OnboardingTextField(
                value = goalWeight, onValueChange = onGoalWeightChange,
                label = "Goal Weight (kg)", keyboardType = KeyboardType.Decimal,
                isError = goalWeightError,
                errorMessage = "20–400 kg",
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
private fun DemographicsStep(
    age: String, onAgeChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    canMove: Boolean,
    mAgeError: Boolean = false,
    mGenderError: Boolean = false,
    onNext: () -> Unit
) {
    OnboardingCard(title = "About You") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OnboardingTextField(
                        value = age, onValueChange = onAgeChange,
                        label = "Age (years)",
                        keyboardType = KeyboardType.Number,
                        isError = mAgeError,
                        errorMessage = "10–120 years",
                    )
                }
            }

            Text("Gender", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GenderChip(
                    text = "Male",
                    isSelected = gender == "male",
                    onClick = { onGenderChange("male") }
                )
                GenderChip(
                    text = "Female",
                    isSelected = gender == "female",
                    onClick = { onGenderChange("female") }
                )
            }
            if (mGenderError) {
                Text(
                    text = "Please select a gender",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    OnboardingButton(text = "CONTINUE", enabled = canMove, onClick = onNext)
}

@Composable
private fun SummaryStep(
    name: String, weightStr: String, goalWeight: String, heightStr: String, activity: String, workouts: Int, age: String, gender: String, water: String,
    onFinish: () -> Unit
) {
    val weightVal = weightStr.toFloatOrNull() ?: 0f
    val heightVal = heightStr.toFloatOrNull() ?: 0f
    val ageVal = age.toIntOrNull() ?: 30
    val activityLevel = mActivityLevels.find { it.title == activity }

    val bmr = if (gender == "male") {
        (10 * weightVal) + (6.25 * heightVal) - (5 * ageVal) + 5
    } else {
        (10 * weightVal) + (6.25 * heightVal) - (5 * ageVal) - 161
    }
    val tdee = (bmr * (activityLevel?.multiplier ?: 1.2f)).toInt()
    val goalWeightVal = goalWeight.toFloatOrNull() ?: weightVal
    val recommended = when {
        weightVal > goalWeightVal -> tdee - 500
        weightVal < goalWeightVal -> tdee + 300
        else -> tdee
    }

    OnboardingCard(title = "Your Plan is Ready!") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Great work, $name! Based on your profile, here's your starter plan:",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Daily Intake", "$recommended kcal", MaterialTheme.colorScheme.primary)
                    SummaryRow("Maintenance", "$tdee kcal", MaterialTheme.colorScheme.onSurfaceVariant)
                    SummaryRow("Water Goal", "$water ml", MaterialTheme.colorScheme.tertiary)
                    SummaryRow("Workouts", "$workouts days/week", MaterialTheme.colorScheme.secondary)
                }
            }
            
            Text(
                text = "You can adjust these goals later in your profile settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
    
    OnboardingButton(text = "START MY JOURNEY", onClick = onFinish)
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActivityOption(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DayChip(day: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RowScope.GenderChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val chipModifier = Modifier.weight(1f)
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = chipModifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(text, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
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
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        supportingText = if (isError && !errorMessage.isNullOrBlank()) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedIndicatorColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorIndicatorColor = MaterialTheme.colorScheme.error,
            errorTextColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            errorSupportingTextColor = MaterialTheme.colorScheme.error
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
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
