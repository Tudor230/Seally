package com.example.seally.profile

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val mActivityLevels = listOf(
    "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
    val profile by vm.profile.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    // Settings State
    var activityType by remember { mutableStateOf("") }
    var workoutDays by remember { mutableIntStateOf(3) }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var waterTargetMlText by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        profile?.let { p ->
            activityType = p.activityType
            workoutDays = p.workoutDaysPerWeek ?: 3
            ageText = p.age?.toString().orEmpty()
            gender = p.gender
            waterTargetMlText = p.waterTargetMl?.toString().orEmpty()
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val cardContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val ageValue = ageText.toIntOrNull()
    val waterTargetValue = waterTargetMlText.toIntOrNull()
    val ageError = ageText.isNotBlank() && (ageValue == null || ageValue !in 10..120)
    val waterTargetError = waterTargetMlText.isNotBlank() && (waterTargetValue == null || waterTargetValue !in 250..10000)
    val canApplySettings = !ageError && !waterTargetError

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Account?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete your profile and all progress. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clear()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("RESET EVERYTHING", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("App Settings", fontWeight = FontWeight.Black, color = onSurfaceColor)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onSurfaceColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = onSurfaceColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Activity Level Section
            SettingsSection(title = "Activity Level") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mActivityLevels.forEach { level ->
                        SelectableOption(
                            title = level,
                            isSelected = activityType == level,
                            onClick = { activityType = level }
                        )
                    }
                }
            }

            // Age & Gender Section
            SettingsSection(title = "Age") {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ageError,
                    supportingText = {
                        if (ageError) {
                            Text("Age must be between 10 and 120.")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = outline,
                        focusedContainerColor = cardContainer,
                        unfocusedContainerColor = cardContainer,
                        focusedTextColor = onSurfaceColor,
                        unfocusedTextColor = onSurfaceColor
                    )
                )
            }

            SettingsSection(title = "Gender") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GenderButton(
                        text = "Male",
                        isSelected = gender == "male",
                        onClick = { gender = "male" }
                    )
                    GenderButton(
                        text = "Female",
                        isSelected = gender == "female",
                        onClick = { gender = "female" }
                    )
                }
            }

            // Workout Days Section
            SettingsSection(title = "Workout Days") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..7).forEach { day ->
                        DayChip(
                            day = day,
                            isSelected = workoutDays == day,
                            onClick = { workoutDays = day }
                        )
                    }
                }
            }

            // Water Target Section
            SettingsSection(title = "Daily Water Goal (ml)") {
                OutlinedTextField(
                    value = waterTargetMlText,
                    onValueChange = { waterTargetMlText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = waterTargetError,
                    supportingText = {
                        if (waterTargetError) {
                            Text("Water target must be between 250 and 10000 ml.")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = outline,
                        focusedContainerColor = cardContainer,
                        unfocusedContainerColor = cardContainer,
                        focusedTextColor = onSurfaceColor,
                        unfocusedTextColor = onSurfaceColor
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            // Save Button
            Button(
                onClick = {
                    profile?.let { p ->
                        vm.save(
                            p.copy(
                                activityType = activityType,
                                workoutDaysPerWeek = workoutDays,
                                age = ageValue,
                                gender = gender,
                                waterTargetMl = waterTargetValue ?: p.waterTargetMl
                            )
                        )
                    }
                    onBackClick()
                },
                enabled = canApplySettings,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("APPLY SETTINGS", fontWeight = FontWeight.Bold)
            }

            // Danger Zone
            SettingsSection(title = "Danger Zone") {
                Surface(
                    onClick = { showResetDialog = true },
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("RESET ACCOUNT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun SelectableOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DayChip(day: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = CircleShape,
        modifier = Modifier.size(42.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RowScope.GenderButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val buttonModifier = Modifier.weight(1f)
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = buttonModifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(text, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
    }
}
